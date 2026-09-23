# ABSENSI SISWA — QR NISN + Flask + SQLite + Fonnte

Paket ini berisi aplikasi absensi berdasarkan alur:

**Kamera HP Android → QR Code berisi NISN → HTTP POST JSON `/scan` → Flask → validasi siswa → cek absensi hari ini → simpan tanggal & jam masuk → Fonnte → WhatsApp orang tua/wali.**

## Isi paket

- `backend/` — server Python Flask, SQLite, halaman admin, API scan, rekap harian, export CSV, integrasi Fonnte.
- `android/` — proyek Android Studio untuk aplikasi scanner QR.
- `.github/workflows/android.yml` — build APK debug otomatis di GitHub Actions.

## Menjalankan backend di Windows

Pastikan Python 3.10+ tersedia.

Cara paling mudah: buka `backend/start_server.bat`.

Atau dari Command Prompt:

```bat
cd backend
py -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
copy .env.example .env
python seed.py
python app.py
```

Buka di laptop: `http://127.0.0.1:5000`

Untuk HP di Wi-Fi yang sama, gunakan IP laptop, misalnya `http://192.168.1.10:5000`.

### Firewall Windows

Bila HP tidak bisa terhubung ke laptop, jalankan Command Prompt sebagai Administrator:

```bat
netsh advfirewall firewall add rule name="Flask Absensi Siswa" dir=in action=allow protocol=TCP localport=5000
```

## Konfigurasi Fonnte

Edit `backend/.env`:

```env
FONNTE_TOKEN=ISI_TOKEN_FONNTE_DI_SINI
FONNTE_COUNTRY_CODE=62
TIMEZONE=Asia/Jayapura
```

Token Fonnte **jangan pernah ditaruh di APK**. Backend yang memanggil `https://api.fonnte.com/send`.

Nomor WhatsApp orang tua/wali disimpan pada `whatsapp_ortu`.

## Data siswa

Admin membuka `http://IP-SERVER:5000/` lalu menambahkan:

- NISN
- Nama lengkap
- Kelas
- WhatsApp orang tua/wali

Database `sekolah.db` dibuat otomatis.

## API scan

`POST /scan` dengan JSON:

```json
{
  "nisn": "0012345678",
  "device_id": "HP-ABSEN-01"
}
```

Sistem otomatis:

1. Validasi NISN.
2. Cek absensi pada tanggal berjalan.
3. Tolak pencatatan kedua untuk siswa yang sama pada hari yang sama.
4. Simpan tanggal dan jam masuk.
5. Bila nomor WA orang tua tersedia dan Fonnte dikonfigurasi, kirim pemberitahuan.

### Rekap hari ini

`GET /api/attendance/today`

### Export CSV

`GET /admin/export-today`

## Build APK tanpa Android Studio (disarankan)

Anda **tidak perlu menginstal Android Studio, JDK, Android SDK, atau Gradle** di komputer. GitHub Actions akan menyiapkan semuanya di server cloud secara otomatis. Workflow menggunakan JDK 17, Android SDK 35, dan Gradle 8.7. Dokumentasi resmi `setup-gradle` mendukung pemilihan versi Gradle melalui `gradle-version`, sedangkan `setup-android` dapat memasang paket SDK yang diperlukan.

### Langkah 1 — Buat repository GitHub

1. Masuk ke GitHub dan buat repository baru, misalnya `absensi-siswa`.
2. Ekstrak ZIP ini.
3. Upload seluruh isi folder `AbsensiSiswaTKA` ke repository tersebut. Pastikan folder `.github/workflows/android.yml` ikut ter-upload.

### Langkah 2 — Jalankan build

1. Buka repository GitHub Anda.
2. Pilih menu **Actions**.
3. Pilih workflow **Build APK Android**.
4. Tekan **Run workflow**.
5. Tunggu sampai status menjadi centang hijau.
6. Buka hasil workflow tersebut.
7. Pada bagian **Artifacts**, download `absensi-siswa-debug-apk`.
8. Di dalam ZIP artifact itu terdapat `app-debug.apk`, yang dapat dipasang pada HP Android untuk pengujian.

Workflow juga otomatis berjalan setiap kali perubahan pada folder `android/` atau file workflow di-push ke repository.

### Hasil APK

File yang dibuat oleh GitHub Actions:

`android/app/build/outputs/apk/debug/app-debug.apk`

Dependency QR scanner menggunakan ZXing Android Embedded dan akan diambil otomatis oleh Gradle saat build.

### Catatan

APK **debug** cocok untuk instalasi dan pengujian di HP sekolah. Untuk distribusi resmi/Play Store, sebaiknya dibuat **release APK/AAB yang ditandatangani (signed)** menggunakan keystore.

## Penggunaan aplikasi Android

1. Instal APK.
2. Masukkan alamat server Flask, contoh `http://192.168.1.10:5000`.
3. Tekan **Simpan Alamat Server**.
4. Tekan **SCAN QR CODE**.
5. Arahkan kamera ke QR yang berisi NISN.
6. Aplikasi menampilkan hasil absensi.

Format QR yang paling sederhana adalah hanya angka NISN, misalnya:

`0012345678`

Aplikasi juga menerima teks `NISN:0012345678`.

## Catatan keamanan

Starter ini cocok untuk jaringan sekolah/LAN. Halaman admin saat ini belum diberi login; sebelum dipasang di internet publik, tambahkan autentikasi admin, HTTPS, rate limiting, dan backup database.
