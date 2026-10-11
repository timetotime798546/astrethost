package com.imagevault.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class PreviewActivity extends Activity {

    public static final String EXTRA_DELETED_ID = "deleted_image_id";

    private String imageId;
    private String token;
    private ImageView ivFull;
    private ProgressBar progress;
    private TextView tvError;
    private TextView tvPreviewId;
    private Button btnDelete;
    private Button btnClose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        ivFull = (ImageView) findViewById(R.id.iv_full_preview);
        progress = (ProgressBar) findViewById(R.id.preview_progress);
        tvError = (TextView) findViewById(R.id.tv_preview_error);
        tvPreviewId = (TextView) findViewById(R.id.tv_preview_id);
        btnDelete = (Button) findViewById(R.id.btn_delete_preview);
        btnClose = (Button) findViewById(R.id.btn_close_preview);

        imageId = getIntent().getStringExtra("image_id");
        token = getIntent().getStringExtra("token");

        if (imageId != null) {
            tvPreviewId.setText(imageId);
        }

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmAndDelete();
            }
        });

        if (imageId == null || token == null) {
            progress.setVisibility(View.GONE);
            tvError.setVisibility(View.VISIBLE);
            tvError.setText("Missing image credentials");
            btnDelete.setEnabled(false);
            return;
        }

        loadImage();
    }

    private void loadImage() {
        progress.setVisibility(View.VISIBLE);
        tvError.setVisibility(View.GONE);

        ImageWorkerApi.fetchImage(token, imageId, new ImageWorkerApi.BitmapCallback() {
            @Override
            public void onSuccess(final Bitmap bitmap) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress.setVisibility(View.GONE);
                        ivFull.setImageBitmap(bitmap);
                    }
                });
            }

            @Override
            public void onError(final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress.setVisibility(View.GONE);
                        tvError.setVisibility(View.VISIBLE);
                        tvError.setText("Failed to load image:\n" + message);
                    }
                });
            }
        });
    }

    private void confirmAndDelete() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Image");
        builder.setMessage("Are you sure you want to permanently delete this photo from your private vault?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                performDelete();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void performDelete() {
        progress.setVisibility(View.VISIBLE);
        btnDelete.setEnabled(false);
        btnClose.setEnabled(false);

        ImageWorkerApi.deleteImage(token, imageId, new ImageWorkerApi.DeleteCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(PreviewActivity.this, "Photo deleted successfully", Toast.LENGTH_SHORT).show();
                        Intent result = new Intent();
                        result.putExtra(EXTRA_DELETED_ID, imageId);
                        setResult(RESULT_OK, result);
                        finish();
                    }
                });
            }

            @Override
            public void onError(final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress.setVisibility(View.GONE);
                        btnDelete.setEnabled(true);
                        btnClose.setEnabled(true);
                        Toast.makeText(PreviewActivity.this, "Failed to delete: " + message, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}