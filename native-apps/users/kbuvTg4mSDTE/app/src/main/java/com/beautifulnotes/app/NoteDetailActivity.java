package com.beautifulnotes.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NoteDetailActivity extends Activity {

    private Button btnBack;
    private Button btnDelete;
    private Button btnSave;
    private View viewColorWhite;
    private View viewColorBlue;
    private View viewColorCream;
    private View viewColorMint;
    private View viewColorPink;
    private ProgressBar pbDetail;
    private LinearLayout llDetailCanvas;
    private TextView tvLocalWords;
    private TextView tvLocalChars;
    private EditText etNoteTitle;
    private EditText etNoteContent;

    private BackendApi backendApi;
    private String noteId = null;
    private String selectedColorHex = "#FFFFFF";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);

        btnBack = (Button) findViewById(R.id.btn_back);
        btnDelete = (Button) findViewById(R.id.btn_delete);
        btnSave = (Button) findViewById(R.id.btn_save);
        viewColorWhite = findViewById(R.id.color_white);
        viewColorBlue = findViewById(R.id.color_blue);
        viewColorCream = findViewById(R.id.color_cream);
        viewColorMint = findViewById(R.id.color_mint);
        viewColorPink = findViewById(R.id.color_pink);
        pbDetail = (ProgressBar) findViewById(R.id.pb_detail);
        llDetailCanvas = (LinearLayout) findViewById(R.id.ll_detail_canvas);
        tvLocalWords = (TextView) findViewById(R.id.tv_local_words);
        tvLocalChars = (TextView) findViewById(R.id.tv_local_chars);
        etNoteTitle = (EditText) findViewById(R.id.et_note_title);
        etNoteContent = (EditText) findViewById(R.id.et_note_content);

        backendApi = new BackendApi(this);
        String token = getIntent().getStringExtra("auth_token");
        backendApi.setToken(token);

        // Map selection dots
        setupColorViewDots();

        // Check if modifying existing document or creating new
        if (getIntent().hasExtra("note_id")) {
            noteId = getIntent().getStringExtra("note_id");
            etNoteTitle.setText(getIntent().getStringExtra("note_title"));
            etNoteContent.setText(getIntent().getStringExtra("note_content"));
            selectedColorHex = getIntent().getStringExtra("note_color");
            btnDelete.setVisibility(View.VISIBLE);
        }

        updateEditorBackgroundCanvas(selectedColorHex);
        performLocalCountCalculations();

        // Action listeners
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executeSaveAction();
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executeDeleteAction();
            }
        });

        // MANDATORY OFFLINE CALCULATION: Recalculate layout limits on text updates
        etNoteContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performLocalCountCalculations();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Interactive Color Tones Click Listeners
        viewColorWhite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateSelectedColor("#FFFFFF");
            }
        });
        viewColorBlue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateSelectedColor("#E8F4F8");
            }
        });
        viewColorCream.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateSelectedColor("#FCF7E3");
            }
        });
        viewColorMint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateSelectedColor("#EAF8F2");
            }
        });
        viewColorPink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateSelectedColor("#FCECEF");
            }
        });
    }

    private void setupColorViewDots() {
        int dotSize = (int) (4 * getResources().getDisplayMetrics().density);
        setupIndividualDot(viewColorWhite, "#FFFFFF", dotSize);
        setupIndividualDot(viewColorBlue, "#E8F4F8", dotSize);
        setupIndividualDot(viewColorCream, "#FCF7E3", dotSize);
        setupIndividualDot(viewColorMint, "#EAF8F2", dotSize);
        setupIndividualDot(viewColorPink, "#FCECEF", dotSize);
    }

    private void setupIndividualDot(View view, String hex, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(Color.parseColor(hex));
        drawable.setStroke(stroke, Color.parseColor("#CCCCCC"));
        view.setBackground(drawable);
    }

    private void updateSelectedColor(String hex) {
        selectedColorHex = hex;
        updateEditorBackgroundCanvas(hex);
    }

    private void updateEditorBackgroundCanvas(String hex) {
        int colorInt = Color.parseColor("#FFFFFF");
        try {
            colorInt = Color.parseColor(hex);
        } catch (Exception e) {
            e.printStackTrace();
        }
        llDetailCanvas.setBackgroundColor(colorInt);
    }

    // MANDATORY OFFLINE CALCULATION: Word & character stats calculations running locally in Native Java
    private void performLocalCountCalculations() {
        String contentText = etNoteContent.getText().toString();
        int charCount = contentText.length();
        int wordCount = 0;

        String cleanText = contentText.trim();
        if (!cleanText.isEmpty()) {
            String[] words = cleanText.split("\\s+");
            wordCount = words.length;
        }

        tvLocalWords.setText("Words: " + wordCount);
        tvLocalChars.setText("Characters: " + charCount);
    }

    private void executeSaveAction() {
        String title = etNoteTitle.getText().toString().trim();
        String content = etNoteContent.getText().toString().trim();

        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(this, "Empty thoughts can not be written to document.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (title.isEmpty()) {
            title = "Untitled Note";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault());
        String timestamp = sdf.format(new Date());

        Note note = new Note(noteId, title, content, selectedColorHex, timestamp);

        setLoadingState(true);

        if (noteId == null) {
            // New Document Creation flow
            backendApi.createNote(note, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setLoadingState(false);
                            Toast.makeText(NoteDetailActivity.this, "Thought saved online", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                }

                @Override
                public void onError(final String errorMessage) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setLoadingState(false);
                            Toast.makeText(NoteDetailActivity.this, "Sync Error: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        } else {
            // Modification Update flow
            backendApi.updateNote(note, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setLoadingState(false);
                            Toast.makeText(NoteDetailActivity.this, "Thought saved online", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                }

                @Override
                public void onError(final String errorMessage) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setLoadingState(false);
                            Toast.makeText(NoteDetailActivity.this, "Update Error: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        }
    }

    private void executeDeleteAction() {
        if (noteId == null) return;

        setLoadingState(true);
        backendApi.deleteNote(noteId, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setLoadingState(false);
                        Toast.makeText(NoteDetailActivity.this, "Thought removed.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }

            @Override
            public void onError(final String errorMessage) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setLoadingState(false);
                        Toast.makeText(NoteDetailActivity.this, "Delete rejected: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            btnSave.setEnabled(false);
            btnDelete.setEnabled(false);
            pbDetail.setVisibility(View.VISIBLE);
        } else {
            btnSave.setEnabled(true);
            btnDelete.setEnabled(true);
            pbDetail.setVisibility(View.GONE);
        }
    }
}