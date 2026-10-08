"""Konfigurasi aplikasi Absensi Siswa.

- Membaca backend/.env
- Membuat rahasia (secret key, API key, kunci QR, password admin) secara otomatis
  bila belum diisi, lalu menuliskannya ke .env
- Memuat zona waktu dengan fallback yang JELAS (bukan diam-diam salah)
"""
import logging
import os
import secrets
from datetime import timedelta, timezone

from dotenv import load_dotenv

try:  # Python 3.9+
    from zoneinfo import ZoneInfo
except ImportError:  # pragma: no cover
    ZoneInfo = None

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
ENV_PATH = os.path.join(BASE_DIR, '.env')
load_dotenv(ENV_PATH)

log = logging.getLogger('absensi')

# Nilai yang dianggap "belum diisi" (placeholder dari .env.example)
PLACEHOLDER_PREFIXES = ('ganti', 'isi_', 'isi-', 'changeme', 'your_')

# Zona waktu Indonesia: dipakai bila database tzdata tidak tersedia (mis. Windows)
FIXED_OFFSETS = {
    'Asia/Jakarta': 7, 'Asia/Pontianak': 7, 'WIB': 7,
    'Asia/Makassar': 8, 'WITA': 8,
    'Asia/Jayapura': 9, 'WIT': 9,
}


def _is_weak(value):
    v = (value or '').strip()
    return (not v) or v.lower().startswith(PLACEHOLDER_PREFIXES)


def ensure_secrets(env_path=ENV_PATH, announce=print):
    """Isi otomatis rahasia yang kosong/placeholder di .env. Mengembalikan dict yang dibuat."""
    generators = {
        'FLASK_SECRET_KEY': lambda: secrets.token_hex(32),
        'QR_SECRET': lambda: secrets.token_hex(32),
        'SCAN_API_KEY': lambda: secrets.token_urlsafe(24),
        'ADMIN_PASSWORD': lambda: secrets.token_urlsafe(9),
    }
    lines = []
    if os.path.exists(env_path):
        with open(env_path, 'r', encoding='utf-8') as f:
            lines = f.read().splitlines()

    present = {}
    for i, line in enumerate(lines):
        stripped = line.strip()
        if '=' in stripped and not stripped.startswith('#'):
            k, v = stripped.split('=', 1)
            present[k.strip()] = (i, v.strip())

    generated = {}
    for key, gen in generators.items():
        cur = present.get(key)
        if cur and not _is_weak(cur[1]):
            continue
        if not cur and not _is_weak(os.environ.get(key)):
            continue  # sudah disediakan lewat environment sistem
        value = gen()
        generated[key] = value
        os.environ[key] = value
        if cur:
            lines[cur[0]] = f'{key}={value}'
        else:
            lines.append(f'{key}={value}')

    if generated:
        with open(env_path, 'w', encoding='utf-8') as f:
            f.write('\n'.join(lines) + '\n')
        announce('=' * 64)
        announce('RAHASIA BARU DIBUAT dan disimpan di backend/.env')
        for k, v in generated.items():
            if k in ('ADMIN_PASSWORD', 'SCAN_API_KEY'):
                announce(f'  {k} = {v}')
            else:
                announce(f'  {k} = (disimpan di .env)')
        announce('Catat password admin & API key di atas. Jangan bagikan file .env.')
        if 'QR_SECRET' in generated:
            announce('QR_SECRET baru: kartu QR harus dicetak (ulang) dari menu "Kartu QR".')
        announce('=' * 64)
    return generated


def get_tz(name):
    """Muat zona waktu. Fallback hanya untuk zona Indonesia, selain itu error jelas."""
    if ZoneInfo is not None:
        try:
            return ZoneInfo(name)
        except Exception:  # ZoneInfoNotFoundError / ValueError
            pass
    if name in FIXED_OFFSETS:
        log.warning('Database zona waktu tidak ditemukan; memakai offset tetap UTC+%s untuk %s. '
                    'Jalankan "pip install tzdata" agar akurat.', FIXED_OFFSETS[name], name)
        return timezone(timedelta(hours=FIXED_OFFSETS[name]), name)
    raise RuntimeError(
        f"Zona waktu '{name}' tidak dikenal. Isi TIMEZONE di .env dengan "
        "Asia/Jakarta (WIB), Asia/Makassar (WITA), atau Asia/Jayapura (WIT)."
    )


def _env(name, default=''):
    return os.environ.get(name, default)


def load_config(overrides=None):
    cfg = {
        'DB_PATH': _env('DB_PATH') or os.path.join(BASE_DIR, 'sekolah.db'),
        'BACKUP_DIR': _env('BACKUP_DIR') or os.path.join(BASE_DIR, 'backups'),
        'UPLOAD_DIR': _env('UPLOAD_DIR') or os.path.join(BASE_DIR, 'uploads'),
        'LOG_DIR': _env('LOG_DIR') or os.path.join(BASE_DIR, 'logs'),
        'LOG_TO_FILE': True,
        'TIMEZONE': _env('TIMEZONE', 'Asia/Jakarta'),
        'FLASK_SECRET_KEY': _env('FLASK_SECRET_KEY'),
        'QR_SECRET': _env('QR_SECRET'),
        'SCAN_API_KEY': _env('SCAN_API_KEY'),
        'ADMIN_USERNAME': _env('ADMIN_USERNAME', 'admin'),
        'ADMIN_PASSWORD': _env('ADMIN_PASSWORD'),
        'FONNTE_TOKEN': _env('FONNTE_TOKEN').strip(),
        'FONNTE_COUNTRY_CODE': _env('FONNTE_COUNTRY_CODE', '62'),
        'ALLOW_LEGACY_NISN': _env('ALLOW_LEGACY_NISN', '0') == '1',
        'SESSION_COOKIE_SECURE': _env('SESSION_COOKIE_SECURE', '0') == '1',
        'SESSION_HOURS': int(_env('SESSION_HOURS', '8') or 8),
        'WA_MAX_ATTEMPTS': 3,
        'BACKUP_KEEP': int(_env('BACKUP_KEEP', '14') or 14),
        'HOST': _env('FLASK_HOST', '0.0.0.0'),
        'PORT': int(_env('FLASK_PORT', '5000') or 5000),
    }
    if _is_weak(cfg['FONNTE_TOKEN']):
        cfg['FONNTE_TOKEN'] = ''
    if overrides:
        cfg.update(overrides)
    return cfg
