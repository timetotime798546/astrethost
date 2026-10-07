package com.shozicapremor.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ProductDetailActivity extends Activity {

    private TextView btnBack, detailCategory, detailName, detailPrice, detailDescription, detailBottomPrice;
    private TextView colorGold, colorCharcoal;
    private TextView size8, size9, size10, size11;
    private LinearLayout layoutImageGallery;
    private Button btnAddToCart;

    private Product selectedProduct;
    private String chosenColor = "Gold";
    private String chosenSize = "9";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        btnBack = findViewById(R.id.btn_detail_back);
        detailCategory = findViewById(R.id.detail_category);
        detailName = findViewById(R.id.detail_name);
        detailPrice = findViewById(R.id.detail_price);
        detailDescription = findViewById(R.id.detail_description);
        detailBottomPrice = findViewById(R.id.detail_bottom_price);

        colorGold = findViewById(R.id.color_option_1);
        colorCharcoal = findViewById(R.id.color_option_2);

        size8 = findViewById(R.id.size_option_8);
        size9 = findViewById(R.id.size_option_9);
        size10 = findViewById(R.id.size_option_10);
        size11 = findViewById(R.id.size_option_11);

        layoutImageGallery = findViewById(R.id.layout_image_gallery);
        btnAddToCart = findViewById(R.id.btn_add_to_cart);

        String shoeId = getIntent().getStringExtra("shoe_id");
        if (shoeId == null) shoeId = "1";

        // Query catalog using static centralized Product method to avoid MainActivity initialization crash
        selectedProduct = Product.getProductById(shoeId);

        if (selectedProduct == null) {
            Toast.makeText(this, "Shoe catalog mapping error.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        populateUI();
        setupInteractiveControls();

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnAddToCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProductToCart();
            }
        });
    }

    private void populateUI() {
        detailCategory.setText(selectedProduct.getCategory().toUpperCase());
        detailName.setText(selectedProduct.getName());
        detailPrice.setText("$" + String.format("%.2f", selectedProduct.getPrice()));
        detailDescription.setText(selectedProduct.getDescription());
        detailBottomPrice.setText("$" + String.format("%.2f", selectedProduct.getPrice()));

        // POPULATE ADAPTIVE MULTI-IMAGE GALLERIES DYNAMICALLY
        layoutImageGallery.removeAllViews();
        String[] urls = selectedProduct.getImageUrls();
        for (int i = 0; i < urls.length; i++) {
            String url = urls[i];
            ImageView img = new ImageView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    (int) (260 * getResources().getDisplayMetrics().density),
                    LinearLayout.LayoutParams.MATCH_PARENT
            );
            lp.setMargins(0, 0, 16, 0);
            img.setLayoutParams(lp);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            img.setBackgroundColor(getResources().getColor(R.color.bg_card));
            layoutImageGallery.addView(img);

            ImageLoader.displayImage(url, img);
        }
    }

    private void setupInteractiveControls() {
        colorGold.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chosenColor = "Gold";
                colorGold.setBackgroundColor(getResources().getColor(R.color.accent_gold));
                colorGold.setTextColor(getResources().getColor(R.color.bg_dark));

                colorCharcoal.setBackgroundColor(0xFF2C2E42);
                colorCharcoal.setTextColor(getResources().getColor(R.color.text_primary));
            }
        });

        colorCharcoal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chosenColor = "Charcoal";
                colorCharcoal.setBackgroundColor(getResources().getColor(R.color.accent_gold));
                colorCharcoal.setTextColor(getResources().getColor(R.color.bg_dark));

                colorGold.setBackgroundColor(0xFF2C2E42);
                colorGold.setTextColor(getResources().getColor(R.color.text_primary));
            }
        });

        // Sizes setup
        final TextView[] sizes = {size8, size9, size10, size11};
        for (int i = 0; i < sizes.length; i++) {
            final TextView tv = sizes[i];
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    chosenSize = tv.getText().toString();
                    for (int j = 0; j < sizes.length; j++) {
                        TextView ot = sizes[j];
                        ot.setBackgroundColor(0xFF2C2E42);
                        ot.setTextColor(getResources().getColor(R.color.text_primary));
                    }
                    tv.setBackgroundColor(getResources().getColor(R.color.accent_gold));
                    tv.setTextColor(getResources().getColor(R.color.bg_dark));
                }
            });
        }
    }

    private void saveProductToCart() {
        SharedPreferences cartPrefs = getSharedPreferences("CartPrefs", MODE_PRIVATE);
        cartPrefs.edit()
                .putString("item_name", selectedProduct.getName())
                .putFloat("item_price", (float) selectedProduct.getPrice())
                .putString("item_size", chosenSize)
                .putString("item_color", chosenColor)
                .apply();

        Toast.makeText(this, "Success! Added " + selectedProduct.getName() + " to checkout cart.", Toast.LENGTH_LONG).show();
        finish();
    }
}