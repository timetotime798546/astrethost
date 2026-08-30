package com.quicknote.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "QuickNotePrefs";
    private static final String KEY_NOTES = "notes_list";

    private EditText noteInput;
    private Button addNoteButton;
    private LinearLayout notesContainer;
    private ScrollView scrollView;
    private TextView emptyTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        noteInput = (EditText) findViewById(R.id.noteInput);
        addNoteButton = (Button) findViewById(R.id.addNoteButton);
        notesContainer = (LinearLayout) findViewById(R.id.notesContainer);
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        emptyTextView = (TextView) findViewById(R.id.emptyTextView);

        addNoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNewNote();
            }
        });

        loadNotes();
    }

    private void saveNewNote() {
        String text = noteInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(this, "Please write something first!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONArray notesArray = getNotesFromPrefs();
            JSONObject newNote = new JSONObject();
            newNote.put("id", UUID.randomUUID().toString());
            newNote.put("text", text);
            newNote.put("timestamp", System.currentTimeMillis());

            notesArray.put(newNote);
            saveNotesToPrefs(notesArray);

            noteInput.setText("");
            loadNotes();
            Toast.makeText(this, "Note saved!", Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private JSONArray getNotesFromPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String notesStr = prefs.getString(KEY_NOTES, "[]");
        try {
            return new JSONArray(notesStr);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    private void saveNotesToPrefs(JSONArray array) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_NOTES, array.toString()).apply();
    }

    private void loadNotes() {
        notesContainer.removeAllViews();
        JSONArray notesArray = getNotesFromPrefs();

        if (notesArray.length() == 0) {
            emptyTextView.setVisibility(View.VISIBLE);
            scrollView.setVisibility(View.GONE);
            return;
        }

        emptyTextView.setVisibility(View.GONE);
        scrollView.setVisibility(View.VISIBLE);

        for (int i = notesArray.length() - 1; i >= 0; i--) {
            try {
                final JSONObject noteObj = notesArray.getJSONObject(i);
                final String id = noteObj.getString("id");
                String text = noteObj.getString("text");

                // Note item card
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.HORIZONTAL);
                
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                cardParams.setMargins(0, 0, 0, 16);
                card.setLayoutParams(cardParams);
                card.setPadding(24, 24, 24, 24);
                card.setBackgroundColor(Color.parseColor("#FFFFFF"));

                // Text view for Note Content
                TextView noteText = new TextView(this);
                noteText.setText(text);
                noteText.setTextColor(Color.parseColor("#333333"));
                noteText.setTextSize(16);
                
                LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1.0f
                );
                noteText.setLayoutParams(textParams);
                card.addView(noteText);

                // Delete Button
                Button deleteBtn = new Button(this);
                deleteBtn.setText("Delete");
                deleteBtn.setTextColor(Color.parseColor("#E53935"));
                deleteBtn.setBackgroundColor(Color.TRANSPARENT);
                deleteBtn.setPadding(16, 0, 16, 0);
                
                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                deleteBtn.setLayoutParams(btnParams);
                
                deleteBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deleteNote(id);
                    }
                });
                
                card.addView(deleteBtn);
                notesContainer.addView(card);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void deleteNote(String id) {
        JSONArray notesArray = getNotesFromPrefs();
        JSONArray updatedArray = new JSONArray();
        for (int i = 0; i < notesArray.length(); i++) {
            try {
                JSONObject note = notesArray.getJSONObject(i);
                if (!note.getString("id").equals(id)) {
                    updatedArray.put(note);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        saveNotesToPrefs(updatedArray);
        loadNotes();
        Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
    }
}