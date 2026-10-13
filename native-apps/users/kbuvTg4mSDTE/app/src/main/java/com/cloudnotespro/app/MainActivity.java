package com.cloudnotespro.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import android.app.Activity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private ListView listViewNotes;
    private View tvEmpty;
    private TextView tvNoteCount;
    private Button fabAddNote;
    private Button btnLogout;
    private BackendApi backendApi;
    private List<NoteModel> noteList;
    private NoteAdapter noteAdapter;

    private static final int REQUEST_CODE_EDITOR = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Initializing MainActivity");
        setContentView(R.layout.activity_main);

        backendApi = new BackendApi(this);

        listViewNotes = findViewById(R.id.listViewNotes);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvNoteCount = findViewById(R.id.tvNoteCount);
        fabAddNote = findViewById(R.id.fabAddNote);
        btnLogout = findViewById(R.id.btnLogout);

        if (listViewNotes == null) {
            Log.e(TAG, "CRITICAL: listViewNotes is null!");
        }
        if (tvEmpty == null) {
            Log.e(TAG, "CRITICAL: tvEmpty is null!");
        }
        if (tvNoteCount == null) {
            Log.e(TAG, "CRITICAL: tvNoteCount is null!");
        }
        if (fabAddNote == null) {
            Log.e(TAG, "CRITICAL: fabAddNote is null!");
        }
        if (btnLogout == null) {
            Log.e(TAG, "CRITICAL: btnLogout is null!");
        }

        noteList = new ArrayList<>();
        noteAdapter = new NoteAdapter(this, noteList);
        if (listViewNotes != null) {
            listViewNotes.setAdapter(noteAdapter);
        }

        if (fabAddNote != null) {
            fabAddNote.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, NoteEditorActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_EDITOR);
                }
            });
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SharedPreferences prefs = getSharedPreferences("CloudNotesPrefs", MODE_PRIVATE);
                    String token = prefs.getString("auth_token", "");

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                backendApi.logout(token);
                            } catch (Exception e) {
                                Log.e(TAG, "Logout network error", e);
                            }
                        }
                    }).start();

                    prefs.edit().remove("auth_token").apply();
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            });
        }

        if (listViewNotes != null) {
            listViewNotes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (position >= 0 && position < noteList.size()) {
                        NoteModel note = noteList.get(position);
                        Intent intent = new Intent(MainActivity.this, NoteEditorActivity.class);
                        intent.putExtra("note_id", note.getId());
                        intent.putExtra("note_title", note.getTitle());
                        intent.putExtra("note_category", note.getCategory());
                        intent.putExtra("note_content", note.getContent());
                        startActivityForResult(intent, REQUEST_CODE_EDITOR);
                    }
                }
            });
        }

        fetchNotes();
    }

    private void fetchNotes() {
        Log.d(TAG, "fetchNotes: Starting cloud notes sync...");
        SharedPreferences prefs = getSharedPreferences("CloudNotesPrefs", MODE_PRIVATE);
        String token = prefs.getString("auth_token", "");

        new Thread(new Runnable() {
            @Override
            public void run() {
                JSONObject response = null;
                try {
                    response = backendApi.readData(token, "notes");
                } catch (Exception e) {
                    Log.e(TAG, "fetchNotes network exception", e);
                }

                final JSONObject finalResponse = response;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (finalResponse != null && finalResponse.optBoolean("success", false)) {
                                Log.d(TAG, "fetchNotes: Success received from backend.");
                                noteList.clear();
                                JSONArray records = finalResponse.optJSONArray("records");
                                if (records == null && finalResponse.has("data")) {
                                    Object dataObj = finalResponse.opt("data");
                                    if (dataObj instanceof JSONArray) {
                                        records = (JSONArray) dataObj;
                                    }
                                }
                                if (records != null) {
                                    for (int i = 0; i < records.length(); i++) {
                                        JSONObject rec = records.getJSONObject(i);
                                        String id = rec.optString("id", rec.optString("_id", ""));
                                        JSONObject data = rec.optJSONObject("data");
                                        if (data != null) {
                                            String title = data.optString("title", "");
                                            String content = data.optString("content", "");
                                            String category = data.optString("category", "");
                                            noteList.add(new NoteModel(id, title, content, category));
                                        } else {
                                            String title = rec.optString("title", "");
                                            String content = rec.optString("content", "");
                                            String category = rec.optString("category", "");
                                            if (!title.isEmpty() || !content.isEmpty()) {
                                                noteList.add(new NoteModel(id, title, content, category));
                                            }
                                        }
                                    }
                                }
                                if (noteAdapter != null) {
                                    noteAdapter.notifyDataSetChanged();
                                }

                                int count = noteList.size();
                                if (tvNoteCount != null) {
                                    tvNoteCount.setText(count + (count == 1 ? " cloud note" : " cloud notes"));
                                }

                                if (noteList.isEmpty()) {
                                    if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
                                    if (listViewNotes != null) listViewNotes.setVisibility(View.GONE);
                                } else {
                                    if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
                                    if (listViewNotes != null) listViewNotes.setVisibility(View.VISIBLE);
                                }
                            } else {
                                String err = finalResponse != null ? finalResponse.optString("error", finalResponse.optString("message", "Failed to load notes")) : "Failed to load notes";
                                Log.e(TAG, "fetchNotes failed: " + err);
                                Toast.makeText(MainActivity.this, err, Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing notes response", e);
                            Toast.makeText(MainActivity.this, "Error parsing notes", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_EDITOR && resultCode == RESULT_OK) {
            fetchNotes();
        }
    }
}