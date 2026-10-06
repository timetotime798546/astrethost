package com.notesphere.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class EditNoteActivity extends Activity {

    private EditText editTitle;
    private EditText editContent;
    private Spinner spinnerCategory;
    private Button btnDelete;
    private Button btnCancel;
    private Button btnSave;
    private TextView textScreenTitle;

    private String noteId = null;
    private String noteTimestamp = null;
    private BackendApi api;
    private ProgressDialog progressDialog;

    private final String[] categories = {"General", "Personal", "Work", "Ideas", "Todo"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);

        editTitle = (EditText) findViewById(R.id.edit_note_title);
        editContent = (EditText) findViewById(R.id.edit_note_content);
        spinnerCategory = (Spinner) findViewById(R.id.spinner_category);
        btnDelete = (Button) findViewById(R.id.button_delete_note);
        btnCancel = (Button) findViewById(R.id.button_cancel_note);
        btnSave = (Button) findViewById(R.id.button_save_note);
        textScreenTitle = (TextView) findViewById(R.id.text_screen_title);

        api = new BackendApi(this);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving...");
        progressDialog.setCancelable(false);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("note_id")) {
            noteId = intent.getStringExtra("note_id");
            String title = intent.getStringExtra("note_title");
            String content = intent.getStringExtra("note_content");
            String category = intent.getStringExtra("note_category");
            noteTimestamp = intent.getStringExtra("note_timestamp");

            textScreenTitle.setText("Edit Note");
            btnDelete.setVisibility(View.VISIBLE);

            editTitle.setText(title);
            editContent.setText(content);

            for (int j = 0; j < categories.length; j++) {
                if (categories[j].equalsIgnoreCase(category)) {
                    spinnerCategory.setSelection(j);
                    break;
                }
            }
        } else {
            textScreenTitle.setText("Create Note");
            btnDelete.setVisibility(View.GONE);
        }

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDelete();
            }
        });
    }

    private void saveNote() {
        String title = editTitle.getText().toString().trim();
        String content = editContent.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a note title", Toast.LENGTH_SHORT).show();
            return;
        }

        if (content.isEmpty()) {
            Toast.makeText(this, "Please write some note content", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();

        if (noteId == null) {
            // Create a new note
            Note newNote = new Note(null, title, content, category, String.valueOf(System.currentTimeMillis()));
            api.createNote(newNote, new ApiCallback<Note>() {
                @Override
                public void onSuccess(Note result) {
                    progressDialog.dismiss();
                    Toast.makeText(EditNoteActivity.this, "Note created online", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                }

                @Override
                public void onError(String error) {
                    progressDialog.dismiss();
                    Toast.makeText(EditNoteActivity.this, "Create failed: " + error, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // Update existing note
            Note updatedNote = new Note(noteId, title, content, category, String.valueOf(System.currentTimeMillis()));
            api.updateNote(updatedNote, new ApiCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    progressDialog.dismiss();
                    Toast.makeText(EditNoteActivity.this, "Note updated online", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                }

                @Override
                public void onError(String error) {
                    progressDialog.dismiss();
                    Toast.makeText(EditNoteActivity.this, "Update failed: " + error, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void confirmDelete() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Note");
        builder.setMessage("Are you sure you want to delete this note?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteNote();
            }
        });
        builder.setNegativeButton("No", null);
        builder.show();
    }

    private void deleteNote() {
        if (noteId != null) {
            progressDialog.setMessage("Deleting...");
            progressDialog.show();
            api.deleteNote(noteId, new ApiCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    progressDialog.dismiss();
                    Toast.makeText(EditNoteActivity.this, "Note deleted online", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                }

                @Override
                public void onError(String error) {
                    progressDialog.dismiss();
                    Toast.makeText(EditNoteActivity.this, "Delete failed: " + error, Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}