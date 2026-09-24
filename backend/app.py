import os
import sqlite3
from datetime import datetime
from zoneinfo import ZoneInfo
from flask import Flask, jsonify, request, render_template, redirect, url_for, flash, Response
import requests
from dotenv import load_dotenv

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
load_dotenv(os.path.join(BASE_DIR, ".env"))
DB_PATH = os.path.join(BASE_DIR, 'sekolah.db')
TIMEZONE = os.getenv('TIMEZONE', 'Asia/Jayapura')

app = Flask(__name__)
app.secret_key = os.getenv('FLASK_SECRET_KEY', 'ganti-secret-key-anda')


def now_local():
    return datetime.now(ZoneInfo(TIMEZONE))


def db():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    conn.execute('PRAGMA foreign_keys = ON')
    return conn


def init_db():
    conn = db()
    conn.executescript('''
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

    CREATE INDEX IF NOT EXISTS idx_absensi_tanggal ON absensi(tanggal);
    CREATE INDEX IF NOT EXISTS idx_siswa_nisn ON siswa(nisn);
    ''')
    conn.commit()
    conn.close()


def send_whatsapp(target: str, message: str):
    token = os.getenv('FONNTE_TOKEN', '').strip()
    if not token or not target:
        return {'sent': False, 'reason': 'FONNTE_TOKEN atau nomor WhatsApp belum diatur'}

    try:
        response = requests.post(
            'https://api.fonnte.com/send',
            headers={'Authorization': token},
            data={
                'target': target,
                'message': message,
                'countryCode': os.getenv('FONNTE_COUNTRY_CODE', '62'),
            },
            timeout=20,
        )
        ok = response.ok
        try:
            body = response.json()
        except Exception:
            body = {'raw': response.text}
        return {'sent': ok, 'response': body, 'http_status': response.status_code}
    except requests.RequestException as exc:
        return {'sent': False, 'reason': str(exc)}


@app.get('/')
def home():
    conn = db()
    students = conn.execute('SELECT * FROM siswa ORDER BY nama COLLATE NOCASE').fetchall()
    
    # Hitung absensi hari ini
    today = now_local().strftime('%Y-%m-%d')
    attended_today = conn.execute(
        "SELECT DISTINCT siswa_id FROM absensi WHERE tanggal = ?", (today,)
    ).fetchall()
    attended_ids = {row['siswa_id'] for row in attended_today}
    
    students_data = []
    for s in students:
        s_dict = dict(s)
        s_dict['sudah_absen'] = s_dict['id'] in attended_ids
        students_data.append(s_dict)

    conn.close()
    return render_template('admin.html', students=students_data, attendance_today=len(attended_ids))


@app.get('/api/health')
def health():
    conn = db()
    total = conn.execute('SELECT COUNT(*) AS c FROM siswa WHERE aktif=1').fetchone()['c']
    today = now_local().strftime('%Y-%m-%d')
    hadir = conn.execute('SELECT COUNT(*) AS c FROM absensi WHERE tanggal=?', (today,)).fetchone()['c']
    conn.close()
    return jsonify({'ok': True, 'timezone': TIMEZONE, 'total_siswa_aktif': total, 'hadir_hari_ini': hadir})


