package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.util.ArrayList;

public class HistoryActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        ListView historyListView = (ListView) findViewById(R.id.history_list_view);

        ArrayList<String> history = getIntent().getStringArrayListExtra("history");

        if (history != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                android.R.id.text1,
                history
            );
            historyListView.setAdapter(adapter);
        }
    }
}