// ==============================================================
// EXPRESS API ROUTER FOR ADMINS & AREAS CRUD OPERATIONS
// ==============================================================

const express = require('express');

/**
 * Creates the express router configured for CRUDing admins and areas on tenant databases.
 * @param {Function} getTenantConnectionPool - Helper to resolve dynamic mysql pools
 * @returns {Router} Express Router
 */
function createAdminsAreasRouter(getTenantConnectionPool) {
    const router = express.Router();

    // ----------------------------------------------------------
    // 1. AREA (CABANG TOKO) ENDPOINTS
    // ----------------------------------------------------------

    // Fetch all areas / branches
    router.get('/areas', async (req, res) => {
        const { db_name } = req.query;
        if (!db_name) {
            return res.status(400).json({ error: 'Missing query parameter: db_name' });
        }
        try {
            const pool = getTenantConnectionPool(db_name);
            const [rows] = await pool.query('SELECT * FROM areas ORDER BY name ASC');
            res.json(rows.map(r => ({
                id: r.id.toString(),
                name: r.name
            })));
        } catch (err) {
            res.status(500).json({ error: 'Failed to fetch areas from VPS', details: err.message });
        }
    });

    // Create a new area
    router.post('/areas', async (req, res) => {
        const { db_name, name } = req.body;
        if (!db_name || !name) {
            return res.status(400).json({ error: 'Missing parameter: db_name or name' });
        }
        try {
            const pool = getTenantConnectionPool(db_name);
            const [result] = await pool.query('INSERT INTO areas (name) VALUES (?)', [name.trim()]);
            res.status(201).json({
                success: true,
                id: result.insertId.toString(),
                message: 'Area successfully created'
            });
        } catch (err) {
            res.status(500).json({ error: 'Failed to add area', details: err.message });
        }
    });

    // Update an existing area
    router.put('/areas/:id', async (req, res) => {
        const areaId = req.params.id;
        const { db_name, name } = req.body;
        if (!db_name || !name) {
            return res.status(400).json({ error: 'Missing parameter: db_name or name' });
        }
        try {
            const pool = getTenantConnectionPool(db_name);
            await pool.query('UPDATE areas SET name = ? WHERE id = ?', [name.trim(), areaId]);
            res.json({ success: true, message: 'Area updated successfully' });
        } catch (err) {
            res.status(500).json({ error: 'Failed to update area', details: err.message });
        }
    });

    // Delete an area
    router.delete('/areas/:id', async (req, res) => {
        const areaId = req.params.id;
        const { db_name } = req.query;
        if (!db_name) {
            return res.status(400).json({ error: 'Missing parameter: db_name' });
        }
        try {
            const pool = getTenantConnectionPool(db_name);
            await pool.query('DELETE FROM areas WHERE id = ?', [areaId]);
            res.json({ success: true, message: 'Area deleted successfully' });
        } catch (err) {
            res.status(500).json({ error: 'Failed to delete area', details: err.message });
        }
    });

    // ----------------------------------------------------------
    // 2. ADMINS (MULTI-ROLE USERS) ENDPOINTS
    // ----------------------------------------------------------

    // Fetch all admins
    router.get('/admins', async (req, res) => {
        const { db_name } = req.query;
        if (!db_name) {
            return res.status(400).json({ error: 'Missing query parameter: db_name' });
        }
        try {
            const pool = getTenantConnectionPool(db_name);
            const [rows] = await pool.query('SELECT * FROM admins ORDER BY name ASC');
            res.json(rows.map(r => ({
                id: r.id.toString(),
                name: r.name,
                username: r.username,
                password: r.password,
                role: r.role,
                area: r.area
            })));
        } catch (err) {
            res.status(500).json({ error: 'Failed to fetch admins from VPS', details: err.message });
        }
    });

    // Create a new admin user
    router.post('/admins', async (req, res) => {
        const { db_name, name, username, password, role, area } = req.body;
        if (!db_name || !name || !username || !password) {
            return res.status(400).json({ error: 'Missing required admin fields: db_name, name, username, password' });
        }
        try {
            const pool = getTenantConnectionPool(db_name);
            const [result] = await pool.query(
                'INSERT INTO admins (name, username, password, role, area) VALUES (?, ?, ?, ?, ?)',
                [name.trim(), username.trim(), password, role || 'Superadmin', area || 'Semua Cabang']
            );
            res.status(201).json({
                success: true,
                id: result.insertId.toString(),
                message: 'Admin successfully created'
            });
        } catch (err) {
            res.status(500).json({ error: 'Failed to add admin user', details: err.message });
        }
    });

    // Update an existing admin user
    router.put('/admins/:id', async (req, res) => {
        const adminId = req.params.id;
        const { db_name, name, username, password, role, area } = req.body;
        if (!db_name || !name || !username || !password) {
            return res.status(400).json({ error: 'Missing required admin fields' });
        }
        try {
            const pool = getTenantConnectionPool(db_name);
            await pool.query(
                'UPDATE admins SET name = ?, username = ?, password = ?, role = ?, area = ? WHERE id = ?',
                [name.trim(), username.trim(), password, role || 'Kasir', area || 'Semua Cabang', adminId]
            );
            res.json({ success: true, message: 'Admin user updated successfully' });
        } catch (err) {
            res.status(500).json({ error: 'Failed to update admin user', details: err.message });
        }
    });

    // Delete an admin user
    router.delete('/admins/:id', async (req, res) => {
        const adminId = req.params.id;
        const { db_name } = req.query;
        if (!db_name) {
            return res.status(400).json({ error: 'Missing parameter: db_name' });
        }
        try {
            const pool = getTenantConnectionPool(db_name);
            await pool.query('DELETE FROM admins WHERE id = ?', [adminId]);
            res.json({ success: true, message: 'Admin user deleted successfully' });
        } catch (err) {
            res.status(500).json({ error: 'Failed to delete admin user', details: err.message });
        }
    });

    return router;
}

module.exports = createAdminsAreasRouter;
