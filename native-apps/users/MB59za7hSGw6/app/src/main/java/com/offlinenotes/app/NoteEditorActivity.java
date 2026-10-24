package com.offlinenotes.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class NoteEditorActivity extends Activity {
    private EditText etTitle;
    private EditText etContent;
    private Spinner spinnerCategory;
    private TextView tvWords;
    private TextView tvChars;
    private TextView tvReadingTime;
    private TextView screenTitle;
    private Button btnTogglePin;
    private Button btnDelete;
    private Button btnSave;
    private Button btnBack;
    private LinearLayout editorRoot;

    private NotesManager notesManager;
    private String currentNoteId = null;
    private boolean isPinned = false;
    private String selectedColorHex = "#FFFFFF";

    private final String[] categories = new String[]{"Personal", "Work", "Ideas", "Tasks", "Study"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        notesManager = new NotesManager(this);

        etTitle = (EditText) findViewById(R.id.et_note_title);
        etContent = (EditText) findViewById(R.id.et_note_content);
        spinnerCategory = (Spinner) findViewById(R.id.spinner_category);
        tvWords = (TextView) findViewById(R.id.tv_calc_words);
        tvChars = (TextView) findViewById(R.id.tv_calc_chars);
        tvReadingTime = (TextView) findViewById(R.id.tv_calc_reading_time);
        screenTitle = (TextView) findViewById(R.id.editor_screen_title);
        btnTogglePin = (Button) findViewById(R.id.btn_toggle_pin);
        btnDelete = (Button) findViewById(R.id.btn_editor_delete);
        btnSave = (Button) findViewById(R.id.btn_editor_save);
        btnBack = (Button) findViewById(R.id.btn_back);
        editorRoot = (LinearLayout) findViewById(R.id.editor_root);

        // Setup Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(adapter);

        setupColorPickers();

        // Check if editing existing note
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("note_id")) {
            currentNoteId = intent.getStringExtra("note_id");
            loadNote(currentNoteId);
        } else {
            screenTitle.setText("New Note");
            btnDelete.setVisibility(View.GONE);
            updatePinButtonUi();
            recalculateMetrics();
        }

        // Live calculation on text change (Offline Business Logic)
        etContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                recalculateMetrics();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

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
                saveCurrentNote();
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDelete();
            }
        });
    }

    private void setupColorPickers() {
        View chipWhite = findViewById(R.id.color_chip_white);
        View chipYellow = findViewById(R.id.color_chip_yellow);
        View chipGreen = findViewById(R.id.color_chip_green);
        View chipBlue = findViewById(R.id.color_chip_blue);
        View chipPurple = findViewById(R.id.color_chip_purple);

        chipWhite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyColorChoice("#FFFFFF");
            }
        });
        chipYellow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyColorChoice("#FEF3C7");
            }
        });
        chipGreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyColorChoice("#D1FAE5");
            }
        });
        chipBlue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyColorChoice("#DBEAFE");
            }
        });
        chipPurple.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyColorChoice("#EDE9FE");
            }
        });
    }

    private void applyColorChoice(String hex) {
        selectedColorHex = hex;
        try {
            int parsedColor = Color.parseColor(hex);
            editorRoot.setBackgroundColor(parsedColor);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadNote(String noteId) {
        Note note = notesManager.getNoteById(noteId);
        if (note != null) {
            screenTitle.setText("Edit Note");
            etTitle.setText(note.getTitle());
            etContent.setText(note.getContent());
            isPinned = note.isPinned();
            selectedColorHex = note.getColorHex();
            applyColorChoice(selectedColorHex);
            btnDelete.setVisibility(View.VISIBLE);
            updatePinButtonUi();

            for (int i = 0; i < categories.length; i++) {
                if (categories[i].equalsIgnoreCase(note.getCategory())) {
                    spinnerCategory.setSelection(i);
                    break;
                }
            }
            recalculateMetrics();
        }
    }

    // Mandatory Local Calculation: Words, Characters, Estimated Reading Time
    private void recalculateMetrics() {
        String content = etContent.getText().toString();
        int charCount = content.length();
        int wordCount = 0;

        String trimmed = content.trim();
        if (!trimmed.isEmpty()) {
            String[] words = trimmed.split("\\s+");
            wordCount = words.length;
        }

        // Reading speed standard: ~200 words per minute
        int readingMinutes = (int) Math.ceil((double) wordCount / 200.0);
        if (wordCount == 0) {
            readingMinutes = 0;
        }

        tvWords.setText("Words: " + wordCount);
        tvChars.setText("Characters: " + charCount);
        tvReadingTime.setText("~" + readingMinutes + " min read");
    }

    private void updatePinButtonUi() {
        if (isPinned) {
            btnTogglePin.setText("★ Pinned");
            btnTogglePin.setTextColor(Color.parseColor("#F59E0B"));
        } else {
            btnTogglePin.setText("☆ Pin");
            btnTogglePin.setTextColor(Color.parseColor("#94A3B8"));
        }
    }

    private void saveCurrentNote() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(this, "Please enter a title or note text", Toast.LENGTH_SHORT).show();
            return;
        }

        String category = categories[spinnerCategory.getSelectedItemPosition()];
        long currentTime = System.currentTimeMillis();

        if (currentNoteId == null) {
            currentNoteId = "note_" + currentTime;
            Note newNote = new Note(currentNoteId, title, content, category, selectedColorHex, isPinned, currentTime, currentTime);
            notesManager.saveNote(newNote);
        } else {
            Note existing = notesManager.getNoteById(currentNoteId);
            long createdAt = (existing != null) ? existing.getCreatedAt() : currentTime;
            Note updatedNote = new Note(currentNoteId, title, content, category, selectedColorHex, isPinned, createdAt, currentTime);
            notesManager.saveNote(updatedNote);
        }

        Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to permanently delete this note?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (currentNoteId != null) {
                            notesManager.deleteNote(currentNoteId);
                            Toast.makeText(NoteEditorActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}