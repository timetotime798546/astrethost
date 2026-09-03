package com.smartnotes.app;

import android.app.Activity;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class NoteActivity extends Activity {

    private NoteDbHelper dbHelper;
    private TextView tvEditorTitle;
    private EditText etTitle;
    private EditText etContent;
    private Spinner spinnerCategory;
    private Button btnSave;
    private Button btnDelete;

    private long noteId = -1;
    private String[] categories = {"Personal", "Work", "Study", "Ideas", "Urgent", "Others"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        dbHelper = new NoteDbHelper(this);

        tvEditorTitle = (TextView) findViewById(R.id.tv_editor_title);
        etTitle = (EditText) findViewById(R.id.et_title);
        etContent = (EditText) findViewById(R.id.et_content);
        spinnerCategory = (Spinner) findViewById(R.id.spinner_category);
        btnSave = (Button) findViewById(R.id.btn_save);
        btnDelete = (Button) findViewById(R.id.btn_delete);

        // Spinner Setup without filter options
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(spinnerAdapter);

        // Setup components if launching in Edit mode
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            noteId = extras.getLong("NOTE_ID", -1);
            if (noteId != -1) {
                tvEditorTitle.setText("Edit Note");
                etTitle.setText(extras.getString("NOTE_TITLE", ""));
                etContent.setText(extras.getString("NOTE_CONTENT", ""));
                String currentCategory = extras.getString("NOTE_CATEGORY", "Personal");
                
                for (int i = 0; i < categories.length; i++) {
                    if (categories[i].equalsIgnoreCase(currentCategory)) {
                        spinnerCategory.setSelection(i);
                        break;
                    }
                }
                
                btnDelete.setVisibility(View.VISIBLE);
            }
        }

        // Save Note
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
            }
        });

        // Delete Note
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteNote();
            }
        });
    }

    private void saveNote() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();

        if (title.isEmpty()) {
            Toast.makeText(this, "Title cannot be empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (content.isEmpty()) {
            Toast.makeText(this, "Content cannot be empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(NoteDbHelper.COLUMN_TITLE, title);
        values.put(NoteDbHelper.COLUMN_CONTENT, content);
        values.put(NoteDbHelper.COLUMN_CATEGORY, category);

        if (noteId == -1) {
            // New entry creation
            long newRowId = db.insert(NoteDbHelper.TABLE_NAME, null, values);
            if (newRowId != -1) {
                Toast.makeText(this, "Note saved successfully!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Error saving note", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Existing row modification
            int count = db.update(
                    NoteDbHelper.TABLE_NAME,
                    values,
                    NoteDbHelper.COLUMN_ID + " = ?",
                    new String[]{String.valueOf(noteId)}
            );
            if (count > 0) {
                Toast.makeText(this, "Note updated successfully!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Error updating note", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void deleteNote() {
        if (noteId == -1) return;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count = db.delete(
                NoteDbHelper.TABLE_NAME,
                NoteDbHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(noteId)}
        );

        if (count > 0) {
            Toast.makeText(this, "Note deleted successfully!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Error deleting note", Toast.LENGTH_SHORT).show();
        }
    }
}