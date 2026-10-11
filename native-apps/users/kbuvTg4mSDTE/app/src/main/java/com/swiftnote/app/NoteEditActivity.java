package com.swiftnote.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public class NoteEditActivity extends Activity {

    private EditText etNoteTitle;
    private EditText etNoteContent;
    private Spinner spCategory;
    private CheckBox cbPinned;
    private Button btnSave;
    private Button btnBack;
    private Button btnDelete;
    private TextView tvEditorHeader;
    private TextView tvLiveWordCount;
    private TextView tvLiveCharCount;
    private TextView tvLiveReadingTime;
    private ProgressBar pbEditLoading;

    private String currentNoteId = "";
    private BackendApi backendApi;
    private SessionManager sessionManager;

    private static final String[] CATEGORIES = new String[]{"Personal", "Work", "Ideas", "Study"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_edit);

        backendApi = new BackendApi(this);
        sessionManager = new SessionManager(this);

        initViews();
        setupSpinner();
        parseIntentData();
        setupRealTimeMetricListeners();
    }

    private void initViews() {
        etNoteTitle = (EditText) findViewById(R.id.etNoteTitle);
        etNoteContent = (EditText) findViewById(R.id.etNoteContent);
        spCategory = (Spinner) findViewById(R.id.spCategory);
        cbPinned = (CheckBox) findViewById(R.id.cbPinned);
        btnSave = (Button) findViewById(R.id.btnSave);
        btnBack = (Button) findViewById(R.id.btnBack);
        btnDelete = (Button) findViewById(R.id.btnDelete);
        tvEditorHeader = (TextView) findViewById(R.id.tvEditorHeader);
        tvLiveWordCount = (TextView) findViewById(R.id.tvLiveWordCount);
        tvLiveCharCount = (TextView) findViewById(R.id.tvLiveCharCount);
        tvLiveReadingTime = (TextView) findViewById(R.id.tvLiveReadingTime);
        pbEditLoading = (ProgressBar) findViewById(R.id.pbEditLoading);

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
                deleteCurrentNote();
            }
        });
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, CATEGORIES);
        spCategory.setAdapter(adapter);
    }

    private void parseIntentData() {
        if (getIntent().hasExtra("id")) {
            currentNoteId = getIntent().getStringExtra("id");
            String title = getIntent().getStringExtra("title");
            String content = getIntent().getStringExtra("content");
            String category = getIntent().getStringExtra("category");
            boolean pinned = getIntent().getBooleanExtra("pinned", false);

            tvEditorHeader.setText("Edit Note");
            btnDelete.setVisibility(View.VISIBLE);
            etNoteTitle.setText(title);
            etNoteContent.setText(content);
            cbPinned.setChecked(pinned);

            if (category != null) {
                for (int i = 0; i < CATEGORIES.length; i++) {
                    if (CATEGORIES[i].equalsIgnoreCase(category)) {
                        spCategory.setSelection(i);
                        break;
                    }
                }
            }
            recalculateMetrics(content);
        } else {
            tvEditorHeader.setText("New Note");
            btnDelete.setVisibility(View.GONE);
            recalculateMetrics("");
        }
    }

    private void setupRealTimeMetricListeners() {
        etNoteContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                recalculateMetrics(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void recalculateMetrics(String text) {
        int words = NoteCalculationHelper.calculateWordCount(text);
        int chars = NoteCalculationHelper.calculateCharacterCount(text);
        int readingMins = NoteCalculationHelper.calculateReadingTimeMinutes(words);

        tvLiveWordCount.setText("Words: " + words);
        tvLiveCharCount.setText("Chars: " + chars);
        tvLiveReadingTime.setText("Reading: " + readingMins + "m");
    }

    private void saveNote() {
        String title = etNoteTitle.getText().toString().trim();
        String content = etNoteContent.getText().toString().trim();
        String category = spCategory.getSelectedItem().toString();
        boolean pinned = cbPinned.isChecked();

        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(this, "Please enter a title or note body", Toast.LENGTH_SHORT).show();
            return;
        }

        NoteModel note = new NoteModel(currentNoteId, title, content, category, pinned, "#FFFFFF");
        JSONObject noteData = note.toDataJson();

        setLoading(true);

        if (currentNoteId == null || currentNoteId.isEmpty()) {
            // CREATE operation
            backendApi.createData(sessionManager.getToken(), "notes", noteData, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    setLoading(false);
                    Toast.makeText(NoteEditActivity.this, "Note saved!", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String errorMessage) {
                    setLoading(false);
                    Toast.makeText(NoteEditActivity.this, "Failed to save: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // UPDATE operation
            backendApi.updateData(sessionManager.getToken(), currentNoteId, noteData, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    setLoading(false);
                    Toast.makeText(NoteEditActivity.this, "Note updated!", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String errorMessage) {
                    setLoading(false);
                    Toast.makeText(NoteEditActivity.this, "Failed to update: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void deleteCurrentNote() {
        if (currentNoteId == null || currentNoteId.isEmpty()) {
            finish();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Note");
        builder.setMessage("Are you sure you want to delete this note?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setLoading(true);
                backendApi.deleteData(sessionManager.getToken(), currentNoteId, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        setLoading(false);
                        Toast.makeText(NoteEditActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        setLoading(false);
                        Toast.makeText(NoteEditActivity.this, "Delete failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void setLoading(boolean loading) {
        pbEditLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
        if (btnDelete.getVisibility() == View.VISIBLE) {
            btnDelete.setEnabled(!loading);
        }
    }
}