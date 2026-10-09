package com.cloudnotespro.app;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
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
    private ImageView imgThumb1, imgThumb2, imgThumb3;
    private TextView txtHeaderBadge;
    private EditText detailTitle, detailDescription;
    private Spinner detailTypeSpinner, detailStatusSpinner;
    private TextView txtDetailDate;
    private Button btnPickDate, btnSaveNote, btnDeleteNote;

    private BackendApi backendApi;
    private String userToken;
    private String noteId = null; 
    private String selectedImageUrl = "";

    // Context-Aware High-Definition Premium Unsplash Images mapped to categories.
    private static final String[] IMGS_WORK = {
        "https://images.unsplash.com/photo-1486312338219-ce68d2c6f44d?w=800&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1498050108023-c5249f4df085?w=800&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=800&auto=format&fit=crop"
    };

    private static final String[] IMGS_PERSONAL = {
        "https://images.unsplash.com/photo-1513542789411-b6a5d4f31634?w=800&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1490730141103-6cac27aaab94?w=800&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1518495973542-4542c06a5843?w=800&auto=format&fit=crop"
    };

    private static final String[] IMGS_IDEAS = {
        "https://images.unsplash.com/photo-1507537297725-24a1c029d3ca?w=800&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1457369804613-52c61a468e7d?w=800&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1499750310107-5fef28a66643?w=800&auto=format&fit=crop"
    };

    private static final String[] IMGS_JOURNAL = {
        "https://images.unsplash.com/photo-1517842645767-c639042777db?w=800&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=800&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1506784983877-45594efa4cbe?w=800&auto=format&fit=crop"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);

        SharedPreferences prefs = getSharedPreferences("CloudNotesPrefs", MODE_PRIVATE);
        userToken = prefs.getString("auth_token", "");
        if (userToken.isEmpty()) {
            Toast.makeText(this, "Session expired, please log in again", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        backendApi = new BackendApi(this);

        imgDetailHeader = (ImageView) findViewById(R.id.imgDetailHeader);
        imgThumb1 = (ImageView) findViewById(R.id.imgThumb1);
        imgThumb2 = (ImageView) findViewById(R.id.imgThumb2);
        imgThumb3 = (ImageView) findViewById(R.id.imgThumb3);
        txtHeaderBadge = (TextView) findViewById(R.id.txtHeaderBadge);
        detailTitle = (EditText) findViewById(R.id.detailTitle);
        detailDescription = (EditText) findViewById(R.id.detailDescription);
        detailTypeSpinner = (Spinner) findViewById(R.id.detailTypeSpinner);
        detailStatusSpinner = (Spinner) findViewById(R.id.detailStatusSpinner);
        txtDetailDate = (TextView) findViewById(R.id.txtDetailDate);
        btnPickDate = (Button) findViewById(R.id.btnPickDate);
        btnSaveNote = (Button) findViewById(R.id.btnSaveNote);
        btnDeleteNote = (Button) findViewById(R.id.btnDeleteNote);

        final String[] types = {"Personal", "Work", "Ideas", "Journal"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        detailTypeSpinner.setAdapter(typeAdapter);

        final String[] statuses = {"Active", "Completed"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statuses);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        detailStatusSpinner.setAdapter(statusAdapter);

        btnPickDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoundHelper.playClick(NoteDetailActivity.this);
                showDatePicker();
            }
        });

        detailTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = types[position];
                txtHeaderBadge.setText(selectedType.toUpperCase());

                final String[] currentImages = getImagesForType(selectedType);
                selectedImageUrl = currentImages[0];
                ImageLoader.loadImage(selectedImageUrl, imgDetailHeader);

                // Dynamically populate thumbnail preview image cards
                ImageLoader.loadImage(currentImages[0], imgThumb1);
                ImageLoader.loadImage(currentImages[1], imgThumb2);
                ImageLoader.loadImage(currentImages[2], imgThumb3);

                imgThumb1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        selectedImageUrl = currentImages[0];
                        ImageLoader.loadImage(selectedImageUrl, imgDetailHeader);
                        SoundHelper.playClick(NoteDetailActivity.this);
                    }
                });

                imgThumb2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        selectedImageUrl = currentImages[1];
                        ImageLoader.loadImage(selectedImageUrl, imgDetailHeader);
                        SoundHelper.playClick(NoteDetailActivity.this);
                    }
                });

                imgThumb3.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        selectedImageUrl = currentImages[2];
                        ImageLoader.loadImage(selectedImageUrl, imgDetailHeader);
                        SoundHelper.playClick(NoteDetailActivity.this);
                    }
                });

                int badgeColor = getTypeBadgeColor(selectedType);
                GradientDrawable badgeShape = new GradientDrawable();
                badgeShape.setColor(badgeColor);
                badgeShape.setCornerRadius(16f);
                txtHeaderBadge.setBackground(badgeShape);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        Calendar c = Calendar.getInstance();
        String defaultDateStr = c.get(Calendar.YEAR) + "-" + (c.get(Calendar.MONTH) + 1) + "-" + c.get(Calendar.DAY_OF_MONTH);
        txtDetailDate.setText(defaultDateStr);

        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("note_id")) {
            noteId = extras.getString("note_id");
            detailTitle.setText(extras.getString("note_title", ""));
            detailDescription.setText(extras.getString("note_description", ""));
            txtDetailDate.setText(extras.getString("note_date", defaultDateStr));
            selectedImageUrl = extras.getString("note_image", "");

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

            if (!selectedImageUrl.isEmpty()) {
                ImageLoader.loadImage(selectedImageUrl, imgDetailHeader);
            }
            btnDeleteNote.setVisibility(View.VISIBLE);
        }

        btnSaveNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoundHelper.playClick(NoteDetailActivity.this);
                saveNote();
            }
        });

        btnDeleteNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoundHelper.playDelete(NoteDetailActivity.this);
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

    private String[] getImagesForType(String type) {
        switch (type.toLowerCase()) {
            case "work":
                return IMGS_WORK;
            case "ideas":
                return IMGS_IDEAS;
            case "journal":
                return IMGS_JOURNAL;
            default:
                return IMGS_PERSONAL;
        }
    }

    private int getTypeBadgeColor(String type) {
        switch (type.toLowerCase()) {
            case "work":
                return 0xFF6366F1; 
            case "ideas":
                return 0xFF8B5CF6; 
            case "journal":
                return 0xFF06B6D4; 
            default:
                return 0xFFEC4899; 
        }
    }

    private void saveNote() {
        String title = detailTitle.getText().toString().trim();
        String description = detailDescription.getText().toString().trim();
        String date = txtDetailDate.getText().toString().trim();
        String type = detailTypeSpinner.getSelectedItem().toString();
        String status = detailStatusSpinner.getSelectedItem().toString();

        if (title.isEmpty() || description.isEmpty()) {
            SoundHelper.playError(NoteDetailActivity.this);
            Toast.makeText(this, "All input fields are mandatory.", Toast.LENGTH_SHORT).show();
            return;
        }

        final ProgressDialog dialog = ProgressDialog.show(this, "Synchronizing Note", "Writing securely to Cloud Server...", true);

        final Note note = new Note(noteId, title, description, date, status, type, selectedImageUrl);

        if (noteId == null) {
            backendApi.createNote(userToken, note.toDataJson(), new BackendApi.ApiCallback<JSONObject>() {
                @Override
                public void onSuccess(JSONObject result) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            SoundHelper.playSuccess(NoteDetailActivity.this);
                            Toast.makeText(NoteDetailActivity.this, "Successfully added cloud note!", Toast.LENGTH_SHORT).show();
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
                            SoundHelper.playError(NoteDetailActivity.this);
                            Toast.makeText(NoteDetailActivity.this, "Save Failed: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        } else {
            backendApi.updateNote(userToken, noteId, note.toDataJson(), new BackendApi.ApiCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            SoundHelper.playSuccess(NoteDetailActivity.this);
                            Toast.makeText(NoteDetailActivity.this, "Note details updated successfully!", Toast.LENGTH_SHORT).show();
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
                            SoundHelper.playError(NoteDetailActivity.this);
                            Toast.makeText(NoteDetailActivity.this, "Update Failed: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        }
    }

    private void deleteNote() {
        if (noteId == null) return;

        final ProgressDialog dialog = ProgressDialog.show(this, "Deleting", "Pruning record from servers...", true);

        backendApi.deleteNote(userToken, noteId, new BackendApi.ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        SoundHelper.playSuccess(NoteDetailActivity.this);
                        Toast.makeText(NoteDetailActivity.this, "Note purged successfully", Toast.LENGTH_SHORT).show();
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
                        SoundHelper.playError(NoteDetailActivity.this);
                        Toast.makeText(NoteDetailActivity.this, "Delete operation failed: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}