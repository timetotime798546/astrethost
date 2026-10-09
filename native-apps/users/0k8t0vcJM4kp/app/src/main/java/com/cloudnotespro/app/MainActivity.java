package com.cloudnotespro.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private ListView listViewNotes;
    private TextView txtStatTotal, txtStatActive, txtStatCompleted, emptyView;
    private EditText searchQuery;
    private Spinner filterType, filterStatus;
    private Button btnLogout, btnAddNote;

    private BackendApi backendApi;
    private String userToken;
    private final List<Note> originalNotesList = new ArrayList<>();
    private final List<Note> filteredNotesList = new ArrayList<>();
    private NoteAdapter noteAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("CloudNotesPrefs", MODE_PRIVATE);
        userToken = prefs.getString("auth_token", "");
        if (userToken.isEmpty()) {
            goToLogin();
            return;
        }

        setContentView(R.layout.activity_main);

        backendApi = new BackendApi(this);

        listViewNotes = (ListView) findViewById(R.id.listViewNotes);
        txtStatTotal = (TextView) findViewById(R.id.txtStatTotal);
        txtStatActive = (TextView) findViewById(R.id.txtStatActive);
        txtStatCompleted = (TextView) findViewById(R.id.txtStatCompleted);
        emptyView = (TextView) findViewById(R.id.emptyView);
        searchQuery = (EditText) findViewById(R.id.searchQuery);
        filterType = (Spinner) findViewById(R.id.filterType);
        filterStatus = (Spinner) findViewById(R.id.filterStatus);
        btnLogout = (Button) findViewById(R.id.btnLogout);
        btnAddNote = (Button) findViewById(R.id.btnAddNote);

        noteAdapter = new NoteAdapter(this, filteredNotesList);
        listViewNotes.setAdapter(noteAdapter);

        setupFilters();

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogout();
            }
        });

        btnAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open NoteDetailActivity in 'create' mode (null note ID)
                Intent intent = new Intent(MainActivity.this, NoteDetailActivity.class);
                startActivity(intent);
            }
        });

        listViewNotes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Note clickedNote = filteredNotesList.get(position);
                Intent intent = new Intent(MainActivity.this, NoteDetailActivity.class);
                intent.putExtra("note_id", clickedNote.getId());
                intent.putExtra("note_title", clickedNote.getTitle());
                intent.putExtra("note_description", clickedNote.getDescription());
                intent.putExtra("note_date", clickedNote.getDate());
                intent.putExtra("note_status", clickedNote.getStatus());
                intent.putExtra("note_type", clickedNote.getType());
                intent.putExtra("note_image", clickedNote.getImageUrl());
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!userToken.isEmpty()) {
            loadCloudNotes();
        }
    }

    private void goToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void performLogout() {
        backendApi.logout(userToken, new BackendApi.ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                clearSessionAndExit();
            }

            @Override
            public void onError(String errorMessage) {
                // Fallback exit even if request fails (e.g. offline token expiry)
                clearSessionAndExit();
            }
        });
    }

    private void clearSessionAndExit() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SharedPreferences.Editor editor = getSharedPreferences("CloudNotesPrefs", MODE_PRIVATE).edit();
                editor.clear();
                editor.apply();
                Toast.makeText(MainActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                goToLogin();
            }
        });
    }

    private void loadCloudNotes() {
        backendApi.getNotes(userToken, new BackendApi.ApiCallback<JSONArray>() {
            @Override
            public void onSuccess(final JSONArray records) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        originalNotesList.clear();
                        for (int i = 0; i < records.length(); i++) {
                            try {
                                JSONObject recObj = records.getJSONObject(i);
                                Note note = Note.fromJsonRecord(recObj);
                                if (note != null) {
                                    originalNotesList.add(note);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        // Recalculate and update interface offline
                        recalculateStatsAndApplyFilters();
                    }
                });
            }

            @Override
            public void onError(final String errorMessage) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Failed loading: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void setupFilters() {
        // Types Filter Array
        String[] types = {"All Categories", "Work", "Personal", "Ideas", "Journal"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterType.setAdapter(typeAdapter);

        // Status Filter Array
        String[] statuses = {"All Status", "Active", "Completed"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statuses);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterStatus.setAdapter(statusAdapter);

        // Filter triggers
        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                recalculateStatsAndApplyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };
        filterType.setOnItemSelectedListener(filterListener);
        filterStatus.setOnItemSelectedListener(filterListener);

        searchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                recalculateStatsAndApplyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    // MANDATORY OFFLINE STATS CALCULATION AND FILTERING
    private void recalculateStatsAndApplyFilters() {
        int totalCount = originalNotesList.size();
        int activeCount = 0;
        int completedCount = 0;

        for (Note note : originalNotesList) {
            if (note.getStatus().equalsIgnoreCase("Completed")) {
                completedCount++;
            } else {
                activeCount++;
            }
        }

        // Display locally calculated stats in top widget cards
        txtStatTotal.setText(String.valueOf(totalCount));
        txtStatActive.setText(String.valueOf(activeCount));
        txtStatCompleted.setText(String.valueOf(completedCount));

        // Filter note records
        String searchStr = searchQuery.getText().toString().toLowerCase().trim();
        String typeFilter = filterType.getSelectedItem().toString();
        String statusFilter = filterStatus.getSelectedItem().toString();

        filteredNotesList.clear();
        for (Note note : originalNotesList) {
            // Apply category filter
            if (!typeFilter.equals("All Categories") && !note.getType().equalsIgnoreCase(typeFilter)) {
                continue;
            }
            // Apply status filter
            if (!statusFilter.equals("All Status") && !note.getStatus().equalsIgnoreCase(statusFilter)) {
                continue;
            }
            // Apply search query match on Title & Description
            if (!searchStr.isEmpty() && 
                !note.getTitle().toLowerCase().contains(searchStr) && 
                !note.getDescription().toLowerCase().contains(searchStr)) {
                continue;
            }
            filteredNotesList.add(note);
        }

        // Refresh ListView Adapter
        noteAdapter.notifyDataSetChanged();

        // Control empty visual hint state
        if (filteredNotesList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.GONE);
        }
    }
}