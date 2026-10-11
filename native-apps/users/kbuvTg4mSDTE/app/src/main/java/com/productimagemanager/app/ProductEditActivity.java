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

import java.io.InputStream;

public class ProductEditActivity extends Activity {

    public static final String EXTRA_PRODUCT_ID = "extra_product_id";
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

    private ProductDbHelper dbHelper;
    private BackendApi backendApi;
    private Handler mainHandler;

    private long productId = -1;
    private Product existingProduct = null;
    private byte[] preparedImageBytes = null;
    private String currentImageId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_edit);

        dbHelper = new ProductDbHelper(this);
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

        productId = getIntent().getLongExtra(EXTRA_PRODUCT_ID, -1);

        if (productId != -1) {
            setTitle("Edit Product");
            btnSaveProduct.setText("Update Product");
            loadProductDetails();
        } else {
            setTitle("Add Product");
            btnSaveProduct.setText("Save Product");
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

    private void loadProductDetails() {
        existingProduct = dbHelper.getProductById(productId);
        if (existingProduct != null) {
            editProductName.setText(existingProduct.getName());
            editProductPrice.setText(String.format(java.util.Locale.US, "%.2f", existingProduct.getPrice()));
            editProductDescription.setText(existingProduct.getDescription());
            currentImageId = existingProduct.getImageId();

            if (!currentImageId.isEmpty()) {
                tvImageSizeInfo.setText("Current Image Worker ID: " + currentImageId);
                fetchRemoteImagePreview(currentImageId);
            }
        }
    }

    private void fetchRemoteImagePreview(String imageId) {
        setLoading(true, "Loading existing image from Worker...");
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
                        tvImageSizeInfo.setText("Image on Worker could not be fetched (" + error + ")");
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
            setLoading(true, "Compressing image to <= 200 KB...");

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
                                tvImageSizeInfo.setText("Ready for upload: " + sizeKb + " KB (Max 200 KB)");
                            }
                        });
                    } catch (final Exception e) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                setLoading(false, null);
                                Toast.makeText(ProductEditActivity.this, "Failed to prepare image: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
            Toast.makeText(this, "Authentication required to upload to Image Worker", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        // If user picked a new image, upload to Image Worker first
        if (preparedImageBytes != null) {
            setLoading(true, "Uploading image to TrumpleDroid Image Worker...");
            ImageWorkerApi.uploadImage(preparedImageBytes, backendApi.getToken(), new ImageWorkerApi.UploadCallback() {
                @Override
                public void onSuccess(final String newImageId) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            // If this was an edit and old image existed, delete old image from worker in background
                            if (productId != -1 && !currentImageId.isEmpty() && !currentImageId.equals(newImageId)) {
                                ImageWorkerApi.deleteImage(currentImageId, backendApi.getToken(), new ImageWorkerApi.DeleteCallback() {
                                    @Override
                                    public void onSuccess() {}
                                    @Override
                                    public void onError(String error) {}
                                });
                            }
                            persistToDatabase(name, price, description, newImageId);
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
            // Keep current image ID
            persistToDatabase(name, price, description, currentImageId);
        }
    }

    private void persistToDatabase(String name, double price, String description, String imageId) {
        if (productId == -1) {
            Product newProduct = new Product(0, name, price, description, imageId, System.currentTimeMillis());
            long newId = dbHelper.insertProduct(newProduct);
            if (newId != -1) {
                Toast.makeText(this, "Product saved successfully", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Failed to insert product locally", Toast.LENGTH_SHORT).show();
                setLoading(false, null);
            }
        } else {
            Product updated = new Product(productId, name, price, description, imageId, System.currentTimeMillis());
            int rows = dbHelper.updateProduct(updated);
            if (rows > 0) {
                Toast.makeText(this, "Product updated successfully", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Failed to update product locally", Toast.LENGTH_SHORT).show();
                setLoading(false, null);
            }
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