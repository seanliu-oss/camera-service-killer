# CameraServiceKiller

Contrary to what google says, `NoOpPrewarmService` is not a no-op. It can consume CPU and battery in the background. This app provides a simple way to periodically attempt to stop that service.
Android app that periodically attempts to stop Google Camera's `NoOpPrewarmService`.

## What It Does

- Schedules a periodic background task with WorkManager every 15 minutes.
- Attempts to stop `com.google.android.apps.camera.prewarm.NoOpPrewarmService` directly.
- Falls back to `killBackgroundProcesses()` cleanup for known camera package names.
- Provides a simple UI with:
  - `Start` / `Stop` scheduling toggle
  - `Test Now` button for immediate manual stop attempt
  - status/debug lines in the main screen

## Why This Exists

On some devices, killing the entire camera process may cause the prewarm service to restart quickly. This app prioritizes direct service stop attempts and keeps periodic cleanup enabled.

## Requirements

- Android device with API 34+ (current app `minSdk = 34`)
- App installed and launched at least once
- Relevant permissions granted when requested (notifications on Android 13+)

## Build and Install

```powershell
cd "C:\Users\sean\AndroidStudioProjects\CameraServiceKiller"
.\gradlew.bat assembleDebug
adb install -r "app\build\outputs\apk\debug\app-debug.apk"
```

## Usage

1. Open the app.
2. Tap `Start` to schedule periodic attempts (15-minute interval).
3. Optional: tap `Test Now` to run an immediate direct stop attempt.
4. Watch:
   - main status line for schedule state
   - debug line for latest worker/test outcome

## Verification Commands (ADB)

Check whether the target service is running:

```powershell
adb shell dumpsys activity services | Select-String "NoOpPrewarmService"
```

Check whether periodic WorkManager job is still scheduled:

```powershell
adb shell dumpsys jobscheduler | Select-String "com.example.cameraservicekiller"
```

## Developer Notes

### Key Files

- `app/src/main/java/com/example/cameraservicekiller/MainActivity.java`
  - UI wiring, start/stop scheduling, and `Test Now` direct stop
- `app/src/main/java/com/example/cameraservicekiller/WorkScheduler.java`
  - Unique periodic work scheduling (`WORK_NAME = NoOpPrewarmServiceKiller`)
  - Interval constant: `INTERVAL_MINUTES = 15`
- `app/src/main/java/com/example/cameraservicekiller/ServiceKillerWorker.java`
  - Background execution logic
  - output data key: `outcome`
- `app/src/main/java/com/example/cameraservicekiller/BootReceiver.java`
  - Re-schedules periodic work on boot and app replacement
- `app/src/main/AndroidManifest.xml`
  - permissions and receiver declarations

### Current Behavior Summary

- Direct stop path:
  - `context.stopService(Intent.setClassName(...NoOpPrewarmService))`
- Cleanup path:
  - scans running processes for known camera package/process names
  - calls `ActivityManager.killBackgroundProcesses(...)`
- Work result:
  - publishes `outcome` in worker output data for UI/debug visibility

### Important Platform Constraints

- `KILL_BACKGROUND_PROCESSES` only affects background processes.
- Cross-app service control behavior can vary by OEM/system policy.
- A service may be restarted by the camera app or system after being stopped.
- WorkManager periodic execution timing is inexact (best effort around interval).

### Permissions Used

- `android.permission.KILL_BACKGROUND_PROCESSES`
- `android.permission.QUERY_ALL_PACKAGES`
- `android.permission.RECEIVE_BOOT_COMPLETED`
- `android.permission.FOREGROUND_SERVICE`
- `android.permission.POST_NOTIFICATIONS`

## Troubleshooting

- If periodic work does not run:
  - ensure app is not battery-restricted
  - reopen app and tap `Start` again
  - confirm scheduled job via `dumpsys jobscheduler`
- If `NoOpPrewarmService` returns quickly:
  - this can be expected on some builds/OEMs
  - use `Test Now` for immediate check and compare with ADB output

## Safety Note

This project is for local device testing/debug workflows. Validate behavior on your own hardware and OS build before relying on it for production automation.

