package com.lockplaytube.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.lockplaytube.app.R;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends Activity {

    private ListView listHistory;
    private TextView txtEmpty;
    private Button btnClearAll;

    private HistoryDbHelper dbHelper;
    private HistoryAdapter adapter;
    private List<HistoryItem> historyItems = new ArrayList<HistoryItem>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        dbHelper = new HistoryDbHelper(this);

        listHistory = (ListView) findViewById(R.id.list_history);
        txtEmpty = (TextView) findViewById(R.id.txt_empty);
        btnClearAll = (Button) findViewById(R.id.btn_clear_all);

        adapter = new HistoryAdapter(this, historyItems, new HistoryAdapter.OnDeleteClickListener() {
            @Override
            public void onDeleteClick(HistoryItem item) {
                dbHelper.deleteHistory(item.getId());
                loadHistory();
            }
        });
        listHistory.setAdapter(adapter);

        listHistory.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HistoryItem item = historyItems.get(position);
                Intent intent = new Intent();
                intent.putExtra("selected_url", item.getUrl());
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        btnClearAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showClearConfirmDialog();
            }
        });

        loadHistory();
    }

    private void loadHistory() {
        historyItems.clear();
        historyItems.addAll(dbHelper.getAllHistory());
        adapter.notifyDataSetChanged();

        if (historyItems.isEmpty()) {
            txtEmpty.setVisibility(View.VISIBLE);
            listHistory.setVisibility(View.GONE);
            btnClearAll.setEnabled(false);
        } else {
            txtEmpty.setVisibility(View.GONE);
            listHistory.setVisibility(View.VISIBLE);
            btnClearAll.setEnabled(true);
        }
    }

    private void showClearConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_clear_title)
                .setMessage(R.string.confirm_clear_msg)
                .setPositiveButton(R.string.clear, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dbHelper.clearAllHistory();
                        loadHistory();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}