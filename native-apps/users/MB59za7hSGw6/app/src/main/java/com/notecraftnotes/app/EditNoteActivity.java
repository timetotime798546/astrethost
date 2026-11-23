package com.notecraftnotes.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class EditNoteActivity extends Activity {

    public static final String EXTRA_NOTE_ID = "extra_note_id";
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_CONTENT = "extra_content";
    public static final String EXTRA_CATEGORY = "extra_category";
    public static final String EXTRA_CATEGORIES_LIST = "extra_categories_list";

    private EditText etTitle, etContent;
    private Spinner spCategory;
    private Button btnSave, btnCancel;
    private TextView tvHeaderTitle, tvLiveWordCount;
    private ProgressBar pbSaving;

    private BackendApi api;
    private String noteId = "";
    private ArrayList<String> categoriesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);

        api = new BackendApi(this);

        etTitle = findViewById(R.id.etNoteTitle);
        etContent = findViewById(R.id.etNoteContent);
        spCategory = findViewById(R.id.spNoteCategory);
        btnSave = findViewById(R.id.btnSaveNote);
        btnCancel = findViewById(R.id.btnCancelEdit);
        tvHeaderTitle = findViewById(R.id.tvEditHeaderTitle);
        tvLiveWordCount = findViewById(R.id.tvLiveWordCount);
        pbSaving = findViewById(R.id.pbEditSaving);

        Intent intent = getIntent();
        if (intent != null) {
            noteId = intent.getStringExtra(EXTRA_NOTE_ID);
            if (noteId == null) noteId = "";

            String title = intent.getStringExtra(EXTRA_TITLE);
            String content = intent.getStringExtra(EXTRA_CONTENT);
            String category = intent.getStringExtra(EXTRA_CATEGORY);

            categoriesList = intent.getStringArrayListExtra(EXTRA_CATEGORIES_LIST);

            if (title != null) etTitle.setText(title);
            if (content != null) etContent.setText(content);

            setupCategorySpinner(category);
        } else {
            setupCategorySpinner("General");
        }

        if (!noteId.isEmpty()) {
            tvHeaderTitle.setText("Edit Note");
        } else {
            tvHeaderTitle.setText("New Note");
        }

        updateLiveWordCount();

        etContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateLiveWordCount();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
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
    }

    private void setupCategorySpinner(String selectedCategory) {
        if (categoriesList == null || categoriesList.isEmpty()) {
            categoriesList = new ArrayList<>();
            categoriesList.add("General");
            categoriesList.add("Personal");
            categoriesList.add("Work");
            categoriesList.add("Ideas");
            categoriesList.add("Tasks");
        }

        // Ensure selectedCategory is in spinner list
        if (selectedCategory != null && !selectedCategory.isEmpty() && !categoriesList.contains(selectedCategory)) {
            categoriesList.add(selectedCategory);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categoriesList
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(adapter);

        if (selectedCategory != null) {
            int pos = categoriesList.indexOf(selectedCategory);
            if (pos >= 0) {
                spCategory.setSelection(pos);
            }
        }
    }

    private void updateLiveWordCount() {
        String text = etContent.getText().toString();
        int charCount = text.length();
        int wordCount = 0;
        if (!text.trim().isEmpty()) {
            wordCount = text.trim().split("\\s+").length;
        }
        tvLiveWordCount.setText("Words: " + wordCount + " | Characters: " + charCount);
    }

    private void saveNote() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();
        String category = spCategory.getSelectedItem() != null ? spCategory.getSelectedItem().toString() : "General";

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a note title", Toast.LENGTH_SHORT).show();
            return;
        }

        String dateStr = new SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(new Date());

        NoteModel note = new NoteModel(noteId, title, content, category, dateStr);

        setLoading(true);

        if (noteId.isEmpty()) {
            // Create new note via POST /data
            api.createData("notes", note.toDataJson(), new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    setLoading(false);
                    Toast.makeText(EditNoteActivity.this, "Note saved", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                }

                @Override
                public void onError(String error) {
                    setLoading(false);
                    Toast.makeText(EditNoteActivity.this, "Error saving note: " + error, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // Update existing note via PUT /data
            api.updateData(noteId, note.toDataJson(), new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    setLoading(false);
                    Toast.makeText(EditNoteActivity.this, "Note updated", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                }

                @Override
                public void onError(String error) {
                    setLoading(false);
                    Toast.makeText(EditNoteActivity.this, "Error updating note: " + error, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void setLoading(boolean loading) {
        pbSaving.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
    }
}