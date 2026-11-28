package com.bhagwatistock.app;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StockOutActivity extends Activity {

    private EditText etSearchAvailable, etSalePrice, etCustomerName, etCustomerPhone, etSaleNotes;
    private ListView lvAvailableMobiles;
    private LinearLayout layoutSelectedInfo;
    private TextView tvSelectedModel, tvSelectedImei, tvSelectedPurchase;

    private ScanHistoryDbHelper dbHelper;
    private List<MobileModel> availableList;
    private MobileModel selectedMobile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_out);

        dbHelper = new ScanHistoryDbHelper(this);
        availableList = new ArrayList<MobileModel>();

        etSearchAvailable = findViewById(R.id.etSearchAvailable);
        etSalePrice = findViewById(R.id.etSalePrice);
        etCustomerName = findViewById(R.id.etCustomerName);
        etCustomerPhone = findViewById(R.id.etCustomerPhone);
        etSaleNotes = findViewById(R.id.etSaleNotes);

        lvAvailableMobiles = findViewById(R.id.lvAvailableMobiles);
        layoutSelectedInfo = findViewById(R.id.layoutSelectedInfo);
        tvSelectedModel = findViewById(R.id.tvSelectedModel);
        tvSelectedImei = findViewById(R.id.tvSelectedImei);
        tvSelectedPurchase = findViewById(R.id.tvSelectedPurchase);

        Button btnBack = findViewById(R.id.btnStockOutBack);
        Button btnConfirm = findViewById(R.id.btnConfirmStockOut);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        long preselectId = getIntent().getLongExtra("preselect_id", -1);
        if (preselectId != -1) {
            MobileModel m = dbHelper.getMobileById(preselectId);
            if (m != null && "AVAILABLE".equals(m.getStatus())) {
                selectMobileForSale(m);
            }
        }

        etSearchAvailable.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadAvailableMobiles(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        lvAvailableMobiles.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < availableList.size()) {
                    selectMobileForSale(availableList.get(position));
                }
            }
        });

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmStockOutTransaction();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAvailableMobiles(etSearchAvailable.getText().toString());
    }

    private void loadAvailableMobiles(String query) {
        availableList.clear();
        availableList.addAll(dbHelper.getAllMobiles("AVAILABLE", query, "ALL"));

        ArrayAdapter<MobileModel> adapter = new ArrayAdapter<MobileModel>(this, android.R.layout.simple_list_item_2, android.R.id.text1, availableList) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                TextView t1 = v.findViewById(android.R.id.text1);
                TextView t2 = v.findViewById(android.R.id.text2);
                MobileModel m = getItem(position);
                if (m != null) {
                    t1.setText(m.getBrand() + " " + m.getModel() + " (" + m.getColor() + ")");
                    t2.setText("IMEI: " + m.getImei1() + " | Cost: ₹" + m.getPurchasePrice());
                }
                return v;
            }
        };

        lvAvailableMobiles.setAdapter(adapter);
    }

    private void selectMobileForSale(MobileModel m) {
        selectedMobile = m;
        layoutSelectedInfo.setVisibility(View.VISIBLE);
        tvSelectedModel.setText(m.getBrand() + " " + m.getModel());
        tvSelectedImei.setText("IMEI 1: " + m.getImei1() + (m.getImei2().isEmpty() ? "" : " / IMEI 2: " + m.getImei2()));
        tvSelectedPurchase.setText("Purchase Price: ₹" + m.getPurchasePrice() + " | Rec. Target: ₹" + m.getSellingPrice());

        if (m.getSellingPrice() > 0) {
            etSalePrice.setText(String.valueOf(m.getSellingPrice()));
        }
    }

    private void confirmStockOutTransaction() {
        if (selectedMobile == null) {
            Toast.makeText(this, "Please select an available mobile from the list above", Toast.LENGTH_SHORT).show();
            return;
        }

        String priceStr = etSalePrice.getText().toString().trim();
        if (priceStr.isEmpty()) {
            Toast.makeText(this, "Please enter the selling price", Toast.LENGTH_SHORT).show();
            return;
        }

        double salePrice = 0.0;
        try {
            salePrice = Double.parseDouble(priceStr);
        } catch (Exception e) {
            Toast.makeText(this, "Invalid selling price", Toast.LENGTH_SHORT).show();
            return;
        }

        String customerName = etCustomerName.getText().toString().trim();
        String customerPhone = etCustomerPhone.getText().toString().trim();
        String notes = etSaleNotes.getText().toString().trim();
        String stockOutDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

        boolean success = dbHelper.markMobileAsSold(selectedMobile.getId(), salePrice, customerName, customerPhone, notes, stockOutDate);

        if (success) {
            Toast.makeText(this, "Sale recorded! Mobile marked as SOLD.", Toast.LENGTH_LONG).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to update mobile status", Toast.LENGTH_SHORT).show();
        }
    }
}