@app.post('/scan')
def scan():
    data = request.get_json(silent=True) or {}
    nisn = str(data.get('nisn', '')).strip()
    device_id = str(data.get('device_id', '')).strip()[:100]

    if not nisn:
        return jsonify({'success': False, 'code': 'NISN_REQUIRED', 'message': 'NISN kosong.'}), 400

    conn = db()
    siswa = conn.execute(
        'SELECT * FROM siswa WHERE nisn=? AND aktif=1 LIMIT 1',
        (nisn,)
    ).fetchone()

    if not siswa:
        conn.close()
        return jsonify({'success': False, 'code': 'STUDENT_NOT_FOUND', 'message': 'NISN tidak terdaftar atau siswa tidak aktif.'}), 404

    current = now_local()
    tanggal = current.strftime('%Y-%m-%d')
    jam = current.strftime('%H:%M:%S')

    existing = conn.execute(
        '''SELECT id, jam_masuk, status FROM absensi WHERE siswa_id=? AND tanggal=? LIMIT 1''',
        (siswa['id'], tanggal)
    ).fetchone()

    if existing:
        conn.close()
        return jsonify({
            'success': True,
            'code': 'ALREADY_ATTENDED',
            'message': f"{siswa['nama']} sudah absen hari ini pada {existing['jam_masuk']}.",
            'student': dict(siswa),
            'attendance': dict(existing),
        })

    try:
        cur = conn.execute(
            '''INSERT INTO absensi (siswa_id, tanggal, jam_masuk, status, device_id, created_at)
               VALUES (?, ?, ?, 'Hadir', ?, ?)''',
            (siswa['id'], tanggal, jam, device_id, current.isoformat())
        )
        conn.commit()
        attendance_id = cur.lastrowid
    except sqlite3.IntegrityError:
        existing = conn.execute(
            'SELECT id, jam_masuk, status FROM absensi WHERE siswa_id=? AND tanggal=?',
            (siswa['id'], tanggal)
        ).fetchone()
        conn.close()
        return jsonify({
            'success': True,
            'code': 'ALREADY_ATTENDED',
            'message': f"{siswa['nama']} sudah absen hari ini.",
            'student': dict(siswa),
            'attendance': dict(existing) if existing else None,
        })

    wa_result = None
    whatsapp = (siswa['whatsapp_ortu'] or '').strip()
    if whatsapp:
        message = (
            f"PEMBERITAHUAN ABSENSI SEKOLAH\n\n"
            f"Nama: {siswa['nama']}\n"
            f"NISN: {siswa['nisn']}\n"
            f"Kelas: {siswa['kelas']}\n"
            f"Status: HADIR\n"
            f"Tanggal: {current.strftime('%d-%m-%Y')}\n"
            f"Jam masuk: {jam}\n\n"
            f"Pesan ini dikirim otomatis oleh sistem absensi sekolah."
        )
        wa_result = send_whatsapp(whatsapp, message)

    conn.close()
    return jsonify({
        'success': True,
        'code': 'ATTENDANCE_RECORDED',
        'message': f"Absensi {siswa['nama']} berhasil dicatat pada {jam}.",
        'student': {
            'nisn': siswa['nisn'],
            'nama': siswa['nama'],
            'kelas': siswa['kelas'],
        },
        'attendance': {
            'id': attendance_id,
            'tanggal': tanggal,
            'jam_masuk': jam,
            'status': 'Hadir',
        },
        'whatsapp': wa_result,
    }), 201


@app.get('/api/attendance/today')
def attendance_today():
    tanggal = now_local().strftime('%Y-%m-%d')
    conn = db()
    rows = conn.execute('''
        SELECT a.id, a.tanggal, a.jam_masuk, a.status, a.device_id,
               s.nisn, s.nama, s.kelas
        FROM absensi a
        JOIN siswa s ON s.id=a.siswa_id
        WHERE a.tanggal=?
        ORDER BY a.jam_masuk ASC
    ''', (tanggal,)).fetchall()
    conn.close()
    return jsonify({'date': tanggal, 'data': [dict(r) for r in rows]})


@app.post('/admin/siswa')
def add_student():
    nisn = request.form.get('nisn', '').strip()
    nama = request.form.get('nama', '').strip()
    kelas = request.form.get('kelas', '').strip()
    wa = request.form.get('whatsapp_ortu', '').strip()
    if not nisn or not nama or not kelas:
        flash('NISN, nama, dan kelas wajib diisi.', 'error')
        return redirect(url_for('home'))
    conn = db()
    try:
        conn.execute(
            'INSERT INTO siswa (nisn,nama,kelas,whatsapp_ortu,created_at) VALUES (?,?,?,?,?)',
            (nisn, nama, kelas, wa, now_local().isoformat())
        )
        conn.commit()
        flash('Siswa berhasil ditambahkan.', 'success')
    except sqlite3.IntegrityError:
        flash('NISN sudah terdaftar.', 'error')
    finally:
        conn.close()
    return redirect(url_for('home'))


