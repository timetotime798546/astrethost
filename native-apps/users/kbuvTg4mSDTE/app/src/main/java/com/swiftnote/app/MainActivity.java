package com.swiftnote.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private TextView tvUserEmail;
    private TextView tvStatTotalNotes;
    private TextView tvStatPinnedNotes;
    private TextView tvStatTotalWords;
    private EditText etSearch;
    private Button btnCatAll, btnCatPersonal, btnCatWork, btnCatIdeas;
    private LinearLayout llNotesContainer;
    private Button btnAddNote;
    private Button btnLogout;
    private ProgressBar pbNotesLoading;

    private BackendApi backendApi;
    private SessionManager sessionManager;
    private List<NoteModel> allNotes = new ArrayList<NoteModel>();
    private String selectedCategory = "All";
    private String searchKeyword = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        backendApi = new BackendApi(this);
        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initViews();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotesFromCloud();
    }

    private void initViews() {
        tvUserEmail = (TextView) findViewById(R.id.tvUserEmail);
        tvStatTotalNotes = (TextView) findViewById(R.id.tvStatTotalNotes);
        tvStatPinnedNotes = (TextView) findViewById(R.id.tvStatPinnedNotes);
        tvStatTotalWords = (TextView) findViewById(R.id.tvStatTotalWords);
        etSearch = (EditText) findViewById(R.id.etSearch);
        btnCatAll = (Button) findViewById(R.id.btnCatAll);
        btnCatPersonal = (Button) findViewById(R.id.btnCatPersonal);
        btnCatWork = (Button) findViewById(R.id.btnCatWork);
        btnCatIdeas = (Button) findViewById(R.id.btnCatIdeas);
        llNotesContainer = (LinearLayout) findViewById(R.id.llNotesContainer);
        btnAddNote = (Button) findViewById(R.id.btnAddNote);
        btnLogout = (Button) findViewById(R.id.btnLogout);
        pbNotesLoading = (ProgressBar) findViewById(R.id.pbNotesLoading);

        tvUserEmail.setText(sessionManager.getEmail());
    }

    private void setupListeners() {
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backendApi.logout(sessionManager.getToken(), new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        sessionManager.clearSession();
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        finish();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        // Clear session locally regardless
                        sessionManager.clearSession();
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        finish();
                    }
                });
            }
        });

        btnAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NoteEditActivity.class);
                startActivity(intent);
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchKeyword = s.toString().trim().toLowerCase();
                renderFilteredNotes();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        View.OnClickListener categoryClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateCategorySelection((Button) v);
            }
        };

        btnCatAll.setOnClickListener(categoryClickListener);
        btnCatPersonal.setOnClickListener(categoryClickListener);
        btnCatWork.setOnClickListener(categoryClickListener);
        btnCatIdeas.setOnClickListener(categoryClickListener);
    }

    private void updateCategorySelection(Button clicked) {
        selectedCategory = clicked.getText().toString();

        btnCatAll.setBackgroundResource(R.drawable.button_secondary);
        btnCatAll.setTextColor(Color.parseColor("#334155"));
        btnCatPersonal.setBackgroundResource(R.drawable.button_secondary);
        btnCatPersonal.setTextColor(Color.parseColor("#334155"));
        btnCatWork.setBackgroundResource(R.drawable.button_secondary);
        btnCatWork.setTextColor(Color.parseColor("#334155"));
        btnCatIdeas.setBackgroundResource(R.drawable.button_secondary);
        btnCatIdeas.setTextColor(Color.parseColor("#334155"));

        clicked.setBackgroundResource(R.drawable.button_primary);
        clicked.setTextColor(Color.WHITE);

        renderFilteredNotes();
    }

    private void loadNotesFromCloud() {
        pbNotesLoading.setVisibility(View.VISIBLE);

        backendApi.readData(sessionManager.getToken(), "notes", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                pbNotesLoading.setVisibility(View.GONE);
                allNotes.clear();

                JSONArray records = response.optJSONArray("records");
                if (records != null) {
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject recordObj = records.optJSONObject(i);
                        if (recordObj != null) {
                            String id = recordObj.optString("id", "");
                            JSONObject dataObj = recordObj.optJSONObject("data");
                            NoteModel note = NoteModel.fromJson(id, dataObj);
                            allNotes.add(note);
                        }
                    }
                }
                updateOfflineCalculations();
                renderFilteredNotes();
            }

            @Override
            public void onError(String errorMessage) {
                pbNotesLoading.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Could not fetch notes: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateOfflineCalculations() {
        // Deterministic calculations locally in Android
        int total = allNotes.size();
        int pinned = NoteCalculationHelper.countPinnedNotes(allNotes);
        int totalWords = NoteCalculationHelper.calculateTotalWordsInNotes(allNotes);

        tvStatTotalNotes.setText(String.valueOf(total));
        tvStatPinnedNotes.setText(String.valueOf(pinned));
        tvStatTotalWords.setText(String.valueOf(totalWords));
    }

    private void renderFilteredNotes() {
        llNotesContainer.removeAllViews();

        List<NoteModel> filtered = new ArrayList<NoteModel>();
        for (int i = 0; i < allNotes.size(); i++) {
            NoteModel note = allNotes.get(i);
            boolean matchesCategory = selectedCategory.equals("All") || note.getCategory().equalsIgnoreCase(selectedCategory);
            boolean matchesSearch = searchKeyword.isEmpty() ||
                    note.getTitle().toLowerCase().contains(searchKeyword) ||
                    note.getContent().toLowerCase().contains(searchKeyword);

            if (matchesCategory && matchesSearch) {
                if (note.isPinned()) {
                    filtered.add(0, note); // Pin on top
                } else {
                    filtered.add(note);
                }
            }
        }

        if (filtered.isEmpty()) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText(allNotes.isEmpty() ? "No notes yet. Tap '+ New Note' to create one!" : "No notes match your filter criteria.");
            emptyTv.setTextSize(14);
            emptyTv.setTextColor(Color.parseColor("#94A3B8"));
            emptyTv.setPadding(16, 40, 16, 16);
            emptyTv.setGravity(android.view.Gravity.CENTER);
            llNotesContainer.addView(emptyTv);
            return;
        }

        for (int i = 0; i < filtered.size(); i++) {
            final NoteModel note = filtered.get(i);
            View itemView = createNoteCardView(note);
            llNotesContainer.addView(itemView);
        }
    }

    private View createNoteCardView(final NoteModel note) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.card_background);
        card.setPadding(20, 16, 20, 16);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 14);
        card.setLayoutParams(params);

        // Header (Title + Pin Indicator)
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(note.getTitle().isEmpty() ? "Untitled Note" : note.getTitle());
        tvTitle.setTextSize(17);
        tvTitle.setTextColor(Color.parseColor("#0F172A"));
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleParam = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvTitle.setLayoutParams(titleParam);
        header.addView(tvTitle);

        if (note.isPinned()) {
            TextView tvPin = new TextView(this);
            tvPin.setText("📌 Pinned");
            tvPin.setTextSize(11);
            tvPin.setTextColor(Color.parseColor("#D97706"));
            tvPin.setBackgroundResource(R.drawable.tag_chip_bg);
            tvPin.setPadding(10, 4, 10, 4);
            header.addView(tvPin);
        }
        card.addView(header);

        // Snippet
        TextView tvSnippet = new TextView(this);
        String snippet = note.getContent();
        if (snippet.length() > 120) {
            snippet = snippet.substring(0, 120) + "...";
        }
        tvSnippet.setText(snippet.isEmpty() ? "No additional text" : snippet);
        tvSnippet.setTextSize(14);
        tvSnippet.setTextColor(Color.parseColor("#475569"));
        tvSnippet.setPadding(0, 8, 0, 12);
        card.addView(tvSnippet);

        // Footer info (Category, Word Count, Reading Time Calculation)
        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView tvCat = new TextView(this);
        tvCat.setText(note.getCategory());
        tvCat.setTextSize(11);
        tvCat.setTextColor(Color.parseColor("#2563EB"));
        tvCat.setBackgroundResource(R.drawable.tag_chip_bg);
        tvCat.setPadding(12, 4, 12, 4);
        footer.addView(tvCat);

        int wordCount = NoteCalculationHelper.calculateWordCount(note.getContent());
        int readingTime = NoteCalculationHelper.calculateReadingTimeMinutes(wordCount);

        TextView tvStats = new TextView(this);
        tvStats.setText(wordCount + " words  •  " + readingTime + " min read");
        tvStats.setTextSize(11);
        tvStats.setTextColor(Color.parseColor("#94A3B8"));
        tvStats.setPadding(14, 0, 0, 0);
        footer.addView(tvStats);

        card.addView(footer);

        // Click to Edit
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NoteEditActivity.class);
                intent.putExtra("id", note.getId());
                intent.putExtra("title", note.getTitle());
                intent.putExtra("content", note.getContent());
                intent.putExtra("category", note.getCategory());
                intent.putExtra("pinned", note.isPinned());
                intent.putExtra("color", note.getColor());
                startActivity(intent);
            }
        });

        // Long click to Quick Delete
        card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                confirmDelete(note);
                return true;
            }
        });

        return card;
    }

    private void confirmDelete(final NoteModel note) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Note");
        builder.setMessage("Are you sure you want to permanently delete '" + (note.getTitle().isEmpty() ? "Untitled" : note.getTitle()) + "'?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                pbNotesLoading.setVisibility(View.VISIBLE);
                backendApi.deleteData(sessionManager.getToken(), note.getId(), new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        Toast.makeText(MainActivity.this, "Note removed", Toast.LENGTH_SHORT).show();
                        loadNotesFromCloud();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        pbNotesLoading.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, "Failed to delete: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}