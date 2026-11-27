package com.boxstockmanager.app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class MainActivity extends Activity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final int PERMISSION_REQUEST_CAMERA = 101;
    private static final int REQUEST_PICK_IMAGE = 102;
    private static final long DEBOUNCE_MS = 2000;

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private MultiFormatReader multiFormatReader;

    private RadioGroup rgStockMode;
    private RadioButton rbStockIn;
    private TextView tvBannerTitle;
    private TextView tvBannerDetails;
    private TextView tvCameraStatus;
    private ListView lvInventory;
    private Button btnUploadImage;

    private DatabaseHelper databaseHelper;
    private BoxAdapter boxAdapter;
    private List<BoxItem> inventoryList = new ArrayList<>();

    private long lastScanTimestamp = 0;
    private String lastScannedCode = "";
    private boolean isProcessingFrame = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        databaseHelper = new DatabaseHelper(this);
        multiFormatReader = new MultiFormatReader();

        surfaceView = (SurfaceView) findViewById(R.id.surface_camera_preview);
        rgStockMode = (RadioGroup) findViewById(R.id.rg_stock_mode);
        rbStockIn = (RadioButton) findViewById(R.id.rb_stock_in);
        tvBannerTitle = (TextView) findViewById(R.id.tv_banner_title);
        tvBannerDetails = (TextView) findViewById(R.id.tv_banner_details);
        tvCameraStatus = (TextView) findViewById(R.id.tv_camera_status);
        lvInventory = (ListView) findViewById(R.id.lv_inventory);
        btnUploadImage = (Button) findViewById(R.id.btn_upload_image);

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        boxAdapter = new BoxAdapter(this, inventoryList);
        lvInventory.setAdapter(boxAdapter);

        btnUploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        checkPermissions();
        loadInventory();
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCameraPreview();
            } else {
                Toast.makeText(this, "Camera permission required for scanning", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                decodeImageUri(imageUri);
            }
        }
    }

    private void decodeImageUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            if (bitmap == null) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                return;
            }

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

            LuminanceSource source = new RGBLuminanceSource(width, height, pixels);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

            Result result = multiFormatReader.decode(binaryBitmap);
            if (result != null && result.getText() != null) {
                processScannedCode(result.getText().trim(), "Uploaded Image");
            } else {
                Toast.makeText(this, "No QR/Barcode found in image", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "QR Decode Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private synchronized void processScannedCode(String qrCode, String sourceInfo) {
        long now = System.currentTimeMillis();
        if (qrCode.equals(lastScannedCode) && (now - lastScanTimestamp) < DEBOUNCE_MS) {
            return;
        }

        lastScannedCode = qrCode;
        lastScanTimestamp = now;

        triggerVibration();

        boolean isStockIn = rbStockIn.isChecked();
        int newQty = databaseHelper.updateStock(qrCode, isStockIn);

        String action = isStockIn ? "Stock IN (+1)" : "Stock OUT (-1)";
        tvBannerTitle.setText("Latest Action: " + action + " via " + sourceInfo);
        tvBannerDetails.setText("Box Code: " + qrCode + " | New Total: " + newQty + " Pcs");

        loadInventory();
    }

    private void triggerVibration() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                vibrator.vibrate(100);
            }
        } catch (Exception ignored) {
        }
    }

    private void loadInventory() {
        inventoryList.clear();
        inventoryList.addAll(databaseHelper.getAllInventory());
        boxAdapter.notifyDataSetChanged();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startCameraPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (surfaceHolder.getSurface() == null) {
            return;
        }
        try {
            if (camera != null) {
                camera.stopPreview();
                camera.setPreviewDisplay(surfaceHolder);
                camera.setPreviewCallback(this);
                camera.startPreview();
            }
        } catch (Exception e) {
            tvCameraStatus.setText("Camera Preview Error");
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopCameraPreview();
    }

    private void startCameraPreview() {
        if (camera == null) {
            try {
                camera = Camera.open();
                if (camera != null) {
                    camera.setDisplayOrientation(90);
                    camera.setPreviewDisplay(surfaceHolder);
                    camera.setPreviewCallback(this);
                    camera.startPreview();
                    tvCameraStatus.setText("Camera Live Scanning Active");
                }
            } catch (Exception e) {
                tvCameraStatus.setText("Camera Initialization Failed");
            }
        }
    }

    private void stopCameraPreview() {
        if (camera != null) {
            try {
                camera.setPreviewCallback(null);
                camera.stopPreview();
                camera.release();
            } catch (Exception ignored) {
            }
            camera = null;
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (isProcessingFrame) {
            return;
        }

        isProcessingFrame = true;
        try {
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size size = parameters.getPreviewSize();
            int width = size.width;
            int height = size.height;

            PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                    data, width, height, 0, 0, width, height, false
            );

            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result = multiFormatReader.decodeWithState(bitmap);

            if (result != null && result.getText() != null) {
                final String text = result.getText().trim();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        processScannedCode(text, "Live Camera");
                    }
                });
            }
        } catch (Exception ignored) {
        } finally {
            multiFormatReader.reset();
            isProcessingFrame = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCameraPreview();
    }

    @Override
    protected void onPause() {

        super.onPause();
        stopCameraPreview();
    }

    private static class BoxAdapter extends BaseAdapter {
        private final Context context;
        private final List<BoxItem> items;
        private final LayoutInflater inflater;

        public BoxAdapter(Context context, List<BoxItem> items) {
            this.context = context;
            this.items = items;
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return items.get(position).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_box_stock, parent, false);
                holder = new ViewHolder();
                holder.tvBoxCode = (TextView) convertView.findViewById(R.id.tv_box_code);
                holder.tvLastUpdated = (TextView) convertView.findViewById(R.id.tv_last_updated);
                holder.tvBoxQuantity = (TextView) convertView.findViewById(R.id.tv_box_quantity);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            BoxItem item = items.get(position);
            holder.tvBoxCode.setText(item.getQrCode());
            holder.tvLastUpdated.setText("Updated: " + item.getLastUpdated());
            holder.tvBoxQuantity.setText(item.getQuantity() + " Pcs");

            return convertView;
        }

        private static class ViewHolder {
            TextView tvBoxCode;
            TextView tvLastUpdated;
            TextView tvBoxQuantity;
        }
    }
}