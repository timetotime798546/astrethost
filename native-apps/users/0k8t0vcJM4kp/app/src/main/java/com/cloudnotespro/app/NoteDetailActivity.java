package com.cloudnotespro.app;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Calendar;
import org.json.JSONObject;

public class NoteDetailActivity extends Activity {
    private ImageView imgDetailHeader;
    private TextView txtHeaderBadge;
    private EditText detailTitle, detailDescription;
    private Spinner detailTypeSpinner, detailStatusSpinner;
    private TextView txtDetailDate;
    private Button btnPickDate, btnSaveNote, btnDeleteNote;

    private BackendApi backendApi;
    private String userToken;
    private String noteId = null; // null if creating note

    // Unsplash Mapping Categories Images
    private static final String IMG_WORK = "https://images.unsplash.com/photo-1486312338219-ce68d2c6f44d?w=500&auto=format&fit=crop";
    private static final String IMG_PERSONAL = "https://images.unsplash.com/photo-1513542789411-b6a5d4f31634?w=500&auto=format&fit=crop";
    private static final String IMG_IDEAS = "https://images.unsplash.com/photo-1507537297725-24a1c029d3ca?w=500&auto=format&fit=crop";
    private static final String IMG_JOURNAL = "https://images.unsplash.com/photo-1517842645767-c639042777db?w=500&auto=format&fit=crop";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);

        SharedPreferences prefs = getSharedPreferences("CloudNotesPrefs", MODE_PRIVATE);
        userToken = prefs.getString("auth_token", "");
        if (userToken.isEmpty()) {
            Toast.makeText(this, "Session expired, please login again", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        backendApi = new BackendApi(this);

        imgDetailHeader = (ImageView) findViewById(R.id.imgDetailHeader);
        txtHeaderBadge = (TextView) findViewById(R.id.txtHeaderBadge);
        detailTitle = (EditText) findViewById(R.id.detailTitle);
        detailDescription = (EditText) findViewById(R.id.detailDescription);
        detailTypeSpinner = (Spinner) findViewById(R.id.detailTypeSpinner);
        detailStatusSpinner = (Spinner) findViewById(R.id.detailStatusSpinner);
        txtDetailDate = (TextView) findViewById(R.id.txtDetailDate);
        btnPickDate = (Button) findViewById(R.id.btnPickDate);
        btnSaveNote = (Button) findViewById(R.id.btnSaveNote);
        btnDeleteNote = (Button) findViewById(R.id.btnDeleteNote);

        // Spinners binding
        final String[] types = {"Personal", "Work", "Ideas", "Journal"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        detailTypeSpinner.setAdapter(typeAdapter);

        final String[] statuses = {"Active", "Completed"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statuses);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        detailStatusSpinner.setAdapter(statusAdapter);

        // Date selector picker
        btnPickDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        // Dynamic context-aware category banner changes in real-time on selection
        detailTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = types[position];
                txtHeaderBadge.setText(selectedType.toUpperCase());
                String imageUrl = getImageUrlForType(selectedType);
                ImageLoader.loadImage(imageUrl, imgDetailHeader);

                // Update overlay badge color
                int badgeColor = getTypeBadgeColor(selectedType);
                GradientDrawable badgeShape = new GradientDrawable();
                badgeShape.setColor(badgeColor);
                badgeShape.setCornerRadius(12f);
                txtHeaderBadge.setBackground(badgeShape);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Initialize with default date
        Calendar c = Calendar.getInstance();
        String defaultDateStr = c.get(Calendar.YEAR) + "-" + (c.get(Calendar.MONTH) + 1) + "-" + c.get(Calendar.DAY_OF_MONTH);
        txtDetailDate.setText(defaultDateStr);

        // Fetch passed intents (determines View vs Create)
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("note_id")) {
            noteId = extras.getString("note_id");
            detailTitle.setText(extras.getString("note_title", ""));
            detailDescription.setText(extras.getString("note_description", ""));
            txtDetailDate.setText(extras.getString("note_date", defaultDateStr));
            
            // Set spinner selections
            String statusValue = extras.getString("note_status", "Active");
            for (int i = 0; i < statuses.length; i++) {
                if (statuses[i].equalsIgnoreCase(statusValue)) {
                    detailStatusSpinner.setSelection(i);
                    break;
                }
            }

            String typeValue = extras.getString("note_type", "Personal");
            for (int i = 0; i < types.length; i++) {
                if (types[i].equalsIgnoreCase(typeValue)) {
                    detailTypeSpinner.setSelection(i);
                    break;
                }
            }

            // Expose Delete option
            btnDeleteNote.setVisibility(View.VISIBLE);
        }

        btnSaveNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
            }
        });

        btnDeleteNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteNote();
            }
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog pickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                String chosenDate = year + "-" + (monthOfYear + 1) + "-" + dayOfMonth;
                txtDetailDate.setText(chosenDate);
            }
        }, year, month, day);
        pickerDialog.show();
    }

    private String getImageUrlForType(String type) {
        switch (type.toLowerCase()) {
            case "work":
                return IMG_WORK;
            case "ideas":
                return IMG_IDEAS;
            case "journal":
                return IMG_JOURNAL;
            default:
                return IMG_PERSONAL;
        }
    }

    private int getTypeBadgeColor(String type) {
        switch (type.toLowerCase()) {
            case "work":
                return 0xFF1976D2; // Blue
            case "ideas":
                return 0xFFFBC02D; // Amber
            case "journal":
                return 0xFF388E3C; // Green
            default:
                return 0xFF7B1FA2; // Purple/Personal
        }
    }

    private void saveNote() {
        String title = detailTitle.getText().toString().trim();
        String description = detailDescription.getText().toString().trim();
        String date = txtDetailDate.getText().toString().trim();
        String type = detailTypeSpinner.getSelectedItem().toString();
        String status = detailStatusSpinner.getSelectedItem().toString();
        String imageUrl = getImageUrlForType(type);

        if (title.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Title and description fields are mandatory", Toast.LENGTH_SHORT).show();
            return;
        }

        final ProgressDialog dialog = ProgressDialog.show(this, "Saving note", "Encrypting to cloud database...", true);

        final Note note = new Note(noteId, title, description, date, status, type, imageUrl);

        if (noteId == null) {
            // Creation
            backendApi.createNote(userToken, note.toDataJson(), new BackendApi.ApiCallback<JSONObject>() {
                @Override
                public void onSuccess(JSONObject result) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            Toast.makeText(NoteDetailActivity.this, "Successfully added new cloud note!", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                }

                @Override
                public void onError(final String errorMessage) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            Toast.makeText(NoteDetailActivity.this, "Save Failed: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        } else {
            // Updating
            backendApi.updateNote(userToken, noteId, note.toDataJson(), new BackendApi.ApiCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            Toast.makeText(NoteDetailActivity.this, "Successfully updated note details!", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                }

                @Override
                public void onError(final String errorMessage) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            Toast.makeText(NoteDetailActivity.this, "Update Failed: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        }
    }

    private void deleteNote() {
        if (noteId == null) return;

        final ProgressDialog dialog = ProgressDialog.show(this, "Deleting note", "Removing dynamic record...", true);

        backendApi.deleteNote(userToken, noteId, new BackendApi.ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        Toast.makeText(NoteDetailActivity.this, "Note deleted successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }

            @Override
            public void onError(final String errorMessage) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        Toast.makeText(NoteDetailActivity.this, "Delete failed: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}