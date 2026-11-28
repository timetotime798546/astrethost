package com.smartocrscanner.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends Activity implements HistoryAdapter.OnScanDeleteListener {

    private ListView lvHistory;
    private TextView tvEmptyHistory;
    private EditText etSearchHistory;
    private ScanHistoryDbHelper dbHelper;
    private HistoryAdapter adapter;
    private List<ScanModel> scanList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        dbHelper = new ScanHistoryDbHelper(this);
        scanList = new ArrayList<ScanModel>();

        lvHistory = findViewById(R.id.lvHistory);
        tvEmptyHistory = findViewById(R.id.tvEmptyHistory);
        etSearchHistory = findViewById(R.id.etSearchHistory);

        Button btnHistoryBack = findViewById(R.id.btnHistoryBack);
        Button btnClearAllHistory = findViewById(R.id.btnClearAllHistory);

        adapter = new HistoryAdapter(this, scanList, this);
        lvHistory.setAdapter(adapter);

        btnHistoryBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnClearAllHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmClearAll();
            }
        });

        etSearchHistory.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadHistoryData(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistoryData(etSearchHistory.getText().toString());
    }

    private void loadHistoryData(String query) {
        scanList.clear();
        if (query != null && !query.trim().isEmpty()) {
            scanList.addAll(dbHelper.searchScans(query.trim()));
        } else {
            scanList.addAll(dbHelper.getAllScans());
        }

        adapter.notifyDataSetChanged();

        if (scanList.isEmpty()) {
            tvEmptyHistory.setVisibility(View.VISIBLE);
        } else {
            tvEmptyHistory.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDelete(final ScanModel model) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Scan Entry")
                .setMessage("Are you sure you want to delete this scan entry from history?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dbHelper.deleteScan(model.getId());
                        loadHistoryData(etSearchHistory.getText().toString());
                        Toast.makeText(HistoryActivity.this, "Scan entry deleted", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmClearAll() {
        if (scanList.isEmpty()) {
            Toast.makeText(this, "History is already empty", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Clear All History")
                .setMessage("Are you sure you want to delete all saved scan logs? This action cannot be undone.")
                .setPositiveButton("Clear All", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dbHelper.clearAllScans();
                        loadHistoryData("");
                        Toast.makeText(HistoryActivity.this, "All scan history cleared", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}