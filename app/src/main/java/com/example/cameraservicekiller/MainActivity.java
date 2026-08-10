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

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus;
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
        tvDebug = findViewById(R.id.tv_debug);
        btnToggle = findViewById(R.id.btn_toggle);
        btnTestNow = findViewById(R.id.btn_test_now);

        btnToggle.setOnClickListener(v -> onToggleClicked());
        btnTestNow.setOnClickListener(v -> onTestNowClicked());

        WorkManager.getInstance(this)
                .getWorkInfosForUniqueWorkLiveData(WorkScheduler.WORK_NAME)
                .observe(this, this::updateDebugOutcome);
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
        }
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
}
