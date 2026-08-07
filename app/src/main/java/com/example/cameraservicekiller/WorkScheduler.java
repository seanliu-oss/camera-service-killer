package com.example.cameraservicekiller;

import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/**
 * Central helper that schedules (or re-schedules) the periodic kill task.
 */
public class WorkScheduler {

    private static final String TAG = "WorkScheduler";

    /** Unique name used to identify the periodic work in WorkManager. */
    public static final String WORK_NAME = "NoOpPrewarmServiceKiller";

    /** Interval between executions. WorkManager minimum is 15 minutes. */
    private static final long INTERVAL_MINUTES = 15;

    private WorkScheduler() {}

    /**
     * Enqueues the periodic worker.  If a task with the same name already
     * exists it is kept as-is (KEEP policy) so reboots / re-installs do not
     * reset the schedule unnecessarily.  Pass {@code replace=true} to force
     * a fresh schedule (e.g., from a "Restart" button).
     */
    public static void schedule(Context context, boolean replace) {
        Constraints constraints = new Constraints.Builder()
                // No network, battery, or storage constraints – run unconditionally.
                .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                ServiceKillerWorker.class,
                INTERVAL_MINUTES,
                TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();

        ExistingPeriodicWorkPolicy policy = replace
                ? ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
                : ExistingPeriodicWorkPolicy.KEEP;

        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, policy, request);

        Log.i(TAG, "Scheduled " + WORK_NAME + " every " + INTERVAL_MINUTES + " minutes. replace=" + replace);
    }
}



