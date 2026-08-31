package com.omnitoolspremium.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
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
import android.widget.TextView;
import android.widget.CheckedTextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TodoActivity extends Activity {

    private EditText editNewTask;
    private ListView listTasks;
    private List<String> taskList;
    private TaskAdapter adapter;
    private SharedPreferences sharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo);

        editNewTask = (EditText) findViewById(R.id.edit_new_task);
        listTasks = (ListView) findViewById(R.id.list_tasks);
        Button btnAddTask = (Button) findViewById(R.id.btn_add_task);

        sharedPrefs = getSharedPreferences("PremiumTodoPrefs", Context.MODE_PRIVATE);
        taskList = new ArrayList<String>();

        Set<String> savedTasks = sharedPrefs.getStringSet("TASKS_SET", new HashSet<String>());
        taskList.addAll(savedTasks);

        adapter = new TaskAdapter(this, taskList);
        listTasks.setAdapter(adapter);

        btnAddTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewTask();
            }
        });
    }

    private void addNewTask() {
        String taskName = editNewTask.getText().toString().trim();
        if (taskName.isEmpty()) {
            Toast.makeText(this, "Task description empty", Toast.LENGTH_SHORT).show();
            return;
        }

        taskList.add(taskName);
        saveTasks();
        adapter.notifyDataSetChanged();
        editNewTask.setText("");
    }

    private void saveTasks() {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putStringSet("TASKS_SET", new HashSet<String>(taskList));
        editor.apply();
    }

    private class TaskAdapter extends BaseAdapter {
        private Context context;
        private List<String> tasks;

        public TaskAdapter(Context context, List<String> tasks) {
            this.context = context;
            this.tasks = tasks;
        }

        @Override
        public int getCount() {
            return tasks.size();
        }

        @Override
        public Object getItem(int position) {
            return tasks.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_multiple_choice, parent, false);
            }

            final CheckedTextView textView = (CheckedTextView) convertView.findViewById(android.R.id.text1);
            final String task = tasks.get(position);
            
            textView.setText(task);
            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tasks.remove(position);
                    saveTasks();
                    notifyDataSetChanged();
                    Toast.makeText(context, "Task Cleared!", Toast.LENGTH_SHORT).show();
                }
            });

            return convertView;
        }
    }
}