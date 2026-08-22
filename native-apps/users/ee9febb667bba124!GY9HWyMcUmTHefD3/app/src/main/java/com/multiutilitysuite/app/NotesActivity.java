package com.multiutilitysuite.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Arrays;

public class NotesActivity extends Activity {
    private SharedPreferences prefs;
    private static final String PREF_KEY = "saved_notes";
    private ArrayList<String> notesList = new ArrayList<String>();
    private LinearLayout notesContainer;
    private EditText inputNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("NotesPref", Context.MODE_PRIVATE);
        loadNotes();

        LinearLayout parent = new LinearLayout(this);
        parent.setOrientation(LinearLayout.VERTICAL);
        parent.setBackgroundColor(Color.parseColor("#F5F5F5"));

        // Custom Header Bar
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setBackgroundColor(Color.parseColor("#3F51B5"));
        header.setPadding(32, 24, 32, 24);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button backButton = new Button(this);
        backButton.setText("< Back");
        backButton.setTextColor(Color.WHITE);
        backButton.setBackgroundColor(Color.TRANSPARENT);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        header.addView(backButton);

        TextView headerTitle = new TextView(this);
        headerTitle.setText("Personal Notes");
        headerTitle.setTextColor(Color.WHITE);
        headerTitle.setTextSize(20);
        headerTitle.setPadding(32, 0, 0, 0);
        header.addView(headerTitle);
        parent.addView(header);

        // Note Creator Input
        LinearLayout inputLayout = new LinearLayout(this);
        inputLayout.setOrientation(LinearLayout.HORIZONTAL);
        inputLayout.setPadding(16, 16, 16, 16);

        inputNote = new EditText(this);
        inputNote.setHint("Type dynamic note here...");
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        inputNote.setLayoutParams(editParams);
        inputLayout.addView(inputNote);

        Button saveBtn = new Button(this);
        saveBtn.setText("Save");
        saveBtn.setBackgroundColor(Color.parseColor("#4CAF50"));
        saveBtn.setTextColor(Color.WHITE);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String noteText = inputNote.getText().toString().trim();
                if (!noteText.isEmpty()) {
                    notesList.add(noteText);
                    saveNotes();
                    renderNotes();
                    inputNote.setText("");
                }
            }
        });
        inputLayout.addView(saveBtn);
        parent.addView(inputLayout);

        // Scrollable Notes list
        ScrollView scrollView = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        scrollView.setLayoutParams(scrollParams);

        notesContainer = new LinearLayout(this);
        notesContainer.setOrientation(LinearLayout.VERTICAL);
        notesContainer.setPadding(16, 16, 16, 16);
        scrollView.addView(notesContainer);
        parent.addView(scrollView);

        renderNotes();
        setContentView(parent);
    }

    private void loadNotes() {
        String rawStr = prefs.getString(PREF_KEY, "");
        notesList.clear();
        if (!rawStr.isEmpty()) {
            notesList.addAll(Arrays.asList(rawStr.split("##NOTE##")));
        }
    }

    private void saveNotes() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < notesList.size(); i++) {
            sb.append(notesList.get(i));
            if (i < notesList.size() - 1) {
                sb.append("##NOTE##");
            }
        }
        prefs.edit().putString(PREF_KEY, sb.toString()).apply();
    }

    private void renderNotes() {
        notesContainer.removeAllViews();
        if (notesList.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("No notes saved yet. Add one above!");
            emptyView.setTextSize(16);
            emptyView.setGravity(Gravity.CENTER);
            emptyView.setPadding(0, 64, 0, 0);
            notesContainer.addView(emptyView);
            return;
        }

        for (int i = 0; i < notesList.size(); i++) {
            final int index = i;
            String note = notesList.get(i);

            LinearLayout noteCard = new LinearLayout(this);
            noteCard.setOrientation(LinearLayout.HORIZONTAL);
            noteCard.setBackgroundColor(Color.WHITE);
            noteCard.setPadding(24, 24, 24, 24);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 8, 0, 8);
            noteCard.setLayoutParams(cardParams);

            TextView noteTextView = new TextView(this);
            noteTextView.setText(note);
            noteTextView.setTextSize(16);
            noteTextView.setTextColor(Color.BLACK);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            noteTextView.setLayoutParams(textParams);
            noteCard.addView(noteTextView);

            Button deleteBtn = new Button(this);
            deleteBtn.setText("X");
            deleteBtn.setBackgroundColor(Color.parseColor("#F44336"));
            deleteBtn.setTextColor(Color.WHITE);
            deleteBtn.setLayoutParams(new LinearLayout.LayoutParams(
                100, LinearLayout.LayoutParams.WRAP_CONTENT));
            deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    notesList.remove(index);
                    saveNotes();
                    renderNotes();
                }
            });
            noteCard.addView(deleteBtn);
            notesContainer.addView(noteCard);
        }
    }
}