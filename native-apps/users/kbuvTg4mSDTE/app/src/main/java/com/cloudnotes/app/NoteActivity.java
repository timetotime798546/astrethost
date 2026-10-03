package com.cloudnotes.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NoteActivity extends Activity {

    private BackendApi api;
    private String noteId = null;

    private TextView noteScreenTitle;
    private EditText noteTitleInput;
    private Spinner noteCategorySpinner;
    private EditText noteContentInput;
    private ProgressBar noteProgressBar;

    private Button btnNoteBack;
    private Button btnNoteSave;

    private final String[] categories = {"Work", "Personal", "Ideas", "Other"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        api = new BackendApi(this);

        noteScreenTitle = (TextView) findViewById(R.id.noteScreenTitle);
        noteTitleInput = (EditText) findViewById(R.id.noteTitleInput);
        noteCategorySpinner = (Spinner) findViewById(R.id.noteCategorySpinner);
        noteContentInput = (EditText) findViewById(R.id.noteContentInput);
        noteProgressBar = (ProgressBar) findViewById(R.id.noteProgressBar);

        btnNoteBack = (Button) findViewById(R.id.btnNoteBack);
        btnNoteSave = (Button) findViewById(R.id.btnNoteSave);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        noteCategorySpinner.setAdapter(adapter);

        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("note_id")) {
            noteId = extras.getString("note_id");
            String title = extras.getString("note_title", "");
            String content = extras.getString("note_content", "");
            String category = extras.getString("note_category", "Other");

            noteScreenTitle.setText("Edit Note");
            noteTitleInput.setText(title);
            noteContentInput.setText(content);

            for (int i = 0; i < categories.length; i++) {
                if (categories[i].equalsIgnoreCase(category)) {
                    noteCategorySpinner.setSelection(i);
                    break;
                }
            }
        } else {
            noteScreenTitle.setText("Create Note");
        }

        btnNoteBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnNoteSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
            }
        });
    }

    private void showProgress(boolean show) {
        noteProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnNoteSave.setEnabled(!show);
        btnNoteBack.setEnabled(!show);
    }

    private void saveNote() {
        String title = noteTitleInput.getText().toString().trim();
        String content = noteContentInput.getText().toString().trim();
        String category = noteCategorySpinner.getSelectedItem().toString();

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress(true);

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        String dateString = sdf.format(new Date());

        if (noteId == null) {
            api.createNote(title, content, category, dateString, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    showProgress(false);
                    Toast.makeText(NoteActivity.this, "Note saved", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String errorMessage) {
                    showProgress(false);
                    Toast.makeText(NoteActivity.this, "Save error: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            api.updateNote(noteId, title, content, category, dateString, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    showProgress(false);
                    Toast.makeText(NoteActivity.this, "Note updated", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String errorMessage) {
                    showProgress(false);
                    Toast.makeText(NoteActivity.this, "Update error: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}