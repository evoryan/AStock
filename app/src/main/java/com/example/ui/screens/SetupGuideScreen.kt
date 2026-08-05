package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupGuideScreen(onBackClick: () -> Unit) {
    var activeTab by remember { mutableStateOf(1) } // 1: SQL Schema, 2: Server JS, 3: Instructions
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val sqlSchemaText = """
-- 1. Buat Database Master
CREATE DATABASE IF NOT EXISTS konter_master;
USE konter_master;

-- Tabel Master Penyimpan Info Tenant & Database Tujuan
CREATE TABLE IF NOT EXISTS tenants (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    tenant_name VARCHAR(100) NOT NULL,
    db_name VARCHAR(100) NOT NULL, -- Nama Database Spesifik Tenant
    accent_color VARCHAR(10) DEFAULT '#F59E0B',
    business_type VARCHAR(50) DEFAULT 'Retail'
);

-- Masukkan Data Contoh Tenant
INSERT INTO tenants (username, password, tenant_name, db_name, accent_color, business_type)
VALUES 
('admin_konterA', 'admin123', 'Buku Jaya Stationer', 'konter_buku_jaya', '#3B82F6', 'Alat Tulis'),
('admin_konterB', 'admin123', 'Gadget Hub Accessories', 'konter_gadget_hub', '#F59E0B', 'Gadget'),
('admin_konterC', 'admin123', 'Aurelia Luxe Boutique', 'konter_fashion_boutique', '#8B5CF6', 'Fashion');


-- 2. Buat Database untuk Tenant A (konter_buku_jaya)
CREATE DATABASE IF NOT EXISTS konter_buku_jaya;
USE konter_buku_jaya;

CREATE TABLE IF NOT EXISTS products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    sku VARCHAR(50) UNIQUE NOT NULL,
    category VARCHAR(50) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    min_stock_alert INT DEFAULT 5
);

CREATE TABLE IF NOT EXISTS transactions (
    id VARCHAR(50) PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL,
    sku VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    operator VARCHAR(100)
);


-- 3. Buat Database untuk Tenant B (konter_gadget_hub)
CREATE DATABASE IF NOT EXISTS konter_gadget_hub;
USE konter_gadget_hub;

-- [Struktur tabel products & transactions sama seperti Tenant A]
CREATE TABLE IF NOT EXISTS products LIKE konter_buku_jaya.products;
CREATE TABLE IF NOT EXISTS transactions LIKE konter_buku_jaya.transactions;
""".trimIndent()

    val serverJsText = """
// server.js - Node.JS Multi-Tenant VPS Backend
const express = require('express');
const mysql = require('mysql2/promise');
const cors = require('cors');

const app = express();
app.use(cors());
app.use(express.json());

// 1. Koneksi awal ke Database Master
const masterConfig = {
    host: 'localhost',
    user: 'root',
    password: 'vps_db_password',
    database: 'konter_master'
};
const masterPool = mysql.createPool(masterConfig);

// 2. Map Cache untuk Connection Pools Tenant
// Mencegah kebocoran memori dengan menggunakan kembali pool yang sudah dibuat
const tenantPools = new Map();

// Fungsi untuk mendapatkan atau membuat pool koneksi ke database tenant secara dinamis
function getTenantConnectionPool(dbName) {
    if (tenantPools.has(dbName)) {
        return tenantPools.get(dbName);
    }
    
    console.log(`[VPS] Membuat pool koneksi baru untuk database: ${'$'}{dbName}`);
    
    // Inisiasi pool koneksi khusus menuju target DB
    const pool = mysql.createPool({
        ...masterConfig,
        database: dbName,
        connectionLimit: 10,
        waitForConnections: true
    });
    
    tenantPools.set(dbName, pool);
    return pool;
}

// 3. Endpoint Login & Deteksi Database Tenant (Alur Kerja Multi-Tenant)
app.post('/api/auth/login', async (req, res) => {
    const { username, password } = req.body;
    
    try {
        // A. Cari user di DB Master (konter_master)
        const [rows] = await masterPool.query(
            'SELECT * FROM tenants WHERE username = ? AND password = ?',
            [username, password]
        );
        
        if (rows.length === 0) {
            return res.status(401).json({ error: 'Kredensial tidak valid' });
        }
        
        const tenant = rows[0];
        
        // B. Ambil db_name yang spesifik untuk user tersebut
        const targetDb = tenant.db_name;
        
        // C. Buat atau ambil pool koneksi ke database tenant
        const tenantPool = getTenantConnectionPool(targetDb);
        
        // Verifikasi koneksi berhasil dengan query ringan
        const [connectionTest] = await tenantPool.query('SELECT 1');
        
        console.log(`[VPS] Tenant ${'$'}{tenant.tenant_name} berhasil terhubung ke ${'$'}{targetDb}`);
        
        // Kirim respon sukses beserta info routing database
        res.json({
            success: true,
            tenant: {
                id: tenant.id,
                name: tenant.tenant_name,
                dbName: tenant.db_name,
                accentColor: tenant.accent_color,
                businessType: tenant.business_type
            },
            message: 'Berhasil mengalokasikan koneksi database tenant'
        });
        
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Kesalahan internal VPS', details: err.message });
    }
});

// 4. Contoh mengambil data dari Database Tenant Dinamis
app.get('/api/products', async (req, res) => {
    // Di dunia nyata, kirim dbName melalui headers JWT Token atau query params
    const { db_name } = req.query;
    
    if (!db_name) {
        return res.status(400).json({ error: 'db_name diperlukan!' });
    }
    
    try {
        const tenantPool = getTenantConnectionPool(db_name);
        const [products] = await tenantPool.query('SELECT * FROM products');
        res.json(products);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Jalankan Server VPS
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`ASTOCK VPS Backend berjalan di port ${'$'}{PORT}`);
});
""".trimIndent()

    val setupInstructions = """
Langkah Cara Deploy di VPS Anda:

1. Persiapan Server VPS Linux (Ubuntu):
   SSH ke VPS Anda dan install Node.js serta MySQL:
   ${"$"} sudo apt update
   ${"$"} sudo apt install nodejs npm mysql-server -y

2. Konfigurasi MySQL Database:
   - Masuk ke MySQL shell:
     ${"$"} sudo mysql
   - Jalankan seluruh perintah SQL yang ada pada Tab 'SQL SCHEMA' di samping untuk menginisiasi database master & database tenant.

3. Inisialisasi Proyek Node.js Backend:
   - Buat direktori baru dan inisialisasi:
     ${"$"} mkdir astock-backend && cd astock-backend
     ${"$"} npm init -y
   - Install library dependencies yang diperlukan:
     ${"$"} npm install express mysql2 cors
   - Buat file 'server.js' dan paste isi kode dari Tab 'SERVER JS' di samping.

4. Menjalankan Server VPS:
   - Agar backend berjalan di background terus menerus, install pm2:
     ${"$"} sudo npm install -g pm2
     ${"$"} pm2 start server.js --name astock-api
     ${"$"} pm2 startup && pm2 save

5. Sambungkan Aplikasi Android ASTOCK:
   - Di halaman login aplikasi ASTOCK ini, aktifkan toggle 'Real VPS API'.
   - Masukkan alamat IP VPS Anda, contoh: http://IP_VPS_ANDA:3000
   - Log in dengan user 'admin_konterA' atau 'admin_konterB'. Sistem akan otomatis merutekan database di VPS Anda secara real-time!
""".trimIndent()

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("ASTOCK Backend Code", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Kode berhasil disalin ke clipboard!", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Panduan VPS Backend",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Elegant Navigation Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E293B))
                    .padding(4.dp)
            ) {
                listOf("SQL Schema" to 1, "Server JS" to 2, "Instruksi" to 3).forEach { (label, index) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (activeTab == index) Color(0xFF0F172A) else Color.Transparent)
                            .clickable { activeTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (activeTab == index) Color(0xFFF59E0B) else Color.Gray,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Card (Copy & Explanation)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x1AF59E0B))
                    .border(1.dp, Color(0x33F59E0B), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Kode Siap Deploy",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Salin kode ini untuk dideploy langsung pada server VPS atau localhost Anda.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
                if (activeTab < 3) {
                    IconButton(
                        onClick = {
                            val codeToCopy = if (activeTab == 1) sqlSchemaText else serverJsText
                            copyToClipboard(codeToCopy)
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color(0xFFF59E0B),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Salin Kode",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Code Display Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF020617))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    val currentText = when (activeTab) {
                        1 -> sqlSchemaText
                        2 -> serverJsText
                        else -> setupInstructions
                    }

                    Text(
                        text = currentText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        ),
                        color = if (activeTab == 3) Color.LightGray else Color(0xFF34D399)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
