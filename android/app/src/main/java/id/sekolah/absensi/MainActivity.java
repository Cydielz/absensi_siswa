package id.sekolah.absensi;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS = "absensi_prefs";
    private static final String KEY_SERVER = "server_url";
    private static final String DEFAULT_SERVER = "http://192.168.1.10:5000";

    private EditText serverInput;
    private TextView resultText;
    private ProgressBar progress;
    private Button saveButton;
    private Button scanButton;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 1. Inflate tampilan dari file activity_main.xml
        setContentView(R.layout.activity_main);

        // 2. Inisialisasi preferensi
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        // 3. Hubungkan komponen UI berdasarkan ID dari XML
        initViews();
    }

    private void initViews() {
        serverInput = findViewById(R.id.serverInput);
        saveButton = findViewById(R.id.saveButton);
        scanButton = findViewById(R.id.scanButton);
        progress = findViewById(R.id.progress);
        resultText = findViewById(R.id.resultText);

        // Muat alamat server yang tersimpan atau default
        if (serverInput != null) {
            serverInput.setText(prefs.getString(KEY_SERVER, DEFAULT_SERVER));
        }

        if (saveButton != null) {
            saveButton.setOnClickListener(v -> saveServerUrl());
        }

        if (scanButton != null) {
            scanButton.setOnClickListener(v -> startQrScanner());
        }
    }

    private void saveServerUrl() {
        if (serverInput == null) return;
        String url = normalizeServerUrl(serverInput.getText().toString());
        if (url.isEmpty()) {
            Toast.makeText(this, "Alamat server belum diisi", Toast.LENGTH_SHORT).show();
            return;
        }
        serverInput.setText(url);
        prefs.edit().putString(KEY_SERVER, url).apply();
        Toast.makeText(this, "Alamat server disimpan", Toast.LENGTH_SHORT).show();
    }

    private String normalizeServerUrl(String value) {
        String s = value == null ? "" : value.trim();
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }

    private void startQrScanner() {
        saveServerUrl();
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Arahkan kamera ke QR Code NISN siswa");
        integrator.setCameraId(0);
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result == null) return;
        if (result.getContents() == null) {
            if (resultText != null) {
                resultText.setText("Scan dibatalkan.\nSilakan tekan SCAN QR CODE lagi.");
            }
            return;
        }
        String nisn = extractNisn(result.getContents());
        if (nisn.isEmpty()) {
            if (resultText != null) {
                resultText.setText("QR terbaca, tetapi isinya tidak dikenali sebagai NISN.\n\nIsi QR: " + result.getContents());
            }
            return;
        }
        submitScan(nisn);
    }

    private String extractNisn(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.regionMatches(true, 0, "NISN:", 0, 5)) s = s.substring(5).trim();
        s = s.replaceAll("[^0-9]", "");
        return s;
    }

    private void submitScan(String nisn) {
        if (serverInput == null) return;
        String server = normalizeServerUrl(serverInput.getText().toString());
        if (server.isEmpty()) {
            if (resultText != null) resultText.setText("Alamat server belum diatur.");
            return;
        }
        prefs.edit().putString(KEY_SERVER, server).apply();
        if (progress != null) progress.setVisibility(View.VISIBLE);
        if (resultText != null) resultText.setText("Memproses NISN " + nisn + " ...");

        executor.execute(() -> {
            String response;
            int status = 0;
            try {
                URL url = new URL(server + "/scan");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(15000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");

                String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                if (deviceId == null || deviceId.isEmpty()) deviceId = "ANDROID";
                JSONObject body = new JSONObject();
                body.put("nisn", nisn);
                body.put("device_id", deviceId);

                byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(bytes);
                }
                status = conn.getResponseCode();
                InputStream stream = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
                response = readAll(stream);
                conn.disconnect();
            } catch (Exception e) {
                response = "NETWORK_ERROR:" + e.getMessage();
            }

            final int httpStatus = status;
            final String finalResponse = response;
            runOnUiThread(() -> showScanResponse(nisn, httpStatus, finalResponse));
        });
    }

    private String readAll(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    private void showScanResponse(String nisn, int httpStatus, String raw) {
        if (progress != null) progress.setVisibility(View.GONE);
        if (resultText == null) return;

        try {
            if (raw.startsWith("NETWORK_ERROR:")) {
                resultText.setText("GAGAL TERHUBUNG\n\nPastikan server Flask aktif dan HP berada di Wi-Fi yang sama.\n\nDetail: " + raw.substring(14));
                return;
            }
            JSONObject obj = new JSONObject(raw);
            String code = obj.optString("code", "");
            String message = obj.optString("message", "Tidak ada pesan.");
            String student = "";
            if (obj.has("student")) {
                JSONObject s = obj.getJSONObject("student");
                student = "\n\nNama: " + s.optString("nama", "-") +
                        "\nNISN: " + s.optString("nisn", nisn) +
                        "\nKelas: " + s.optString("kelas", "-");
            }
            String extra = "";
            if ("ATTENDANCE_RECORDED".equals(code)) {
                JSONObject a = obj.optJSONObject("attendance");
                if (a != null) extra = "\nJam masuk: " + a.optString("jam_masuk", "-");
            }
            String wa = "";
            if (obj.has("whatsapp")) {
                JSONObject w = obj.optJSONObject("whatsapp");
                if (w != null) wa = "\nWhatsApp: " + (w.optBoolean("sent", false) ? "notifikasi terkirim" : "tidak terkirim / belum dikonfigurasi");
            }
            resultText.setText(message + student + extra + wa + "\n\nHTTP " + httpStatus);
        } catch (Exception e) {
            resultText.setText("Respons server tidak dapat dibaca.\n\n" + raw);
        }
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}