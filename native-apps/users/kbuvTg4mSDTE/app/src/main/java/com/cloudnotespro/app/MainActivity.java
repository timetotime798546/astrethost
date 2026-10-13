package com.cloudnotespro.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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

    private ListView listViewNotes;
    private TextView tvEmpty, tvNoteCount;
    private Button fabAddNote;
    private Button btnLogout;
    private BackendApi backendApi;
    private List<NoteModel> noteList;
    private NoteAdapter noteAdapter;

    private static final int REQUEST_CODE_EDITOR = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        backendApi = new BackendApi(this);

        listViewNotes = findViewById(R.id.listViewNotes);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvNoteCount = findViewById(R.id.tvNoteCount);
        fabAddNote = findViewById(R.id.fabAddNote);
        btnLogout = findViewById(R.id.btnLogout);

        noteList = new ArrayList<>();
        noteAdapter = new NoteAdapter(this, noteList);
        listViewNotes.setAdapter(noteAdapter);

        fabAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NoteEditorActivity.class);
                startActivityForResult(intent, REQUEST_CODE_EDITOR);
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = getSharedPreferences("CloudNotesPrefs", MODE_PRIVATE);
                String token = prefs.getString("auth_token", "");

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        backendApi.logout(token);
                    }
                }).start();

                prefs.edit().remove("auth_token").apply();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        listViewNotes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                NoteModel note = noteList.get(position);
                Intent intent = new Intent(MainActivity.this, NoteEditorActivity.class);
                intent.putExtra("note_id", note.getId());
                intent.putExtra("note_title", note.getTitle());
                intent.putExtra("note_category", note.getCategory());
                intent.putExtra("note_content", note.getContent());
                startActivityForResult(intent, REQUEST_CODE_EDITOR);
            }
        });

        fetchNotes();
    }

    private void fetchNotes() {
        SharedPreferences prefs = getSharedPreferences("CloudNotesPrefs", MODE_PRIVATE);
        String token = prefs.getString("auth_token", "");

        new Thread(new Runnable() {
            @Override
            public void run() {
                JSONObject response = backendApi.readData(token, "notes");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (response != null && response.optBoolean("success", false)) {
                                noteList.clear();
                                JSONArray records = response.optJSONArray("records");
                                if (records != null) {
                                    for (int i = 0; i < records.length(); i++) {
                                        JSONObject rec = records.getJSONObject(i);
                                        String id = rec.optString("id");
                                        JSONObject data = rec.optJSONObject("data");
                                        if (data != null) {
                                            String title = data.optString("title", "");
                                            String content = data.optString("content", "");
                                            String category = data.optString("category", "");
                                            noteList.add(new NoteModel(id, title, content, category));
                                        }
                                    }
                                }
                                noteAdapter.notifyDataSetChanged();

                                int count = noteList.size();
                                tvNoteCount.setText(count + (count == 1 ? " cloud note" : " cloud notes"));

                                if (noteList.isEmpty()) {
                                    tvEmpty.setVisibility(View.VISIBLE);
                                    listViewNotes.setVisibility(View.GONE);
                                } else {
                                    tvEmpty.setVisibility(View.GONE);
                                    listViewNotes.setVisibility(View.VISIBLE);
                                }
                            } else {
                                Toast.makeText(MainActivity.this, "Failed to load notes", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
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