package com.quicknotes.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
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
    private TextView tvUserHeader;
    private Button btnLogout;
    private EditText etSearch;
    private LinearLayout containerNotes;
    private TextView tvEmptyState;
    private ProgressBar progressBar;
    private Button fabAddNote;

    private TextView tvStatTotalNotes;
    private TextView tvStatPinnedNotes;
    private TextView tvStatTotalWords;

    private TextView chipAll;
    private TextView chipWork;
    private TextView chipPersonal;
    private TextView chipIdeas;
    private TextView chipStudy;

    private SessionManager sessionManager;
    private BackendApi backendApi;

    private final List<NoteModel> allNotes = new ArrayList<NoteModel>();
    private String selectedCategory = "All";
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        backendApi = new BackendApi(this);

        tvUserHeader = (TextView) findViewById(R.id.tv_user_header);
        btnLogout = (Button) findViewById(R.id.btn_logout);
        etSearch = (EditText) findViewById(R.id.et_search);
        containerNotes = (LinearLayout) findViewById(R.id.container_notes);
        tvEmptyState = (TextView) findViewById(R.id.tv_empty_state);
        progressBar = (ProgressBar) findViewById(R.id.progress_main);
        fabAddNote = (Button) findViewById(R.id.fab_add_note);

        tvStatTotalNotes = (TextView) findViewById(R.id.tv_stat_total_notes);
        tvStatPinnedNotes = (TextView) findViewById(R.id.tv_stat_pinned_notes);
        tvStatTotalWords = (TextView) findViewById(R.id.tv_stat_total_words);

        chipAll = (TextView) findViewById(R.id.chip_all);
        chipWork = (TextView) findViewById(R.id.chip_work);
        chipPersonal = (TextView) findViewById(R.id.chip_personal);
        chipIdeas = (TextView) findViewById(R.id.chip_ideas);
        chipStudy = (TextView) findViewById(R.id.chip_study);

        String userEmail = sessionManager.getUserEmail();
        if (userEmail != null && !userEmail.isEmpty()) {
            tvUserHeader.setText("Synced: " + userEmail);
        }

        setupCategoryChips();
        setupSearch();

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogout();
            }
        });

        fabAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NoteEditActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchNotes();
    }

    private void setupCategoryChips() {
        View.OnClickListener chipListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();
                if (id == R.id.chip_all) selectedCategory = "All";
                else if (id == R.id.chip_work) selectedCategory = "Work";
                else if (id == R.id.chip_personal) selectedCategory = "Personal";
                else if (id == R.id.chip_ideas) selectedCategory = "Ideas";
                else if (id == R.id.chip_study) selectedCategory = "Study";

                updateChipStyles();
                renderFilteredNotes();
            }
        };

        chipAll.setOnClickListener(chipListener);
        chipWork.setOnClickListener(chipListener);
        chipPersonal.setOnClickListener(chipListener);
        chipIdeas.setOnClickListener(chipListener);
        chipStudy.setOnClickListener(chipListener);
    }

    private void updateChipStyles() {
        TextView[] chips = {chipAll, chipWork, chipPersonal, chipIdeas, chipStudy};
        String[] names = {"All", "Work", "Personal", "Ideas", "Study"};

        for (int i = 0; i < chips.length; i++) {
            if (names[i].equalsIgnoreCase(selectedCategory)) {
                chips[i].setTextColor(Color.parseColor("#3B82F6"));
            } else {
                chips[i].setTextColor(Color.parseColor("#64748B"));
            }
        }
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim().toLowerCase();
                renderFilteredNotes();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void fetchNotes() {
        progressBar.setVisibility(View.VISIBLE);
        String token = sessionManager.getToken();

        backendApi.getNotes(token, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressBar.setVisibility(View.GONE);
                allNotes.clear();
                JSONArray records = response.optJSONArray("records");
                if (records != null) {
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject item = records.optJSONObject(i);
                        if (item != null) {
                            String id = item.optString("id", "");
                            JSONObject data = item.optJSONObject("data");
                            allNotes.add(NoteModel.fromJson(id, data));
                        }
                    }
                }
                updateOfflineCalculations();
                renderFilteredNotes();
            }

            @Override
            public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateOfflineCalculations() {
        int totalNotes = allNotes.size();
        int pinnedCount = NoteCalculationHelper.countPinnedNotes(allNotes);
        int totalWords = NoteCalculationHelper.calculateTotalWordsInNotes(allNotes);

        tvStatTotalNotes.setText(String.valueOf(totalNotes));
        tvStatPinnedNotes.setText(String.valueOf(pinnedCount));
        tvStatTotalWords.setText(String.valueOf(totalWords));
    }

    private void renderFilteredNotes() {
        containerNotes.removeAllViews();
        List<NoteModel> filtered = new ArrayList<NoteModel>();

        for (int i = 0; i < allNotes.size(); i++) {
            NoteModel note = allNotes.get(i);
            boolean matchesCat = selectedCategory.equalsIgnoreCase("All") ||
                    note.getCategory().equalsIgnoreCase(selectedCategory);

            boolean matchesSearch = currentSearchQuery.isEmpty() ||
                    note.getTitle().toLowerCase().contains(currentSearchQuery) ||
                    note.getContent().toLowerCase().contains(currentSearchQuery) ||
                    note.getCategory().toLowerCase().contains(currentSearchQuery);

            if (matchesCat && matchesSearch) {
                // Keep pinned notes first
                if (note.isPinned()) {
                    filtered.add(0, note);
                } else {
                    filtered.add(note);
                }
            }
        }

        if (filtered.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            return;
        }

        tvEmptyState.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < filtered.size(); i++) {
            final NoteModel note = filtered.get(i);
            View itemView = inflater.inflate(R.layout.item_note, containerNotes, false);

            LinearLayout rootCard = (LinearLayout) itemView.findViewById(R.id.item_card_root);
            TextView tvTitle = (TextView) itemView.findViewById(R.id.item_note_title);
            TextView tvPin = (TextView) itemView.findViewById(R.id.item_note_pin_badge);
            TextView tvContent = (TextView) itemView.findViewById(R.id.item_note_content);
            TextView tvCategory = (TextView) itemView.findViewById(R.id.item_note_category);
            TextView tvMetrics = (TextView) itemView.findViewById(R.id.item_note_metrics);
            Button btnDelete = (Button) itemView.findViewById(R.id.btn_delete_note);

            tvTitle.setText(note.getTitle().isEmpty() ? "Untitled" : note.getTitle());
            tvContent.setText(note.getContent().isEmpty() ? "(No content)" : note.getContent());
            tvCategory.setText(note.getCategory());
            tvPin.setVisibility(note.isPinned() ? View.VISIBLE : View.GONE);

            // Offline Word and Reading Time Calculation
            int words = NoteCalculationHelper.calculateWordCount(note.getContent());
            int readMins = NoteCalculationHelper.calculateReadingTimeMinutes(words);
            tvMetrics.setText(words + " words • " + readMins + " min read");

            // Apply custom card tint
            try {
                if (note.getColor() != null && !note.getColor().isEmpty()) {
                    rootCard.setBackgroundColor(Color.parseColor(note.getColor()));
                }
            } catch (Exception ignored) {}

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, NoteEditActivity.class);
                    intent.putExtra("note_id", note.getId());
                    intent.putExtra("note_title", note.getTitle());
                    intent.putExtra("note_content", note.getContent());
                    intent.putExtra("note_category", note.getCategory());
                    intent.putExtra("note_color", note.getColor());
                    intent.putExtra("note_pinned", note.isPinned());
                    startActivity(intent);
                }
            });

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmDeleteNote(note);
                }
            });

            containerNotes.addView(itemView);
        }
    }

    private void confirmDeleteNote(final NoteModel note) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete '" + note.getTitle() + "'?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteNote(note.getId());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteNote(String noteId) {
        progressBar.setVisibility(View.VISIBLE);
        String token = sessionManager.getToken();

        backendApi.deleteNote(token, noteId, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                fetchNotes();
            }

            @Override
            public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Failed to delete: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log Out", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sessionManager.clear();
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}