package com.example.cameraservicekiller;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * Periodic worker that runs the same suppression routine used by foreground mode.
 */
public class ServiceKillerWorker extends Worker {

    private static final String TAG = "ServiceKillerWorker";

    public ServiceKillerWorker(@NonNull android.content.Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String finalOutcome = KillerActions.runStopAttempt(getApplicationContext());
        Log.i(TAG, "Run complete. " + finalOutcome);

        androidx.work.Data outputData = new androidx.work.Data.Builder()
                .putString("outcome", finalOutcome)
                .build();

        return Result.success(outputData);
    }
}
