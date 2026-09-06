package com.classicnotes.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EditNoteActivity extends Activity {

    private DatabaseHelper dbHelper;
    private EditText editTitle;
    private EditText editContent;
    private Spinner categorySpinner;
    private Button btnBack;
    private Button btnSave;
    private Button btnDelete;
    private LinearLayout deleteActionBar;
    private TextView editorTitleTextView;

    private int noteId = -1;
    private int currentNoteCategoryId = -1;
    private List<Category> categoriesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);

        dbHelper = new DatabaseHelper(this);

        editTitle = (EditText) findViewById(R.id.edit_note_title);
        editContent = (EditText) findViewById(R.id.edit_note_content);
        categorySpinner = (Spinner) findViewById(R.id.edit_note_category_spinner);
        btnBack = (Button) findViewById(R.id.btn_back);
        btnSave = (Button) findViewById(R.id.btn_save);
        btnDelete = (Button) findViewById(R.id.btn_delete_note);
        deleteActionBar = (LinearLayout) findViewById(R.id.delete_action_bar);
        editorTitleTextView = (TextView) findViewById(R.id.editor_title);

        Intent intent = getIntent();
        if (intent.hasExtra("note_id")) {
            noteId = intent.getIntExtra("note_id", -1);
            String title = intent.getStringExtra("note_title");
            String content = intent.getStringExtra("note_content");
            currentNoteCategoryId = intent.getIntExtra("note_category_id", -1);

            editTitle.setText(title);
            editContent.setText(content);
            editorTitleTextView.setText("Edit Note");
            deleteActionBar.setVisibility(View.VISIBLE);
        } else {
            editorTitleTextView.setText("Add Note");
            deleteActionBar.setVisibility(View.GONE);
        }

        setupCategorySpinner();

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteNote();
            }
        });
    }

    private void setupCategorySpinner() {
        categoriesList.clear();
        categoriesList.add(new Category(-1, "Uncategorized"));
        categoriesList.addAll(dbHelper.getAllCategories());

        List<String> categoryNames = new ArrayList<>();
        for (int i = 0; i < categoriesList.size(); i++) {
            categoryNames.add(categoriesList.get(i).getName());
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categoryNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(spinnerAdapter);

        int selectIndex = 0;
        for (int i = 0; i < categoriesList.size(); i++) {
            if (categoriesList.get(i).getId() == currentNoteCategoryId) {
                selectIndex = i;
                break;
            }
        }
        categorySpinner.setSelection(selectIndex);
    }

    private void saveNote() {
        String title = editTitle.getText().toString().trim();
        String content = editContent.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a note title", Toast.LENGTH_SHORT).show();
            return;
        }

        int spinnerPosition = categorySpinner.getSelectedItemPosition();
        int categoryId = categoriesList.get(spinnerPosition).getId();

        String currentDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date());

        if (noteId == -1) {
            long id = dbHelper.addNote(title, content, categoryId, currentDate);
            if (id != -1) {
                Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Error saving note", Toast.LENGTH_SHORT).show();
            }
        } else {
            int rowsUpdated = dbHelper.updateNote(noteId, title, content, categoryId, currentDate);
            if (rowsUpdated > 0) {
                Toast.makeText(this, "Note updated", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Error updating note", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void deleteNote() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dbHelper.deleteNote(noteId);
                        Toast.makeText(EditNoteActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}