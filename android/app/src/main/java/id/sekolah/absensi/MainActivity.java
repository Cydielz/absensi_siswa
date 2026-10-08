package id.sekolah.absensi;

<<<<<<< HEAD
import android.content.SharedPreferences;
=======
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
>>>>>>> 200af4a (Absensi Siswa v2)
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
<<<<<<< HEAD
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
=======
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
>>>>>>> 200af4a (Absensi Siswa v2)
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;

import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Layar scan KONTINU: kamera selalu aktif, setiap kartu siswa yang terbaca langsung dikirim ke server.
 * Hasil tampil sebagai kartu berwarna besar (hijau/kuning/biru/merah) + foto siswa bila ada.
 */
public class MainActivity extends AppCompatActivity {
    private static final int REQ_CAMERA = 100;
    private static final long SAME_CODE_COOLDOWN_MS = 6000;
    private static final long RESULT_VISIBLE_MS = 5000;
    private static final String SETUP_PREFIX = "ABSEN-SETUP:";

<<<<<<< HEAD
    private EditText serverInput;
    private TextView resultText;
    private ProgressBar progress;
    private Button saveButton;
    private Button scanButton;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private SharedPreferences prefs;
=======
    private static final int C_GREEN = Color.rgb(15, 138, 95);
    private static final int C_AMBER = Color.rgb(217, 119, 6);
    private static final int C_BLUE = Color.rgb(37, 99, 235);
    private static final int C_RED = Color.rgb(198, 40, 40);
    private static final int C_SLATE = Color.rgb(51, 65, 85);

    private AppPrefs prefs;
    private DecoratedBarcodeView barcodeView;
    private LinearLayout resultCard;
    private ImageView ivPhoto;
    private TextView tvStatus;
    private TextView tvName;
    private TextView tvDetail;
    private TextView tvServer;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable clearRunnable = this::showIdle;
    private ToneGenerator tone;

    private boolean busy = false;
    private boolean askedPermission = false;
    private String lastText = "";
    private long lastTime = 0;
    private int successCount = 0;

    private final BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            String text = result.getText();
            if (text == null || busy) return;
            text = text.trim();
            long now = SystemClock.elapsedRealtime();
            if (text.equals(lastText) && now - lastTime < SAME_CODE_COOLDOWN_MS) return;
            lastText = text;
            lastTime = now;
            handleScan(text);
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
        }
    };
