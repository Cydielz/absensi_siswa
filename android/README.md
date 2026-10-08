# Aplikasi Android — Scanner Absensi

Scanner QR kontinu (ZXing) yang mengirim hasil ke server Flask.

## Build
Otomatis lewat GitHub Actions (`.github/workflows/android.yml`) → unduh artifact `absensi-siswa-debug-apk`.
Atau Android Studio: buka folder `android/`, Run.

## Pemakaian
1. Buka aplikasi, izinkan kamera.
2. Hubungkan ke server: scan QR dari web admin (*Pengaturan → Hubungkan HP Scanner*) **atau** isi manual di ⚙ Pengaturan, lalu *Tes koneksi*.
3. Biarkan terbuka; arahkan ke kartu siswa. Kode sama dalam 6 detik diabaikan agar tidak terkirim ganda.

Hasil: **hijau** hadir · **kuning** terlambat · **biru** sudah absen · **merah** ditolak/gagal.

## Catatan teknis
- Hanya QR berformat `ABSEN:<nisn>:<tanda-tangan>` yang diterima server; aplikasi mengirim isi QR apa adanya.
- `/scan` memakai header `X-API-Key`. Kunci disimpan di SharedPreferences (privat aplikasi; `allowBackup=false`).
- Lalu lintas HTTP biasa dipakai karena server berada di LAN sekolah.
