package com.imagevault.app;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class PreviewActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        final ImageView ivFull = (ImageView) findViewById(R.id.iv_full_preview);
        final ProgressBar progress = (ProgressBar) findViewById(R.id.preview_progress);
        final TextView tvError = (TextView) findViewById(R.id.tv_preview_error);
        Button btnClose = (Button) findViewById(R.id.btn_close_preview);

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        String imageId = getIntent().getStringExtra("image_id");
        String token = getIntent().getStringExtra("token");

        if (imageId == null || token == null) {
            progress.setVisibility(View.GONE);
            tvError.setVisibility(View.VISIBLE);
            tvError.setText("Missing image credentials");
            return;
        }

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
}