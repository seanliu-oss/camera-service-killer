package com.example.cameraservicekiller;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.List;

/** Shared kill/stop logic used by both WorkManager and foreground service mode. */
public final class KillerActions {

    private static final String TAG = "KillerActions";

    private static final String GOOGLE_CAMERA_PACKAGE = "com.google.android.GoogleCamera";
    private static final String NOOP_PREWARM_SERVICE =
            "com.google.android.apps.camera.prewarm.NoOpPrewarmService";

    private static final String[] TARGET_PACKAGES = {
            "com.google.android.GoogleCamera",
            "com.android.camera2",
            "com.android.camera",
            "com.sec.android.app.camera",
            "com.huawei.camera",
            "com.oppo.camera",
            "com.coloros.camera",
    };

    private KillerActions() {
    }

    public static String runStopAttempt(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) {
            Log.e(TAG, "ActivityManager unavailable - skipping run.");
            return "am=unavailable";
        }

        String outcome = attemptDirectServiceStop(context);
        boolean anyKilled = false;

        List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
        if (processes != null) {
            for (ActivityManager.RunningAppProcessInfo proc : processes) {
                if (isTargetPackage(proc.processName)) {
                    am.killBackgroundProcesses(proc.processName);
                    anyKilled = true;
                }
            }
        }

        for (String pkg : TARGET_PACKAGES) {
            am.killBackgroundProcesses(pkg);
        }

        return outcome + " / cleanup=" + (anyKilled ? "yes" : "no");
    }

    public static String attemptDirectServiceStop(Context context) {
        Intent intent = new Intent();
        intent.setClassName(GOOGLE_CAMERA_PACKAGE, NOOP_PREWARM_SERVICE);
        try {
            boolean stopped = context.stopService(intent);
            return "stopService=" + stopped;
        } catch (SecurityException e) {
            Log.w(TAG, "Direct stopService denied by platform/app permissions.", e);
            return "stopService=denied";
        } catch (Exception e) {
            Log.w(TAG, "Direct stopService failed unexpectedly.", e);
            return "stopService=error";
        }
    }

    private static boolean isTargetPackage(String processName) {
        if (processName == null) return false;
        for (String pkg : TARGET_PACKAGES) {
            if (processName.equals(pkg) || processName.startsWith(pkg + ":")) {
                return true;
            }
        }
        return false;
    }
}

