package com.cloudnotes.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class NoteActivity extends Activity {

    private EditText etTitle;
    private EditText etContent;
    private Spinner spinnerCategory;
    private Button btnSave;
    private Button btnDelete;
    private Button btnBack;
    private TextView tvHeaderTitle;
    private ProgressBar pbLoading;

    private BackendApi backendApi;
    private Note editingNote = null;
    private boolean isEditMode = false;

    private final String[] categories = {"General", "Work", "Personal", "Ideas", "Todo"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(android.R.style.Theme_Material_Light_NoActionBar);
        setContentView(R.layout.activity_note);

        backendApi = new BackendApi(this);

        etTitle = (EditText) findViewById(R.id.et_note_title);
        etContent = (EditText) findViewById(R.id.et_note_content);
        spinnerCategory = (Spinner) findViewById(R.id.spinner_category);
        btnSave = (Button) findViewById(R.id.btn_save);
        btnDelete = (Button) findViewById(R.id.btn_delete);
        btnBack = (Button) findViewById(R.id.btn_back);
        tvHeaderTitle = (TextView) findViewById(R.id.tv_editor_title);
        pbLoading = (ProgressBar) findViewById(R.id.pb_editor_loading);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(spinnerAdapter);

        if (getIntent().hasExtra("note")) {
            editingNote = (Note) getIntent().getSerializableExtra("note");
            isEditMode = true;
        }

        if (isEditMode && editingNote != null) {
            tvHeaderTitle.setText("Edit Note");
            etTitle.setText(editingNote.title);
            etContent.setText(editingNote.content);
            btnDelete.setVisibility(View.VISIBLE);

            for (int i = 0; i < categories.length; i++) {
                if (categories[i].equalsIgnoreCase(editingNote.category)) {
                    spinnerCategory.setSelection(i);
                    break;
                }
            }
        } else {
            tvHeaderTitle.setText("New Note");
            btnDelete.setVisibility(View.GONE);
        }

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
            }
        });

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
            Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        pbLoading.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);
        btnDelete.setEnabled(false);

        if (isEditMode && editingNote != null) {
            backendApi.updateNote(editingNote.id, title, content, category, new BackendApi.ApiCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(NoteActivity.this, "Note updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String error) {
                    pbLoading.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    btnDelete.setEnabled(true);
                    Toast.makeText(NoteActivity.this, "Failed to update note: " + error, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            backendApi.createNote(title, content, category, new BackendApi.ApiCallback<Note>() {
                @Override
                public void onSuccess(Note note) {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(NoteActivity.this, "Note saved successfully", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String error) {
                    pbLoading.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    Toast.makeText(NoteActivity.this, "Failed to save note: " + error, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void deleteNote() {
        if (editingNote == null) return;

        pbLoading.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);
        btnDelete.setEnabled(false);

        backendApi.deleteNote(editingNote.id, new BackendApi.ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(NoteActivity.this, "Note deleted successfully", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String error) {
                pbLoading.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                btnDelete.setEnabled(true);
                Toast.makeText(NoteActivity.this, "Failed to delete note: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}