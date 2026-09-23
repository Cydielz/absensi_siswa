# APK Absensi Siswa

Aplikasi Android ini adalah scanner QR NISN. Setelah QR terbaca, APK mengirim JSON ke backend Flask:

`POST /scan`

```json
{"nisn":"0012345678","device_id":"ANDROID_DEVICE_ID"}
```

### Build APK
1. Buka folder `android` menggunakan Android Studio.
2. Tunggu Gradle melakukan sync dependency.
3. Pastikan Android SDK 35 terpasang.
4. Pilih **Build > Build APK(s)**.
5. APK debug akan berada di `app/build/outputs/apk/debug/app-debug.apk`.

### Konfigurasi server di APK
Alamat server dapat diisi di layar utama, contoh:
`http://192.168.1.10:5000`

HP dan laptop/server harus bisa saling terhubung di jaringan yang sama. Untuk penggunaan melalui internet, sebaiknya gunakan HTTPS/reverse proxy.

### Catatan
Token Fonnte tidak ditanam di APK. Token disimpan di server Flask sebagai environment variable.
