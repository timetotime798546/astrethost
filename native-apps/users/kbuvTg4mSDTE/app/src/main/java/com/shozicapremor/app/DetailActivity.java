package com.shozicapremor.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class DetailActivity extends Activity {

    private ShopActivity.Shoe product;
    private String selectedSize = "8";
    private int quantity = 1;
    private double itemPrice = 0.0;

    private TextView tvQtyValue, tvLiveSubtotal;
    private Button btnSize7, btnSize8, btnSize9, btnSize10, btnSize11;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        int shoeIndex = getIntent().getIntExtra("shoe_index", 0);
        product = ShopActivity.shoeDatabase.get(shoeIndex);
        itemPrice = product.price;

        // Elements
        TextView tvHeader = (TextView) findViewById(R.id.tvDetailTitleHeader);
        TextView tvCat = (TextView) findViewById(R.id.tvDetailCategory);
        TextView tvName = (TextView) findViewById(R.id.tvDetailName);
        TextView tvPrice = (TextView) findViewById(R.id.tvDetailPrice);
        TextView tvDesc = (TextView) findViewById(R.id.tvDetailDescription);

        tvHeader.setText(product.name);
        tvCat.setText(product.category.toUpperCase());
        tvName.setText(product.name);
        tvPrice.setText("$" + String.format("%.2f", itemPrice));
        tvDesc.setText(product.description);

        // Load multi-images preview to fulfill contextual horizontal scrolling multi-images requirement
        ImageView ivPrimary = (ImageView) findViewById(R.id.ivDetailPrimary);
        ImageView ivSec1 = (ImageView) findViewById(R.id.ivDetailSec1);
        ImageView ivSec2 = (ImageView) findViewById(R.id.ivDetailSec2);

        ImageLoader.loadImage(product.images[0], ivPrimary);
        ImageLoader.loadImage(product.images[1], ivSec1);
        ImageLoader.loadImage(product.images[2], ivSec2);

        // Size elements
        btnSize7 = (Button) findViewById(R.id.btnSize7);
        btnSize8 = (Button) findViewById(R.id.btnSize8);
        btnSize9 = (Button) findViewById(R.id.btnSize9);
        btnSize10 = (Button) findViewById(R.id.btnSize10);
        btnSize11 = (Button) findViewById(R.id.btnSize11);

        tvQtyValue = (TextView) findViewById(R.id.tvQtyValue);
        tvLiveSubtotal = (TextView) findViewById(R.id.tvLiveSubtotal);

        // Standard Back button
        findViewById(R.id.btnDetailBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Size Click Listeners
        btnSize7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { setSize("7", btnSize7); }
        });
        btnSize8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { setSize("8", btnSize8); }
        });
        btnSize9.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { setSize("9", btnSize9); }
        });
        btnSize10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { setSize("10", btnSize10); }
        });
        btnSize11.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { setSize("11", btnSize11); }
        });

        // Initialize default size styling
        setSize("8", btnSize8);

        // Quantity Buttons
        findViewById(R.id.btnQtyMinus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (quantity > 1) {
                    quantity--;
                    updateCalculation();
                }
            }
        });

        findViewById(R.id.btnQtyPlus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                quantity++;
                updateCalculation();
            }
        });

        // Add to Cart
        findViewById(R.id.btnAddToCart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CartManager.CartItem item = new CartManager.CartItem(
                    product.name,
                    product.price,
                    quantity,
                    "US " + selectedSize,
                    product.images[0]
                );
                CartManager.getInstance().addItem(item);
                Toast.makeText(DetailActivity.this, "Added exclusive footwear to cart", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        updateCalculation();
    }

    private void setSize(String size, Button activeBtn) {
        selectedSize = size;
        // Unselect all
        btnSize7.setBackgroundColor(0xFFF0F1F4); btnSize7.setTextColor(0xFF333333);
        btnSize8.setBackgroundColor(0xFFF0F1F4); btnSize8.setTextColor(0xFF333333);
        btnSize9.setBackgroundColor(0xFFF0F1F4); btnSize9.setTextColor(0xFF333333);
        btnSize10.setBackgroundColor(0xFFF0F1F4); btnSize10.setTextColor(0xFF333333);
        btnSize11.setBackgroundColor(0xFFF0F1F4); btnSize11.setTextColor(0xFF333333);

        // Select chosen one
        activeBtn.setBackgroundColor(0xFF111111);
        activeBtn.setTextColor(0xFFFFFFFF);
    }

    // MANDATORY LOCAL DYNAMIC CALCULATION
    private void updateCalculation() {
        tvQtyValue.setText(String.valueOf(quantity));
        double sub = itemPrice * quantity;
        tvLiveSubtotal.setText("$" + String.format("%.2f", sub));
    }
}