package com.alfariskirana.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class AddPurchaseActivity extends Activity {

    private DatabaseHelper dbHelper;
    private Spinner spProduct;
    private EditText etSupplier;
    private EditText etQty;
    private EditText etAmount;
    private List<Product> productList = new ArrayList<Product>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_purchase);

        dbHelper = new DatabaseHelper(this);

        spProduct = (Spinner) findViewById(R.id.sp_purchase_product);
        etSupplier = (EditText) findViewById(R.id.et_purchase_supplier);
        etQty = (EditText) findViewById(R.id.et_purchase_qty);
        etAmount = (EditText) findViewById(R.id.et_purchase_amount);
        Button btnSave = (Button) findViewById(R.id.btn_save_purchase);

        productList = dbHelper.getAllProducts("");
        List<String> prodNames = new ArrayList<String>();
        for (int i = 0; i < productList.size(); i++) {
            Product p = productList.get(i);
            prodNames.add(p.getName() + " (Current Stock: " + p.getStock() + " " + p.getUnit() + ")");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, prodNames);
        spProduct.setAdapter(adapter);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePurchase();
            }
        });
    }

    private void savePurchase() {
        if (productList.isEmpty()) {
            Toast.makeText(this, "No products available to restock", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedIdx = spProduct.getSelectedItemPosition();
        Product p = productList.get(selectedIdx);

        String supplier = etSupplier.getText().toString().trim();
        String qtyStr = etQty.getText().toString().trim();
        String amtStr = etAmount.getText().toString().trim();

        if (qtyStr.isEmpty() || amtStr.isEmpty()) {
            Toast.makeText(this, "Please enter quantity and purchase amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double qty = Double.parseDouble(qtyStr);
        double amt = Double.parseDouble(amtStr);

        dbHelper.recordPurchase(supplier, p.getId(), qty, amt);
        Toast.makeText(this, "Stock added successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }
}