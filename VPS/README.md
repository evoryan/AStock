# ASTOCK - Panduan Deploy Backend Multi-Tenant di VPS

Repository ini berisi backend berbasis **Node.js + Express + MySQL** yang didesain khusus untuk mendukung multi-tenant database routing untuk aplikasi **ASTOCK**.

## Detail Konfigurasi VPS Anda:
* **Alamat API/Server**: `103.253.245.25:3900`
* **Port Layanan Node.js**: `3900`
* **MySQL User**: `konter`
* **MySQL Password**: `08Delapan`

---

## 🛠️ Langkah 1: Persiapan Server VPS (Ubuntu / Debian)

Hubungkan ke VPS Anda melalui SSH:
```bash
ssh root@103.253.245.25
```

Pastikan sistem Anda sudah memiliki Node.js, NPM, dan MySQL Server. Jika belum, instal dengan perintah berikut:

### 1. Update Paket Sistem
```bash
sudo apt update && sudo apt upgrade -y
```

### 2. Instal Node.js (Versi 18 atau 20 direkomendasikan)
```bash
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs
```
Pastikan instalasi berhasil dengan memeriksa versi:
```bash
node -v
npm -v
```

### 3. Instal PM2 (Process Manager agar Server Aktif Selamanya)
PM2 berguna agar ketika terminal ditutup, server Node.js Anda tetap berjalan di background secara otomatis.
```bash
sudo npm install -g pm2
```

---

## 💾 Langkah 2: Setup Database di MySQL VPS

1. Masuk ke MySQL CLI atau gunakan phpMyAdmin / Navicat / DBeaver dengan credential Anda:
   ```bash
   mysql -u konter -p
   # Masukkan password: 08Delapan
   ```

2. Jalankan isi dari file `schema.sql` untuk membuat database master routing (`konter_master`) dan database tenant sampel (`konter_miftah`, `konter_budi`, & `konter_anita`) beserta tabel pendukung (`products`, `transactions`, `modal_awal`, `incomes`, `expenses`, `admins`, `areas`).

3. **Manajemen Tenant Interaktif (`manage_tenant.sh`)**:
   Gunakan script bash interaktif di VPS untuk menambah, menghapus, atau melihat daftar tenant secara otomatis:
   ```bash
   chmod +x manage_tenant.sh
   ./manage_tenant.sh
   ```
   Atau gunakan perintah langsung:
   - List Tenant: `./manage_tenant.sh list`
   - Tambah Tenant: `./manage_tenant.sh add`
   - Hapus Tenant: `./manage_tenant.sh delete`

4. Pastikan konfigurasi user MySQL Anda memperbolehkan koneksi luar jika database Anda di-host secara terpisah (skip langkah ini jika database berada di dalam server VPS yang sama dengan aplikasi Node.js):
   ```sql
   -- Memberikan hak akses ke user konter
   GRANT ALL PRIVILEGES ON *.* TO 'konter'@'%' IDENTIFIED BY '08Delapan';
   FLUSH PRIVILEGES;
   ```

---

## 🚀 Langkah 3: Upload dan Menjalankan Aplikasi di VPS

1. **Upload folder `VPS`** dari proyek ini ke server VPS Anda (bisa menggunakan FTP/SFTP seperti FileZilla, atau menggunakan `scp` command line).
   Contoh upload menggunakan terminal komputer lokal Anda:
   ```bash
   scp -r ./VPS root@103.253.245.25:/var/www/astock-backend
   ```

2. Masuk ke direktori aplikasi di VPS Anda:
   ```bash
   cd /var/www/astock-backend
   ```

3. Instal semua dependencies node modules yang dibutuhkan:
   ```bash
   npm install --production
   ```

4. Verifikasi file `.env` Anda sudah benar sesuai dengan database lokal VPS:
   ```bash
   nano .env
   ```
   *(Tips: Jika MySQL berjalan di VPS yang sama dengan Node.js, ubah `DB_HOST=103.253.245.25` menjadi `DB_HOST=localhost` atau `127.0.0.1` untuk mempercepat query).*

5. **Jalankan Aplikasi menggunakan PM2** agar aktif selamanya:
   ```bash
   pm2 start server.js --name "astock-api"
   ```

6. Simpan konfigurasi PM2 agar otomatis berjalan kembali jika VPS Anda direstart:
   ```bash
   pm2 save
   pm2 startup
   ```

---

## 🔍 Langkah 4: Monitor & Pengujian

### Memeriksa Status Aplikasi:
```bash
pm2 status
```

### Melihat Log Server Realtime:
```bash
pm2 logs astock-api
```

### Menguji API Endpoint:
Gunakan browser atau alat terminal (seperti Curl/Postman) untuk mengetes apakah API VPS Anda sudah dapat dijangkau publik:

**Health check:**
```bash
curl http://103.253.245.25:3900/api/health
```
*(Response normal: `{"status":"UP", ...}`)*

---

## 📱 Hubungkan dengan Aplikasi Android ASTOCK
Pada halaman Login aplikasi Android ASTOCK, silakan pilih opsi **"Gunakan Real VPS"** dan masukkan URL server Anda:
`http://103.253.245.25:3900`
