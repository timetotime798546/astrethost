package com.offlinecctvrecord.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("deprecation")
public class MainActivity extends Activity {

    private static final int PERMISSION_REQ_CODE = 101;

    private Camera mCamera;
    private CameraPreview mPreview;
    private MediaRecorder mMediaRecorder;
    private FrameLayout mPreviewContainer;

    private TextView tvLiveClock;
    private TextView tvStorageStats;
    private TextView tvRecTimer;
    private TextView tvChannelTitle;
    private View recIndicatorDot;
    private Button btnRecordToggle;
    private Button btnFlipCamera;
    private Button btnToggleTorch;
    private Button btnViewVault;

    private boolean isRecording = false;
    private boolean isTorchOn = false;
    private int currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    private File currentOutputFile = null;

    private long recordingStartTime = 0;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private SimpleDateFormat clockFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    private SimpleDateFormat fileDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);

    private PowerManager.WakeLock mWakeLock;

    // Clock update runnable
    private final Runnable clockRunnable = new Runnable() {
        @Override
        public void run() {
            Date now = new Date();
            tvLiveClock.setText(clockFormat.format(now));

            if (isRecording) {
                long elapsed = System.currentTimeMillis() - recordingStartTime;
                int seconds = (int) (elapsed / 1000) % 60;
                int minutes = (int) ((elapsed / (1000 * 60)) % 60);
                int hours = (int) ((elapsed / (1000 * 60 * 60)));
                tvRecTimer.setText(String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds));

                // Blink the recording dot
                recIndicatorDot.setVisibility(recIndicatorDot.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
            }

            mHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        initViews();
        checkAndRequestPermissions();

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) {
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OfflineCCTV:RecordingLock");
        }
    }

    private void initViews() {
        mPreviewContainer = (FrameLayout) findViewById(R.id.camera_preview_container);
        tvLiveClock = (TextView) findViewById(R.id.tv_live_clock);
        tvStorageStats = (TextView) findViewById(R.id.tv_storage_stats);
        tvRecTimer = (TextView) findViewById(R.id.tv_rec_timer);
        tvChannelTitle = (TextView) findViewById(R.id.tv_channel_title);
        recIndicatorDot = findViewById(R.id.rec_indicator_dot);

        btnRecordToggle = (Button) findViewById(R.id.btn_record_toggle);
        btnFlipCamera = (Button) findViewById(R.id.btn_flip_camera);
        btnToggleTorch = (Button) findViewById(R.id.btn_toggle_torch);
        btnViewVault = (Button) findViewById(R.id.btn_view_vault);

        btnRecordToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRecording) {
                    stopCctvRecording();
                } else {
                    startCctvRecording();
                }
            }
        });

        btnFlipCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRecording) {
                    Toast.makeText(MainActivity.this, "Cannot flip camera while recording!", Toast.LENGTH_SHORT).show();
                    return;
                }
                flipCamera();
            }
        });

        btnToggleTorch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleTorch();
            }
        });

        btnViewVault.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RecordingsActivity.class);
                startActivity(intent);
            }
        });

        mHandler.post(clockRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStorageStats();
        if (hasAllPermissions() && mCamera == null) {
            setupCamera(currentCameraId);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isRecording) {
            stopCctvRecording();
        }
        releaseCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(clockRunnable);
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    private void updateStorageStats() {
        List<RecordingModel> recordings = StorageHelper.getSavedRecordings(this);
        long totalCctvBytes = StorageHelper.getTotalCctvBytes(this);
        long freeBytes = StorageHelper.getAvailableInternalDiskSpace(this);

        String stats = "Clips: " + recordings.size() + " (" + StorageHelper.formatBytes(totalCctvBytes) + ") | Free: " + StorageHelper.formatBytes(freeBytes);
        tvStorageStats.setText(stats);
    }

    private boolean hasAllPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!hasAllPermissions()) {
                requestPermissions(new String[]{
                        android.Manifest.permission.CAMERA,
                        android.Manifest.permission.RECORD_AUDIO,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                }, PERMISSION_REQ_CODE);
            } else {
                setupCamera(currentCameraId);
            }
        } else {
            setupCamera(currentCameraId);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_CODE) {
            boolean allGranted = true;
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                setupCamera(currentCameraId);
            } else {
                Toast.makeText(this, R.string.permission_required, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupCamera(int cameraId) {
        releaseCamera();
        try {
            mCamera = Camera.open(cameraId);
            if (mCamera != null) {
                mCamera.setDisplayOrientation(90);
                mPreview = new CameraPreview(this, mCamera);
                mPreviewContainer.removeAllViews();
                mPreviewContainer.addView(mPreview);

                String camLabel = (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) ? "CAM-01 [BACK - LIVE]" : "CAM-02 [FRONT - LIVE]";
                tvChannelTitle.setText(camLabel);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Camera initialization failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            try {
                mCamera.stopPreview();
                mCamera.release();
            } catch (Exception e) {
                // ignore
            }
            mCamera = null;
        }
    }

    private void flipCamera() {
        int numCameras = Camera.getNumberOfCameras();
        if (numCameras < 2) {
            Toast.makeText(this, "Only one camera available on this device.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            currentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        isTorchOn = false;
        setupCamera(currentCameraId);
    }

    private void toggleTorch() {
        if (mCamera == null) return;
        if (currentCameraId != Camera.CameraInfo.CAMERA_FACING_BACK) {
            Toast.makeText(this, "Torch only available on Back Camera.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Camera.Parameters params = mCamera.getParameters();
            List<String> supportedFlash = params.getSupportedFlashModes();
            if (supportedFlash == null || !supportedFlash.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                Toast.makeText(this, "Torch mode not supported on this device.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isTorchOn) {
                params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                mCamera.setParameters(params);
                isTorchOn = true;
                btnToggleTorch.setText("TORCH:ON");
            } else {
                params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                mCamera.setParameters(params);
                isTorchOn = false;
                btnToggleTorch.setText("TORCH:OFF");
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to toggle torch: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean prepareVideoRecorder() {
        if (mCamera == null) {
            return false;
        }

        mMediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set CamcorderProfile (defaults to 720p or high quality for CCTV)
        boolean profileSet = false;
        if (CamcorderProfile.hasProfile(currentCameraId, CamcorderProfile.QUALITY_720P)) {
            mMediaRecorder.setProfile(CamcorderProfile.get(currentCameraId, CamcorderProfile.QUALITY_720P));
            profileSet = true;
        } else if (CamcorderProfile.hasProfile(currentCameraId, CamcorderProfile.QUALITY_480P)) {
            mMediaRecorder.setProfile(CamcorderProfile.get(currentCameraId, CamcorderProfile.QUALITY_480P));
            profileSet = true;
        } else if (CamcorderProfile.hasProfile(currentCameraId, CamcorderProfile.QUALITY_HIGH)) {
            mMediaRecorder.setProfile(CamcorderProfile.get(currentCameraId, CamcorderProfile.QUALITY_HIGH));
            profileSet = true;
        } else if (CamcorderProfile.hasProfile(currentCameraId, CamcorderProfile.QUALITY_LOW)) {
            mMediaRecorder.setProfile(CamcorderProfile.get(currentCameraId, CamcorderProfile.QUALITY_LOW));
            profileSet = true;
        }

        if (!profileSet) {
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        }

        // Step 4: Output File
        File cctvDir = StorageHelper.getCctvDirectory(this);
        String fileName = "CCTV_" + fileDateFormat.format(new Date()) + ".mp4";
        currentOutputFile = new File(cctvDir, fileName);
        mMediaRecorder.setOutputFile(currentOutputFile.getAbsolutePath());

        // Step 5: Orientation hint
        if (currentCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            mMediaRecorder.setOrientationHint(270);
        } else {
            mMediaRecorder.setOrientationHint(90);
        }

        // Step 6: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void startCctvRecording() {
        if (!hasAllPermissions()) {
            checkAndRequestPermissions();
            return;
        }

        if (StorageHelper.getAvailableInternalDiskSpace(this) < 50 * 1024 * 1024) { // Less than 50MB
            Toast.makeText(this, "Low disk space! Free up memory to record CCTV video.", Toast.LENGTH_LONG).show();
            return;
        }

        if (prepareVideoRecorder()) {
            try {
                mMediaRecorder.start();
                isRecording = true;
                recordingStartTime = System.currentTimeMillis();

                if (mWakeLock != null && !mWakeLock.isHeld()) {
                    mWakeLock.acquire(4 * 60 * 60 * 1000L); // 4 hour safety limit
                }

                btnRecordToggle.setText("■ STOP CCTV RECORDING");
                btnRecordToggle.setBackgroundColor(0xFFB71C1C);
                tvRecTimer.setVisibility(View.VISIBLE);
                recIndicatorDot.setVisibility(View.VISIBLE);

                Toast.makeText(this, "CCTV Recording Active. Storing offline...", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                releaseMediaRecorder();
                Toast.makeText(this, "Recording failed to start: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            releaseMediaRecorder();
            Toast.makeText(this, "Could not prepare camera recorder.", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopCctvRecording() {
        if (!isRecording) return;

        try {
            if (mMediaRecorder != null) {
                mMediaRecorder.stop();
            }
        } catch (Exception e) {
            if (currentOutputFile != null && currentOutputFile.exists()) {
                currentOutputFile.delete();
            }
        }

        releaseMediaRecorder();
        isRecording = false;

        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }

        btnRecordToggle.setText("● START CCTV RECORDING");
        btnRecordToggle.setBackgroundColor(getResources().getColor(R.color.cctv_rec_red));
        tvRecTimer.setVisibility(View.GONE);
        recIndicatorDot.setVisibility(View.GONE);

        updateStorageStats();

        if (currentOutputFile != null && currentOutputFile.exists() && currentOutputFile.length() > 0) {
            showSavedDialog(currentOutputFile);
        }
    }

    private void showSavedDialog(final File savedFile) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("CCTV Footage Saved");
        builder.setMessage("Offline CCTV clip has been safely saved:\n\n"
                + savedFile.getName() + "\nSize: " + StorageHelper.formatBytes(savedFile.length()));
        builder.setPositiveButton("Watch Clip", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(MainActivity.this, VideoPlayerActivity.class);
                intent.putExtra("video_path", savedFile.getAbsolutePath());
                intent.putExtra("video_title", savedFile.getName());
                startActivity(intent);
            }
        });
        builder.setNegativeButton("OK", null);
        builder.show();
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            if (mCamera != null) {
                try {
                    mCamera.lock();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }
}