package com.example.cameraservicekiller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Restores the periodic kill schedule after a device reboot or app update.
 * Declared in the manifest with RECEIVE_BOOT_COMPLETED and MY_PACKAGE_REPLACED.
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "Received: " + action + " – re-scheduling killer.");
        // KEEP policy: don't reset the timer if work already exists.
        WorkScheduler.schedule(context, false);
    }
}

