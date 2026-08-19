package com.example.callsentry;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

class NotificationHelper {
    private static final String CH_ACTIVE = "callsentry_active";
    private static final String CH_ALERTS = "callsentry_alerts";
    private static final int ID_ACTIVE = 1001;
    private static final int ID_SCAM_BASE = 2000;

    static void createChannels(Context c) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        NotificationChannel active = new NotificationChannel(CH_ACTIVE, "Protection status", NotificationManager.IMPORTANCE_LOW);
        active.setDescription("Shows when CallSentry call screening is enabled"); active.setShowBadge(false);
        NotificationChannel alerts = new NotificationChannel(CH_ALERTS, "Scam call alerts", NotificationManager.IMPORTANCE_HIGH);
        alerts.setDescription("Alerts when CallSentry blocks a likely scam call");
        nm.createNotificationChannel(active); nm.createNotificationChannel(alerts);
    }

    static boolean canNotify(Context c) {
        return Build.VERSION.SDK_INT < 33 || c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    static boolean roleHeld(Context c) {
        if (Build.VERSION.SDK_INT < 29) return false;
        RoleManager rm = c.getSystemService(RoleManager.class);
        return rm != null && rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
    }

    static void syncProtectionState(Context c) {
        createChannels(c);
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (!roleHeld(c)) { nm.cancel(ID_ACTIVE); return; }
        showProtection(c);
    }

    static void showProtection(Context c) {
        if (!canNotify(c)) return;
        createChannels(c);
        Intent open = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 1, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = new Notification.Builder(c, CH_ACTIVE)
                .setSmallIcon(com.example.callsentry.R.drawable.ic_stat_callsentry)
                .setContentTitle("CallSentry protection active")
                .setContentText("Screening unknown calls for scams and caller identity")
                .setContentIntent(pi).setOngoing(true).setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_SERVICE).setVisibility(Notification.VISIBILITY_PUBLIC);
        c.getSystemService(NotificationManager.class).notify(ID_ACTIVE, b.build());
    }

    static void showScamBlocked(Context c, String number, String name, int reports, boolean verificationFailed) {
        if (!canNotify(c)) return;
        createChannels(c);
        String who = CnamLookup.meaningful(name) ? name.trim() : format(number);
        StringBuilder detail = new StringBuilder();
        if (reports > 0) detail.append(reports).append(" recent FTC complaint").append(reports == 1 ? "" : "s");
        if (verificationFailed) { if (detail.length() > 0) detail.append(" • "); detail.append("caller ID failed verification"); }
        if (detail.length() == 0) detail.append("Matched CallSentry scam rules");

        Intent open = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 2, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = new Notification.Builder(c, CH_ALERTS)
                .setSmallIcon(com.example.callsentry.R.drawable.ic_stat_callsentry)
                .setContentTitle("Likely scam call blocked")
                .setContentText(who + " — " + detail)
                .setStyle(new Notification.BigTextStyle().bigText(who + "\n" + format(number) + "\n" + detail))
                .setContentIntent(pi).setAutoCancel(true).setCategory(Notification.CATEGORY_CALL)
                .setPriority(Notification.PRIORITY_HIGH).setVisibility(Notification.VISIBILITY_PUBLIC);
        c.getSystemService(NotificationManager.class).notify(ID_SCAM_BASE + (int)(System.currentTimeMillis() % 1000), b.build());
    }

    static String format(String raw) {
        String n = ScamDb.norm(raw);
        if (n.length() == 10) return String.format("(%s) %s-%s", n.substring(0,3), n.substring(3,6), n.substring(6));
        return raw == null || raw.isEmpty() ? "Unknown number" : raw;
    }
}
