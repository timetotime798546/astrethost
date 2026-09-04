package com.camerapro.app;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("deprecation")
public class MainActivity extends Activity {

    private static final String TAG = "CameraPro";
    private static final int CAMERA_PERMISSION_CODE = 100;

    private Camera mCamera;
    private int mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;

    private Button btnSwitch;
    private Button btnFlash;
    private Button btnShutter;
    private ImageView imgThumbnail;

    // Preview Overlays layout components
    private RelativeLayout previewOverlay;
    private ImageView imgFullPreview;
    private Button btnClosePreview;
    private Button btnDeletePreview;

    private File mLastCapturedFile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind UI Elements
        mSurfaceView = (SurfaceView) findViewById(R.id.camera_preview);
        btnSwitch = (Button) findViewById(R.id.btn_switch);
        btnFlash = (Button) findViewById(R.id.btn_flash);
        btnShutter = (Button) findViewById(R.id.btn_shutter);
        imgThumbnail = (ImageView) findViewById(R.id.img_thumbnail);

        previewOverlay = (RelativeLayout) findViewById(R.id.preview_overlay);
        imgFullPreview = (ImageView) findViewById(R.id.img_full_preview);
        btnClosePreview = (Button) findViewById(R.id.btn_close_preview);
        btnDeletePreview = (Button) findViewById(R.id.btn_delete_preview);

        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(mSurfaceHolderCallback);

        // Setup interaction systems
        setupClickListeners();

        // Check and acquire necessary system access
        checkCameraPermissions();

