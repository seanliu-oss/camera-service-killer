package com.example.cameraservicekiller;

import android.Manifest;
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

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus;
    private TextView tvNextRun;
    private TextView tvDebug;
    private Button btnToggle;
    private Button btnTestNow;

    private final ActivityResultLauncher<String> notifPermLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> startKiller());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tv_status);
        tvNextRun = findViewById(R.id.tv_next_run);
        tvDebug = findViewById(R.id.tv_debug);
        btnToggle = findViewById(R.id.btn_toggle);
        btnTestNow = findViewById(R.id.btn_test_now);

        btnToggle.setOnClickListener(v -> onToggleClicked());
        btnTestNow.setOnClickListener(v -> onTestNowClicked());

        WorkManager.getInstance(this)
                .getWorkInfosForUniqueWorkLiveData(WorkScheduler.WORK_NAME)
                .observe(this, this::updateFromWorkInfos);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshRunningUi();
    }

    private void onToggleClicked() {
        if (KillerForegroundService.isEnabled(this)) {
            KillerForegroundService.requestStop(this);
            WorkManager.getInstance(this).cancelUniqueWork(WorkScheduler.WORK_NAME);
            refreshRunningUi();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            startKiller();
        }
    }

    private void startKiller() {
        KillerForegroundService.requestStart(this);
        // Keep periodic worker as backup if the foreground service is stopped unexpectedly.
        WorkScheduler.schedule(this, true);
        refreshRunningUi();
    }

    private void onTestNowClicked() {
        String outcome = KillerActions.runStopAttempt(this);
        tvDebug.setText(getString(R.string.debug_last_outcome, outcome));
    }

    private void refreshRunningUi() {
        if (KillerForegroundService.isEnabled(this)) {
            btnToggle.setText(R.string.btn_stop);
            tvStatus.setText(R.string.status_running);
        } else {
            btnToggle.setText(R.string.btn_start);
            tvStatus.setText(R.string.status_stopped);
            tvNextRun.setText(R.string.next_run_not_scheduled);
        }
    }

    private void updateFromWorkInfos(List<WorkInfo> infos) {
        updateDebugOutcome(infos);
        updateNextRun(infos);
    }

    private void updateDebugOutcome(List<WorkInfo> infos) {
        if (infos == null || infos.isEmpty()) {
            return;
        }

        String outcome = infos.get(0).getOutputData().getString("outcome");
        if (outcome != null && !outcome.isEmpty()) {
            tvDebug.setText(getString(R.string.debug_last_outcome, outcome));
        }
    }

    private void updateNextRun(List<WorkInfo> infos) {
        WorkInfo active = findActiveWork(infos);
        if (active == null) {
            tvNextRun.setText(R.string.next_run_not_scheduled);
            return;
        }

        long nextMs = readNextScheduleTimeMillis(active);
        if (nextMs <= 0L) {
            // Fallback estimate if next schedule time is unavailable from WorkInfo.
            nextMs = System.currentTimeMillis() + WorkScheduler.getIntervalMinutes() * 60_000L;
        }

        if (nextMs <= 0L) {
            tvNextRun.setText(R.string.next_run_unavailable);
            return;
        }

        String formatted = new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                .format(new Date(nextMs));
        tvNextRun.setText(getString(R.string.next_run_at, formatted));
    }

    private WorkInfo findActiveWork(List<WorkInfo> infos) {
        if (infos == null || infos.isEmpty()) {
            return null;
        }

        for (WorkInfo info : infos) {
            WorkInfo.State state = info.getState();
            if (state == WorkInfo.State.ENQUEUED || state == WorkInfo.State.RUNNING) {
                return info;
            }
        }
        return null;
    }

    private long readNextScheduleTimeMillis(WorkInfo info) {
        try {
            Method method = WorkInfo.class.getMethod("getNextScheduleTimeMillis");
            Object value = method.invoke(info);
            if (value instanceof Long) {
                return (Long) value;
            }
        } catch (Exception ignored) {
            // Method not available on older WorkManager APIs.
        }
        return -1L;
    }
}
