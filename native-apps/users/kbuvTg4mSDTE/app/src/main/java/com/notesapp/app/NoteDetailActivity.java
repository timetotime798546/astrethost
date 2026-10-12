package com.notesapp.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public class NoteDetailActivity extends Activity {
    private EditText etNoteTitle, etNoteContent;
    private Spinner spinnerCategory;
    private CheckBox cbPinned;
    private RadioGroup rgColor;
    private RadioButton rbBlue, rbGreen, rbOrange, rbPurple;
    private TextView tvLiveWordCount, tvLiveCharCount;
    private Button btnBack, btnSaveNote, btnDeleteNote;
    private ProgressBar pbDetailLoading;

    private BackendApi backendApi;
    private String authToken = "";
    private String noteId = "";
    private boolean isEditMode = false;

    private static final String[] CATEGORIES = {"Personal", "Work", "Ideas", "Todo"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);

        SharedPreferences prefs = getSharedPreferences("notes_prefs", Context.MODE_PRIVATE);
        authToken = prefs.getString("auth_token", "");

        backendApi = new BackendApi(this);

        etNoteTitle = findViewById(R.id.etNoteTitle);
        etNoteContent = findViewById(R.id.etNoteContent);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        cbPinned = findViewById(R.id.cbPinned);
        rgColor = findViewById(R.id.rgColor);
        rbBlue = findViewById(R.id.rbBlue);
        rbGreen = findViewById(R.id.rbGreen);
        rbOrange = findViewById(R.id.rbOrange);
        rbPurple = findViewById(R.id.rbPurple);
        tvLiveWordCount = findViewById(R.id.tvLiveWordCount);
        tvLiveCharCount = findViewById(R.id.tvLiveCharCount);
        btnBack = findViewById(R.id.btnBack);
        btnSaveNote = findViewById(R.id.btnSaveNote);
        btnDeleteNote = findViewById(R.id.btnDeleteNote);
        pbDetailLoading = findViewById(R.id.pbDetailLoading);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, CATEGORIES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        if (getIntent().hasExtra("note_id")) {
            isEditMode = true;
            noteId = getIntent().getStringExtra("note_id");
            etNoteTitle.setText(getIntent().getStringExtra("note_title"));
            etNoteContent.setText(getIntent().getStringExtra("note_content"));
            cbPinned.setChecked(getIntent().getBooleanExtra("note_pinned", false));

            String cat = getIntent().getStringExtra("note_category");
            if (cat != null) {
                for (int i = 0; i < CATEGORIES.length; i++) {
                    if (CATEGORIES[i].equalsIgnoreCase(cat)) {
                        spinnerCategory.setSelection(i);
                        break;
                    }
                }
            }

            String col = getIntent().getStringExtra("note_color");
            if ("#10B981".equalsIgnoreCase(col)) rbGreen.setChecked(true);
            else if ("#F59E0B".equalsIgnoreCase(col)) rbOrange.setChecked(true);
            else if ("#8B5CF6".equalsIgnoreCase(col)) rbPurple.setChecked(true);
            else rbBlue.setChecked(true);

            btnDeleteNote.setVisibility(View.VISIBLE);
        }

        updateLiveCounts();

        etNoteContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateLiveCounts();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnSaveNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
            }
        });

        btnDeleteNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteNote();
            }
        });
    }

    private void updateLiveCounts() {
        String text = etNoteContent.getText().toString();
        
        // Native Java calculations
        int charCount = text.length();
        int wordCount = text.trim().isEmpty() ? 0 : text.trim().split("\\s+").length;

        tvLiveWordCount.setText("Words: " + wordCount);
        tvLiveCharCount.setText("Characters: " + charCount);
    }

    private String getSelectedColorHex() {
        int checkedId = rgColor.getCheckedRadioButtonId();
        if (checkedId == R.id.rbGreen) return "#10B981";
        if (checkedId == R.id.rbOrange) return "#F59E0B";
        if (checkedId == R.id.rbPurple) return "#8B5CF6";
        return "#2563EB";
    }

    private void saveNote() {
        String title = etNoteTitle.getText().toString().trim();
        String content = etNoteContent.getText().toString().trim();

        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(this, "Please write a title or content", Toast.LENGTH_SHORT).show();
            return;
        }

        NoteModel note = new NoteModel();
        note.setTitle(title.isEmpty() ? "Untitled Note" : title);
        note.setContent(content);
        note.setCategory((String) spinnerCategory.getSelectedItem());
        note.setColor(getSelectedColorHex());
        note.setPinned(cbPinned.isChecked());

        setLoading(true);

        if (isEditMode) {
            backendApi.updateNote(authToken, noteId, note.toDataJson(), new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(final JSONObject response) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setLoading(false);
                            Toast.makeText(NoteDetailActivity.this, "Note updated!", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                }

                @Override
                public void onError(final String error) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setLoading(false);
                            Toast.makeText(NoteDetailActivity.this, "Update failed: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        } else {
            backendApi.createNote(authToken, note.toDataJson(), new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(final JSONObject response) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setLoading(false);
                            Toast.makeText(NoteDetailActivity.this, "Note saved!", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                }

                @Override
                public void onError(final String error) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setLoading(false);
                            Toast.makeText(NoteDetailActivity.this, "Save failed: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        }
    }

    private void deleteNote() {
        if (!isEditMode || noteId.isEmpty()) return;

        setLoading(true);
        backendApi.deleteNote(authToken, noteId, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(final JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setLoading(false);
                        Toast.makeText(NoteDetailActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setLoading(false);
                        Toast.makeText(NoteDetailActivity.this, "Delete failed: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        pbDetailLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSaveNote.setEnabled(!loading);
        btnDeleteNote.setEnabled(!loading);
    }
}