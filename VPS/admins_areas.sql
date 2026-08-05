-- ==============================================================
-- DATABASE SCHEMA FOR ADMINS, ROLES, AND AREAS CRUD (VPS MYSQL)
-- ==============================================================

-- 1. Table structure for Cabang/Area
CREATE TABLE IF NOT EXISTS `areas` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(100) NOT NULL UNIQUE,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Table structure for Admins/Multi-role users
CREATE TABLE IF NOT EXISTS `admins` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(100) NOT NULL,
  `username` VARCHAR(50) NOT NULL UNIQUE,
  `password` VARCHAR(100) NOT NULL,
  `role` VARCHAR(50) NOT NULL DEFAULT 'Kasir', -- Values: 'Admin', 'Kasir', 'Mitra'
  `area` VARCHAR(100) NOT NULL DEFAULT 'Semua Cabang',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Seed initial sample data for Cabang/Area
INSERT IGNORE INTO `areas` (`id`, `name`) VALUES 
(1, 'Cabang Utama'), 
(2, 'Cabang Jakarta Selatan'), 
(3, 'Cabang Surabaya Barat');

-- 4. Seed initial sample data for Admins/Multi-role users
INSERT IGNORE INTO `admins` (`id`, `name`, `username`, `password`, `role`, `area`) VALUES
(1, 'Miftah Hidayat', 'miftah_admin', 'admin123', 'Admin', 'Semua Cabang'),
(2, 'Rina Lestari', 'rina_kasir', 'kasir123', 'Kasir', 'Cabang Jakarta Selatan'),
(3, 'Joko Susilo', 'joko_mitra', 'mitra123', 'Mitra', 'Cabang Surabaya Barat');
