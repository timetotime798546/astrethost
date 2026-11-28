package com.bhagwatistock.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends Activity {

    private ListView lvHistory;
    private TextView tvEmptyHistory;
    private ScanHistoryDbHelper dbHelper;
    private List<InventoryLogModel> logList;
    private String currentActionFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        dbHelper = new ScanHistoryDbHelper(this);
        logList = new ArrayList<InventoryLogModel>();

        lvHistory = findViewById(R.id.lvHistory);
        tvEmptyHistory = findViewById(R.id.tvEmptyHistory);

        Button btnBack = findViewById(R.id.btnHistoryBack);
        Button btnClearAll = findViewById(R.id.btnClearAllHistory);

        Button btnFilterAll = findViewById(R.id.btnFilterAllLogs);
        Button btnFilterStockIn = findViewById(R.id.btnFilterStockInLogs);
        Button btnFilterStockOut = findViewById(R.id.btnFilterStockOutLogs);
        Button btnFilterEdits = findViewById(R.id.btnFilterEditLogs);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnClearAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmClearLogs();
            }
        });

        btnFilterAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentActionFilter = "ALL";
                loadLogs();
            }
        });

        btnFilterStockIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentActionFilter = "STOCK_IN";
                loadLogs();
            }
        });

        btnFilterStockOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentActionFilter = "STOCK_OUT";
                loadLogs();
            }
        });

        btnFilterEdits.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentActionFilter = "EDIT";
                loadLogs();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLogs();
    }

    private void loadLogs() {
        logList.clear();
        logList.addAll(dbHelper.getInventoryLogs(currentActionFilter));

        if (logList.isEmpty()) {
            tvEmptyHistory.setVisibility(View.VISIBLE);
        } else {
            tvEmptyHistory.setVisibility(View.GONE);
        }

        ArrayAdapter<InventoryLogModel> adapter = new ArrayAdapter<InventoryLogModel>(this, 0, logList) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_inventory_log, parent, false);
                }

                InventoryLogModel log = getItem(position);
                if (log != null) {
                    TextView tvAction = convertView.findViewById(R.id.itemLogAction);
                    TextView tvTimestamp = convertView.findViewById(R.id.itemLogTimestamp);
                    TextView tvDetails = convertView.findViewById(R.id.itemLogDetails);

                    tvAction.setText(log.getAction());
                    tvTimestamp.setText(log.getTimestamp());
                    tvDetails.setText(log.getDetails());

                    if ("STOCK_IN".equals(log.getAction())) {
                        tvAction.setBackgroundColor(getResources().getColor(R.color.accent));
                    } else if ("STOCK_OUT".equals(log.getAction())) {
                        tvAction.setBackgroundColor(getResources().getColor(R.color.status_sold));
                    } else if ("DELETE".equals(log.getAction())) {
                        tvAction.setBackgroundColor(getResources().getColor(R.color.danger));
                    } else {
                        tvAction.setBackgroundColor(getResources().getColor(R.color.primary));
                    }
                }
                return convertView;
            }
        };

        lvHistory.setAdapter(adapter);
    }

    private void confirmClearLogs() {
        if (logList.isEmpty()) {
            Toast.makeText(this, "Audit logs are empty", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Clear Audit Logs")
                .setMessage("Are you sure you want to clear all inventory transaction logs?")
                .setPositiveButton("Clear All", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dbHelper.clearInventoryLogs();
                        loadLogs();
                        Toast.makeText(HistoryActivity.this, "Logs cleared", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}