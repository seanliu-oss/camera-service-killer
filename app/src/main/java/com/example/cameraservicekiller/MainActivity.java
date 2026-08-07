package com.example.cameraservicekiller;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus;
    private TextView tvDebug;
    private Button btnToggle;
    private Button btnTestNow;

    // Launcher for POST_NOTIFICATIONS permission (Android 13+)
    private final ActivityResultLauncher<String> notifPermLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        // Whether granted or not, proceed with scheduling.
                        startKiller();
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tv_status);
        tvDebug = findViewById(R.id.tv_debug);
        btnToggle = findViewById(R.id.btn_toggle);
        btnTestNow = findViewById(R.id.btn_test_now);
        btnTestNow.setOnClickListener(v -> onTestNowClicked());

        // Observe work state so the UI always reflects reality.
        WorkManager.getInstance(this)
                .getWorkInfosForUniqueWorkLiveData(WorkScheduler.WORK_NAME)
                .observe(this, this::updateUi);

        btnToggle.setOnClickListener(v -> onToggleClicked());
    }

    private void onToggleClicked() {
        WorkManager wm = WorkManager.getInstance(this);
        List<WorkInfo> infos = null;
        try {
            infos = wm.getWorkInfosForUniqueWork(WorkScheduler.WORK_NAME).get();
        } catch (Exception ignored) {}

        boolean isActive = isActive(infos);

        if (isActive) {
            // Cancel the existing periodic work.
            wm.cancelUniqueWork(WorkScheduler.WORK_NAME);
            tvStatus.setText(R.string.status_stopped);
        } else {
            // Request POST_NOTIFICATIONS permission on Android 13+ before scheduling.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                startKiller();
            }
        }
    }

    private void startKiller() {
        // CANCEL_AND_REENQUEUE resets the 15-minute timer from now.
        WorkScheduler.schedule(this, true);
    }

    private void onTestNowClicked() {
        // Directly stop the camera service for immediate testing
        tvDebug.setText("Running test stop...");
        Intent intent = new Intent();
        intent.setClassName("com.google.android.GoogleCamera",
                "com.google.android.apps.camera.prewarm.NoOpPrewarmService");
        try {
            boolean stopped = stopService(intent);
            tvDebug.setText("Test: stopService result=" + stopped);
        } catch (Exception e) {
            tvDebug.setText("Test failed: " + e.getMessage());
        }
    }

    private void updateUi(List<WorkInfo> infos) {
        if (isActive(infos)) {
            btnToggle.setText(R.string.btn_stop);
            String next = getNextRunEstimate();
            tvStatus.setText(getString(R.string.status_running, next));
        } else {
            btnToggle.setText(R.string.btn_start);
            tvStatus.setText(R.string.status_stopped);
        }

        // Show the last work outcome if available
        if (infos != null && !infos.isEmpty()) {
            WorkInfo latestWork = infos.get(0);
            String outcome = latestWork.getOutputData().getString("outcome");
            if (outcome != null && !outcome.isEmpty()) {
                tvDebug.setText("Last outcome: " + outcome);
            } else {
                tvDebug.setText("");
            }
        } else {
            tvDebug.setText("");
        }
    }

    private boolean isActive(List<WorkInfo> infos) {
        if (infos == null || infos.isEmpty()) return false;
        for (WorkInfo info : infos) {
            WorkInfo.State state = info.getState();
            if (state == WorkInfo.State.ENQUEUED || state == WorkInfo.State.RUNNING) {
                return true;
            }
        }
        return false;
    }

    private String getNextRunEstimate() {
        // WorkManager does not expose the exact next-run time, so show a simple estimate.
        long nextMillis = System.currentTimeMillis() + 15 * 60 * 1000L;
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(nextMillis));
    }
}












