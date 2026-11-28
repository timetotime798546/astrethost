package com.smartocrscanner.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class AllMobilesActivity extends Activity {

    private ListView lvAllMobiles;
    private TextView tvEmptyAllMobiles;
    private EditText etSearchAll;
    private Spinner spinnerStatusFilter;

    private ScanHistoryDbHelper dbHelper;
    private List<MobileModel> allMobilesList;
    private String selectedStatus = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_mobiles);

        dbHelper = new ScanHistoryDbHelper(this);
        allMobilesList = new ArrayList<MobileModel>();

        lvAllMobiles = findViewById(R.id.lvAllMobiles);
        tvEmptyAllMobiles = findViewById(R.id.tvEmptyAllMobiles);
        etSearchAll = findViewById(R.id.etSearchAll);
        spinnerStatusFilter = findViewById(R.id.spinnerStatusFilter);

        Button btnBack = findViewById(R.id.btnAllMobilesBack);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        String[] statuses = new String[]{"ALL", "AVAILABLE", "SOLD"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, statuses);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatusFilter.setAdapter(spinnerAdapter);

        spinnerStatusFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedStatus = (String) parent.getItemAtPosition(position);
                loadAllMobilesData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        etSearchAll.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadAllMobilesData();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAllMobilesData();
    }

    private void loadAllMobilesData() {
        String query = etSearchAll.getText().toString();
        allMobilesList.clear();
        allMobilesList.addAll(dbHelper.getAllMobiles(selectedStatus, query, "ALL"));

        if (allMobilesList.isEmpty()) {
            tvEmptyAllMobiles.setVisibility(View.VISIBLE);
        } else {
            tvEmptyAllMobiles.setVisibility(View.GONE);
        }

        ArrayAdapter<MobileModel> adapter = new ArrayAdapter<MobileModel>(this, 0, allMobilesList) {
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
                tvPrice.setText("Cost: $" + m.getPurchasePrice() + " | Sale: $" + m.getSellingPrice());
                tvDate.setText("In: " + m.getStockInDate());

                btnStockOut.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(AllMobilesActivity.this, StockOutActivity.class);
                        intent.putExtra("preselect_id", m.getId());
                        startActivity(intent);
                    }
                });

                btnEdit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(AllMobilesActivity.this, AddEditMobileActivity.class);
                        intent.putExtra("mobile_id", m.getId());
                        startActivity(intent);
                    }
                });

                btnDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String now = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(new java.util.Date());
                        dbHelper.deleteMobile(m.getId(), now);
                        loadAllMobilesData();
                        Toast.makeText(AllMobilesActivity.this, "Mobile deleted", Toast.LENGTH_SHORT).show();
                    }
                });

                return convertView;
            }
        };

        lvAllMobiles.setAdapter(adapter);
    }
}