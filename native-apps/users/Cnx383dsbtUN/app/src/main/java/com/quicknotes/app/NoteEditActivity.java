package com.quicknotes.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NoteEditActivity extends Activity {
    private LinearLayout editRootView;
    private Button btnBack;
    private TextView tvModeTitle;
    private Button btnTogglePin;
    private Button btnSave;
    private TextView tvWordCount;
    private TextView tvReadTime;
    private Spinner spinnerCategory;
    private Spinner spinnerColor;
    private EditText etTitle;
    private EditText etContent;
    private ProgressBar progressBar;

    private SessionManager sessionManager;
    private BackendApi backendApi;

    private String existingNoteId = null;
    private boolean isPinned = false;
    private String selectedColorHex = "#FFFFFF";

    private final String[] categories = {"General", "Work", "Personal", "Ideas", "Study"};
    private final String[] colorNames = {"Default White", "Sunny Yellow", "Calm Blue", "Mint Green", "Soft Pink"};
    private final String[] colorHexCodes = {"#FFFFFF", "#FEF9C3", "#DBEAFE", "#DCFCE7", "#FCE7F3"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_edit);

        sessionManager = new SessionManager(this);
        backendApi = new BackendApi(this);

        editRootView = (LinearLayout) findViewById(R.id.edit_root_view);
        btnBack = (Button) findViewById(R.id.btn_back_note);
        tvModeTitle = (TextView) findViewById(R.id.tv_editor_mode_title);
        btnTogglePin = (Button) findViewById(R.id.btn_toggle_pin);
        btnSave = (Button) findViewById(R.id.btn_save_note);
        tvWordCount = (TextView) findViewById(R.id.tv_live_word_count);
        tvReadTime = (TextView) findViewById(R.id.tv_live_read_time);
        spinnerCategory = (Spinner) findViewById(R.id.spinner_category);
        spinnerColor = (Spinner) findViewById(R.id.spinner_color);
        etTitle = (EditText) findViewById(R.id.et_note_title);
        etContent = (EditText) findViewById(R.id.et_note_content);
        progressBar = (ProgressBar) findViewById(R.id.progress_editor);

        setupSpinners();
        setupLiveCalculators();

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("note_id")) {
            existingNoteId = intent.getStringExtra("note_id");
            tvModeTitle.setText("Edit Note");
            etTitle.setText(intent.getStringExtra("note_title"));
            etContent.setText(intent.getStringExtra("note_content"));
            isPinned = intent.getBooleanExtra("note_pinned", false);

            String cat = intent.getStringExtra("note_category");
            if (cat != null) {
                for (int i = 0; i < categories.length; i++) {
                    if (categories[i].equalsIgnoreCase(cat)) {
                        spinnerCategory.setSelection(i);
                        break;
                    }
                }
            }

            String col = intent.getStringExtra("note_color");
            if (col != null) {
                for (int i = 0; i < colorHexCodes.length; i++) {
                    if (colorHexCodes[i].equalsIgnoreCase(col)) {
                        spinnerColor.setSelection(i);
                        selectedColorHex = col;
                        break;
                    }
                }
            }
        } else {
            tvModeTitle.setText("New Note");
        }

        updatePinButtonUi();
        recalculateTextMetrics();

        btnTogglePin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isPinned = !isPinned;
                updatePinButtonUi();
            }
        });

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
    }

    private void setupSpinners() {
        ArrayAdapter<String> catAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(catAdapter);

        ArrayAdapter<String> colAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, colorNames);
        spinnerColor.setAdapter(colAdapter);

        spinnerColor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedColorHex = colorHexCodes[position];
                try {
                    editRootView.setBackgroundColor(Color.parseColor(selectedColorHex));
                } catch (Exception ignored) {}
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupLiveCalculators() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                recalculateTextMetrics();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etContent.addTextChangedListener(watcher);
    }

    private void recalculateTextMetrics() {
        String content = etContent.getText().toString();
        int words = NoteCalculationHelper.calculateWordCount(content);
        int chars = NoteCalculationHelper.calculateCharCount(content);
        int readTime = NoteCalculationHelper.calculateReadingTimeMinutes(words);

        tvWordCount.setText("Words: " + words + " | Characters: " + chars);
        tvReadTime.setText("Read time: " + readTime + " min");
    }

    private void updatePinButtonUi() {
        if (isPinned) {
            btnTogglePin.setText("★ Pinned");
            btnTogglePin.setTextColor(Color.parseColor("#F59E0B"));
        } else {
            btnTogglePin.setText("☆ Pin");
            btnTogglePin.setTextColor(Color.parseColor("#64748B"));
        }
    }

    private void saveNote() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();
        String category = categories[spinnerCategory.getSelectedItemPosition()];

        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(this, "Cannot save an empty note", Toast.LENGTH_SHORT).show();
            return;
        }

        if (title.isEmpty()) {
            title = "Untitled Note";
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

        NoteModel note = new NoteModel();
        note.setTitle(title);
        note.setContent(content);
        note.setCategory(category);
        note.setColor(selectedColorHex);
        note.setPinned(isPinned);
        note.setUpdatedAt(timestamp);

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        String token = sessionManager.getToken();

        if (existingNoteId != null && !existingNoteId.isEmpty()) {
            // Update
            backendApi.updateNote(token, existingNoteId, note.toDataJson(), new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(NoteEditActivity.this, "Note updated!", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String message) {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    Toast.makeText(NoteEditActivity.this, "Update error: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Create
            backendApi.createNote(token, note.toDataJson(), new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(NoteEditActivity.this, "Note saved!", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String message) {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    Toast.makeText(NoteEditActivity.this, "Save error: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}