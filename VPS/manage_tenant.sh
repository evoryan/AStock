#!/usr/bin/env bash
# ==============================================================================
# ASTOCK VPS TENANT MANAGEMENT SCRIPT (INTERACTIVE BASH)
# Developed for Akbar Media Group
# ==============================================================================
# Usage:
#   ./manage_tenant.sh            (Interactive menu mode)
#   ./manage_tenant.sh list       (List all tenants)
#   ./manage_tenant.sh add        (Add new tenant)
#   ./manage_tenant.sh delete     (Delete tenant)
# ==============================================================================

# ANSI Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Database Configuration (Environment variables with fallbacks)
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-konter}"
DB_PASS="${DB_PASSWORD:-08Delapan}"
MASTER_DB="konter_master"

# Function to run MySQL query cleanly
exec_mysql() {
    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" -e "$1" 2>/dev/null
}

# Function to check MySQL connection
check_db_connection() {
    if ! command -v mysql &> /dev/null; then
        echo -e "${RED}[ERROR] Command 'mysql' tidak ditemukan! Silakan install mysql-client terlebih dahulu.${NC}"
        return 1
    fi

    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" -e "USE $MASTER_DB;" &>/dev/null
    if [ $? -ne 0 ]; then
        echo -e "${YELLOW}[WARNING] Gagal terhubung ke MySQL dengan user default '$DB_USER'.${NC}"
        echo -e "${CYAN}Silakan masukkan kredensial MySQL secara manual:${NC}"
        read -p "MySQL Host [$DB_HOST]: " input_host
        read -p "MySQL User [$DB_USER]: " input_user
        read -s -p "MySQL Password: " input_pass
        echo ""

        DB_HOST="${input_host:-$DB_HOST}"
        DB_USER="${input_user:-$DB_USER}"
        DB_PASS="${input_pass}"

        exec_mysql "CREATE DATABASE IF NOT EXISTS $MASTER_DB;"
        if [ $? -ne 0 ]; then
            echo -e "${RED}[ERROR] Gagal terhubung ke MySQL Server! Pastikan service MySQL running.${NC}"
            exit 1
        fi
    fi
    return 0
}

# ------------------------------------------------------------------------------
# 1. LIST TENANTS
# ------------------------------------------------------------------------------
list_tenants() {
    echo -e "\n${CYAN}${BOLD}=== DAFTAR TENANT ASTOCK TERDAFTAR ===${NC}\n"
    
    # Run MySQL query to select all tenants
    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" \
          -e "USE $MASTER_DB; SELECT id AS 'ID', username AS 'USERNAME', tenant_name AS 'NAMA KONTER', db_name AS 'DATABASE', accent_color AS 'AKSEN', business_type AS 'JENIS USAHA', created_at AS 'TANGGAL DAFTAR' FROM tenants ORDER BY id ASC;" \
          2>/dev/null

    if [ $? -ne 0 ]; then
        echo -e "${RED}[ERROR] Gagal mengambil daftar tenant dari database master '$MASTER_DB'.${NC}"
    fi
    echo ""
}

