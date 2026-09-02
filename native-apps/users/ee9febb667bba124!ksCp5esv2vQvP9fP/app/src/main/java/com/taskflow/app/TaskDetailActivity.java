package com.taskflow.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class TaskDetailActivity extends Activity {

    private DatabaseHelper dbHelper;
    private int loggedUserId;
    private int taskId = -1;
    private boolean isFavState = false;

    private EditText etTitle, etDesc, etDueDate;
    private Spinner spCategory;
    private CheckBox cbCompleted;
    private Button btnSave, btnDelete, btnShare, btnFavToggle, btnBack;
    private TextView tvHeader;

    private String[] categories = {"Work", "Personal", "Urgent", "Education", "Health", "Finance"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        dbHelper = new DatabaseHelper(this);
        SharedPreferences prefs = getSharedPreferences("taskflow_session", MODE_PRIVATE);
        loggedUserId = prefs.getInt("logged_user_id", -1);

        etTitle = (EditText) findViewById(R.id.etDetailTaskTitle);
        etDesc = (EditText) findViewById(R.id.etDetailTaskDesc);
        etDueDate = (EditText) findViewById(R.id.etDetailDueDate);
        spCategory = (Spinner) findViewById(R.id.spDetailCategory);
        cbCompleted = (CheckBox) findViewById(R.id.cbDetailIsCompleted);
        tvHeader = (TextView) findViewById(R.id.tvDetailTitleHeader);

        btnSave = (Button) findViewById(R.id.btnDetailSave);
        btnDelete = (Button) findViewById(R.id.btnDetailDelete);
        btnShare = (Button) findViewById(R.id.btnDetailShare);
        btnFavToggle = (Button) findViewById(R.id.btnDetailToggleFavorite);
        btnBack = (Button) findViewById(R.id.btnDetailBack);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spCategory.setAdapter(adapter);

        // Access detail intent validation checks
        if (getIntent().hasExtra("task_id")) {
            taskId = getIntent().getIntExtra("task_id", -1);
        }

        if (taskId != -1) {
            tvHeader.setText("Edit Task");
            btnDelete.setVisibility(View.VISIBLE);
            loadTaskData();
        } else {
            tvHeader.setText("New Task");
            btnDelete.setVisibility(View.GONE);
        }

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnFavToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isFavState = !isFavState;
                btnFavToggle.setText(isFavState ? "★" : "☆");
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performSaveOperation();
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(TaskDetailActivity.this)
                    .setTitle("Confirm Deletion")
                    .setMessage("Are you sure you want to permanently delete this task item?")
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            SQLiteDatabase db = dbHelper.getWritableDatabase();
                            db.delete(DatabaseHelper.TABLE_ITEMS, DatabaseHelper.ITEM_COL_ID + "=?", new String[]{String.valueOf(taskId)});
                            dbHelper.logActivity(loggedUserId, "Deleted Task", "Successfully deleted task index key " + taskId);
                            Toast.makeText(TaskDetailActivity.this, "Task deleted successfully!", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
            }
        });

        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String shareText = "TaskFlow Action Tracker\n"
                        + "Task: " + etTitle.getText().toString() + "\n"
                        + "Desc: " + etDesc.getText().toString() + "\n"
                        + "Category: " + spCategory.getSelectedItem().toString() + "\n"
                        + "Due date: " + etDueDate.getText().toString();
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Track: " + etTitle.getText().toString());
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                startActivity(Intent.createChooser(shareIntent, "Share task parameters via"));
            }
        });
    }

    private void loadTaskData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_ITEMS, null, DatabaseHelper.ITEM_COL_ID + "=?",
                new String[]{String.valueOf(taskId)}, null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                etTitle.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ITEM_COL_TITLE)));
                etDesc.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ITEM_COL_DESC)));
                etDueDate.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ITEM_COL_DUE_DATE)));
                cbCompleted.setChecked(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.ITEM_COL_COMPLETED)) == 1);

                isFavState = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.ITEM_COL_FAVORITE)) == 1;
                btnFavToggle.setText(isFavState ? "★" : "☆");

                String cat = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ITEM_COL_CATEGORY));
                for (int i = 0; i < categories.length; i++) {
                    if (categories[i].equalsIgnoreCase(cat)) {
                        spCategory.setSelection(i);
                        break;
                    }
                }
            }
            cursor.close();
        }
    }

    private void performSaveOperation() {
        String t = etTitle.getText().toString().trim();
        String d = etDesc.getText().toString().trim();
        String date = etDueDate.getText().toString().trim();
        String cat = spCategory.getSelectedItem().toString();
        boolean isCompleted = cbCompleted.isChecked();

        if (t.isEmpty()) {
            Toast.makeText(this, "Validation Error: Title is mandatory!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (date.isEmpty()) {
            date = "No due date";
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.ITEM_COL_TITLE, t);
        values.put(DatabaseHelper.ITEM_COL_DESC, d);
        values.put(DatabaseHelper.ITEM_COL_CATEGORY, cat);
        values.put(DatabaseHelper.ITEM_COL_DUE_DATE, date);
        values.put(DatabaseHelper.ITEM_COL_COMPLETED, isCompleted ? 1 : 0);
        values.put(DatabaseHelper.ITEM_COL_FAVORITE, isFavState ? 1 : 0);

        if (taskId == -1) {
            values.put(DatabaseHelper.ITEM_COL_USER_ID, loggedUserId);
            db.insert(DatabaseHelper.TABLE_ITEMS, null, values);
            dbHelper.logActivity(loggedUserId, "Created task", "Successfully initialized task title: " + t);
            Toast.makeText(this, "Task item successfully saved!", Toast.LENGTH_SHORT).show();
        } else {
            db.update(DatabaseHelper.TABLE_ITEMS, values, DatabaseHelper.ITEM_COL_ID + "=?", new String[]{String.valueOf(taskId)});
            dbHelper.logActivity(loggedUserId, "Updated task", "Modified task specifications of index: " + t);
            Toast.makeText(this, "Task properties updated!", Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}