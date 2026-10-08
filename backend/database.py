"""Akses SQLite, skema, dan migrasi otomatis dari versi lama."""
import sqlite3

STATUS_HADIR = ('Hadir', 'Terlambat')
STATUS_SEMUA = ('Hadir', 'Terlambat', 'Izin', 'Sakit', 'Alpa')

DEFAULT_TEMPLATE = (
    "PEMBERITAHUAN ABSENSI SEKOLAH\n\n"
    "Nama: {nama}\n"
    "NISN: {nisn}\n"
    "Kelas: {kelas}\n"
    "Status: {status}\n"
    "Tanggal: {tanggal}\n"
    "Jam masuk: {jam}\n\n"
    "Pesan ini dikirim otomatis oleh sistem absensi {sekolah}."
)

DEFAULT_SETTINGS = {
    'nama_sekolah': 'Sekolah Anda',
    'batas_terlambat': '07:15',
    'wa_aktif': '1',
    'wa_template': DEFAULT_TEMPLATE,
}

SCHEMA = '''
CREATE TABLE IF NOT EXISTS siswa (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nisn TEXT NOT NULL UNIQUE,
    nama TEXT NOT NULL,
    kelas TEXT NOT NULL,
    whatsapp_ortu TEXT,
    aktif INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL,
    qr_version INTEGER NOT NULL DEFAULT 1,
    foto TEXT
);

CREATE TABLE IF NOT EXISTS absensi (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    siswa_id INTEGER NOT NULL,
    tanggal TEXT NOT NULL,
    jam_masuk TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'Hadir',
    device_id TEXT,
    created_at TEXT NOT NULL,
    keterangan TEXT,
    updated_at TEXT,
    FOREIGN KEY(siswa_id) REFERENCES siswa(id) ON DELETE CASCADE,
    UNIQUE(siswa_id, tanggal)
);

CREATE TABLE IF NOT EXISTS wa_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    siswa_id INTEGER,
    absensi_id INTEGER,
    target TEXT NOT NULL,
    message TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending',
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TEXT,
    last_response TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT
);

CREATE TABLE IF NOT EXISTS pengaturan (
    kunci TEXT PRIMARY KEY,
    nilai TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_absensi_tanggal ON absensi(tanggal);
CREATE INDEX IF NOT EXISTS idx_siswa_nisn ON siswa(nisn);
CREATE INDEX IF NOT EXISTS idx_siswa_kelas ON siswa(kelas);
CREATE INDEX IF NOT EXISTS idx_wa_status ON wa_log(status, next_attempt_at);
'''


def connect(path):
    conn = sqlite3.connect(path, timeout=10)
    conn.row_factory = sqlite3.Row
    conn.execute('PRAGMA foreign_keys = ON')
    conn.execute('PRAGMA busy_timeout = 10000')
    return conn


def _ensure_column(conn, table, column, ddl):
    cols = {r['name'] for r in conn.execute(f'PRAGMA table_info({table})')}
    if column not in cols:
        conn.execute(f'ALTER TABLE {table} ADD COLUMN {column} {ddl}')


def init_db(path):
    """Buat tabel baru dan migrasikan database versi lama (tanpa menghapus data)."""
    conn = connect(path)
    try:
        conn.execute('PRAGMA journal_mode = WAL')
        conn.executescript(SCHEMA)
        # Migrasi dari versi 1.x
        _ensure_column(conn, 'siswa', 'qr_version', 'INTEGER NOT NULL DEFAULT 1')
        _ensure_column(conn, 'siswa', 'foto', 'TEXT')
        _ensure_column(conn, 'absensi', 'keterangan', 'TEXT')
        _ensure_column(conn, 'absensi', 'updated_at', 'TEXT')
        for k, v in DEFAULT_SETTINGS.items():
            conn.execute('INSERT OR IGNORE INTO pengaturan (kunci, nilai) VALUES (?, ?)', (k, v))
        conn.commit()
    finally:
        conn.close()


def get_setting(conn, key, default=None):
    row = conn.execute('SELECT nilai FROM pengaturan WHERE kunci=?', (key,)).fetchone()
    if row is not None:
        return row['nilai']
    return DEFAULT_SETTINGS.get(key, default)


def set_setting(conn, key, value):
    conn.execute(
        'INSERT INTO pengaturan (kunci, nilai) VALUES (?, ?) '
        'ON CONFLICT(kunci) DO UPDATE SET nilai=excluded.nilai',
        (key, value),
    )
