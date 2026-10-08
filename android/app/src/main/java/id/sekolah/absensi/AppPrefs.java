package id.sekolah.absensi;

import android.content.Context;
import android.content.SharedPreferences;

/** Penyimpanan alamat server & API key perangkat. */
final class AppPrefs {
    private static final String PREFS = "absensi_prefs";
    private static final String KEY_SERVER = "server_url";
    private static final String KEY_API = "api_key";

    private final SharedPreferences sp;

    AppPrefs(Context context) {
        sp = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    String server() {
        return normalizeServer(sp.getString(KEY_SERVER, ""));
    }

    String apiKey() {
        return sp.getString(KEY_API, "").trim();
    }

    boolean isConfigured() {
        return !server().isEmpty() && !apiKey().isEmpty();
    }

    void save(String server, String apiKey) {
        sp.edit()
                .putString(KEY_SERVER, normalizeServer(server))
                .putString(KEY_API, apiKey == null ? "" : apiKey.trim())
                .apply();
    }

    static String normalizeServer(String value) {
        String s = value == null ? "" : value.trim();
        if (s.isEmpty()) return "";
        if (!s.startsWith("http://") && !s.startsWith("https://")) s = "http://" + s;
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }
}
