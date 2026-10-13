package com.cloudnotespro.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import android.app.Activity;

import org.json.JSONObject;

public class NoteEditorActivity extends Activity {

    private EditText etTitle, etCategory, etContent;
    private Button btnSave, btnDelete;
    private BackendApi backendApi;
    private String noteId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        backendApi = new BackendApi(this);

        etTitle = findViewById(R.id.etTitle);
        etCategory = findViewById(R.id.etCategory);
        etContent = findViewById(R.id.etContent);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);

        noteId = getIntent().getStringExtra("note_id");
        if (noteId != null) {
            etTitle.setText(getIntent().getStringExtra("note_title"));
            etCategory.setText(getIntent().getStringExtra("note_category"));
            etContent.setText(getIntent().getStringExtra("note_content"));
            btnDelete.setVisibility(View.VISIBLE);
            setTitle("Edit Note");
        } else {
            setTitle("New Note");
        }

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = etTitle.getText().toString().trim();
                String category = etCategory.getText().toString().trim();
                String content = etContent.getText().toString().trim();

                if (title.isEmpty()) {
                    Toast.makeText(NoteEditorActivity.this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                SharedPreferences prefs = getSharedPreferences("CloudNotesPrefs", MODE_PRIVATE);
                String token = prefs.getString("auth_token", "");

                btnSave.setEnabled(false);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject dataObj = new JSONObject();
                            dataObj.put("title", title);
                            dataObj.put("category", category);
                            dataObj.put("content", content);

                            JSONObject result;
                            if (noteId == null) {
                                result = backendApi.createData(token, "notes", dataObj);
                            } else {
                                result = backendApi.updateData(token, noteId, dataObj);
                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    btnSave.setEnabled(true);
                                    if (result != null && result.optBoolean("success", false)) {
                                        Toast.makeText(NoteEditorActivity.this, "Note saved successfully", Toast.LENGTH_SHORT).show();
                                        setResult(RESULT_OK);
                                        finish();
                                    } else {
                                        String err = result != null ? result.optString("error", "Failed to save note") : "Failed to save note";
                                        Toast.makeText(NoteEditorActivity.this, err, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        } catch (Exception e) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    btnSave.setEnabled(true);
                                    Toast.makeText(NoteEditorActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }).start();
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (noteId == null) return;

                SharedPreferences prefs = getSharedPreferences("CloudNotesPrefs", MODE_PRIVATE);
                String token = prefs.getString("auth_token", "");

                btnDelete.setEnabled(false);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject result = backendApi.deleteData(token, noteId);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                btnDelete.setEnabled(true);
                                if (result != null && result.optBoolean("success", false)) {
                                    Toast.makeText(NoteEditorActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_OK);
                                    finish();
                                } else {
                                    Toast.makeText(NoteEditorActivity.this, "Failed to delete note", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                }).start();
            }
        });
    }
}