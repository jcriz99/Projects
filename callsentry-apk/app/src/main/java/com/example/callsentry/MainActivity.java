package com.example.callsentry;

import android.Manifest;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView status, stats, lookupResult;
    private Switch protectionToggle;
    private ScamDb db;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        db = new ScamDb(this);
        build();
        NotificationHelper.createChannels(this);
        SyncJob.schedule(this);
        requestNotificationPermission();
        new Thread(() -> {
            int n = FtcSync.sync(this, 14);
            runOnUiThread(() -> refresh("Updated FTC data: " + n + " reports imported"));
        }).start();
    }

    private void build() {
        ScrollView sv = new ScrollView(this); sv.setBackgroundColor(Color.rgb(8, 16, 29));
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(dp(24), dp(28), dp(24), dp(36)); sv.addView(l);

        ImageView icon = new ImageView(this); icon.setImageResource(com.example.callsentry.R.drawable.callsentry_icon);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(92), dp(92)); ip.gravity = Gravity.CENTER_HORIZONTAL; icon.setLayoutParams(ip); l.addView(icon);
        TextView title = t("CallSentry", 30, Color.WHITE); title.setGravity(Gravity.CENTER_HORIZONTAL); l.addView(title);
        TextView sub = t("Scam blocking + caller identification", 16, Color.rgb(160, 183, 213)); sub.setGravity(Gravity.CENTER_HORIZONTAL); l.addView(sub);

        status = t("", 18, Color.WHITE); status.setPadding(0, dp(22), 0, dp(10)); l.addView(status);

        protectionToggle = new Switch(this);
        protectionToggle.setText("CallSentry protection");
        protectionToggle.setTextColor(Color.WHITE);
        protectionToggle.setTextSize(18);
        protectionToggle.setPadding(0, dp(8), 0, dp(8));
        protectionToggle.setChecked(ProtectionState.enabled(this));
        protectionToggle.setOnCheckedChangeListener((button, checked) -> {
            ProtectionState.setEnabled(this, checked);
            if (checked && !NotificationHelper.roleHeld(this)) enableRole();
            refresh(checked ? "Protection turned on" : "Protection turned off");
        });
        l.addView(protectionToggle);
        l.addView(t("Turn this OFF any time to pause CallSentry without uninstalling it. While off, every call passes through untouched: no blocking, scam alerts, caller-ID card, or caller-name lookup is performed. Turn it back on to resume protection.", 14, Color.rgb(166, 185, 210)));

        Button enable = button("Enable / reselect CallSentry as screening app"); enable.setOnClickListener(v -> enableRole()); l.addView(enable);
        stats = t("", 15, Color.rgb(190, 204, 224)); l.addView(stats);

        section(l, "Caller identification");
        Switch online = new Switch(this); online.setText("Look up caller names online when carrier name is missing"); online.setTextColor(Color.WHITE); online.setTextSize(15); online.setChecked(CnamLookup.onlineEnabled(this));
        online.setOnCheckedChangeListener((b, checked) -> CnamLookup.setOnlineEnabled(this, checked)); l.addView(online);
        l.addView(t("CallSentry first uses any caller name supplied by your carrier. If none is available, it can query FreeCNAM. This sends only the unknown phone number to FreeCNAM and may return a business or caller name; CNAM is caller-ID data, not proof of identity.", 13, Color.rgb(150, 169, 194)));

        EditText lookup = new EditText(this); lookup.setHint("Enter a phone number to test caller ID"); lookup.setHintTextColor(Color.rgb(110, 130, 155)); lookup.setTextColor(Color.WHITE); lookup.setInputType(InputType.TYPE_CLASS_PHONE); l.addView(lookup);
        Button lookupButton = button("Look up number"); l.addView(lookupButton);
        lookupResult = t("", 15, Color.rgb(163, 210, 255)); l.addView(lookupResult);
        lookupButton.setOnClickListener(v -> {
            String n = ScamDb.norm(lookup.getText().toString());
            if (n.length() != 10) { lookupResult.setText("Enter a 10-digit US/Canada number."); return; }
            lookupResult.setText("Looking up…");
            new Thread(() -> {
                CnamLookup.Result r = CnamLookup.resolve(this, n, null);
                runOnUiThread(() -> lookupResult.setText(r == null ? "No caller name found for " + NotificationHelper.format(n) : r.name + "\nSource: " + r.source));
            }).start();
        });

        section(l, "Scam protection");
        l.addView(t("Known high-risk calls are rejected before they ring. You receive a CallSentry alert showing the number and why it was blocked. Calls with 1–2 recent FTC complaints are labeled as possible spam but still allowed; 3+ recent complaints are blocked. A failed carrier verification plus an FTC complaint is also blocked.", 14, Color.rgb(190, 204, 224)));
        Button sync = button("Refresh FTC scam database now"); sync.setOnClickListener(v -> new Thread(() -> {
            int n = FtcSync.sync(this, 30); runOnUiThread(() -> refresh("Refresh complete: " + n + " reports imported"));
        }).start()); l.addView(sync);

        section(l, "Status notification");
        l.addView(t("The ongoing CallSentry notification appears only while protection is ON and CallSentry is Android's selected screening app. Turning protection OFF removes it immediately.", 14, Color.rgb(190, 204, 224)));

        section(l, "Android screening provider");
        l.addView(t("The master switch above pauses all CallSentry processing instantly. Android may still list CallSentry as the selected caller-ID/spam provider. If you want Samsung or another app to become the provider instead, use the system settings below.", 13, Color.rgb(150, 169, 194)));
        Button settings = button("Android caller ID / spam settings"); settings.setOnClickListener(v -> {
            try { startActivity(new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)); } catch (Exception ignored) { }
        }); l.addView(settings);
        setContentView(sv); refresh("");
    }

    private void section(LinearLayout l, String s) { TextView v = t(s, 20, Color.WHITE); v.setPadding(0, dp(24), 0, dp(8)); l.addView(v); }
    private TextView t(String s, int z, int color) { TextView v = new TextView(this); v.setText(s); v.setTextSize(z); v.setTextColor(color); v.setPadding(0, dp(7), 0, dp(7)); return v; }
    private Button button(String s) { Button b = new Button(this); b.setText(s); b.setAllCaps(false); return b; }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 77);
    }

    private void enableRole() {
        if (Build.VERSION.SDK_INT >= 29) {
            RoleManager rm = getSystemService(RoleManager.class);
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING), 42);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (protectionToggle != null && protectionToggle.isChecked() != ProtectionState.enabled(this)) protectionToggle.setChecked(ProtectionState.enabled(this));
        refresh("");
        NotificationHelper.syncProtectionState(this);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 42) { refresh(""); NotificationHelper.syncProtectionState(this); }
    }

    @Override public void onRequestPermissionsResult(int req, String[] perms, int[] results) {
        super.onRequestPermissionsResult(req, perms, results);
        if (req == 77) NotificationHelper.syncProtectionState(this);
    }

    private void refresh(String msg) {
        boolean held = NotificationHelper.roleHeld(this);
        boolean enabled = ProtectionState.enabled(this);
        String state;
        if (held && enabled) state = "✓ Protection is ACTIVE";
        else if (held) state = "⏸ Protection is OFF — calls pass through untouched";
        else if (enabled) state = "Protection is ON, but CallSentry is not Android's active screening app";
        else state = "⏸ Protection is OFF";
        status.setText(state + (msg.isEmpty() ? "" : "\n" + msg));
        stats.setText("Local FTC reputation records: " + db.totalReports() + "\nLast refresh: " + db.lastSyncText() + "\nCaller-name lookup: " + (CnamLookup.onlineEnabled(this) ? "Carrier + FreeCNAM" : "Carrier only"));
    }
}
