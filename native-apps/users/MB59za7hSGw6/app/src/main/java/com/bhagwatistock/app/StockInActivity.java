package com.bhagwatistock.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class StockInActivity extends Activity {

    private ListView lvStockIn;
    private TextView tvEmptyStockIn;
    private EditText etSearchStockIn;
    private ScanHistoryDbHelper dbHelper;
    private List<MobileModel> stockInList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_in);

        dbHelper = new ScanHistoryDbHelper(this);
        stockInList = new ArrayList<MobileModel>();

        lvStockIn = findViewById(R.id.lvStockIn);
        tvEmptyStockIn = findViewById(R.id.tvEmptyStockIn);
        etSearchStockIn = findViewById(R.id.etSearchStockIn);

        Button btnBack = findViewById(R.id.btnStockInBack);
        Button btnAddHeader = findViewById(R.id.btnAddStockInHeader);
        Button btnCamera = findViewById(R.id.btnStockInCamera);
        Button btnGallery = findViewById(R.id.btnStockInGallery);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnAddHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(StockInActivity.this, AddEditMobileActivity.class));
            }
        });

        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StockInActivity.this, MainActivity.class);
                intent.putExtra("trigger_camera", true);
                startActivity(intent);
            }
        });

        btnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StockInActivity.this, MainActivity.class);
                intent.putExtra("trigger_gallery", true);
                startActivity(intent);
            }
        });

        etSearchStockIn.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadStockInData(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStockInData(etSearchStockIn.getText().toString());
    }

    private void loadStockInData(String query) {
        stockInList.clear();
        stockInList.addAll(dbHelper.getAllMobiles("ALL", query, "ALL"));

        if (stockInList.isEmpty()) {
            tvEmptyStockIn.setVisibility(View.VISIBLE);
        } else {
            tvEmptyStockIn.setVisibility(View.GONE);
        }

        ArrayAdapter<MobileModel> adapter = new ArrayAdapter<MobileModel>(this, 0, stockInList) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_mobile_stock, parent, false);
                }
                final MobileModel m = getItem(position);
                if (m == null) return convertView;

                TextView tvTitle = convertView.findViewById(R.id.itemMobileTitle);
                TextView tvStatus = convertView.findViewById(R.id.itemMobileStatusBadge);
                TextView tvSpecs = convertView.findViewById(R.id.itemMobileSpecs);
                TextView tvImei = convertView.findViewById(R.id.itemMobileImei);
                TextView tvPrice = convertView.findViewById(R.id.itemMobilePrice);
                TextView tvDate = convertView.findViewById(R.id.itemMobileDate);

                Button btnStockOut = convertView.findViewById(R.id.btnItemStockOutAction);
                Button btnEdit = convertView.findViewById(R.id.btnItemEditMobile);
                Button btnDelete = convertView.findViewById(R.id.btnItemDeleteMobile);

                tvTitle.setText(m.getBrand() + " " + m.getModel());
                tvStatus.setText(m.getStatus());
                if ("SOLD".equals(m.getStatus())) {
                    tvStatus.setBackgroundColor(getResources().getColor(R.color.status_sold));
                    btnStockOut.setVisibility(View.GONE);
                } else {
                    tvStatus.setBackgroundColor(getResources().getColor(R.color.status_green));
                    btnStockOut.setVisibility(View.VISIBLE);
                }

                String specs = (m.getRam().isEmpty() ? "" : m.getRam() + " RAM | ") +
                               (m.getStorage().isEmpty() ? "" : m.getStorage() + " | ") +
                               m.getColor();
                tvSpecs.setText(specs);
                tvImei.setText("IMEI: " + m.getImei1() + (m.getSerialNumber().isEmpty() ? "" : " | S/N: " + m.getSerialNumber()));
                tvPrice.setText("Cost: ₹" + m.getPurchasePrice() + " | Selling: ₹" + m.getSellingPrice());
                tvDate.setText("Entry: " + m.getStockInDate());

                btnStockOut.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(StockInActivity.this, StockOutActivity.class);
                        intent.putExtra("preselect_id", m.getId());
                        startActivity(intent);
                    }
                });

                btnEdit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(StockInActivity.this, AddEditMobileActivity.class);
                        intent.putExtra("mobile_id", m.getId());
                        startActivity(intent);
                    }
                });

                btnDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String now = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(new java.util.Date());
                        dbHelper.deleteMobile(m.getId(), now);
                        loadStockInData(etSearchStockIn.getText().toString());
                        Toast.makeText(StockInActivity.this, "Mobile entry deleted", Toast.LENGTH_SHORT).show();
                    }
                });

                return convertView;
            }
        };

        lvStockIn.setAdapter(adapter);
    }
}