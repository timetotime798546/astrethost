package com.inventorysalesmanager.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.util.Locale;

public class ProductDetailActivity extends Activity {

    private DatabaseHelper dbHelper;
    private long productId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        dbHelper = new DatabaseHelper(this);
        productId = getIntent().getLongExtra("product_id", -1);

        TextView tvName = (TextView) findViewById(R.id.detail_name);
        TextView tvSku = (TextView) findViewById(R.id.detail_sku);
        TextView tvPurchase = (TextView) findViewById(R.id.detail_purchase_price);
        TextView tvSelling = (TextView) findViewById(R.id.detail_selling_price);
        TextView tvStock = (TextView) findViewById(R.id.detail_stock);
        TextView tvMinStock = (TextView) findViewById(R.id.detail_min_stock);

        TextView tvUnitsSold = (TextView) findViewById(R.id.perf_units_sold);
        TextView tvRevenue = (TextView) findViewById(R.id.perf_revenue);
        TextView tvProfit = (TextView) findViewById(R.id.perf_profit);

        Button btnBack = (Button) findViewById(R.id.btn_back);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Load details
        Product p = dbHelper.getProduct(productId);
        if (p != null) {
            tvName.setText(p.getName());
            tvSku.setText("SKU: " + p.getSku());
            tvPurchase.setText(String.format(Locale.getDefault(), "$%.2f", p.getPurchasePrice()));
            tvSelling.setText(String.format(Locale.getDefault(), "$%.2f", p.getSellingPrice()));
            tvStock.setText(p.getStockQty() + " units");
            tvMinStock.setText(p.getMinStock() + " units");

            if (p.isLowStock()) {
                tvStock.setTextColor(getResources().getColor(R.color.alert_red));
            }

            int unitsSold = dbHelper.getProductUnitsSold(productId);
            double revenue = dbHelper.getProductRevenueGenerated(productId);
            double cost = unitsSold * p.getPurchasePrice();
            double profit = revenue - cost;

            tvUnitsSold.setText(unitsSold + " units");
            tvRevenue.setText(String.format(Locale.getDefault(), "$%.2f", revenue));
            tvProfit.setText(String.format(Locale.getDefault(), "$%.2f", profit));
        } else {
            finish();
        }
    }
}