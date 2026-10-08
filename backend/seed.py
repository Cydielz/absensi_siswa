"""Opsional: isi data contoh untuk uji coba. Jalankan manual:  python seed.py --contoh
Jangan dijalankan di server produksi (nomor WA contoh bukan nomor asli)."""
import sys
import config as cfgmod
import database as dbm
import services as svc

if '--contoh' not in sys.argv:
    print('Tambahkan --contoh untuk benar-benar memasukkan data contoh.')
    raise SystemExit(0)

cfg = cfgmod.load_config()
cfg['TZ'] = cfgmod.get_tz(cfg['TIMEZONE'])
dbm.init_db(cfg['DB_PATH'])
conn = dbm.connect(cfg['DB_PATH'])
for nisn, nama, kelas in [('0012345678', 'Contoh Siswa 1', 'X IPA 1'), ('0012345679', 'Contoh Siswa 2', 'X IPA 1')]:
    conn.execute('INSERT OR IGNORE INTO siswa (nisn,nama,kelas,whatsapp_ortu,created_at) VALUES (?,?,?,?,?)',
                 (nisn, nama, kelas, '', svc.now_str(cfg['TZ'])))
conn.commit()
print('Data contoh dimasukkan (tanpa nomor WA).')
