package com.youtubebackgroundplayer.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

public class HistoryActivity extends Activity {

    private ImageButton btnBack;
    private Button btnClearAll;
    private ListView lvHistory;
    private TextView tvEmpty;

    private HistoryDbHelper dbHelper;
    private HistoryAdapter adapter;
    private List<HistoryItem> historyList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        dbHelper = new HistoryDbHelper(this);

        btnBack = findViewById(R.id.btn_back_history);
        btnClearAll = findViewById(R.id.btn_clear_all);
        lvHistory = findViewById(R.id.lv_history);
        tvEmpty = findViewById(R.id.tv_empty);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnClearAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmClearAll();
            }
        });

        lvHistory.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HistoryItem item = historyList.get(position);
                Intent resultIntent = new Intent();
                resultIntent.putExtra("target_url", item.getUrl());
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });

        loadHistoryData();
    }

    private void loadHistoryData() {
        historyList = dbHelper.getAllHistory();
        if (historyList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            lvHistory.setVisibility(View.GONE);
            btnClearAll.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            lvHistory.setVisibility(View.VISIBLE);
            btnClearAll.setVisibility(View.VISIBLE);

            adapter = new HistoryAdapter(this, historyList, new HistoryAdapter.OnDeleteClickListener() {
                @Override
                public void onDeleteClick(HistoryItem item) {
                    dbHelper.deleteHistory(item.getId());
                    loadHistoryData();
                }
            });
            lvHistory.setAdapter(adapter);
        }
    }

    private void confirmClearAll() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_history)
                .setMessage(R.string.confirm_clear_history)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dbHelper.clearAllHistory();
                        loadHistoryData();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }
}