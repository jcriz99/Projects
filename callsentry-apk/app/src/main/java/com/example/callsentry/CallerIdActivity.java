package com.example.callsentry;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CallerIdActivity extends Activity {
    private TextView nameView, sourceView;
    private String number, carrierName;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setShowWhenLocked(true);

        number = getIntent().getStringExtra("number");
        carrierName = getIntent().getStringExtra("carrier_name");
        int reports = getIntent().getIntExtra("reports", 0);
        int verification = getIntent().getIntExtra("verification", 0);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);
        WindowManager.LayoutParams wp = getWindow().getAttributes();
        wp.width = WindowManager.LayoutParams.MATCH_PARENT;
        wp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        wp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        wp.y = dp(42);
        getWindow().setAttributes(wp);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(22), dp(18), dp(22), dp(18));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(14, 27, 48));
        bg.setCornerRadius(dp(22));
        bg.setStroke(dp(1), reports > 0 ? Color.rgb(255, 121, 90) : Color.rgb(70, 160, 255));
        card.setBackground(bg);
        card.setOnClickListener(v -> finish());

        String status;
        int statusColor;
        if (reports > 0) { status = reports == 1 ? "POSSIBLE SPAM • 1 FTC REPORT" : "POSSIBLE SPAM • " + reports + " FTC REPORTS"; statusColor = Color.rgb(255, 169, 87); }
        else if (verification == 2) { status = "CALLER ID FAILED VERIFICATION"; statusColor = Color.rgb(255, 125, 105); }
        else if (verification == 1) { status = "NUMBER VERIFIED BY CARRIER"; statusColor = Color.rgb(112, 220, 155); }
        else { status = "CALLSENTRY CALLER ID"; statusColor = Color.rgb(112, 186, 255); }

        card.addView(text(status, 13, statusColor));
        String initial = CnamLookup.meaningful(carrierName) ? carrierName.trim() : "Looking up caller…";
        nameView = text(initial, 25, Color.WHITE); nameView.setPadding(0, dp(7), 0, dp(3)); card.addView(nameView);
        card.addView(text(NotificationHelper.format(number), 18, Color.rgb(210, 220, 235)));
        sourceView = text(CnamLookup.meaningful(carrierName) ? "Name provided by carrier" : "Checking caller-name directory…", 13, Color.rgb(155, 174, 198));
        sourceView.setPadding(0, dp(7), 0, 0); card.addView(sourceView);
        card.addView(text("Tap this card to dismiss", 12, Color.rgb(125, 143, 168)));
        setContentView(card);

        new Thread(() -> {
            CnamLookup.Result r = CnamLookup.resolve(this, number, carrierName);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (r != null && CnamLookup.meaningful(r.name)) {
                    nameView.setText(r.name);
                    sourceView.setText("Caller name: " + r.source + ("FreeCNAM".equals(r.source) ? " • directory result, not identity verification" : ""));
                } else {
                    nameView.setText("Unknown caller");
                    sourceView.setText(CnamLookup.onlineEnabled(this) ? "No caller name was available" : "Online caller-name lookup is off");
                }
            });
        }).start();

        new Handler(Looper.getMainLooper()).postDelayed(() -> { if (!isFinishing()) finish(); }, 30000);
    }

    @Override public boolean onTouchEvent(android.view.MotionEvent e) {
        if (e.getAction() == android.view.MotionEvent.ACTION_OUTSIDE) finish();
        return super.onTouchEvent(e);
    }

    private TextView text(String s, int sp, int color) {
        TextView v = new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(color); v.setGravity(Gravity.START); return v;
    }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