>>>>>>> 200af4a (Absensi Siswa v2)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
<<<<<<< HEAD
        
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
=======
        prefs = new AppPrefs(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        try {
            tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 90);
        } catch (RuntimeException e) {
            tone = null;
        }
        buildUi();
        barcodeView.getBarcodeView().setDecoderFactory(
                new DefaultDecoderFactory(Collections.singletonList(BarcodeFormat.QR_CODE)));
        barcodeView.setStatusText("");
        barcodeView.decodeContinuous(callback);
        showIdle();
    }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, float sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(15, 23, 42));

        // --- header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setBackgroundColor(Color.rgb(11, 61, 145));
        header.setPadding(dp(14), dp(10), dp(14), dp(8));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("ABSENSI SISWA", 18, Color.WHITE, true);
        bar.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView settingsBtn = text("\u2699 Pengaturan", 14, Color.WHITE, true);
        settingsBtn.setPadding(dp(10), dp(8), dp(10), dp(8));
        settingsBtn.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        bar.addView(settingsBtn);
        header.addView(bar);

        tvServer = text("", 12, Color.rgb(191, 215, 245), false);
        header.addView(tvServer);
        root.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // --- kamera
        barcodeView = new DecoratedBarcodeView(this);
        root.addView(barcodeView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        // --- kartu hasil
        resultCard = new LinearLayout(this);
        resultCard.setOrientation(LinearLayout.HORIZONTAL);
        resultCard.setGravity(Gravity.CENTER_VERTICAL);
        resultCard.setPadding(dp(14), dp(14), dp(14), dp(14));

        ivPhoto = new ImageView(this);
        ivPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams photoLp = new LinearLayout.LayoutParams(dp(92), dp(112));
        photoLp.rightMargin = dp(14);
        resultCard.addView(ivPhoto, photoLp);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        tvStatus = text("", 24, Color.WHITE, true);
        tvName = text("", 19, Color.WHITE, true);
        tvDetail = text("", 15, Color.WHITE, false);
        info.addView(tvStatus);
        info.addView(tvName);
        info.addView(tvDetail);
        resultCard.addView(info, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(dp(10), dp(10), dp(10), dp(10));
        resultCard.setMinimumHeight(dp(140));
        root.addView(resultCard, cardLp);

        setContentView(root);
    }

    // ------------------------------------------------------------ tampilan hasil
    private void setCard(int color, String status, String name, String detail, Bitmap photo) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(14));
        resultCard.setBackground(bg);
        tvStatus.setText(status);
        tvName.setText(name);
        tvName.setVisibility(name.isEmpty() ? View.GONE : View.VISIBLE);
        tvDetail.setText(detail);
        if (photo != null) {
            ivPhoto.setImageBitmap(photo);
            ivPhoto.setVisibility(View.VISIBLE);
        } else {
            ivPhoto.setImageDrawable(null);
            ivPhoto.setVisibility(View.GONE);
        }
    }

    private void showIdle() {
        if (!prefs.isConfigured()) {
            setCard(C_SLATE, "BELUM DIATUR",
                    "", "Buka \u2699 Pengaturan, atau scan QR \"Hubungkan HP Scanner\" dari halaman admin.", null);
        } else {
            setCard(C_SLATE, "SIAP SCAN", "", "Arahkan kamera ke QR pada kartu siswa.", null);
        }
    }

    private void scheduleClear() {
        handler.removeCallbacks(clearRunnable);
        handler.postDelayed(clearRunnable, RESULT_VISIBLE_MS);
    }

    private void refreshHeader() {
        String server = prefs.server();
        String s = server.isEmpty() ? "Server: belum diatur" : "Server: " + server;
        tvServer.setText(s + "   \u2022   Tercatat sesi ini: " + successCount);
    }

    // ------------------------------------------------------------------ scan
    private void handleScan(String text) {
        if (text.startsWith(SETUP_PREFIX)) {
            applySetup(text.substring(SETUP_PREFIX.length()));
            return;
        }
        if (!prefs.isConfigured()) {
            feedback(false);
            setCard(C_RED, "BELUM DIATUR", "", "Buka \u2699 Pengaturan atau scan QR setup dari halaman admin.", null);
            scheduleClear();
            return;
        }
        busy = true;
        handler.removeCallbacks(clearRunnable);
        setCard(C_SLATE, "MEMPROSES\u2026", "", "", null);
>>>>>>> 200af4a (Absensi Siswa v2)

        final String server = prefs.server();
        final String key = prefs.apiKey();
        final String device = deviceId();
        executor.execute(() -> {
            ApiClient.ScanResult r = ApiClient.scan(server, key, device, text);
            Bitmap photo = null;
            if (!r.networkError && !r.fotoUrl.isEmpty()) {
                photo = ApiClient.fetchBitmap(server + r.fotoUrl, key);
            }
            final Bitmap finalPhoto = photo;
            runOnUiThread(() -> showResult(r, finalPhoto));
        });
    }

    private void showResult(ApiClient.ScanResult r, Bitmap photo) {
        busy = false;
        if (r.networkError) {
            feedback(false);
            lastText = ""; // izinkan scan ulang segera setelah koneksi pulih
            setCard(C_RED, "GAGAL TERHUBUNG", "", r.message, null);
        } else if (r.http == 401) {
            feedback(false);
            setCard(C_RED, "API KEY SALAH", "", "Periksa \u2699 Pengaturan atau scan ulang QR setup dari admin.", null);
        } else if ("ATTENDANCE_RECORDED".equals(r.code)) {
            successCount++;
            refreshHeader();
            feedback(true);
            String detail = "Kelas " + r.kelas + "  \u2022  " + r.jam
                    + (r.waQueued ? "\nWA orang tua: diantrekan" : "");
            if ("Terlambat".equals(r.status)) {
                setCard(C_AMBER, "TERLAMBAT", r.nama, detail, photo);
            } else {
                setCard(C_GREEN, "HADIR", r.nama, detail, photo);
            }
        } else if ("ALREADY_ATTENDED".equals(r.code)) {
            feedback(false);
            setCard(C_BLUE, "SUDAH ABSEN", r.nama, "Kelas " + r.kelas + "  \u2022  tercatat " + r.jam, photo);
        } else {
            feedback(false);
            lastText = "";
            String title;
            switch (r.code) {
                case "QR_INVALID":
                    title = "QR TIDAK VALID";
                    break;
                case "QR_LEGACY":
                    title = "KARTU LAMA";
                    break;
                case "STUDENT_NOT_FOUND":
                    title = "TIDAK TERDAFTAR";
                    break;
                default:
                    title = "DITOLAK";
            }
            String msg = r.message.isEmpty() ? "HTTP " + r.http : r.message;
            setCard(C_RED, title, "", msg, null);
        }
        scheduleClear();
    }

<<<<<<< HEAD
    private void showScanResponse(String nisn, int httpStatus, String raw) {
        if (progress != null) progress.setVisibility(View.GONE);
        if (resultText == null) return;

=======
    private void applySetup(String json) {
>>>>>>> 200af4a (Absensi Siswa v2)
        try {
            JSONObject o = new JSONObject(json);
            String url = o.getString("u");
            String key = o.getString("k");
            prefs.save(url, key);
            refreshHeader();
            feedback(true);
            setCard(C_GREEN, "PENGATURAN TERSIMPAN", "", "Server: " + prefs.server(), null);
        } catch (Exception e) {
            feedback(false);
            setCard(C_RED, "QR SETUP RUSAK", "", "Buat ulang QR dari halaman admin.", null);
        }
        scheduleClear();
    }

    private String deviceId() {
        String id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        return (id == null || id.isEmpty()) ? "ANDROID" : id;
    }

    private void feedback(boolean success) {
        if (tone != null) {
            tone.startTone(success ? ToneGenerator.TONE_PROP_ACK : ToneGenerator.TONE_PROP_NACK, success ? 120 : 250);
        }
        Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (v == null || !v.hasVibrator()) return;
        long ms = success ? 60 : 220;
        if (Build.VERSION.SDK_INT >= 26) {
            v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(ms);
        }
    }

    // ------------------------------------------------------ izin kamera & siklus
    private void resumeCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            barcodeView.resume();
        } else if (!askedPermission) {
            askedPermission = true;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
        } else {
            setCard(C_RED, "IZIN KAMERA DITOLAK", "", "Aktifkan izin Kamera untuk aplikasi ini di Pengaturan HP.", null);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA) resumeCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshHeader();
        if (!busy) showIdle();
        resumeCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
        handler.removeCallbacks(clearRunnable);
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        if (tone != null) tone.release();
        super.onDestroy();
    }
}