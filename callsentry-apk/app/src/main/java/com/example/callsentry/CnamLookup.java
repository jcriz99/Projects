package com.example.callsentry;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

class CnamLookup {
    static final String PREFS = "callsentry_settings";
    static final String PREF_ONLINE_CNAM = "online_cnam";
    private static final long CACHE_MS = 30L * 24 * 60 * 60 * 1000;

    static class Result {
        final String name;
        final String source;
        Result(String name, String source) { this.name = name; this.source = source; }
    }

    static boolean onlineEnabled(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return p.getBoolean(PREF_ONLINE_CNAM, true);
    }

    static void setOnlineEnabled(Context c, boolean enabled) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(PREF_ONLINE_CNAM, enabled).apply();
    }

    static boolean meaningful(String value) {
        if (value == null) return false;
        String s = value.trim();
        if (s.isEmpty()) return false;
        String u = s.toUpperCase(Locale.US);
        return !(u.equals("UNKNOWN") || u.equals("UNAVAILABLE") || u.equals("OUT OF AREA") || u.equals("WIRELESS CALLER") || u.equals("CELL PHONE") || u.equals("MOBILE CALLER") || u.equals("PRIVATE") || u.equals("ANONYMOUS") || u.equals("INVALID NUMBER") || u.equals("TOLL FREE CALL"));
    }

    static Result resolve(Context c, String number, String carrierName) {
        String n = ScamDb.norm(number);
        ScamDb db = new ScamDb(c);
        if (meaningful(carrierName)) {
            String clean = clean(carrierName); db.saveCallerName(n, clean, "Carrier caller ID"); return new Result(clean, "Carrier caller ID");
        }
        Result cached = db.cachedCallerName(n, CACHE_MS);
        if (cached != null && meaningful(cached.name)) return cached;
        if (!onlineEnabled(c) || n.length() != 10) return null;

        HttpURLConnection h = null;
        try {
            URL url = new URL("https://freecnam.org/dip?q=" + n);
            h = (HttpURLConnection) url.openConnection();
            h.setConnectTimeout(1600); h.setReadTimeout(1800); h.setRequestProperty("User-Agent", "CallSentry/1.1 Android");
            if (h.getResponseCode() != 200) return null;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(h.getInputStream(), StandardCharsets.UTF_8))) {
                String name = r.readLine();
                if (!meaningful(name)) return null;
                name = clean(name); db.saveCallerName(n, name, "FreeCNAM"); return new Result(name, "FreeCNAM");
            }
        } catch (Exception ignored) { return null; }
        finally { if (h != null) h.disconnect(); }
    }

    private static String clean(String s) {
        s = s == null ? "" : s.trim().replaceAll("\\s+", " ");
        if (s.length() > 48) s = s.substring(0, 48);
        return s;
    }
}
