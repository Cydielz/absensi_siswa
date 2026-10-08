# Backend Absensi Siswa

## 1. Jalankan di Windows/XAMPP/Python
XAMPP tidak diperlukan; backend ini berjalan dengan Python Flask.

```bat
cd backend
py -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
copy .env.example .env
py seed.py
py app.py
```

Server akan mendengarkan pada `http://0.0.0.0:5000`.
Dari HP yang berada di Wi-Fi yang sama, gunakan alamat laptop, misalnya:
`http://192.168.1.10:5000`.

## 2. Konfigurasi Fonnte
Atur `FONNTE_TOKEN` pada environment/`.env` server. Jangan masukkan token ke APK.
Nomor WA orang tua disimpan di kolom `whatsapp_ortu`.

## 3. Endpoint scan
`POST /scan`
```json
{"nisn":"0012345678","device_id":"HP-ABSEN-01"}
```

Sistem:
- mencari NISN aktif di tabel `siswa`;
- mengecek apakah siswa sudah absen pada tanggal berjalan;
- bila belum, mencatat `tanggal`, `jam_masuk`, `status=Hadir`;
- bila nomor orang tua tersedia, mengirim notifikasi melalui Fonnte;
- bila sudah pernah absen, tidak membuat baris kedua.

## 4. API rekap
`GET /api/attendance/today`

## 5. Database
SQLite dibuat otomatis dengan nama `sekolah.db`.

## Jika muncul "Internal Server Error" di Windows

Paket ini sudah menambahkan `tzdata` dan fallback UTC+09:00 agar zona waktu `Asia/Jayapura` tidak menyebabkan Flask HTTP 500 pada Windows. Setelah mengganti file, jalankan ulang:

```bat
cd backend
.venv\Scripts\activate
pip install -r requirements.txt
py seed.py
py app.py
```

Tes di laptop dengan membuka `http://127.0.0.1:5000/api/health`. Jika berhasil, browser akan menampilkan JSON dengan `"ok": true`.
