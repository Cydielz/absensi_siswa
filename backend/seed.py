import os
from datetime import datetime
from zoneinfo import ZoneInfo
from app import db, init_db

init_db()
rows = [
    ('0012345678', 'Contoh Siswa 1', 'X IPA 1', '081234567890'),
    ('0012345679', 'Contoh Siswa 2', 'X IPA 1', '081298765432'),
]
conn = db()
for nisn,nama,kelas,wa in rows:
    conn.execute('''INSERT OR IGNORE INTO siswa (nisn,nama,kelas,whatsapp_ortu,created_at)
                    VALUES (?,?,?,?,?)''', (nisn,nama,kelas,wa,datetime.now(ZoneInfo('Asia/Jayapura')).isoformat()))
conn.commit(); conn.close()
print('Database siap. Contoh siswa dimasukkan bila belum ada.')
