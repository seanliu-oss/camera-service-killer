package com.example.cameraservicekiller;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;

/**
 * Periodic worker that attempts to suppress camera prewarm activity.
 *
 * Runtime finding: killing the Google Camera process can cause NoOpPrewarmService
 * to respawn quickly, so this worker first attempts a direct service stop for the
 * known component and then falls back to killBackgroundProcesses for package cleanup.
 */
public class ServiceKillerWorker extends Worker {

    private static final String TAG = "ServiceKillerWorker";

    private static final String GOOGLE_CAMERA_PACKAGE = "com.google.android.GoogleCamera";
    private static final String NOOP_PREWARM_SERVICE =
            "com.google.android.apps.camera.prewarm.NoOpPrewarmService";

    /**
     * Packages known to host NoOpPrewarmService.
     * Add or remove entries here to match the device's camera stack.
     */
    private static final String[] TARGET_PACKAGES = {
            "com.google.android.GoogleCamera",   // Pixel / GCam
            "com.android.camera2",               // AOSP Camera
            "com.android.camera",                // Older AOSP Camera
            "com.sec.android.app.camera",        // Samsung Camera
            "com.huawei.camera",                 // Huawei Camera
            "com.oppo.camera",                   // OPPO Camera
            "com.coloros.camera",                // ColorOS Camera
    };

    public ServiceKillerWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        if (am == null) {
            Log.e(TAG, "ActivityManager unavailable – skipping.");
            return Result.failure();
        }

        String outcome = attemptDirectServiceStop(context);
        boolean anyKilled = false;

        // First pass: try to detect a running NoOpPrewarmService by scanning
        // RunningAppProcessInfo entries (works for background processes).
        List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
        if (processes != null) {
            for (ActivityManager.RunningAppProcessInfo proc : processes) {
                if (isTargetPackage(proc.processName)) {
                    Log.i(TAG, "Found target process: " + proc.processName
                            + " (importance=" + proc.importance + "). Killing…");
                    am.killBackgroundProcesses(proc.processName);
                    anyKilled = true;
                }
            }
        }

        // Second pass: call killBackgroundProcesses on every known package name
        // even if we did not see it in the process list (covers edge-cases where
        // the process name differs from the package name).
        for (String pkg : TARGET_PACKAGES) {
            Log.d(TAG, "Calling killBackgroundProcesses for: " + pkg);
            am.killBackgroundProcesses(pkg);
        }

        String finalOutcome = outcome + " / cleanup=" + (anyKilled ? "yes" : "no");
        Log.i(TAG, "Run complete. " + finalOutcome);

        androidx.work.Data outputData = new androidx.work.Data.Builder()
                .putString("outcome", finalOutcome)
                .build();

        return Result.success(outputData);
    }

    private String attemptDirectServiceStop(Context context) {
        Intent intent = new Intent();
        intent.setClassName(GOOGLE_CAMERA_PACKAGE, NOOP_PREWARM_SERVICE);
        try {
            boolean stopped = context.stopService(intent);
            String result = "stopService=" + stopped;
            Log.i(TAG, "Direct stopService(NoOpPrewarmService) attempted, " + result);
            return result;
        } catch (SecurityException e) {
            Log.w(TAG, "Direct stopService denied by platform/app permissions.", e);
            return "stopService=denied";
        } catch (Exception e) {
            Log.w(TAG, "Direct stopService failed unexpectedly.", e);
            return "stopService=error";
        }
    }

    /** Returns true if the given process name matches any of the target packages. */
    private boolean isTargetPackage(String processName) {
        if (processName == null) return false;
        for (String pkg : TARGET_PACKAGES) {
            if (processName.equals(pkg) || processName.startsWith(pkg + ":")) {
                return true;
            }
        }
        return false;
    }
}





