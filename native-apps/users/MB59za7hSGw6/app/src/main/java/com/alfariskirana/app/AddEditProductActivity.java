package com.alfariskirana.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class AddEditProductActivity extends Activity {

    private DatabaseHelper dbHelper;
    private long productId = -1;

    private TextView tvTitle;
    private EditText etName;
    private EditText etCategory;
    private Spinner spUnit;
    private EditText etCost;
    private EditText etSellPrice;
    private EditText etStock;
    private Button btnSave;
    private Button btnDelete;

    private final String[] unitOptions = {"किलो (KG)", "पीस (Piece)"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_product);

        dbHelper = new DatabaseHelper(this);

        tvTitle = (TextView) findViewById(R.id.tv_form_title);
        etName = (EditText) findViewById(R.id.et_product_name);
        etCategory = (EditText) findViewById(R.id.et_product_category);
        spUnit = (Spinner) findViewById(R.id.sp_product_unit);
        etCost = (EditText) findViewById(R.id.et_product_cost);
        etSellPrice = (EditText) findViewById(R.id.et_product_sell_price);
        etStock = (EditText) findViewById(R.id.et_product_stock);
        btnSave = (Button) findViewById(R.id.btn_save_product);
        btnDelete = (Button) findViewById(R.id.btn_delete_product);

        ArrayAdapter<String> unitAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, unitOptions);
        spUnit.setAdapter(unitAdapter);

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
            etCost.setText(String.valueOf(p.getPurchasePrice()));
            etSellPrice.setText(String.valueOf(p.getSellingPrice()));
            etStock.setText(String.valueOf(p.getStock()));

            if (p.getUnit() != null) {
                String u = p.getUnit().toLowerCase();
                if (u.contains("kg") || u.contains("kilo") || u.contains("किलो")) {
                    spUnit.setSelection(0);
                } else {
                    spUnit.setSelection(1);
                }
            }
        }
    }

    private void saveProduct() {
        String name = etName.getText().toString().trim();
        String category = etCategory.getText().toString().trim();
        String unit = spUnit.getSelectedItem().toString();
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

        Product p = new Product(productId, name, category, cost, sellPrice, stock, unit);
        dbHelper.saveProduct(p);
        Toast.makeText(this, "Product saved successfully", Toast.LENGTH_SHORT).show();
        finish();
    }
}