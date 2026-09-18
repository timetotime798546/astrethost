package com.quicknotes.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements NoteAdapter.NoteDeleteListener {

    private EditText editTextNote;
    private Button buttonAddNote;
    private ListView listViewNotes;
    private TextView textViewEmpty;
    
    private NotesDatabaseHelper dbHelper;
    private NoteAdapter adapter;
    private List<Note> notesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new NotesDatabaseHelper(this);
        notesList = new ArrayList<Note>();

        editTextNote = (EditText) findViewById(R.id.editTextNote);
        buttonAddNote = (Button) findViewById(R.id.buttonAddNote);
        listViewNotes = (ListView) findViewById(R.id.listViewNotes);
        textViewEmpty = (TextView) findViewById(R.id.textViewEmpty);

        adapter = new NoteAdapter(this, notesList, dbHelper, this);
        listViewNotes.setAdapter(adapter);

        refreshNotesList();

        buttonAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNote();
            }
        });
    }

    private void addNote() {
        String noteText = editTextNote.getText().toString().trim();
        if (noteText.isEmpty()) {
            Toast.makeText(MainActivity.this, "Please enter a note", Toast.LENGTH_SHORT).show();
            return;
        }

        long id = dbHelper.addNote(noteText);
        if (id != -1) {
            Toast.makeText(MainActivity.this, "Note added!", Toast.LENGTH_SHORT).show();
            editTextNote.setText("");
            refreshNotesList();
        } else {
            Toast.makeText(MainActivity.this, "Failed to save note", Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshNotesList() {
        notesList.clear();
        notesList.addAll(dbHelper.getAllNotes());
        adapter.notifyDataSetChanged();

        if (notesList.isEmpty()) {
            listViewNotes.setVisibility(View.GONE);
            textViewEmpty.setVisibility(View.VISIBLE);
        } else {
            listViewNotes.setVisibility(View.VISIBLE);
            textViewEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onNoteDeleted() {
        Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
        refreshNotesList();
    }
}