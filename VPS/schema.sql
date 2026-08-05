-- ==============================================================
-- CLEAN DATABASE SCHEMA INITIALIZATION FOR ASTOCK VPS MYSQL (NO MOCK DATA)
-- ==============================================================
-- System: ASTOCK Multi-Tenancy POS & Inventory Management System
-- Purpose: Complete clean Database Schema ready for Production Server (VPS)
-- Usage: Import this file into your MySQL/MariaDB database server.

SET FOREIGN_KEY_CHECKS = 0;

-- --------------------------------------------------------------
-- PART 1: MASTER ROUTING DATABASE SETUP (konter_master)
-- Stores tenant metadata, credentials, and database mappings.
-- --------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `konter_master` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `konter_master`;

DROP TABLE IF EXISTS `tenants`;
CREATE TABLE `tenants` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(50) NOT NULL UNIQUE,
  `password` VARCHAR(100) NOT NULL,
  `tenant_name` VARCHAR(100) NOT NULL,
  `db_name` VARCHAR(64) NOT NULL UNIQUE,
  `accent_color` VARCHAR(10) DEFAULT '#F59E0B',
  `business_type` VARCHAR(50) DEFAULT 'Retail',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- --------------------------------------------------------------
-- PART 2: TENANT DATABASE STRUCTURE TEMPLATE
-- The tables below form the schema structure required for each tenant database.
-- (Creation script for 'konter_default' as baseline template)
-- --------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `konter_default` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `konter_default`;

-- 1. Table Products (Stok & Barang)
DROP TABLE IF EXISTS `products`;
CREATE TABLE `products` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(255) NOT NULL,
  `sku` VARCHAR(100) NOT NULL UNIQUE,
  `category` VARCHAR(100) DEFAULT 'Umum',
  `price` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `stock` INT NOT NULL DEFAULT 0,
  `min_stock_alert` INT NOT NULL DEFAULT 5,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Table Transactions (Penjualan / Kasir POS)
DROP TABLE IF EXISTS `transactions`;
CREATE TABLE `transactions` (
  `id` VARCHAR(100) PRIMARY KEY,
  `product_name` VARCHAR(255) NOT NULL,
  `sku` VARCHAR(100) NOT NULL,
  `quantity` INT NOT NULL,
  `total_price` DECIMAL(12,2) NOT NULL,
  `timestamp` VARCHAR(100) NOT NULL,
  `operator` VARCHAR(100) DEFAULT 'Kasir',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Table Modal Awal & Config Pembukuan
DROP TABLE IF EXISTS `modal_awal`;
CREATE TABLE `modal_awal` (
  `id` INT PRIMARY KEY DEFAULT 1,
  `modal_awal` DECIMAL(15,2) NOT NULL DEFAULT 0.00,
  `online_income` DECIMAL(15,2) NOT NULL DEFAULT 0.00,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Initialize default zero row for modal_awal
INSERT INTO `modal_awal` (`id`, `modal_awal`, `online_income`) VALUES (1, 0.00, 0.00)
ON DUPLICATE KEY UPDATE `id` = 1;

-- 4. Table Incomes (Pemasukkan Lain-Lain)
DROP TABLE IF EXISTS `incomes`;
CREATE TABLE `incomes` (
  `id` VARCHAR(100) PRIMARY KEY,
  `amount` DECIMAL(15,2) NOT NULL DEFAULT 0.00,
  `description` TEXT,
  `timestamp` VARCHAR(100) NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Table Expenses (Pengeluaran: Belanja Stok, Operasional, Gaji, BON, dll)
DROP TABLE IF EXISTS `expenses`;
CREATE TABLE `expenses` (
  `id` VARCHAR(100) PRIMARY KEY,
  `category` VARCHAR(100) NOT NULL DEFAULT 'Belanja stok',
  `amount` DECIMAL(15,2) NOT NULL DEFAULT 0.00,
  `description` TEXT,
  `timestamp` VARCHAR(100) NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. Table Admins & Operator (Hak Akses: Superadmin, Admin, Kasir, Mitra)
DROP TABLE IF EXISTS `admins`;
CREATE TABLE `admins` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(50) NOT NULL UNIQUE,
  `password` VARCHAR(100) NOT NULL,
  `role` VARCHAR(50) NOT NULL DEFAULT 'Superadmin',
  `name` VARCHAR(100) NOT NULL,
  `area` VARCHAR(100) NOT NULL DEFAULT 'Semua Cabang',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. Table Areas / Cabang (Multi-Cabang & Wilayah Operational)
DROP TABLE IF EXISTS `areas`;
CREATE TABLE `areas` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `area_name` VARCHAR(100) NOT NULL,
  `description` TEXT,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;

-- ==============================================================
-- END OF CLEAN DATABASE SCHEMA
-- ==============================================================
