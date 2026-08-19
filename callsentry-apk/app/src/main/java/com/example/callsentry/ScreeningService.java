package com.example.callsentry;

import android.content.Intent;
import android.os.Build;
import android.telecom.Call;
import android.telecom.CallScreeningService;
import android.telecom.Connection;

public class ScreeningService extends CallScreeningService {
    @Override public void onScreenCall(Call.Details d) {
        // Master pause: remain a valid Android screening provider, but make no
        // scam/caller-ID decision and do not show any CallSentry UI or alerts.
        if (!ProtectionState.enabled(this)) {
            CallResponse pass = new CallResponse.Builder()
                    .setDisallowCall(false)
                    .setRejectCall(false)
                    .setSilenceCall(false)
                    .setSkipCallLog(false)
                    .setSkipNotification(false)
                    .build();
            respondToCall(d, pass);
            NotificationHelper.syncProtectionState(this);
            return;
        }

        String raw = d.getHandle() == null ? "" : d.getHandle().getSchemeSpecificPart();
        String n = ScamDb.norm(raw);
        int verify = Connection.VERIFICATION_STATUS_NOT_VERIFIED;
        if (Build.VERSION.SDK_INT >= 30) verify = d.getCallerNumberVerificationStatus();
        String carrierName = d.getCallerDisplayName();

        ScamDb db = new ScamDb(this);
        int reports = db.recentCount(n, 30);
        boolean failed = Build.VERSION.SDK_INT >= 30 && verify == Connection.VERIFICATION_STATUS_FAILED;
        boolean block = reports >= 3 || (failed && reports >= 1);

        CallResponse.Builder response = new CallResponse.Builder().setSkipCallLog(false).setSkipNotification(false);
        if (block) response.setDisallowCall(true).setRejectCall(true).setSilenceCall(false);
        else response.setDisallowCall(false).setRejectCall(false).setSilenceCall(false);
        respondToCall(d, response.build());

        db.log(n, block ? "BLOCK" : "ALLOW", reports, failed);
        NotificationHelper.showProtection(this);

        if (block) {
            NotificationHelper.showScamBlocked(this, n, carrierName, reports, failed);
        } else if (!n.isEmpty()) {
            Intent i = new Intent(this, CallerIdActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    .putExtra("number", n)
                    .putExtra("carrier_name", carrierName)
                    .putExtra("reports", reports)
                    .putExtra("verification", verify == Connection.VERIFICATION_STATUS_PASSED ? 1 : (failed ? 2 : 0));
            try { startActivity(i); } catch (Exception ignored) { }
        }
    }
}