# ------------------------------------------------------------------------------
# 2. TAMBAH TENANT BARU
# ------------------------------------------------------------------------------
add_tenant() {
    echo -e "\n${GREEN}${BOLD}=== TAMBAH TENANT BARU ===${NC}\n"

    read -p "Username Login Store    : " USERNAME
    if [ -z "$USERNAME" ]; then
        echo -e "${RED}[ERROR] Username tidak boleh kosong!${NC}"
        return
    fi

    # Check if username already exists
    EXISTING=$(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" -N -e "USE $MASTER_DB; SELECT COUNT(*) FROM tenants WHERE username='$USERNAME';" 2>/dev/null)
    if [ "$EXISTING" -gt 0 ]; then
        echo -e "${RED}[ERROR] Username '$USERNAME' sudah digunakan oleh tenant lain!${NC}"
        return
    fi

    read -p "Password Login Store    : " PASSWORD
    if [ -z "$PASSWORD" ]; then
        echo -e "${RED}[ERROR] Password tidak boleh kosong!${NC}"
        return
    fi

    read -p "Nama Konter / Toko     : " TENANT_NAME
    if [ -z "$TENANT_NAME" ]; then
        TENANT_NAME="$USERNAME Cell"
    fi

    DEFAULT_DB="konter_${USERNAME}"
    read -p "Nama Database Tenant [$DEFAULT_DB]: " DB_NAME
    DB_NAME="${DB_NAME:-$DEFAULT_DB}"

    read -p "Warna Aksen Hex [#F59E0B]: " ACCENT_COLOR
    ACCENT_COLOR="${ACCENT_COLOR:-#F59E0B}"

    read -p "Jenis Usaha [Retail Konter]: " BUSINESS_TYPE
    BUSINESS_TYPE="${BUSINESS_TYPE:-Retail Konter}"

    echo -e "\n${YELLOW}Sedang memproses pendaftaran tenant & mengalokasikan database...${NC}"

    # 1. Insert record into master database
    INSERT_SQL="USE $MASTER_DB; INSERT INTO tenants (username, password, tenant_name, db_name, accent_color, business_type) VALUES ('$USERNAME', '$PASSWORD', '$TENANT_NAME', '$DB_NAME', '$ACCENT_COLOR', '$BUSINESS_TYPE');"
    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" -e "$INSERT_SQL" 2>/dev/null

    if [ $? -ne 0 ]; then
        echo -e "${RED}[ERROR] Gagal menambahkan record tenant ke $MASTER_DB!${NC}"
        return
    fi

    # 2. Create sub-database and setup schemas
    SCHEMA_SQL="
    CREATE DATABASE IF NOT EXISTS \`$DB_NAME\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    USE \`$DB_NAME\`;

    CREATE TABLE IF NOT EXISTS \`products\` (
      \`id\` INT AUTO_INCREMENT PRIMARY KEY,
      \`name\` VARCHAR(255) NOT NULL,
      \`sku\` VARCHAR(100) NOT NULL UNIQUE,
      \`category\` VARCHAR(100) DEFAULT 'Umum',
      \`price\` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
      \`stock\` INT NOT NULL DEFAULT 0,
      \`min_stock_alert\` INT NOT NULL DEFAULT 5,
      \`updated_at\` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

    CREATE TABLE IF NOT EXISTS \`transactions\` (
      \`id\` VARCHAR(100) PRIMARY KEY,
      \`product_name\` VARCHAR(255) NOT NULL,
      \`sku\` VARCHAR(100) NOT NULL,
      \`quantity\` INT NOT NULL,
      \`total_price\` DECIMAL(12,2) NOT NULL,
      \`timestamp\` VARCHAR(100) NOT NULL,
      \`operator\` VARCHAR(100) DEFAULT 'Kasir',
      \`created_at\` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

    CREATE TABLE IF NOT EXISTS \`modal_awal\` (
      \`id\` INT PRIMARY KEY DEFAULT 1,
      \`modal_awal\` DECIMAL(15,2) NOT NULL DEFAULT 0.00,
      \`online_income\` DECIMAL(15,2) NOT NULL DEFAULT 0.00,
      \`updated_at\` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

    CREATE TABLE IF NOT EXISTS \`incomes\` (
      \`id\` VARCHAR(100) PRIMARY KEY,
      \`amount\` DECIMAL(15,2) NOT NULL DEFAULT 0.00,
      \`description\` TEXT,
      \`timestamp\` VARCHAR(100) NOT NULL,
      \`created_at\` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

    CREATE TABLE IF NOT EXISTS \`expenses\` (
      \`id\` VARCHAR(100) PRIMARY KEY,
      \`category\` VARCHAR(100) NOT NULL DEFAULT 'Belanja stok',
      \`amount\` DECIMAL(15,2) NOT NULL DEFAULT 0.00,
      \`description\` TEXT,
      \`timestamp\` VARCHAR(100) NOT NULL,
      \`created_at\` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

    CREATE TABLE IF NOT EXISTS \`admins\` (
      \`id\` INT AUTO_INCREMENT PRIMARY KEY,
      \`username\` VARCHAR(50) NOT NULL UNIQUE,
      \`password\` VARCHAR(100) NOT NULL,
      \`role\` VARCHAR(50) DEFAULT 'Kasir',
      \`name\` VARCHAR(100) NOT NULL,
      \`created_at\` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

    CREATE TABLE IF NOT EXISTS \`areas\` (
      \`id\` INT AUTO_INCREMENT PRIMARY KEY,
      \`area_name\` VARCHAR(100) NOT NULL,
      \`description\` TEXT,
      \`created_at\` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

    INSERT INTO \`modal_awal\` (\`id\`, \`modal_awal\`, \`online_income\`) VALUES (1, 0.00, 0.00) ON DUPLICATE KEY UPDATE \`id\`=1;
    "

    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" -e "$SCHEMA_SQL" 2>/dev/null

    if [ $? -eq 0 ]; then
        echo -e "\n${GREEN}${BOLD}✔ SUKSES! Tenant '$TENANT_NAME' berhasil dibuat.${NC}"
        echo -e "${CYAN}Detail Login:${NC}"
        echo -e " - Username  : ${BOLD}$USERNAME${NC}"
        echo -e " - Password  : ${BOLD}$PASSWORD${NC}"
        echo -e " - Database  : ${BOLD}$DB_NAME${NC}"
    else
        echo -e "${RED}[ERROR] Gagal membuat skema database '$DB_NAME'!${NC}"
    fi
    echo ""
}

# ------------------------------------------------------------------------------
# 3. HAPUS TENANT
# ------------------------------------------------------------------------------
delete_tenant() {
    echo -e "\n${RED}${BOLD}=== HAPUS TENANT ===${NC}\n"
    list_tenants

    read -p "Masukkan Username Tenant yang akan dihapus: " TARGET_USER
    if [ -z "$TARGET_USER" ]; then
        echo -e "${YELLOW}Batal menghapus tenant.${NC}"
        return
    fi

    # Fetch db_name for the tenant
    DB_NAME=$(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" -N -e "USE $MASTER_DB; SELECT db_name FROM tenants WHERE username='$TARGET_USER';" 2>/dev/null)

    if [ -z "$DB_NAME" ]; then
        echo -e "${RED}[ERROR] Tenant dengan username '$TARGET_USER' tidak ditemukan!${NC}"
        return
    fi

    echo -e "${RED}${BOLD}PERINGATAN: Tindakan ini akan menghapus permanen database '$DB_NAME' dan seluruh datanya!${NC}"
    read -p "Apakah Anda yakin ingin menghapus '$TARGET_USER'? (y/N): " CONFIRM
    if [[ "$CONFIRM" != "y" && "$CONFIRM" != "Y" ]]; then
        echo -e "${YELLOW}Penghapusan dibatalkan.${NC}"
        return
    fi

    # 1. Drop the sub-database
    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" -e "DROP DATABASE IF EXISTS \`$DB_NAME\`;" 2>/dev/null

    # 2. Delete entry from master database
    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" -e "USE $MASTER_DB; DELETE FROM tenants WHERE username='$TARGET_USER';" 2>/dev/null

    echo -e "\n${GREEN}✔ Tenant '$TARGET_USER' dan database '$DB_NAME' telah berhasil dihapus.${NC}\n"
}

# ------------------------------------------------------------------------------
# MAIN INTERACTIVE MENU
# ------------------------------------------------------------------------------
show_menu() {
    clear
    echo -e "${CYAN}${BOLD}"
    echo "=========================================================="
    echo "       ASTOCK VPS - MANAJEMEN TENANT INTERAKTIF          "
    echo "              ©2026 Akbar Media Group                     "
    echo "=========================================================="
    echo -e "${NC}"
    echo -e "${BOLD}Pilih Menu:${NC}"
    echo -e " ${GREEN}1.${NC} List Tenant (Daftar Toko/Konter)"
    echo -e " ${GREEN}2.${NC} Tambah Tenant Baru"
    echo -e " ${GREEN}3.${NC} Hapus Tenant"
    echo -e " ${RED}4.${NC} Keluar"
    echo ""
    read -p "Pilihan Anda [1-4]: " CHOICE

    case $CHOICE in
        1)
            list_tenants
            read -p "Tekan [Enter] untuk kembali ke menu utama..."
            show_menu
            ;;
        2)
            add_tenant
            read -p "Tekan [Enter] untuk kembali ke menu utama..."
            show_menu
            ;;
        3)
            delete_tenant
            read -p "Tekan [Enter] untuk kembali ke menu utama..."
            show_menu
            ;;
        4)
            echo -e "${CYAN}Terima kasih telah menggunakan ASTOCK Tenant Management CLI.${NC}"
            exit 0
            ;;
        *)
            echo -e "${RED}Pilihan tidak valid!${NC}"
            sleep 1
            show_menu
            ;;
    esac
}

# Main script entrypoint
check_db_connection || exit 1

if [ "$1" == "list" ]; then
    list_tenants
elif [ "$1" == "add" ]; then
    add_tenant
elif [ "$1" == "delete" ]; then
    delete_tenant
else
    show_menu
fi
