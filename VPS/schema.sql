-- ==============================================================
-- SCHEMA INITIALIZATION FOR ASTOCK VPS MYSQL MULTI-TENANCY
-- ==============================================================
-- Developed for Akbar Media Group - ASTOCK System
-- Deploy this script directly to your MySQL Server instance.

-- --------------------------------------------------------------
-- PART 1: MASTER ROUTING DATABASE SETUP (konter_master)
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

-- Seed Sample Master Tenants
INSERT INTO `tenants` (`username`, `password`, `tenant_name`, `db_name`, `accent_color`, `business_type`) VALUES
('miftah', 'miftah123', 'Miftah Cell', 'konter_miftah', '#F59E0B', 'Konter Pulsa & Aksesoris'),
('budi', 'budi123', 'Budi Store', 'konter_budi', '#F59E0B', 'Toko Gadget'),
('anita', 'anita123', 'Anita Multi-Shop', 'konter_anita', '#F59E0B', 'Konter Paket Data');


-- --------------------------------------------------------------
-- PART 2: TENANT DATABASE TEMPLATE & SCHEMAS
-- Structure for all tenant databases (products, transactions,
-- modal_awal, incomes, expenses, admins, areas)
-- --------------------------------------------------------------

-- EXAMPLE TENANT 1: Create 'konter_miftah' Database
CREATE DATABASE IF NOT EXISTS `konter_miftah` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `konter_miftah`;

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

-- 2. Table Transactions (Penjualan / Kasir)
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

-- 4. Table Incomes (Pemasukkan Lain-Lain)
DROP TABLE IF EXISTS `incomes`;
CREATE TABLE `incomes` (
  `id` VARCHAR(100) PRIMARY KEY,
  `amount` DECIMAL(15,2) NOT NULL DEFAULT 0.00,
  `description` TEXT,
  `timestamp` VARCHAR(100) NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Table Expenses (Pengeluaran: Belanja stok, Gaji karyawan, Operasional dan lainnya, BON, Modal Awal)
DROP TABLE IF EXISTS `expenses`;
CREATE TABLE `expenses` (
  `id` VARCHAR(100) PRIMARY KEY,
  `category` VARCHAR(100) NOT NULL DEFAULT 'Belanja stok',
  `amount` DECIMAL(15,2) NOT NULL DEFAULT 0.00,
  `description` TEXT,
  `timestamp` VARCHAR(100) NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. Table Admins & Operator (Manajemen Pengguna Tenant)
DROP TABLE IF EXISTS `admins`;
CREATE TABLE `admins` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(50) NOT NULL UNIQUE,
  `password` VARCHAR(100) NOT NULL,
  `role` VARCHAR(50) DEFAULT 'Kasir',
  `name` VARCHAR(100) NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. Table Areas / Cabang
DROP TABLE IF EXISTS `areas`;
CREATE TABLE `areas` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `area_name` VARCHAR(100) NOT NULL,
  `description` TEXT,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed Sample Data for konter_miftah
INSERT INTO `products` (`name`, `sku`, `category`, `price`, `stock`, `min_stock_alert`) VALUES
('Pulsa Telkomsel 10k', 'PLSTSEL10K', 'Pulsa', 12000.00, 999, 10),
('Pulsa XL Axiata 5k', 'PLSXLAX5K', 'Pulsa', 7000.00, 999, 10),
('Kabel Data Type-C Orico', 'KBLTYPECORC', 'Aksesoris', 25000.00, 15, 3),
('Tempered Glass Redmi Note 12', 'TGREDMIN12', 'Aksesoris', 15000.00, 8, 2),
('Paket Indosat Freedom 10GB', 'PKTIDSF10G', 'Paket Data', 45000.00, 50, 5);

INSERT INTO `modal_awal` (`id`, `modal_awal`, `online_income`) VALUES (1, 1000000.00, 0.00);

INSERT INTO `expenses` (`id`, `category`, `amount`, `description`, `timestamp`) VALUES
('exp_init_1', 'Belanja stok', 1000000.00, 'Modal Awal', '2026-08-01 08:00');


-- --------------------------------------------------------------
-- EXAMPLE TENANT 2: Create 'konter_budi' Database
-- --------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `konter_budi` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `konter_budi`;

CREATE TABLE `products` LIKE `konter_miftah`.`products`;
CREATE TABLE `transactions` LIKE `konter_miftah`.`transactions`;
CREATE TABLE `modal_awal` LIKE `konter_miftah`.`modal_awal`;
CREATE TABLE `incomes` LIKE `konter_miftah`.`incomes`;
CREATE TABLE `expenses` LIKE `konter_miftah`.`expenses`;
CREATE TABLE `admins` LIKE `konter_miftah`.`admins`;
CREATE TABLE `areas` LIKE `konter_miftah`.`areas`;

INSERT INTO `products` (`name`, `sku`, `category`, `price`, `stock`, `min_stock_alert`) VALUES
('iPhone 15 Pro Max 256GB', 'IPH15PM256', 'Gadget', 22500000.00, 3, 1),
('Samsung Galaxy S24 Ultra', 'SAMS24ULTRA', 'Gadget', 19900000.00, 5, 1),
('Charger Anker GaN 30W', 'CHGANKER30W', 'Aksesoris', 180000.00, 20, 5);

INSERT INTO `modal_awal` (`id`, `modal_awal`, `online_income`) VALUES (1, 5000000.00, 0.00);


-- --------------------------------------------------------------
-- EXAMPLE TENANT 3: Create 'konter_anita' Database
-- --------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `konter_anita` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `konter_anita`;

CREATE TABLE `products` LIKE `konter_miftah`.`products`;
CREATE TABLE `transactions` LIKE `konter_miftah`.`transactions`;
CREATE TABLE `modal_awal` LIKE `konter_miftah`.`modal_awal`;
CREATE TABLE `incomes` LIKE `konter_miftah`.`incomes`;
CREATE TABLE `expenses` LIKE `konter_miftah`.`expenses`;
CREATE TABLE `admins` LIKE `konter_miftah`.`admins`;
CREATE TABLE `areas` LIKE `konter_miftah`.`areas`;

INSERT INTO `modal_awal` (`id`, `modal_awal`, `online_income`) VALUES (1, 2000000.00, 0.00);