        // Restore latest image taken on launch
        loadLatestThumbnail();
    }

    private void checkCameraPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
            } else {
                initCamera();
            }
        } else {
            initCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initCamera();
            } else {
                Toast.makeText(this, "App chalu karne ke liye Camera access zaruri hai!", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void initCamera() {
        safeCameraOpen(mCameraId);
    }

    private boolean safeCameraOpen(int id) {
        boolean qOpened = false;
        try {
            releaseCameraAndPreview();
            mCamera = Camera.open(id);
            mCameraId = id;
            qOpened = (mCamera != null);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open camera interface", e);
            Toast.makeText(this, "Camera chalu karne me samasya aayi!", Toast.LENGTH_SHORT).show();
        }
        return qOpened;
    }

    private void releaseCameraAndPreview() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private final SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            // Surface established, waiting configuration on changed
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (mHolder.getSurface() == null) {
                return;
            }
            if (mCamera == null) {
                return;
            }

            try {
                mCamera.stopPreview();
            } catch (Exception e) {
                // Ignore failure if preview wasn't active
            }

            try {
                mCamera.setPreviewDisplay(holder);

                // Configure Camera parameters safely
                Camera.Parameters parameters = mCamera.getParameters();
                List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
                Camera.Size optimalSize = getOptimalPreviewSize(sizes, width, height);
                if (optimalSize != null) {
                    parameters.setPreviewSize(optimalSize.width, optimalSize.height);
                }

                // Choose robust picture dimensions
                List<Camera.Size> picSizes = parameters.getSupportedPictureSizes();
                if (picSizes != null && !picSizes.isEmpty()) {
                    parameters.setPictureSize(picSizes.get(0).width, picSizes.get(0).height);
                }

                // Enable optimal continuous focus model
                List<String> focusModes = parameters.getSupportedFocusModes();
                if (focusModes != null && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                }

                mCamera.setParameters(parameters);
                setCameraDisplayOrientation(MainActivity.this, mCameraId, mCamera);
                mCamera.startPreview();
            } catch (Exception e) {
                Log.e(TAG, "Error launching preview window", e);
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            releaseCameraAndPreview();
        }
    };

    private void setupClickListeners() {
        // Click camera trigger
        btnShutter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                capturePhoto();
            }
        });

        // Click front/back switch
        btnSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchCamera();
            }
        });

        // Click flash cycling configuration
        btnFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cycleFlashMode();
            }
        });

        // Touch preview to request Manual Autofocus
        mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    triggerManualAutoFocus();
                }
                return true;
            }
        });

        // Click small thumbnail to see fullscreen image
        imgThumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFullscreenOverlay();
            }
        });

        // Click back from full preview
        btnClosePreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                previewOverlay.setVisibility(View.GONE);
            }
        });

        // Click delete button inside overlay screen
        btnDeletePreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteLastCapturedPhoto();
            }
        });
    }

    private void capturePhoto() {
        if (mCamera == null) {
            Toast.makeText(this, "Camera ready nahi hai!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnShutter.setEnabled(false);
        try {
            mCamera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    saveCapturedData(data);
                    // Resume previews post action
                    try {
                        mCamera.startPreview();
                    } catch (Exception ex) {
                        Log.e(TAG, "Restart preview error", ex);
                    }
                    btnShutter.setEnabled(true);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Capture action exception", e);
            Toast.makeText(this, "Tasveer lene me error!", Toast.LENGTH_SHORT).show();
            btnShutter.setEnabled(true);
        }
    }

    private void saveCapturedData(byte[] data) {
        File directory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (directory == null) {
            Toast.makeText(this, "Storage check karein!", Toast.LENGTH_SHORT).show();
            return;
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String photoFileName = "IMG_" + timeStamp + ".jpg";
        File imageFile = new File(directory, photoFileName);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(imageFile);
            fos.write(data);
            fos.flush();
            mLastCapturedFile = imageFile;

            Toast.makeText(this, "Photo save ho gayi: " + photoFileName, Toast.LENGTH_SHORT).show();

            // Refresh UI thumbnail image
            Bitmap thumb = decodeSampledBitmapFromFile(imageFile.getAbsolutePath(), 100, 100);
            if (thumb != null) {
                imgThumbnail.setImageBitmap(thumb);
                imgThumbnail.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Saving stream failure", e);
            Toast.makeText(this, "Photo save karne me fail hua!", Toast.LENGTH_SHORT).show();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    private void triggerManualAutoFocus() {
        if (mCamera != null) {
            try {
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        // Lens focused!
                    }
                });
            } catch (Exception e) {
                // Focus capability unsupported under this condition (e.g. front camera)
            }
        }
    }

    private void switchCamera() {
        mCameraId = (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) ?
                Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK;

        safeCameraOpen(mCameraId);

        if (mCamera != null && mHolder != null) {
            try {
                mCamera.setPreviewDisplay(mHolder);
                Camera.Parameters parameters = mCamera.getParameters();

                // Dynamic preview dimensions based on camera selection
                List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
                Camera.Size optimalSize = getOptimalPreviewSize(sizes, mSurfaceView.getWidth(), mSurfaceView.getHeight());
                if (optimalSize != null) {
                    parameters.setPreviewSize(optimalSize.width, optimalSize.height);
                }

                mCamera.setParameters(parameters);
                setCameraDisplayOrientation(this, mCameraId, mCamera);
                mCamera.startPreview();
            } catch (Exception e) {
                Log.e(TAG, "Failed switching preview source surface", e);
            }
        }
    }

    private void cycleFlashMode() {
        if (mCamera == null) return;
        Camera.Parameters parameters = mCamera.getParameters();
        List<String> modes = parameters.getSupportedFlashModes();
        if (modes == null || modes.isEmpty()) {
            Toast.makeText(this, "Aapke is Camera me Flash support nahi hai!", Toast.LENGTH_SHORT).show();
            return;
        }

        String current = parameters.getFlashMode();
        String nextMode = Camera.Parameters.FLASH_MODE_OFF;
        String statusLabel = "Flash: OFF";

        if (Camera.Parameters.FLASH_MODE_OFF.equals(current)) {
            if (modes.contains(Camera.Parameters.FLASH_MODE_ON)) {
                nextMode = Camera.Parameters.FLASH_MODE_ON;
                statusLabel = "Flash: ON";
            } else if (modes.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
                nextMode = Camera.Parameters.FLASH_MODE_AUTO;
                statusLabel = "Flash: AUTO";
            }
        } else if (Camera.Parameters.FLASH_MODE_ON.equals(current)) {
            if (modes.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
                nextMode = Camera.Parameters.FLASH_MODE_AUTO;
                statusLabel = "Flash: AUTO";
            } else {
                nextMode = Camera.Parameters.FLASH_MODE_OFF;
                statusLabel = "Flash: OFF";
            }
        } else {
            nextMode = Camera.Parameters.FLASH_MODE_OFF;
            statusLabel = "Flash: OFF";
        }

        try {
            parameters.setFlashMode(nextMode);
            mCamera.setParameters(parameters);
            btnFlash.setText(statusLabel);
        } catch (Exception e) {
            Toast.makeText(this, "Flash badalne me dikkat aayi!", Toast.LENGTH_SHORT).show();
        }
    }

    private void showFullscreenOverlay() {
        if (mLastCapturedFile != null && mLastCapturedFile.exists()) {
            Bitmap previewBitmap = decodeSampledBitmapFromFile(mLastCapturedFile.getAbsolutePath(), 1000, 1000);
            if (previewBitmap != null) {
                imgFullPreview.setImageBitmap(previewBitmap);
                previewOverlay.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this, "Tasveer load karne me fail!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void deleteLastCapturedPhoto() {
        if (mLastCapturedFile != null && mLastCapturedFile.exists()) {
            if (mLastCapturedFile.delete()) {
                Toast.makeText(this, "Photo safaltapurvak mitayi gayi!", Toast.LENGTH_SHORT).show();
                previewOverlay.setVisibility(View.GONE);
                loadLatestThumbnail();
            } else {
                Toast.makeText(this, "Photo mitane me koi samasya aayi!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadLatestThumbnail() {
        File directory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (directory != null && directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null && files.length > 0) {
                File latest = files[0];
                for (File f : files) {
                    if (f.lastModified() > latest.lastModified()) {
                        latest = f;
                    }
                }
                if (latest.isFile() && latest.getName().endsWith(".jpg")) {
                    mLastCapturedFile = latest;
                    Bitmap thumb = decodeSampledBitmapFromFile(latest.getAbsolutePath(), 100, 100);
                    if (thumb != null) {
                        imgThumbnail.setImageBitmap(thumb);
                        imgThumbnail.setVisibility(View.VISIBLE);
                        return;
                    }
                }
            }
        }
        imgThumbnail.setVisibility(View.GONE);
        mLastCapturedFile = null;
    }

    // Mathematical resolution mapping calculation logic
    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w;
        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    // Layout configuration alignment systems helper
    public static void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // Compensate mirroring layout logic
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    // Downsampling layout scale helper to prevent Out Of Memory crashes
    public static Bitmap decodeSampledBitmapFromFile(String filePath, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, options);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCamera == null && checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            initCamera();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCameraAndPreview();
    }
}