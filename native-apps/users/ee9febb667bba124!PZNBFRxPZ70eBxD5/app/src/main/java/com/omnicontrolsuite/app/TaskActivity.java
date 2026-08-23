package com.omnicontrolsuite.app;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import java.util.ArrayList;

public class TaskActivity extends Activity {

    private DatabaseHelper dbHelper;
    private EditText editTaskName;
    private ListView listTasks;
    private TaskAdapter adapter;
    private ArrayList<TaskItem> taskList;

    private static class TaskItem {
        int id;
        String title;
        int status;

        TaskItem(int id, String title, int status) {
            this.id = id;
            this.title = title;
            this.status = status;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

        dbHelper = new DatabaseHelper(this);
        taskList = new ArrayList<TaskItem>();

        editTaskName = (EditText) findViewById(R.id.editTaskName);
        listTasks = (ListView) findViewById(R.id.listTasks);
        Button btnBack = (Button) findViewById(R.id.btnBack);
        Button btnAddTask = (Button) findViewById(R.id.btnAddTask);
        Button btnPurge = (Button) findViewById(R.id.btnClearAllTasks);

        adapter = new TaskAdapter();
        listTasks.setAdapter(adapter);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnAddTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String val = editTaskName.getText().toString().trim();
                if (!val.isEmpty()) {
                    dbHelper.insertTask(val);
                    editTaskName.setText("");
                    refreshTaskList();
                } else {
                    Toast.makeText(TaskActivity.this, "Please type a task name first", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnPurge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dbHelper.purgeTasks();
                refreshTaskList();
                Toast.makeText(TaskActivity.this, "Queue cleared completely", Toast.LENGTH_SHORT).show();
            }
        });

        refreshTaskList();
    }

    private void refreshTaskList() {
        taskList.clear();
        Cursor cursor = dbHelper.getAllTasks();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TASK_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TASK_TITLE));
                int status = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TASK_STATUS));
                taskList.add(new TaskItem(id, title, status));
            }
            cursor.close();
        }
        adapter.notifyDataSetChanged();
    }

    private class TaskAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return taskList.size();
        }

        @Override
        public Object getItem(int position) {
            return taskList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return taskList.get(position).id;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(TaskActivity.this).inflate(android.R.layout.simple_list_item_multiple_choice, parent, false);
            }
            final TaskItem item = taskList.get(position);
            final CheckBox chk = (CheckBox) convertView.findViewById(android.R.id.text1);
            chk.setText(item.title);
            
            // Prevent checkbox listener loop triggers
            chk.setOnCheckedChangeListener(null);
            chk.setChecked(item.status == 1);
            
            chk.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    int newStatus = isChecked ? 1 : 0;
                    dbHelper.updateTaskStatus(item.id, newStatus);
                    item.status = newStatus;
                }
            });

            return convertView;
        }
    }
}