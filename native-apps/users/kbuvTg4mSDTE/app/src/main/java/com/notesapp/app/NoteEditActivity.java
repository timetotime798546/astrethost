package com.notesapp.app;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class NoteEditActivity extends android.app.Activity {

    private static final String TAG = "NoteEditActivity";

    private EditText noteTitleEditText;
    private EditText noteContentEditText;
    private EditText noteCategoryEditText;
    private Button saveNoteButton;
    private Button deleteNoteButton;

    private Note currentNote;
    private BackendApi backendApi;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_edit);

        mainHandler = new Handler(Looper.getMainLooper());
        backendApi = new BackendApi(this);

        noteTitleEditText = (EditText) findViewById(R.id.noteTitleEditText);
        noteContentEditText = (EditText) findViewById(R.id.noteContentEditText);
        noteCategoryEditText = (EditText) findViewById(R.id.noteCategoryEditText);
        saveNoteButton = (Button) findViewById(R.id.saveNoteButton);
        deleteNoteButton = (Button) findViewById(R.id.deleteNoteButton);

        currentNote = (Note) getIntent().getSerializableExtra("note");

        if (currentNote != null) {
            // Existing note: populate fields
            noteTitleEditText.setText(currentNote.getTitle());
            noteContentEditText.setText(currentNote.getContent());
            noteCategoryEditText.setText(currentNote.getCategory());
            deleteNoteButton.setVisibility(View.VISIBLE);
        } else {
            // New note: hide delete button
            deleteNoteButton.setVisibility(View.GONE);
        }

        saveNoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
            }
        });

        deleteNoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteConfirmationDialog();
            }
        });
    }

    private void saveNote() {
        String title = noteTitleEditText.getText().toString().trim();
        String content = noteContentEditText.getText().toString().trim();
        String category = noteCategoryEditText.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject data = new JSONObject();
        try {
            data.put("title", title);
            data.put("content", content);
            data.put("category", category);
        } catch (JSONException e) {
            Log.e(TAG, "JSON error creating note data: " + e.getMessage());
            Toast.makeText(this, "Error preparing note data.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentNote == null) {
            // Create new note
            backendApi.createRecord("notes", data, new BackendApi.BackendApiCallback() {
                @Override
                public void onSuccess(final JSONObject response) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(NoteEditActivity.this, "Note created successfully!", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        }
                    });
                }

                @Override
                public void onError(final String error) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.e(TAG, "Failed to create note: " + error);
                            Toast.makeText(NoteEditActivity.this, "Failed to create note: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        } else {
            // Update existing note
            backendApi.updateRecord(currentNote.getId(), data, new BackendApi.BackendApiCallback() {
                @Override
                public void onSuccess(final JSONObject response) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(NoteEditActivity.this, "Note updated successfully!", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        }
                    });
                }

                @Override
                public void onError(final String error) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.e(TAG, "Failed to update note: " + error);
                            Toast.makeText(NoteEditActivity.this, "Failed to update note: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        }
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note permanently?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteNote();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteNote() {
        if (currentNote != null && currentNote.getId() != null) {
            backendApi.deleteRecord(currentNote.getId(), new BackendApi.BackendApiCallback() {
                @Override
                public void onSuccess(final JSONObject response) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(NoteEditActivity.this, "Note deleted successfully!", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        }
                    });
                }

                @Override
                public void onError(final String error) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.e(TAG, "Failed to delete note: " + error);
                            Toast.makeText(NoteEditActivity.this, "Failed to delete note: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        } else {
            Toast.makeText(this, "Cannot delete a note that doesn't exist.", Toast.LENGTH_SHORT).show();
        }
    }
}