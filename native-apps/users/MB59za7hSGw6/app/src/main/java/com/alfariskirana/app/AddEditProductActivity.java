package com.alfariskirana.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class AddEditProductActivity extends Activity {

    private DatabaseHelper dbHelper;
    private long productId = -1;

    private TextView tvTitle;
    private EditText etName;
    private EditText etCategory;
    private EditText etUnit;
    private EditText etCost;
    private EditText etSellPrice;
    private EditText etStock;
    private Button btnSave;
    private Button btnDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_product);

        dbHelper = new DatabaseHelper(this);

        tvTitle = (TextView) findViewById(R.id.tv_form_title);
        etName = (EditText) findViewById(R.id.et_product_name);
        etCategory = (EditText) findViewById(R.id.et_product_category);
        etUnit = (EditText) findViewById(R.id.et_product_unit);
        etCost = (EditText) findViewById(R.id.et_product_cost);
        etSellPrice = (EditText) findViewById(R.id.et_product_sell_price);
        etStock = (EditText) findViewById(R.id.et_product_stock);
        btnSave = (Button) findViewById(R.id.btn_save_product);
        btnDelete = (Button) findViewById(R.id.btn_delete_product);

        productId = getIntent().getLongExtra("product_id", -1);
        if (productId > 0) {
            tvTitle.setText("Edit Grocery Item");
            btnDelete.setVisibility(View.VISIBLE);
            loadProductData();
        }

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProduct();
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dbHelper.deleteProduct(productId);
                Toast.makeText(AddEditProductActivity.this, "Product deleted", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void loadProductData() {
        Product p = dbHelper.getProductById(productId);
        if (p != null) {
            etName.setText(p.getName());
            etCategory.setText(p.getCategory());
            etUnit.setText(p.getUnit());
            etCost.setText(String.valueOf(p.getPurchasePrice()));
            etSellPrice.setText(String.valueOf(p.getSellingPrice()));
            etStock.setText(String.valueOf(p.getStock()));
        }
    }

    private void saveProduct() {
        String name = etName.getText().toString().trim();
        String category = etCategory.getText().toString().trim();
        String unit = etUnit.getText().toString().trim();
        String costStr = etCost.getText().toString().trim();
        String sellStr = etSellPrice.getText().toString().trim();
        String stockStr = etStock.getText().toString().trim();

        if (name.isEmpty() || sellStr.isEmpty()) {
            Toast.makeText(this, "Please enter product name and selling price", Toast.LENGTH_SHORT).show();
            return;
        }

        double cost = costStr.isEmpty() ? 0 : Double.parseDouble(costStr);
        double sellPrice = Double.parseDouble(sellStr);
        double stock = stockStr.isEmpty() ? 0 : Double.parseDouble(stockStr);
        if (unit.isEmpty()) unit = "kg";

        Product p = new Product(productId, name, category, cost, sellPrice, stock, unit);
        dbHelper.saveProduct(p);
        Toast.makeText(this, "Product saved successfully", Toast.LENGTH_SHORT).show();
        finish();
    }
}