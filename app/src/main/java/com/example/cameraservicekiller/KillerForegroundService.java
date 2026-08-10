package com.example.cameraservicekiller;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class KillerForegroundService extends Service {

    private static final String TAG = "KillerFgService";
    private static final String ACTION_START = "com.example.cameraservicekiller.action.START";
    private static final String ACTION_STOP = "com.example.cameraservicekiller.action.STOP";

    private static final String CHANNEL_ID = "camera_killer_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final long INTERVAL_MS = 15 * 60 * 1000L;

    private static final String PREFS = "killer_prefs";
    private static final String KEY_ENABLED = "fg_enabled";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean loopActive;

    private final Runnable killLoop = new Runnable() {
        @Override
        public void run() {
            String outcome = KillerActions.runStopAttempt(getApplicationContext());
            Log.i(TAG, "Foreground run complete: " + outcome);
            updateNotification(outcome);
            if (loopActive) {
                handler.postDelayed(this, INTERVAL_MS);
            }
        }
    };

    public static void requestStart(Context context) {
        setEnabled(context, true);
        Intent intent = new Intent(context, KillerForegroundService.class);
        intent.setAction(ACTION_START);
        ContextCompat.startForegroundService(context, intent);
    }

    public static void requestStop(Context context) {
        setEnabled(context, false);
        Intent intent = new Intent(context, KillerForegroundService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }

    public static boolean isEnabled(Context context) {
        return context.getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_ENABLED, false);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_STOP.equals(action)) {
            stopLoop();
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(NOTIFICATION_ID, buildNotification("Starting..."));
        if (!loopActive) {
            loopActive = true;
            handler.post(killLoop);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopLoop();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void stopLoop() {
        loopActive = false;
        handler.removeCallbacks(killLoop);
    }

    private void updateNotification(String outcome) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.notify(NOTIFICATION_ID, buildNotification(outcome));
        }
    }

    private Notification buildNotification(String outcome) {
        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String now = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String text = "Last run " + now + " - " + outcome;

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(getString(R.string.fg_notification_title))
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void createNotificationChannel() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.fg_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
        );
        nm.createNotificationChannel(channel);
    }

    private static void setEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ENABLED, enabled)
                .apply();
    }
}

