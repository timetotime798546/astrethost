package com.tasksync.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends Activity {

    private ArrayList<String> tasks;
    private int completedCount = 0;
    private TextView completionStats;
    private EditText taskInput;
    private Button addButton;
    private ListView taskListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_