@app.post('/admin/siswa/<int:siswa_id>/toggle')
def toggle_student(siswa_id):
    conn = db()
    row = conn.execute('SELECT aktif FROM siswa WHERE id=?', (siswa_id,)).fetchone()
    if row:
        conn.execute('UPDATE siswa SET aktif=? WHERE id=?', (0 if row['aktif'] else 1, siswa_id))
        conn.commit()
    conn.close()
    return redirect(url_for('home'))


@app.get('/admin/export-today')
def export_today():
    from csv import writer
    from io import StringIO
    tanggal = now_local().strftime('%Y-%m-%d')
    conn = db()
    rows = conn.execute('''
        SELECT s.nisn,s.nama,s.kelas,a.tanggal,a.jam_masuk,a.status
        FROM absensi a JOIN siswa s ON s.id=a.siswa_id WHERE a.tanggal=?
        ORDER BY a.jam_masuk
    ''', (tanggal,)).fetchall()
    conn.close()
    out = StringIO()
    w = writer(out)
    w.writerow(['NISN', 'Nama', 'Kelas', 'Tanggal', 'Jam Masuk', 'Status'])
    for r in rows:
        w.writerow([r['nisn'], r['nama'], r['kelas'], r['tanggal'], r['jam_masuk'], r['status']])
    return Response(out.getvalue(), mimetype='text/csv; charset=utf-8', headers={
        'Content-Disposition': f'attachment; filename="absensi-{tanggal}.csv"'
    })


@app.get('/api/qr/<nisn>')
def qr_payload(nisn):
    conn = db()
    row = conn.execute('SELECT nisn,nama,kelas FROM siswa WHERE nisn=? AND aktif=1', (nisn,)).fetchone()
    conn.close()
    if not row:
        return jsonify({'success': False, 'message': 'Siswa tidak ditemukan.'}), 404
    return jsonify({'success': True, 'qr_value': row['nisn'], 'student': dict(row)})


# ==========================================
# ROUTE PERBAIKAN: EDIT DATA SISWA
# ==========================================
@app.route('/admin/siswa/<int:id>/edit', methods=['POST'])
def edit_siswa(id):
    nisn = request.form.get('nisn', '').strip()
    nama = request.form.get('nama', '').strip()
    kelas = request.form.get('kelas', '').strip()
    wa = request.form.get('whatsapp_ortu', '').strip()
    
    conn = db()
    try:
        conn.execute(
            "UPDATE siswa SET nisn=?, nama=?, kelas=?, whatsapp_ortu=? WHERE id=?", 
            (nisn, nama, kelas, wa, id)
        )
        conn.commit()
        flash('Data siswa berhasil diperbarui!', 'success')
    except sqlite3.IntegrityError:
        flash('Gagal memperbarui: NISN sudah digunakan oleh siswa lain.', 'error')
    finally:
        conn.close()
    return redirect(url_for('home'))


# ==========================================
# ROUTE PERBAIKAN: HAPUS SISWA
# ==========================================
@app.route('/admin/siswa/<int:id>/delete', methods=['POST'])
def delete_siswa(id):
    conn = db()
    conn.execute("DELETE FROM siswa WHERE id=?", (id,))
    conn.commit()
    conn.close()
    flash('Siswa berhasil dihapus!', 'success')
    return redirect(url_for('home'))


if __name__ == '__main__':
    init_db()
    host = os.getenv('FLASK_HOST', '0.0.0.0')
    port = int(os.getenv('FLASK_PORT', '5000'))
    app.run(host=host, port=port, debug=False)