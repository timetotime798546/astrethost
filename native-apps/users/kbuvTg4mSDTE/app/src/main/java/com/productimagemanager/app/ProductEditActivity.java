package com.productimagemanager.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public class ProductEditActivity extends Activity {

    public static final String EXTRA_PRODUCT_ID = "extra_product_id";
    public static final String EXTRA_PRODUCT_NAME = "extra_product_name";
    public static final String EXTRA_PRODUCT_PRICE = "extra_product_price";
    public static final String EXTRA_PRODUCT_DESC = "extra_product_desc";
    public static final String EXTRA_PRODUCT_IMAGE_ID = "extra_product_image_id";

    private static final int REQ_CHOOSE_IMAGE = 101;

    private EditText editProductName;
    private EditText editProductPrice;
    private EditText editProductDescription;
    private ImageView imagePreview;
    private Button btnSelectImage;
    private TextView tvImageSizeInfo;
    private Button btnSaveProduct;
    private ProgressBar progressBar;
    private TextView tvStatusMessage;

    private BackendApi backendApi;
    private Handler mainHandler;

    private String productId = "";
    private byte[] preparedImageBytes = null;
    private String currentImageId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_edit);

        backendApi = new BackendApi(this);
        mainHandler = new Handler(Looper.getMainLooper());

        editProductName = (EditText) findViewById(R.id.editProductName);
        editProductPrice = (EditText) findViewById(R.id.editProductPrice);
        editProductDescription = (EditText) findViewById(R.id.editProductDescription);
        imagePreview = (ImageView) findViewById(R.id.imagePreview);
        btnSelectImage = (Button) findViewById(R.id.btnSelectImage);
        tvImageSizeInfo = (TextView) findViewById(R.id.tvImageSizeInfo);
        btnSaveProduct = (Button) findViewById(R.id.btnSaveProduct);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        tvStatusMessage = (TextView) findViewById(R.id.tvStatusMessage);

        productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        if (productId != null && !productId.trim().isEmpty()) {
            setTitle("Edit Cloud Product");
            btnSaveProduct.setText("Update Product in Cloud");

            String name = getIntent().getStringExtra(EXTRA_PRODUCT_NAME);
            double price = getIntent().getDoubleExtra(EXTRA_PRODUCT_PRICE, 0.0);
            String desc = getIntent().getStringExtra(EXTRA_PRODUCT_DESC);
            currentImageId = getIntent().getStringExtra(EXTRA_PRODUCT_IMAGE_ID);
            if (currentImageId == null) {
                currentImageId = "";
            }

            if (name != null) editProductName.setText(name);
            if (price > 0) editProductPrice.setText(String.format(java.util.Locale.US, "%.2f", price));
            if (desc != null) editProductDescription.setText(desc);

            if (!currentImageId.isEmpty()) {
                tvImageSizeInfo.setText("Current Image Worker ID: " + currentImageId);
                fetchRemoteImagePreview(currentImageId);
            }
        } else {
            productId = "";
            setTitle("Add Cloud Product");
            btnSaveProduct.setText("Save to Cloud");
        }

        btnSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGalleryPicker();
            }
        });

        btnSaveProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProductProcess();
            }
        });
    }

    private void fetchRemoteImagePreview(String imageId) {
        setLoading(true, "Loading existing image from Image Worker...");
        ImageWorkerApi.fetchImage(imageId, backendApi.getToken(), new ImageWorkerApi.ImageDownloadCallback() {
            @Override
            public void onSuccess(final Bitmap bitmap) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        setLoading(false, null);
                        imagePreview.setImageBitmap(bitmap);
                    }
                });
            }

            @Override
            public void onError(final String error) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        setLoading(false, null);
                        tvImageSizeInfo.setText("Image preview unavailable (" + error + ")");
                    }
                });
            }
        });
    }

    private void openGalleryPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Product Image"), REQ_CHOOSE_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CHOOSE_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            final Uri selectedUri = data.getData();
            setLoading(true, "Compressing image for upload (<= 200 KB)...");

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final byte[] compressed = ImageUtils.compressAndResizeUri(ProductEditActivity.this, selectedUri);
                        final Bitmap preview = BitmapFactory.decodeByteArray(compressed, 0, compressed.length);

                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                setLoading(false, null);
                                preparedImageBytes = compressed;
                                imagePreview.setImageBitmap(preview);
                                int sizeKb = compressed.length / 1024;
                                tvImageSizeInfo.setText("Ready for upload: " + sizeKb + " KB (Limit: 200 KB)");
                            }
                        });
                    } catch (final Exception e) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                setLoading(false, null);
                                Toast.makeText(ProductEditActivity.this, "Failed to compress image: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            }).start();
        }
    }

    private void saveProductProcess() {
        final String name = editProductName.getText().toString().trim();
        final String priceStr = editProductPrice.getText().toString().trim();
        final String description = editProductDescription.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Product name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        final double price;
        try {
            price = Double.parseDouble(priceStr);
            if (price < 0) {
                Toast.makeText(this, "Price cannot be negative", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid numeric price", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!backendApi.isLoggedIn()) {
            Toast.makeText(this, "Sign-in required to save to cloud", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        // If a new image was chosen, upload to Image Worker first
        if (preparedImageBytes != null) {
            setLoading(true, "Uploading image to TrumpleDroid Image Worker...");
            ImageWorkerApi.uploadImage(preparedImageBytes, backendApi.getToken(), new ImageWorkerApi.UploadCallback() {
                @Override
                public void onSuccess(final String newImageId) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            // If updating and old image exists, clean it up remotely
                            if (!productId.isEmpty() && !currentImageId.isEmpty() && !currentImageId.equals(newImageId)) {
                                ImageWorkerApi.deleteImage(currentImageId, backendApi.getToken(), new ImageWorkerApi.DeleteCallback() {
                                    @Override
                                    public void onSuccess() {}
                                    @Override
                                    public void onError(String error) {}
                                });
                            }
                            persistToCloud(name, price, description, newImageId);
                        }
                    });
                }

                @Override
                public void onError(final String error) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            setLoading(false, error);
                            Toast.makeText(ProductEditActivity.this, "Image upload failed: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        } else {
            // Keep existing image ID
            persistToCloud(name, price, description, currentImageId);
        }
    }

    private void persistToCloud(String name, double price, String description, String imageId) {
        setLoading(true, "Persisting product record to backend...");
        try {
            JSONObject dataObj = new JSONObject();
            dataObj.put("name", name);
            dataObj.put("price", price);
            dataObj.put("description", description);
            dataObj.put("image_id", imageId);

            if (productId.isEmpty()) {
                // CREATE: POST /data
                backendApi.createRecord("products", dataObj, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                setLoading(false, null);
                                Toast.makeText(ProductEditActivity.this, "Product created in cloud successfully", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            }
                        });
                    }

                    @Override
                    public void onError(final String error) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                setLoading(false, error);
                                Toast.makeText(ProductEditActivity.this, "Failed to create in cloud: " + error, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            } else {
                // UPDATE: PUT /data
                backendApi.updateRecord(productId, dataObj, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                setLoading(false, null);
                                Toast.makeText(ProductEditActivity.this, "Product updated in cloud successfully", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            }
                        });
                    }

                    @Override
                    public void onError(final String error) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                setLoading(false, error);
                                Toast.makeText(ProductEditActivity.this, "Failed to update in cloud: " + error, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            }
        } catch (Exception e) {
            setLoading(false, e.getMessage());
            Toast.makeText(this, "JSON build error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setLoading(final boolean loading, final String message) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
                btnSaveProduct.setEnabled(!loading);
                btnSelectImage.setEnabled(!loading);
                if (message != null && !message.isEmpty()) {
                    tvStatusMessage.setVisibility(View.VISIBLE);
                    tvStatusMessage.setText(message);
                } else {
                    tvStatusMessage.setVisibility(View.GONE);
                }
            }
        });
    }
}