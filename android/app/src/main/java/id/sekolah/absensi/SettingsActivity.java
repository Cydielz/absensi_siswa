package id.sekolah.absensi;

import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Pengaturan alamat server & API key, plus tes koneksi. */
public class SettingsActivity extends AppCompatActivity {
    private AppPrefs prefs;
    private EditText etServer;
    private EditText etKey;
    private TextView tvResult;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new AppPrefs(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(243, 247, 251));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(Color.rgb(11, 61, 145));
        header.setPadding(dp(14), dp(12), dp(14), dp(12));
        TextView back = label("\u2190 Kembali", 15, Color.WHITE, true);
        back.setPadding(0, dp(4), dp(16), dp(4));
        back.setOnClickListener(v -> finish());
        header.addView(back);
        header.addView(label("Pengaturan", 18, Color.WHITE, true));
        root.addView(header);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView tip = label("Cara termudah: di komputer admin buka Pengaturan \u2192 \"Hubungkan HP Scanner\", "
                + "lalu arahkan kamera di layar scan ke QR tersebut. Atau isi manual di bawah.",
                13, Color.rgb(71, 85, 105), false);
        body.addView(tip);

        body.addView(label("Alamat server", 13, Color.rgb(100, 116, 139), false));
        etServer = new EditText(this);
        etServer.setSingleLine(true);
        etServer.setHint("http://192.168.1.10:5000");
        etServer.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        etServer.setText(prefs.server());
        body.addView(etServer);

        body.addView(label("API key", 13, Color.rgb(100, 116, 139), false));
        etKey = new EditText(this);
        etKey.setSingleLine(true);
        etKey.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etKey.setText(prefs.apiKey());
        body.addView(etKey);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setPadding(0, dp(12), 0, dp(8));
        Button save = new Button(this);
        save.setText("Simpan");
        save.setOnClickListener(v -> save());
        Button test = new Button(this);
        test.setText("Tes koneksi");
        test.setOnClickListener(v -> testConnection());
        buttons.addView(save, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        buttons.addView(test, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        body.addView(buttons);

        tvResult = label("", 15, Color.rgb(16, 35, 63), false);
        body.addView(tvResult);

        String id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        TextView dev = label("ID perangkat: " + (id == null ? "-" : id), 12, Color.rgb(100, 116, 139), false);
        dev.setPadding(0, dp(24), 0, 0);
        body.addView(dev);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);
    }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView label(String text, float sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setPadding(0, dp(6), 0, dp(2));
        return t;
    }

    private void save() {
        prefs.save(etServer.getText().toString(), etKey.getText().toString());
        etServer.setText(prefs.server());
        tvResult.setTextColor(Color.rgb(15, 138, 95));
        tvResult.setText("Tersimpan. Tekan \"Tes koneksi\" untuk memastikan.");
    }

    private void testConnection() {
        save();
        final String server = prefs.server();
        final String key = prefs.apiKey();
        if (server.isEmpty() || key.isEmpty()) {
            tvResult.setTextColor(Color.rgb(198, 40, 40));
            tvResult.setText("Alamat server dan API key wajib diisi.");
            return;
        }
        tvResult.setTextColor(Color.rgb(71, 85, 105));
        tvResult.setText("Menghubungi server\u2026");
        executor.execute(() -> {
            ApiClient.PingResult p = ApiClient.ping(server, key);
            runOnUiThread(() -> {
                tvResult.setTextColor(p.ok ? Color.rgb(15, 138, 95) : Color.rgb(198, 40, 40));
                tvResult.setText(p.message);
            });
        });
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
