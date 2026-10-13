package com.quicknotes.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class NoteEditorActivity extends Activity {

    private EditText etTitle;
    private EditText etContent;
    private TextView tvHeader;
    private TextView tvLiveWords;
    private TextView tvLiveChars;
    private TextView tvLiveReadTime;
    private Button btnPinToggle;
    private Button btnDelete;
    private Button btnSave;
    private Button btnBack;

    private Button optCatGeneral;
    private Button optCatWork;
    private Button optCatPersonal;
    private Button optCatIdeas;
    private Button optCatUrgent;

    private NotesStorage storage;
    private String currentNoteId = null;
    private String selectedCategory = "General";
    private boolean isPinned = false;
    private long createdAt = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        storage = new NotesStorage(this);

        initViews();
        setupListeners();
        loadIncomingData();
        updateCategoryHighlight();
        updateCalculations();
    }

    private void initViews() {
        etTitle = (EditText) findViewById(R.id.et_note_title);
        etContent = (EditText) findViewById(R.id.et_note_content);
        tvHeader = (TextView) findViewById(R.id.tv_editor_header);
        tvLiveWords = (TextView) findViewById(R.id.tv_live_word_count);
        tvLiveChars = (TextView) findViewById(R.id.tv_live_char_count);
        tvLiveReadTime = (TextView) findViewById(R.id.tv_live_read_time);
        btnPinToggle = (Button) findViewById(R.id.btn_pin_toggle);
        btnDelete = (Button) findViewById(R.id.btn_delete_note);
        btnSave = (Button) findViewById(R.id.btn_save_note);
        btnBack = (Button) findViewById(R.id.btn_back);

        optCatGeneral = (Button) findViewById(R.id.opt_cat_general);
        optCatWork = (Button) findViewById(R.id.opt_cat_work);
        optCatPersonal = (Button) findViewById(R.id.opt_cat_personal);
        optCatIdeas = (Button) findViewById(R.id.opt_cat_ideas);
        optCatUrgent = (Button) findViewById(R.id.opt_cat_urgent);
    }

    private void loadIncomingData() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("note_id")) {
            currentNoteId = intent.getStringExtra("note_id");
            String title = intent.getStringExtra("title");
            String content = intent.getStringExtra("content");
            selectedCategory = intent.getStringExtra("category");
            if (selectedCategory == null || selectedCategory.length() == 0) {
                selectedCategory = "General";
            }
            isPinned = intent.getBooleanExtra("is_pinned", false);
            createdAt = intent.getLongExtra("created_at", System.currentTimeMillis());

            tvHeader.setText(R.string.edit_note);
            etTitle.setText(title);
            etContent.setText(content);
            btnDelete.setVisibility(View.VISIBLE);
            updatePinButtonText();
        } else {
            currentNoteId = null;
            tvHeader.setText(R.string.new_note);
            createdAt = System.currentTimeMillis();
            btnDelete.setVisibility(View.GONE);
            updatePinButtonText();
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnPinToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isPinned = !isPinned;
                updatePinButtonText();
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
                confirmDelete();
            }
        });

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateCalculations();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etContent.addTextChangedListener(textWatcher);
        etTitle.addTextChangedListener(textWatcher);

        View.OnClickListener categoryClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();
                if (id == R.id.opt_cat_general) {
                    selectedCategory = "General";
                } else if (id == R.id.opt_cat_work) {
                    selectedCategory = "Work";
                } else if (id == R.id.opt_cat_personal) {
                    selectedCategory = "Personal";
                } else if (id == R.id.opt_cat_ideas) {
                    selectedCategory = "Ideas";
                } else if (id == R.id.opt_cat_urgent) {
                    selectedCategory = "Urgent";
                }
                updateCategoryHighlight();
            }
        };

        optCatGeneral.setOnClickListener(categoryClickListener);
        optCatWork.setOnClickListener(categoryClickListener);
        optCatPersonal.setOnClickListener(categoryClickListener);
        optCatIdeas.setOnClickListener(categoryClickListener);
        optCatUrgent.setOnClickListener(categoryClickListener);
    }

    private void updatePinButtonText() {
        if (isPinned) {
            btnPinToggle.setText(R.string.unpin);
            btnPinToggle.setBackgroundColor(0xFFEAB308); // Gold highlight
        } else {
            btnPinToggle.setText(R.string.pin);
            btnPinToggle.setBackgroundColor(0xFF334155);
        }
    }

    private void updateCategoryHighlight() {
        resetCategoryButton(optCatGeneral, "General");
        resetCategoryButton(optCatWork, "Work");
        resetCategoryButton(optCatPersonal, "Personal");
        resetCategoryButton(optCatIdeas, "Ideas");
        resetCategoryButton(optCatUrgent, "Urgent");

        if ("General".equalsIgnoreCase(selectedCategory)) {
            optCatGeneral.setText("• General");
        } else if ("Work".equalsIgnoreCase(selectedCategory)) {
            optCatWork.setText("• Work");
        } else if ("Personal".equalsIgnoreCase(selectedCategory)) {
            optCatPersonal.setText("• Personal");
        } else if ("Ideas".equalsIgnoreCase(selectedCategory)) {
            optCatIdeas.setText("• Ideas");
        } else if ("Urgent".equalsIgnoreCase(selectedCategory)) {
            optCatUrgent.setText("• Urgent");
        }
    }

    private void resetCategoryButton(Button btn, String label) {
        btn.setText(label);
    }

    /**
     * Local deterministic calculations for live note metrics.
     */
    private void updateCalculations() {
        String body = etContent.getText().toString();
        int words = NoteStatsCalculator.calculateWordCount(body);
        int chars = NoteStatsCalculator.calculateCharCount(body);
        int readMinutes = NoteStatsCalculator.calculateReadingTimeMinutes(words);

        tvLiveWords.setText(words + " words");
        tvLiveChars.setText(chars + " chars");
        tvLiveReadTime.setText(readMinutes + " min read");
    }

    private void saveNote() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (title.length() == 0 && content.length() == 0) {
            Toast.makeText(this, "Please enter a title or note content", Toast.LENGTH_SHORT).show();
            return;
        }

        if (title.length() == 0) {
            // First 25 characters of content as default title
            if (content.length() > 25) {
                title = content.substring(0, 25) + "...";
            } else {
                title = content;
            }
        }

        long now = System.currentTimeMillis();
        if (currentNoteId == null) {
            currentNoteId = String.valueOf(now);
            createdAt = now;
        }

        Note note = new Note(currentNoteId, title, content, selectedCategory, isPinned, createdAt, now);
        boolean saved = storage.saveOrUpdateNote(note);

        if (saved) {
            Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to save note", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDelete() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.delete);
        builder.setMessage(R.string.confirm_delete);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (currentNoteId != null) {
                    storage.deleteNote(currentNoteId);
                    Toast.makeText(NoteEditorActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }
}