package com.smartnotes.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class NoteEditorActivity extends Activity {

    private TextView tvHeader;
    private Spinner spinnerCategory;
    private EditText etTitle, etContent;
    private Button btnCancel, btnDelete, btnSave;

    private BackendApi api;
    private ProgressDialog progressDialog;
    private String noteId = null; // null if creating a new note
    private final String[] CATEGORIES = {"Personal", "Work", "Study", "Ideas", "Other"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        api = new BackendApi(this);

        tvHeader = (TextView) findViewById(R.id.tv_editor_title);
        spinnerCategory = (Spinner) findViewById(R.id.spinner_category);
        etTitle = (EditText) findViewById(R.id.et_title);
        etContent = (EditText) findViewById(R.id.et_content);
        btnCancel = (Button) findViewById(R.id.btn_cancel);
        btnDelete = (Button) findViewById(R.id.btn_delete);
        btnSave = (Button) findViewById(R.id.btn_save);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Processing dynamic operation...");
        progressDialog.setCancelable(false);

        // Populate spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, CATEGORIES);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(spinnerAdapter);

        // Read intent options to check if we are editing or creating
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("id")) {
            noteId = extras.getString("id");
            String title = extras.getString("title");
            String content = extras.getString("content");
            String category = extras.getString("category");

            tvHeader.setText("Edit Note");
            etTitle.setText(title);
            etContent.setText(content);
            btnDelete.setVisibility(View.VISIBLE);

            // Select category spinner item matching note
            for (int i = 0; i < CATEGORIES.length; i++) {
                if (CATEGORIES[i].equalsIgnoreCase(category)) {
                    spinnerCategory.setSelection(i);
                    break;
                }
            }
        }

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (noteId != null) {
                    deleteCurrentNote();
                }
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNoteData();
            }
        });
    }

    private void saveNoteData() {
        final String title = etTitle.getText().toString().trim();
        final String content = etContent.getText().toString().trim();
        final String category = spinnerCategory.getSelectedItem().toString();

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "Title and details content cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();

        if (noteId == null) {
            // Add note to collection
            api.createNote(title, content, category, new BackendApi.ApiCallback<Note>() {
                @Override
                public void onSuccess(Note result) {
                    progressDialog.dismiss();
                    Toast.makeText(NoteEditorActivity.this, "Note saved!", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String error) {
                    progressDialog.dismiss();
                    Toast.makeText(NoteEditorActivity.this, "Save error: " + error, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // Update note in collection
            api.updateNote(noteId, title, content, category, new BackendApi.ApiCallback<String>() {
                @Override
                public void onSuccess(String resultId) {
                    progressDialog.dismiss();
                    Toast.makeText(NoteEditorActivity.this, "Note updated!", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String error) {
                    progressDialog.dismiss();
                    Toast.makeText(NoteEditorActivity.this, "Update error: " + error, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void deleteCurrentNote() {
        progressDialog.show();
        api.deleteNote(noteId, new BackendApi.ApiCallback<String>() {
            @Override
            public void onSuccess(String resultId) {
                progressDialog.dismiss();
                Toast.makeText(NoteEditorActivity.this, "Note removed successfully", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String error) {
                progressDialog.dismiss();
                Toast.makeText(NoteEditorActivity.this, "Delete failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}