// ASTOCK VPS Multi-Tenant Backend Server
// Developed for Node.js + Express + MySQL

const express = require('express');
const mysql = require('mysql2/promise');
const cors = require('cors');
require('dotenv').config();
const createAdminsAreasRouter = require('./admins_areas_api');

const app = express();
const PORT = process.env.PORT || 3900;

// Enable CORS so the Android Client can connect securely from the internet
app.use(cors());
app.use(express.json());

// Log incoming API requests for auditing
app.use((req, res, next) => {
    console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
    next();
});

// Master Database Pool Configuration
const masterPoolConfig = {
    host: process.env.DB_HOST || '103.253.245.25',
    port: parseInt(process.env.DB_PORT || '3306'),
    user: process.env.DB_USER || 'konter',
    password: process.env.DB_PASSWORD || '08Delapan',
    database: process.env.DB_MASTER_NAME || 'konter_master',
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
};

console.log('[ASTOCK] Connecting to master database at:', masterPoolConfig.host);
const masterPool = mysql.createPool(masterPoolConfig);

// Keep track of active connection pools per tenant (In-Memory Pool Caching)
// This avoids reconnecting or creating redundant pools for each request
const tenantPools = new Map();

/**
 * Gets or dynamically creates a connection pool for a specific tenant database.
 * @param {string} dbName - The database name of the tenant
 * @returns {Pool} mysql2 Connection Pool
 */
function getTenantConnectionPool(dbName) {
    if (tenantPools.has(dbName)) {
        return tenantPools.get(dbName);
    }

    console.log(`[VPS] Dynamic connection pool initialized for tenant database: "${dbName}"`);
    
    const poolConfig = {
        host: masterPoolConfig.host,
        port: masterPoolConfig.port,
        user: masterPoolConfig.user,
        password: masterPoolConfig.password,
        database: dbName,
        waitForConnections: true,
        connectionLimit: 10,
        queueLimit: 0
    };

    const pool = mysql.createPool(poolConfig);
    tenantPools.set(dbName, pool);
    return pool;
}

// ==================== API ENDPOINTS ====================

// Health Check Endpoint
app.get('/api/health', (req, res) => {
    res.json({
        status: 'UP',
        timestamp: new Date(),
        vps_port: PORT,
        active_tenant_pools: Array.from(tenantPools.keys())
    });
});

// 1. Authenticate Tenant & Route Database (MASTER LOOKUP)
app.post('/api/auth/login', async (req, res) => {
    const { username, password } = req.body;

    if (!username || !password) {
        return res.status(400).json({ error: 'Username and Password are required!' });
    }

    try {
        // Query master database to locate the tenant credentials
        const [rows] = await masterPool.query(
            'SELECT * FROM tenants WHERE username = ? AND password = ?',
            [username, password]
        );

        if (rows.length === 0) {
            return res.status(401).json({ error: 'Incorrect credentials or user does not exist!' });
        }

        const tenant = rows[0];
        
        // Dynamically instantiate the connection pool for this tenant database to test compatibility
        const tenantPool = getTenantConnectionPool(tenant.db_name);
        
        // Quick verification query on the tenant's database
        await tenantPool.query('SELECT 1');

        console.log(`[VPS] Auth Success: "${tenant.tenant_name}" mapped to database: "${tenant.db_name}"`);

        // Send back verified tenant metadata
        res.json({
            success: true,
            tenant: {
                id: tenant.id.toString(),
                name: tenant.tenant_name,
                ownerName: tenant.username, // mapping owner to master username
                dbName: tenant.db_name,
                accentColor: tenant.accent_color,
                businessType: tenant.business_type
            }
        });

    } catch (err) {
        console.error('[AUTH ERROR]', err);
        res.status(500).json({ 
            error: 'Database connection failed. Please ensure your tenant SQL schemas are fully imported!', 
            details: err.message 
        });
    }
});

// 2. Fetch Products for Active Tenant
app.get('/api/products', async (req, res) => {
    const { db_name } = req.query;

    if (!db_name) {
        return res.status(400).json({ error: 'Missing parameter: db_name' });
    }

    try {
        const pool = getTenantConnectionPool(db_name);
        const [products] = await pool.query('SELECT * FROM products ORDER BY name ASC');
        
        // Transform standard DB rows to camelCase fields matching the Kotlin Models
        const formattedProducts = products.map(p => ({
            id: p.id.toString(),
            name: p.name,
            sku: p.sku,
            category: p.category,
            price: parseFloat(p.price),
            stock: p.stock,
            minStockAlert: p.min_stock_alert
        }));

        res.json(formattedProducts);
    } catch (err) {
        res.status(500).json({ error: 'Failed to fetch products', details: err.message });
    }
});

