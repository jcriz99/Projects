package com.example.callsentry;

import android.content.Context;
import android.content.SharedPreferences;

final class ProtectionState {
    private static final String PREFS = "callsentry_settings";
    private static final String KEY_ENABLED = "protection_enabled";

    private ProtectionState() { }

    static boolean enabled(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, true);
    }

    static void setEnabled(Context c, boolean enabled) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        p.edit().putBoolean(KEY_ENABLED, enabled).apply();
        NotificationHelper.syncProtectionState(c);
    }
}
