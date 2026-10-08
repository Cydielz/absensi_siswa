package id.sekolah.absensi;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

/** Klien HTTP sederhana ke backend Flask. Semua method BLOCKING: panggil dari thread latar belakang. */
final class ApiClient {
    private ApiClient() {}

    static final class ScanResult {
        boolean networkError;
        int http;
        String code = "";
        String message = "";
        String nama = "";
        String kelas = "";
        String nisn = "";
        String status = "";
        String jam = "";
        String fotoUrl = "";
        boolean waQueued;
    }

    static final class PingResult {
        boolean ok;
        String message = "";
    }

    static ScanResult scan(String server, String apiKey, String deviceId, String qr) {
        ScanResult r = new ScanResult();
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(server + "/scan").openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(10000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("X-API-Key", apiKey);

            JSONObject body = new JSONObject();
            body.put("qr", qr);
            body.put("device_id", deviceId);
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bytes);
            }
            r.http = conn.getResponseCode();
            InputStream stream = r.http >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String raw = readAll(stream);
            try {
                JSONObject o = new JSONObject(raw);
                r.code = o.optString("code", "");
                r.message = o.optString("message", "");
                JSONObject s = o.optJSONObject("student");
                if (s != null) {
                    r.nama = s.optString("nama", "");
                    r.kelas = s.optString("kelas", "");
                    r.nisn = s.optString("nisn", "");
                    r.fotoUrl = s.optString("foto_url", "");
                }
                JSONObject a = o.optJSONObject("attendance");
                if (a != null) {
                    r.status = a.optString("status", "");
                    r.jam = a.optString("jam_masuk", "");
                }
                JSONObject wa = o.optJSONObject("whatsapp");
                if (wa != null) r.waQueued = wa.optBoolean("queued", false);
            } catch (Exception parse) {
                r.code = "BAD_RESPONSE";
                r.message = "Respons server tidak dikenali (HTTP " + r.http + "). Periksa alamat server.";
            }
        } catch (Exception e) {
            r.networkError = true;
            r.message = friendly(e);
        } finally {
            if (conn != null) conn.disconnect();
        }
        return r;
    }

    static PingResult ping(String server, String apiKey) {
        PingResult p = new PingResult();
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(server + "/api/device/ping").openConnection();
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("X-API-Key", apiKey);
            int code = conn.getResponseCode();
            if (code == 401) {
                p.message = "Server terjangkau, tetapi API key SALAH.";
                return p;
            }
            if (code >= 400) {
                p.message = "Server membalas HTTP " + code + ". Periksa alamat server.";
                return p;
            }
            JSONObject o = new JSONObject(readAll(conn.getInputStream()));
            p.ok = o.optBoolean("ok", false);
            p.message = p.ok
                    ? "Terhubung \u2714  Sekolah: " + o.optString("sekolah", "-")
                    + "\nBatas terlambat: " + o.optString("batas_terlambat", "-")
                    : "Balasan server tidak dikenali.";
        } catch (Exception e) {
            p.message = friendly(e);
        } finally {
            if (conn != null) conn.disconnect();
        }
        return p;
    }

    static Bitmap fetchBitmap(String url, String apiKey) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(6000);
            conn.setRequestProperty("X-API-Key", apiKey);
            if (conn.getResponseCode() != 200) return null;
            try (InputStream in = conn.getInputStream()) {
                return BitmapFactory.decodeStream(in);
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String readAll(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    private static String friendly(Exception e) {
        if (e instanceof UnknownHostException) return "Alamat server tidak ditemukan. Periksa penulisan alamat.";
        if (e instanceof SocketTimeoutException) return "Server tidak merespons (timeout). Periksa Wi-Fi & firewall.";
        if (e instanceof ConnectException) return "Tidak bisa terhubung. Pastikan server menyala & satu Wi-Fi.";
        return "Gagal terhubung: " + e.getMessage();
    }
}