// 3. Add New Product to Tenant Database
app.post('/api/products', async (req, res) => {
    const { db_name, name, sku, category, price, stock, min_stock_alert } = req.body;

    if (!db_name || !name || !sku || !price || stock === undefined) {
        return res.status(400).json({ error: 'Missing required product information!' });
    }

    try {
        const pool = getTenantConnectionPool(db_name);
        
        const [result] = await pool.query(
            'INSERT INTO products (name, sku, category, price, stock, min_stock_alert) VALUES (?, ?, ?, ?, ?, ?)',
            [name, sku, category || 'Umum', price, stock, min_stock_alert || 5]
        );

        res.status(201).json({
            success: true,
            productId: result.insertId,
            message: 'Product successfully added'
        });
    } catch (err) {
        res.status(500).json({ error: 'Failed to add product', details: err.message });
    }
});

// 4. Update Product Stock / Adjustments
app.put('/api/products/:id/stock', async (req, res) => {
    const productId = req.params.id;
    const { db_name, stock } = req.body;

    if (!db_name || stock === undefined) {
        return res.status(400).json({ error: 'Missing parameters: db_name or stock' });
    }

    try {
        const pool = getTenantConnectionPool(db_name);
        
        await pool.query(
            'UPDATE products SET stock = ? WHERE id = ?',
            [stock, productId]
        );

        res.json({ success: true, message: 'Stock updated successfully' });
    } catch (err) {
        res.status(500).json({ error: 'Failed to update stock', details: err.message });
    }
});

// 5. Delete Product
app.delete('/api/products/:id', async (req, res) => {
    const productId = req.params.id;
    const { db_name } = req.query;

    if (!db_name) {
        return res.status(400).json({ error: 'Missing parameter: db_name' });
    }

    try {
        const pool = getTenantConnectionPool(db_name);
        
        await pool.query('DELETE FROM products WHERE id = ?', [productId]);
        res.json({ success: true, message: 'Product deleted successfully' });
    } catch (err) {
        res.status(500).json({ error: 'Failed to delete product', details: err.message });
    }
});

// 6. Fetch Sales Transactions
app.get('/api/transactions', async (req, res) => {
    const { db_name } = req.query;

    if (!db_name) {
        return res.status(400).json({ error: 'Missing parameter: db_name' });
    }

    try {
        const pool = getTenantConnectionPool(db_name);
        const [transactions] = await pool.query('SELECT * FROM transactions ORDER BY timestamp DESC');

        const formattedTxs = transactions.map(t => ({
            id: t.id,
            productName: t.product_name,
            sku: t.sku,
            quantity: t.quantity,
            totalPrice: parseFloat(t.total_price),
            timestamp: t.timestamp,
            operator: t.operator
        }));

        res.json(formattedTxs);
    } catch (err) {
        res.status(500).json({ error: 'Failed to fetch transactions', details: err.message });
    }
});

// 7. Add New Transaction (POS checkout logic)
app.post('/api/transactions', async (req, res) => {
    const { db_name, id, productName, sku, quantity, totalPrice, timestamp, operator } = req.body;

    if (!db_name || !id || !productName || !sku || !quantity || !totalPrice) {
        return res.status(400).json({ error: 'Missing required transaction fields' });
    }

    try {
        const pool = getTenantConnectionPool(db_name);

        // Record transaction details inside SQL
        await pool.query(
            'INSERT INTO transactions (id, product_name, sku, quantity, total_price, timestamp, operator) VALUES (?, ?, ?, ?, ?, ?, ?)',
            [id, productName, sku, quantity, totalPrice, timestamp, operator || 'Kasir']
        );

        res.status(201).json({ success: true, message: 'Transaction saved' });
    } catch (err) {
        res.status(500).json({ error: 'Failed to save transaction', details: err.message });
    }
});

// 8. Fetch & Save Modal Awal / Config Pembukuan
app.get('/api/pembukuan/modal_awal', async (req, res) => {
    const { db_name } = req.query;
    if (!db_name) return res.status(400).json({ error: 'Missing parameter: db_name' });
    try {
        const pool = getTenantConnectionPool(db_name);
        const [rows] = await pool.query('SELECT modal_awal, online_income FROM modal_awal WHERE id = 1');
        if (rows.length > 0) {
            res.json({ modalAwal: parseFloat(rows[0].modal_awal), onlineIncome: parseFloat(rows[0].online_income) });
        } else {
            res.json({ modalAwal: 0, onlineIncome: 0 });
        }
    } catch (err) {
        res.status(500).json({ error: 'Failed to fetch modal_awal', details: err.message });
    }
});

app.post('/api/pembukuan/modal_awal', async (req, res) => {
    const { db_name, modalAwal, onlineIncome } = req.body;
    if (!db_name) return res.status(400).json({ error: 'Missing parameter: db_name' });
    try {
        const pool = getTenantConnectionPool(db_name);
        await pool.query(
            'INSERT INTO modal_awal (id, modal_awal, online_income) VALUES (1, ?, ?) ON DUPLICATE KEY UPDATE modal_awal = VALUES(modal_awal), online_income = VALUES(online_income)',
            [modalAwal || 0, onlineIncome || 0]
        );
        res.json({ success: true, message: 'Modal awal saved' });
    } catch (err) {
        res.status(500).json({ error: 'Failed to save modal_awal', details: err.message });
    }
});

