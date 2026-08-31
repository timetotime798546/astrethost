package com.omnitoolspremium.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.Map;

public class NotesActivity extends Activity {

    private ListView listNotes;
    private ArrayList<String> noteKeys;
    private ArrayList<String> noteTitles;
    private ArrayAdapter<String> adapter;
    private SharedPreferences sharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        listNotes = (ListView) findViewById(R.id.list_notes);
        Button btnCreateNote = (Button) findViewById(R.id.btn_create_note);

        sharedPrefs = getSharedPreferences("PremiumNotesPrefs", Context.MODE_PRIVATE);
        noteKeys = new ArrayList<String>();
        noteTitles = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, noteTitles);
        listNotes.setAdapter(adapter);

        listNotes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(NotesActivity.this, NoteEditActivity.class);
                intent.putExtra("NOTE_KEY", noteKeys.get(position));
                startActivityForResult(intent, 102);
            }
        });

        btnCreateNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NotesActivity.this, NoteEditActivity.class);
                intent.putExtra("NOTE_KEY", "NEW_" + System.currentTimeMillis());
                startActivityForResult(intent, 102);
            }
        });

        loadNotes();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            loadNotes();
        }
    }

    private void loadNotes() {
        noteKeys.clear();
        noteTitles.clear();
        Map<String, ?> allEntries = sharedPrefs.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String key = entry.getKey();
            String rawVal = entry.getValue().toString();
            String title = key;
            if (rawVal.contains("\n")) {
                title = rawVal.substring(0, Math.min(rawVal.indexOf("\n"), 20)) + "...";
            } else {
                title = rawVal.substring(0, Math.min(rawVal.length(), 20));
            }
            noteKeys.add(key);
            noteTitles.add(title);
        }
        if (noteTitles.isEmpty()) {
            noteTitles.add("No custom entries. Tap write below!");
            noteKeys.add("EMPTY");
        }
        adapter.notifyDataSetChanged();
    }
}