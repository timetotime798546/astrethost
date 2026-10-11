package com.imagevault.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.List;

public class MainActivity extends Activity {

    private static final int REQ_CODE_PICK_IMAGE = 101;

    private BackendApi backendApi;
    private GalleryAdapter adapter;
    private GridView gvGallery;
    private TextView tvEmptyGallery;
    private TextView tvUploadStatus;
    private ProgressBar mainProgress;
    private Button btnPickUpload;
    private Button btnRefresh;
    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        backendApi = new BackendApi(this);

        if (!backendApi.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        gvGallery = (GridView) findViewById(R.id.gv_gallery);
        tvEmptyGallery = (TextView) findViewById(R.id.tv_empty_gallery);
        tvUploadStatus = (TextView) findViewById(R.id.tv_upload_status);
        mainProgress = (ProgressBar) findViewById(R.id.main_progress);
        btnPickUpload = (Button) findViewById(R.id.btn_pick_upload);
        btnRefresh = (Button) findViewById(R.id.btn_refresh);
        btnLogout = (Button) findViewById(R.id.btn_logout);

        adapter = new GalleryAdapter(this, backendApi.getToken());
        gvGallery.setAdapter(adapter);

        adapter.setOnItemClickListener(new GalleryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(String imageId) {
                Intent intent = new Intent(MainActivity.this, PreviewActivity.class);
                intent.putExtra("image_id", imageId);
                intent.putExtra("token", backendApi.getToken());
                startActivity(intent);
            }
        });

        btnPickUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openPhotoPicker();
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadVaultImages();
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backendApi.logout(null);
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            }
        });

        loadVaultImages();
    }

    private void openPhotoPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), REQ_CODE_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE_PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            processAndUploadImage(imageUri);
        }
    }

    private void processAndUploadImage(final Uri imageUri) {
        tvUploadStatus.setText("Compressing image under 200 KB...");
        mainProgress.setVisibility(View.VISIBLE);
        btnPickUpload.setEnabled(false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final byte[] compressedBytes = ImageCompressor.compressUriToUnder200Kb(MainActivity.this, imageUri);
                    final int kb = compressedBytes.length / 1024;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvUploadStatus.setText("Uploading " + kb + " KB to Private Image Worker...");
                        }
                    });

                    ImageWorkerApi.uploadImage(backendApi.getToken(), compressedBytes, new ImageWorkerApi.UploadCallback() {
                        @Override
                        public void onSuccess(final String imageId) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mainProgress.setVisibility(View.GONE);
                                    btnPickUpload.setEnabled(true);
                                    tvUploadStatus.setText("Upload successful (" + kb + " KB)");
                                    adapter.addItem(imageId);
                                    tvEmptyGallery.setVisibility(View.GONE);
                                }
                            });
                        }

                        @Override
                        public void onError(final String message) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mainProgress.setVisibility(View.GONE);
                                    btnPickUpload.setEnabled(true);
                                    tvUploadStatus.setText("Upload failed: " + message);
                                }
                            });
                        }
                    });

                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mainProgress.setVisibility(View.GONE);
                            btnPickUpload.setEnabled(true);
                            tvUploadStatus.setText("Error preparing image: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    private void loadVaultImages() {
        mainProgress.setVisibility(View.VISIBLE);
        tvUploadStatus.setText("Fetching private vault photos...");

        ImageWorkerApi.listImages(backendApi.getToken(), new ImageWorkerApi.ImageListCallback() {
            @Override
            public void onSuccess(final List<String> imageIds) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mainProgress.setVisibility(View.GONE);
                        adapter.setItems(imageIds);
                        tvUploadStatus.setText(imageIds.size() + " photos securely stored in vault");
                        if (imageIds.isEmpty()) {
                            tvEmptyGallery.setVisibility(View.VISIBLE);
                        } else {
                            tvEmptyGallery.setVisibility(View.GONE);
                        }
                    }
                });
            }

            @Override
            public void onError(final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mainProgress.setVisibility(View.GONE);
                        tvUploadStatus.setText("Error: " + message);
                    }
                });
            }
        });
    }
}