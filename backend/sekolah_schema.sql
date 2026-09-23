-- Schema referensi. Aplikasi Flask membuat tabel otomatis saat pertama kali dijalankan.
CREATE TABLE IF NOT EXISTS siswa (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nisn TEXT NOT NULL UNIQUE,
    nama TEXT NOT NULL,
    kelas TEXT NOT NULL,
    whatsapp_ortu TEXT,
    aktif INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS absensi (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    siswa_id INTEGER NOT NULL,
    tanggal TEXT NOT NULL,
    jam_masuk TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'Hadir',
    device_id TEXT,
    created_at TEXT NOT NULL,
    FOREIGN KEY(siswa_id) REFERENCES siswa(id) ON DELETE CASCADE,
    UNIQUE(siswa_id, tanggal)
);
