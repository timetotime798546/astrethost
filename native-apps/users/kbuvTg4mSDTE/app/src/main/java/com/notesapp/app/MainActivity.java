package com.notesapp.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements NoteAdapter.OnNoteClickListener {
    private TextView tvStatTotal, tvStatPinned, tvStatWords, tvEmptyState;
    private EditText etSearch;
    private Button btnFilterAll, btnFilterWork, btnFilterPersonal, btnFilterIdeas, btnFilterTodo;
    private Button btnLogout, btnAddNote;
    private ListView lvNotes;
    private ProgressBar pbMainLoading;

    private BackendApi backendApi;
    private NoteAdapter adapter;
    private List<NoteModel> allNotes = new ArrayList<NoteModel>();
    private List<NoteModel> filteredNotes = new ArrayList<NoteModel>();

    private String selectedCategory = "All";
    private String searchQuery = "";
    private String authToken = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("notes_prefs", Context.MODE_PRIVATE);
        authToken = prefs.getString("auth_token", "");

        if (authToken.isEmpty()) {
            startActivity(new Intent(MainActivity.this, AuthActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        backendApi = new BackendApi(this);

        tvStatTotal = findViewById(R.id.tvStatTotal);
        tvStatPinned = findViewById(R.id.tvStatPinned);
        tvStatWords = findViewById(R.id.tvStatWords);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        etSearch = findViewById(R.id.etSearch);
        lvNotes = findViewById(R.id.lvNotes);
        pbMainLoading = findViewById(R.id.pbMainLoading);

        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilterWork = findViewById(R.id.btnFilterWork);
        btnFilterPersonal = findViewById(R.id.btnFilterPersonal);
        btnFilterIdeas = findViewById(R.id.btnFilterIdeas);
        btnFilterTodo = findViewById(R.id.btnFilterTodo);
        btnLogout = findViewById(R.id.btnLogout);
        btnAddNote = findViewById(R.id.btnAddNote);

        adapter = new NoteAdapter(this, filteredNotes, this);
        lvNotes.setAdapter(adapter);

        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotesFromBackend();
    }

    private void setupListeners() {
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSharedPreferences("notes_prefs", Context.MODE_PRIVATE).edit().clear().apply();
                startActivity(new Intent(MainActivity.this, AuthActivity.class));
                finish();
            }
        });

        btnAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NoteDetailActivity.class);
                startActivity(intent);
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().toLowerCase().trim();
                applyFiltersAndCalculations();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        View.OnClickListener filterClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v == btnFilterAll) selectedCategory = "All";
                else if (v == btnFilterWork) selectedCategory = "Work";
                else if (v == btnFilterPersonal) selectedCategory = "Personal";
                else if (v == btnFilterIdeas) selectedCategory = "Ideas";
                else if (v == btnFilterTodo) selectedCategory = "Todo";

                updateFilterButtonStyles();
                applyFiltersAndCalculations();
            }
        };

        btnFilterAll.setOnClickListener(filterClickListener);
        btnFilterWork.setOnClickListener(filterClickListener);
        btnFilterPersonal.setOnClickListener(filterClickListener);
        btnFilterIdeas.setOnClickListener(filterClickListener);
        btnFilterTodo.setOnClickListener(filterClickListener);
    }

    private void updateFilterButtonStyles() {
        btnFilterAll.setBackgroundColor(selectedCategory.equals("All") ? Color.parseColor("#2563EB") : Color.parseColor("#94A3B8"));
        btnFilterWork.setBackgroundColor(selectedCategory.equals("Work") ? Color.parseColor("#2563EB") : Color.parseColor("#94A3B8"));
        btnFilterPersonal.setBackgroundColor(selectedCategory.equals("Personal") ? Color.parseColor("#2563EB") : Color.parseColor("#94A3B8"));
        btnFilterIdeas.setBackgroundColor(selectedCategory.equals("Ideas") ? Color.parseColor("#2563EB") : Color.parseColor("#94A3B8"));
        btnFilterTodo.setBackgroundColor(selectedCategory.equals("Todo") ? Color.parseColor("#2563EB") : Color.parseColor("#94A3B8"));
    }

    private void loadNotesFromBackend() {
        pbMainLoading.setVisibility(View.VISIBLE);
        backendApi.fetchNotes(authToken, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(final JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pbMainLoading.setVisibility(View.GONE);
                        allNotes.clear();
                        if (response.optBoolean("success", false)) {
                            JSONArray records = response.optJSONArray("records");
                            if (records != null) {
                                for (int i = 0; i < records.length(); i++) {
                                    JSONObject rec = records.optJSONObject(i);
                                    if (rec != null) {
                                        allNotes.add(NoteModel.fromRecordJson(rec));
                                    }
                                }
                            }
                        }
                        applyFiltersAndCalculations();
                    }
                });
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pbMainLoading.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, "Sync error: " + error, Toast.LENGTH_SHORT).show();
                        applyFiltersAndCalculations();
                    }
                });
            }
        });
    }

    private void applyFiltersAndCalculations() {
        filteredNotes.clear();

        int totalWords = 0;
        int pinnedCount = 0;

        for (int i = 0; i < allNotes.size(); i++) {
            NoteModel note = allNotes.get(i);
            
            // Offline Java Calculations
            totalWords += note.getWordCount();
            if (note.isPinned()) {
                pinnedCount++;
            }

            boolean matchesCategory = selectedCategory.equals("All") || note.getCategory().equalsIgnoreCase(selectedCategory);
            boolean matchesSearch = searchQuery.isEmpty()
                    || note.getTitle().toLowerCase().contains(searchQuery)
                    || note.getContent().toLowerCase().contains(searchQuery);

            if (matchesCategory && matchesSearch) {
                if (note.isPinned()) {
                    filteredNotes.add(0, note); // Show pinned on top
                } else {
                    filteredNotes.add(note);
                }
            }
        }

        // Update Analytics Counters UI
        tvStatTotal.setText(String.valueOf(allNotes.size()));
        tvStatPinned.setText(String.valueOf(pinnedCount));
        tvStatWords.setText(String.valueOf(totalWords));

        adapter.updateData(filteredNotes);

        if (filteredNotes.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onNoteClick(NoteModel note) {
        Intent intent = new Intent(MainActivity.this, NoteDetailActivity.class);
        intent.putExtra("note_id", note.getId());
        intent.putExtra("note_title", note.getTitle());
        intent.putExtra("note_content", note.getContent());
        intent.putExtra("note_category", note.getCategory());
        intent.putExtra("note_color", note.getColor());
        intent.putExtra("note_pinned", note.isPinned());
        startActivity(intent);
    }
}