package com.example.callsentry;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        SyncJob.schedule(context);
        NotificationHelper.syncProtectionState(context);
    }
}
