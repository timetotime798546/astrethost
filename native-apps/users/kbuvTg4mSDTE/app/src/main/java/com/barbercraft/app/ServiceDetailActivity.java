package com.barbercraft.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ServiceDetailActivity extends Activity {

    private ServiceItem currentService;
    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_detail);

        backendApi = new BackendApi(this);

        currentService = (ServiceItem) getIntent().getSerializableExtra("service_item");
        if (currentService == null) {
            Toast.makeText(this, "Service not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Button btnBack = (Button) findViewById(R.id.btnBack);
        TextView tvDetailTitle = (TextView) findViewById(R.id.tvDetailTitle);
        TextView tvDetailName = (TextView) findViewById(R.id.tvDetailName);
        TextView tvDetailPrice = (TextView) findViewById(R.id.tvDetailPrice);
        TextView tvDetailDuration = (TextView) findViewById(R.id.tvDetailDuration);
        TextView tvDetailDescription = (TextView) findViewById(R.id.tvDetailDescription);
        Button btnProceedBooking = (Button) findViewById(R.id.btnProceedBooking);
        LinearLayout galleryContainer = (LinearLayout) findViewById(R.id.galleryContainer);

        tvDetailTitle.setText(currentService.getName());
        tvDetailName.setText(currentService.getName());
        tvDetailPrice.setText("$" + String.format("%.2f", currentService.getPrice()));
        tvDetailDuration.setText("Estimated Time: " + currentService.getDurationMinutes() + " Minutes");
        tvDetailDescription.setText(currentService.getDescription());

        // Dynamic Multi-Image Handling: populate responsive dynamic image assets
        String[] gallery = currentService.getGalleryImages();
        if (gallery != null && gallery.length > 0) {
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            int imageWidth = (int) (260 * metrics.density);
            int imageHeight = (int) (180 * metrics.density);
            int marginEnd = (int) (12 * metrics.density);

            for (int i = 0; i < gallery.length; i++) {
                ImageView imageView = new ImageView(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(imageWidth, imageHeight);
                params.rightMargin = marginEnd;
                imageView.setLayoutParams(params);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setBackgroundColor(0xFFCBD5E1);

                ImageLoader.getInstance().displayImage(gallery[i], imageView);
                galleryContainer.addView(imageView);
            }
        }

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnProceedBooking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!backendApi.isLoggedIn()) {
                    Toast.makeText(ServiceDetailActivity.this, "Please sign in before booking", Toast.LENGTH_SHORT).show();
                    Intent loginIntent = new Intent(ServiceDetailActivity.this, LoginActivity.class);
                    startActivity(loginIntent);
                } else {
                    Intent bookIntent = new Intent(ServiceDetailActivity.this, BookingActivity.class);
                    bookIntent.putExtra("service_item", currentService);
                    startActivity(bookIntent);
                }
            }
        });
    }
}