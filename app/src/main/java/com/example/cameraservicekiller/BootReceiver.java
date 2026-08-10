package com.example.cameraservicekiller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Restores scheduling and (if enabled) foreground reliability mode after reboot/update.
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "Received: " + action + " - restoring killer state.");

        WorkScheduler.schedule(context, false);

        if (KillerForegroundService.isEnabled(context)) {
            KillerForegroundService.requestStart(context);
        }
    }
}
