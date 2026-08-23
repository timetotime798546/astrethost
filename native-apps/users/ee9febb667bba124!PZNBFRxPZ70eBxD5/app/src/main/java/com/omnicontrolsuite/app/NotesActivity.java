package com.omnicontrolsuite.app;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import java.util.ArrayList;

public class NotesActivity extends Activity {

    private DatabaseHelper dbHelper;
    private EditText editTitle;
    private EditText editContent;
    private Button btnDelete;
    private ListView listNotes;

    private ArrayList<Integer> noteIds;
    private ArrayList<String> noteTitles;
    private ArrayAdapter<String> adapter;

    private int activeNoteId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        dbHelper = new DatabaseHelper(this);
        noteIds = new ArrayList<Integer>();
        noteTitles = new ArrayList<String>();

        editTitle = (EditText) findViewById(R.id.editNoteTitle);
        editContent = (EditText) findViewById(R.id.editNoteContent);
        btnDelete = (Button) findViewById(R.id.btnDeleteNote);
        listNotes = (ListView) findViewById(R.id.listNotes);

        Button btnBack = (Button) findViewById(R.id.btnBack);
        Button btnNew = (Button) findViewById(R.id.btnNewNote);
        Button btnSave = (Button) findViewById(R.id.btnSaveNote);

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, noteTitles);
        listNotes.setAdapter(adapter);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetEditor();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = editTitle.getText().toString().trim();
                String content = editContent.getText().toString().trim();

                if (title.isEmpty()) {
                    Toast.makeText(NotesActivity.this, "Please enter a note title", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (activeNoteId == -1) {
                    dbHelper.insertNote(title, content);
                    Toast.makeText(NotesActivity.this, "Note successfully created", Toast.LENGTH_SHORT).show();
                } else {
                    dbHelper.updateNote(activeNoteId, title, content);
                    Toast.makeText(NotesActivity.this, "Note updated", Toast.LENGTH_SHORT).show();
                }
                resetEditor();
                loadNotes();
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activeNoteId != -1) {
                    dbHelper.deleteNote(activeNoteId);
                    Toast.makeText(NotesActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                    resetEditor();
                    loadNotes();
                }
            }
        });

        listNotes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                activeNoteId = noteIds.get(position);
                loadNoteInEditor(activeNoteId);
            }
        });

        loadNotes();
    }

    private void resetEditor() {
        activeNoteId = -1;
        editTitle.setText("");
        editContent.setText("");
        btnDelete.setVisibility(View.GONE);
    }

    private void loadNotes() {
        noteIds.clear();
        noteTitles.clear();

        Cursor cursor = dbHelper.getAllNotes();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTE_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTE_TITLE));
                noteIds.add(id);
                noteTitles.add(title);
            }
            cursor.close();
        }
        adapter.notifyDataSetChanged();
    }

    private void loadNoteInEditor(int noteId) {
        Cursor cursor = dbHelper.getAllNotes();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTE_ID));
                if (id == noteId) {
                    String title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTE_TITLE));
                    String content = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTE_CONTENT));
                    editTitle.setText(title);
                    editContent.setText(content);
                    btnDelete.setVisibility(View.VISIBLE);
                    break;
                }
            }
            cursor.close();
        }
    }
}