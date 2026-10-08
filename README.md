# Absensi Siswa v2

Absensi siswa dengan **scan QR kartu**, dashboard admin berbasis web, dan **notifikasi WhatsApp** ke orang tua (Fonnte).
Server berjalan di satu komputer sekolah (Windows/Linux); HP guru/petugas memakai aplikasi Android sebagai scanner.

```
HP Android (scan QR kontinu) ──Wi-Fi──▶ Server Flask + SQLite ──▶ Antrean WA (latar belakang) ──▶ Fonnte
                                         ▲
                              Admin: browser (login)
```

## Yang baru di v2
| Area | Perubahan |
|---|---|
| Keamanan | Login admin + proteksi CSRF; `/scan` wajib **API key**; QR bertanda tangan HMAC (tidak bisa dibuat sendiri dari NISN); batas percobaan login; secret/API key/password dibuat otomatis |
| Android | Scan **kontinu** (tanpa menekan tombol per siswa), hasil berwarna (hijau/kuning/biru/merah), foto siswa, bunyi & getar, QR setup otomatis, tes koneksi |
| Alur | WA dikirim di **latar belakang** (scan selalu cepat), log + kirim ulang otomatis/manual; status **Terlambat**, Izin, Sakit, Alpa; tombol "Tandai Alpa" |
| Admin | Dashboard per tanggal/kelas, siswa (cari, filter, paginasi, edit, hapus), import Excel/CSV, cetak kartu QR, rekap bulanan + % kehadiran, ekspor Excel/CSV, naik kelas massal, pengaturan, ganti password, backup |
| Teknis | Backup harian otomatis, server produksi (waitress), log berkas, zona waktu eksplisit, 37 tes otomatis, migrasi database v1 otomatis |

## Menjalankan server
1. Pasang Python 3.10+.
2. Windows: klik dua kali `backend/start_server.bat` (Linux/macOS: `backend/start_server.sh`).
3. **Pertama kali**, jendela server menampilkan **password admin** dan **API key** — catat. Semua rahasia tersimpan di `backend/.env`.
4. Isi di `backend/.env`: `FONNTE_TOKEN` dan `TIMEZONE` (WIB `Asia/Jakarta`, WITA `Asia/Makassar`, WIT `Asia/Jayapura`), lalu jalankan ulang.
5. Buka alamat yang tertera di jendela server (mis. `http://192.168.1.20:5000`) dan login.

> Beri komputer server **IP tetap** (reservasi DHCP di router) agar alamat tidak berubah. Izinkan port 5000 di firewall Windows untuk jaringan privat.

## Alur kerja harian
1. **Awal tahun:** Import siswa (menu *Import*) → cetak kartu (menu *Kartu QR*) → bagikan.
2. **Hubungkan HP:** menu *Pengaturan → Hubungkan HP Scanner* menampilkan QR. Buka aplikasi di HP, arahkan kamera ke QR itu — alamat & API key terisi otomatis.
3. **Pagi hari:** petugas membiarkan aplikasi terbuka, siswa menunjukkan kartu. Layar hijau = hadir, kuning = terlambat, biru = sudah absen, merah = ditolak.
4. **Setelah jam masuk:** di *Dashboard*, isi Izin/Sakit sesuai surat, lalu klik **Tandai yang belum absen = Alpa**.
5. **Akhir bulan:** menu *Rekap* → unduh Excel.

## Keamanan — hal penting
- Kartu hilang/dibagikan → *Edit siswa → Reset kartu*, lalu cetak ulang.
- Foto siswa (menu Edit) ditampilkan di layar scanner agar petugas dapat memastikan pemilik kartu.
- **Jangan unggah `backend/.env` atau `sekolah.db` ke GitHub** (sudah di `.gitignore`).
- Jika arsip lama berisi `.env` pernah dibagikan, **ganti token Fonnte** di dashboard Fonnte.
- Aplikasi ini untuk jaringan lokal sekolah. Jangan membuka port 5000 ke internet tanpa HTTPS.

## Migrasi dari versi 1
1. Salin file v2 **menimpa** folder lama, **kecuali** `backend/.env` dan `backend/sekolah.db` (jangan dihapus).
2. Jalankan server. Database dimigrasi otomatis (data siswa & absensi aman). Rahasia baru (`QR_SECRET`, API key, password admin) ditambahkan ke `.env` Anda.
3. **Cetak ulang kartu QR** — QR lama (NISN polos) ditolak. Sementara masa transisi boleh `ALLOW_LEGACY_NISN=1` di `.env` (kurang aman, matikan setelah kartu baru dibagikan).
4. Instal APK baru dari GitHub Actions (artifact `absensi-siswa-debug-apk`).
5. Hapus folder `backend/venv` lama bila ada (tidak perlu ikut disimpan).

## Pengembangan
```
cd backend && pip install -r requirements-dev.txt && python -m pytest -q
```
Android: lihat `android/README.md`. CI GitHub Actions menjalankan tes backend lalu membuild APK.

## Batasan yang diketahui
- Scan tidak disimpan offline di HP; bila Wi-Fi putus, layar merah "Gagal terhubung" dan kartu perlu di-scan ulang.
- Hari efektif rekap dihitung dari hari yang ada siswa hadir/terlambat (belum ada kalender libur).
- Password admin awal tersimpan polos di `.env`; ganti lewat *Pengaturan* agar tersimpan sebagai hash.
