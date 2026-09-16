package com.simplenotepad.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "SimpleNotepadPrefs";
    private static final String KEY_NOTES = "notes_list";

    private EditText noteInput;
    private Button btnSave;
    private Button btnClear;
    private ListView notesListView;

    private ArrayList<String> notesList;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        noteInput = (EditText) findViewById(R.id.note_input);
        btnSave = (Button) findViewById(R.id.btn_save);
        btnClear = (Button) findViewById(R.id.btn_clear);
        notesListView = (ListView) findViewById(R.id.notes_listview);

        notesList = loadNotes();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, notesList);
        notesListView.setAdapter(adapter);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String noteText = noteInput.getText().toString().trim();
                if (noteText.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter some text first!", Toast.LENGTH_SHORT).show();
                } else {
                    notesList.add(noteText);
                    adapter.notifyDataSetChanged();
                    saveNotes(notesList);
                    noteInput.setText("");
                    Toast.makeText(MainActivity.this, "Note saved!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (notesList.isEmpty()) {
                    Toast.makeText(MainActivity.this, "No notes to clear!", Toast.LENGTH_SHORT).show();
                    return;
                }

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Clear All Notes")
                        .setMessage("Are you sure you want to delete all notes?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                notesList.clear();
                                adapter.notifyDataSetChanged();
                                saveNotes(notesList);
                                Toast.makeText(MainActivity.this, "All notes cleared!", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });

        notesListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete Note")
                        .setMessage("Do you want to delete this note?")
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                notesList.remove(position);
                                adapter.notifyDataSetChanged();
                                saveNotes(notesList);
                                Toast.makeText(MainActivity.this, "Note deleted!", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            }
        });
    }

    private ArrayList<String> loadNotes() {
        ArrayList<String> list = new ArrayList<String>();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String json = prefs.getString(KEY_NOTES, null);
        if (json != null) {
            try {
                JSONArray jsonArray = new JSONArray(json);
                for (int i = 0; i < jsonArray.length(); i++) {
                    list.add(jsonArray.getString(i));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    private void saveNotes(ArrayList<String> list) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < list.size(); i++) {
            jsonArray.put(list.get(i));
        }
        editor.putString(KEY_NOTES, jsonArray.toString());
        editor.apply();
    }
}