// 9. Fetch Incomes (Pemasukkan Lain-Lain)
app.get('/api/pembukuan/incomes', async (req, res) => {
    const { db_name } = req.query;
    if (!db_name) return res.status(400).json({ error: 'Missing parameter: db_name' });
    try {
        const pool = getTenantConnectionPool(db_name);
        const [rows] = await pool.query('SELECT * FROM incomes ORDER BY timestamp DESC');
        const formatted = rows.map(r => ({
            id: r.id,
            amount: parseFloat(r.amount),
            description: r.description,
            timestamp: r.timestamp
        }));
        res.json(formatted);
    } catch (err) {
        res.status(500).json({ error: 'Failed to fetch incomes', details: err.message });
    }
});

app.post('/api/pembukuan/incomes', async (req, res) => {
    const { db_name, id, amount, description, timestamp } = req.body;
    if (!db_name || !amount) return res.status(400).json({ error: 'Missing required income parameters' });
    try {
        const pool = getTenantConnectionPool(db_name);
        const entryId = id || Date.now().toString();
        await pool.query(
            'INSERT INTO incomes (id, amount, description, timestamp) VALUES (?, ?, ?, ?)',
            [entryId, amount, description || 'Pemasukkan Lain-Lain', timestamp || new Date().toISOString()]
        );
        res.status(201).json({ success: true, id: entryId, message: 'Income entry saved' });
    } catch (err) {
        res.status(500).json({ error: 'Failed to save income', details: err.message });
    }
});

app.delete('/api/pembukuan/incomes/:id', async (req, res) => {
    const incomeId = req.params.id;
    const { db_name } = req.query;
    if (!db_name) return res.status(400).json({ error: 'Missing parameter: db_name' });
    try {
        const pool = getTenantConnectionPool(db_name);
        await pool.query('DELETE FROM incomes WHERE id = ?', [incomeId]);
        res.json({ success: true, message: 'Income entry deleted' });
    } catch (err) {
        res.status(500).json({ error: 'Failed to delete income', details: err.message });
    }
});

// 10. Fetch Expenses (Pengeluaran - Belanja Stok, Gaji Karyawan, Operasional, BON)
app.get('/api/pembukuan/expenses', async (req, res) => {
    const { db_name } = req.query;
    if (!db_name) return res.status(400).json({ error: 'Missing parameter: db_name' });
    try {
        const pool = getTenantConnectionPool(db_name);
        const [rows] = await pool.query('SELECT * FROM expenses ORDER BY timestamp DESC');
        const formatted = rows.map(r => ({
            id: r.id,
            category: r.category,
            amount: parseFloat(r.amount),
            description: r.description,
            timestamp: r.timestamp
        }));
        res.json(formatted);
    } catch (err) {
        res.status(500).json({ error: 'Failed to fetch expenses', details: err.message });
    }
});

app.post('/api/pembukuan/expenses', async (req, res) => {
    const { db_name, id, category, amount, description, timestamp } = req.body;
    if (!db_name || !amount) return res.status(400).json({ error: 'Missing required expense parameters' });
    try {
        const pool = getTenantConnectionPool(db_name);
        const entryId = id || Date.now().toString();
        await pool.query(
            'INSERT INTO expenses (id, category, amount, description, timestamp) VALUES (?, ?, ?, ?, ?)',
            [entryId, category || 'Belanja stok', amount, description || '', timestamp || new Date().toISOString()]
        );
        res.status(201).json({ success: true, id: entryId, message: 'Expense entry saved' });
    } catch (err) {
        res.status(500).json({ error: 'Failed to save expense', details: err.message });
    }
});

app.delete('/api/pembukuan/expenses/:id', async (req, res) => {
    const expenseId = req.params.id;
    const { db_name } = req.query;
    if (!db_name) return res.status(400).json({ error: 'Missing parameter: db_name' });
    try {
        const pool = getTenantConnectionPool(db_name);
        await pool.query('DELETE FROM expenses WHERE id = ?', [expenseId]);
        res.json({ success: true, message: 'Expense entry deleted' });
    } catch (err) {
        res.status(500).json({ error: 'Failed to delete expense', details: err.message });
    }
});

// Register Admins and Areas CRUD router
app.use('/api', createAdminsAreasRouter(getTenantConnectionPool));

// Start listening for inbound connections on your VPS Port
app.listen(PORT, '0.0.0.0', () => {
    console.log(`\n======================================================`);
    console.log(` ASTOCK Backend Server is running successfully!`);
    console.log(` - Port    : ${PORT}`);
    console.log(` - URL     : http://103.253.245.25:${PORT}`);
    console.log(`======================================================\n`);
});
