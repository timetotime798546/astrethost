package com.cloudnotes.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
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

public class MainActivity extends Activity {

    private BackendApi api;
    private List<Note> noteList;
    private NoteAdapter adapter;

    private EditText searchEditText;
    private ListView notesListView;
    private TextView emptyStateView;
    private ProgressBar mainProgressBar;

    private TextView statsNotesCount;
    private TextView statsWordsCount;

    private String currentCategory = "ALL";
    private String currentSearchQuery = "";

    private Button catAll, catWork, catPersonal, catIdeas, catOther;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        api = new BackendApi(this);
        noteList = new ArrayList<>();

        searchEditText = (EditText) findViewById(R.id.searchEditText);
        notesListView = (ListView) findViewById(R.id.notesListView);
        emptyStateView = (TextView) findViewById(R.id.emptyStateView);
        mainProgressBar = (ProgressBar) findViewById(R.id.mainProgressBar);

        statsNotesCount = (TextView) findViewById(R.id.statsNotesCount);
        statsWordsCount = (TextView) findViewById(R.id.statsWordsCount);

        catAll = (Button) findViewById(R.id.catAll);
        catWork = (Button) findViewById(R.id.catWork);
        catPersonal = (Button) findViewById(R.id.catPersonal);
        catIdeas = (Button) findViewById(R.id.catIdeas);
        catOther = (Button) findViewById(R.id.catOther);

        setupCategoryButtons();

        adapter = new NoteAdapter(this, noteList, new NoteAdapter.OnNoteDeleteListener() {
            @Override
            public void onDelete(final Note note) {
                confirmDeleteNote(note);
            }
        });
        notesListView.setAdapter(adapter);

        notesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Note selectedNote = adapter.getItem(position);
                Intent intent = new Intent(MainActivity.this, NoteActivity.class);
                intent.putExtra("note_id", selectedNote.getId());
                intent.putExtra("note_title", selectedNote.getTitle());
                intent.putExtra("note_content", selectedNote.getContent());
                intent.putExtra("note_category", selectedNote.getCategory());
                intent.putExtra("note_date", selectedNote.getDate());
                startActivity(intent);
            }
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                applyFiltering();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.btnAddNote).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NoteActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.btnLogout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                api.clearToken();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchNotes();
    }

    private void fetchNotes() {
        mainProgressBar.setVisibility(View.VISIBLE);
        emptyStateView.setVisibility(View.GONE);

        api.getNotes(new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                mainProgressBar.setVisibility(View.GONE);
                try {
                    JSONObject obj = new JSONObject(response);
                    if (obj.optBoolean("success")) {
                        noteList.clear();
                        JSONArray records = obj.getJSONArray("records");
                        for (int i = 0; i < records.length(); i++) {
                            JSONObject rec = records.getJSONObject(i);
                            String id = rec.getString("id");
                            JSONObject data = rec.getJSONObject("data");
                            String title = data.optString("title", "Untitled");
                            String content = data.optString("content", "");
                            String category = data.optString("category", "Other");
                            String date = data.optString("date", "");

                            noteList.add(new Note(id, title, content, category, date));
                        }
                        applyFiltering();
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to load notes", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Error parsing notes: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                mainProgressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Sync error: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupCategoryButtons() {
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                catAll.setBackgroundColor(Color.parseColor("#E0E0E0"));
                catAll.setTextColor(Color.parseColor("#333333"));
                catWork.setBackgroundColor(Color.parseColor("#E0E0E0"));
                catWork.setTextColor(Color.parseColor("#333333"));
                catPersonal.setBackgroundColor(Color.parseColor("#E0E0E0"));
                catPersonal.setTextColor(Color.parseColor("#333333"));
                catIdeas.setBackgroundColor(Color.parseColor("#E0E0E0"));
                catIdeas.setTextColor(Color.parseColor("#333333"));
                catOther.setBackgroundColor(Color.parseColor("#E0E0E0"));
                catOther.setTextColor(Color.parseColor("#333333"));

                v.setBackgroundColor(Color.parseColor("#1976D2"));
                ((Button) v).setTextColor(Color.WHITE);

                int id = v.getId();
                if (id == R.id.catAll) {
                    currentCategory = "ALL";
                } else if (id == R.id.catWork) {
                    currentCategory = "Work";
                } else if (id == R.id.catPersonal) {
                    currentCategory = "Personal";
                } else if (id == R.id.catIdeas) {
                    currentCategory = "Ideas";
                } else if (id == R.id.catOther) {
                    currentCategory = "Other";
                }

                applyFiltering();
            }
        };

        catAll.setOnClickListener(clickListener);
        catWork.setOnClickListener(clickListener);
        catPersonal.setOnClickListener(clickListener);
        catIdeas.setOnClickListener(clickListener);
        catOther.setOnClickListener(clickListener);
    }

    private void applyFiltering() {
        adapter.filter(currentCategory, currentSearchQuery);

        if (adapter.getCount() == 0) {
            emptyStateView.setVisibility(View.VISIBLE);
        } else {
            emptyStateView.setVisibility(View.GONE);
        }

        // MANDATORY OFFLINE CALCULATION IMPLEMENTATION
        // Calculate note count and sum of word count locally from parsed dataset
        int totalNotes = adapter.getCount();
        int totalWords = 0;
        for (int i = 0; i < totalNotes; i++) {
            Note note = adapter.getItem(i);
            totalWords += note.getWordCount();
        }

        statsNotesCount.setText("Notes Count: " + totalNotes);
        statsWordsCount.setText("Total Words: " + totalWords);
    }

    private void confirmDeleteNote(final Note note) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete '" + note.getTitle() + "'?")
                .setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        executeDeletion(note);
                    }
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void executeDeletion(final Note note) {
        mainProgressBar.setVisibility(View.VISIBLE);
        api.deleteNote(note.getId(), new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                mainProgressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                fetchNotes();
            }

            @Override
            public void onError(String errorMessage) {
                mainProgressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Failed to delete: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}