package com.quicknotes.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;
    private ListView notesListView;
    private NoteAdapter noteAdapter;
    private List<Note> noteList;

    private TextView statTotal, statPersonal, statWork, statIdea;
    private EditText searchBar;
    private Button btnFilterAll, btnFilterPersonal, btnFilterWork, btnFilterIdea;
    private Button btnAddNoteHeader;
    private LinearLayout emptyStateView;

    // Current filter states
    private String selectedCategoryFilter = "All";
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        noteList = new ArrayList<>();

        // Initialize UI Elements
        notesListView = (ListView) findViewById(R.id.notesListView);
        emptyStateView = (LinearLayout) findViewById(R.id.emptyStateView);

        statTotal = (TextView) findViewById(R.id.statTotal);
        statPersonal = (TextView) findViewById(R.id.statPersonal);
        statWork = (TextView) findViewById(R.id.statWork);
        statIdea = (TextView) findViewById(R.id.statIdea);

        searchBar = (EditText) findViewById(R.id.searchBar);

        btnFilterAll = (Button) findViewById(R.id.btnFilterAll);
        btnFilterPersonal = (Button) findViewById(R.id.btnFilterPersonal);
        btnFilterWork = (Button) findViewById(R.id.btnFilterWork);
        btnFilterIdea = (Button) findViewById(R.id.btnFilterIdea);

        btnAddNoteHeader = (Button) findViewById(R.id.btnAddNoteHeader);

        // Setup Adapter
        noteAdapter = new NoteAdapter(this, noteList, new NoteAdapter.OnNoteActionListener() {
            @Override
            public void onEdit(Note note) {
                showNoteDialog(note);
            }

            @Override
            public void onDelete(final Note note) {
                showDeleteConfirmation(note);
            }

            @Override
            public void onShare(Note note) {
                shareNote(note);
            }
        });
        notesListView.setAdapter(noteAdapter);

        // Load Initial Data & Statistics
        refreshNotesList();

        // Add Note Action Trigger
        btnAddNoteHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNoteDialog(null);
            }
        });

        // Filter Action Click Handlers
        btnFilterAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCategoryFilter("All");
            }
        });
        btnFilterPersonal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCategoryFilter("Personal");
            }
        });
        btnFilterWork.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCategoryFilter("Work");
            }
        });
        btnFilterIdea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCategoryFilter("Idea");
            }
        });

        // Search text change handler
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim();
                refreshNotesList();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setCategoryFilter(String category) {
        selectedCategoryFilter = category;

        // Reset backgrounds of filter selections
        btnFilterAll.setBackgroundResource(R.drawable.button_secondary_bg);
        btnFilterAll.setTextColor(getResources().getColor(R.color.text_dark));
        btnFilterPersonal.setBackgroundResource(R.drawable.button_secondary_bg);
        btnFilterPersonal.setTextColor(getResources().getColor(R.color.text_dark));
        btnFilterWork.setBackgroundResource(R.drawable.button_secondary_bg);
        btnFilterWork.setTextColor(getResources().getColor(R.color.text_dark));
        btnFilterIdea.setBackgroundResource(R.drawable.button_secondary_bg);
        btnFilterIdea.setTextColor(getResources().getColor(R.color.text_dark));

        // Mark active choice
        if ("All".equals(category)) {
            btnFilterAll.setBackgroundResource(R.drawable.button_bg);
            btnFilterAll.setTextColor(getResources().getColor(R.color.white));
        } else if ("Personal".equals(category)) {
            btnFilterPersonal.setBackgroundResource(R.drawable.button_bg);
            btnFilterPersonal.setTextColor(getResources().getColor(R.color.white));
        } else if ("Work".equals(category)) {
            btnFilterWork.setBackgroundResource(R.drawable.button_bg);
            btnFilterWork.setTextColor(getResources().getColor(R.color.white));
        } else if ("Idea".equals(category)) {
            btnFilterIdea.setBackgroundResource(R.drawable.button_bg);
            btnFilterIdea.setTextColor(getResources().getColor(R.color.white));
        }

        refreshNotesList();
    }

    private void refreshNotesList() {
        noteList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String selection = null;
        List<String> selectionArgs = new ArrayList<>();

        if (!"All".equals(selectedCategoryFilter)) {
            selection = DatabaseHelper.COLUMN_CATEGORY + " = ?";
            selectionArgs.add(selectedCategoryFilter);
        }

        if (!TextUtils.isEmpty(currentSearchQuery)) {
            if (selection == null) {
                selection = "(" + DatabaseHelper.COLUMN_TITLE + " LIKE ? OR " + DatabaseHelper.COLUMN_CONTENT + " LIKE ?)";
            } else {
                selection += " AND (" + DatabaseHelper.COLUMN_TITLE + " LIKE ? OR " + DatabaseHelper.COLUMN_CONTENT + " LIKE ?)";
            }
            selectionArgs.add("%" + currentSearchQuery + "%");
            selectionArgs.add("%" + currentSearchQuery + "%");
        }

        String[] selectionArgsArray = selectionArgs.toArray(new String[0]);

        Cursor cursor = db.query(
                DatabaseHelper.TABLE_NAME,
                null,
                selection,
                selectionArgsArray,
                null,
                null,
                DatabaseHelper.COLUMN_ID + " DESC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TITLE));
                String content = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CONTENT));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CATEGORY));
                String timestamp = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TIMESTAMP));

                noteList.add(new Note(id, title, content, category, timestamp));
            }
            cursor.close();
        }

        noteAdapter.notifyDataSetChanged();

        if (noteList.isEmpty()) {
            emptyStateView.setVisibility(View.VISIBLE);
        } else {
            emptyStateView.setVisibility(View.GONE);
        }

        updateStatistics();
    }

    private void updateStatistics() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        int total = 0, personal = 0, work = 0, idea = 0;

        Cursor totalCursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_NAME, null);
        if (totalCursor != null && totalCursor.moveToFirst()) {
            total = totalCursor.getInt(0);
            totalCursor.close();
        }

        Cursor personalCursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_NAME + " WHERE " + DatabaseHelper.COLUMN_CATEGORY + " = 'Personal'", null);
        if (personalCursor != null && personalCursor.moveToFirst()) {
            personal = personalCursor.getInt(0);
            personalCursor.close();
        }

        Cursor workCursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_NAME + " WHERE " + DatabaseHelper.COLUMN_CATEGORY + " = 'Work'", null);
        if (workCursor != null && workCursor.moveToFirst()) {
            work = workCursor.getInt(0);
            workCursor.close();
        }

        Cursor ideaCursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_NAME + " WHERE " + DatabaseHelper.COLUMN_CATEGORY + " = 'Idea'", null);
        if (ideaCursor != null && ideaCursor.moveToFirst()) {
            idea = ideaCursor.getInt(0);
            ideaCursor.close();
        }

        statTotal.setText("Total: " + total);
        statPersonal.setText("Personal: " + personal);
        statWork.setText("Work: " + work);
        statIdea.setText("Ideas: " + idea);
    }

    private void showNoteDialog(final Note existingNote) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_note, null);
        dialogBuilder.setView(dialogView);

        final AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.setCanceledOnTouchOutside(true);

        final TextView dialogTitle = (TextView) dialogView.findViewById(R.id.dialogTitle);
        final EditText dialogNoteTitle = (EditText) dialogView.findViewById(R.id.dialogNoteTitle);
        final EditText dialogNoteContent = (EditText) dialogView.findViewById(R.id.dialogNoteContent);
        final RadioGroup dialogTagGroup = (RadioGroup) dialogView.findViewById(R.id.dialogTagGroup);
        final RadioButton radioPersonal = (RadioButton) dialogView.findViewById(R.id.radioPersonal);
        final RadioButton radioWork = (RadioButton) dialogView.findViewById(R.id.radioWork);
        final RadioButton radioIdea = (RadioButton) dialogView.findViewById(R.id.radioIdea);
        Button dialogBtnCancel = (Button) dialogView.findViewById(R.id.dialogBtnCancel);
        Button dialogBtnSave = (Button) dialogView.findViewById(R.id.dialogBtnSave);

        if (existingNote != null) {
            dialogTitle.setText("Edit Note");
            dialogNoteTitle.setText(existingNote.getTitle());
            dialogNoteContent.setText(existingNote.getContent());

            if ("Personal".equalsIgnoreCase(existingNote.getCategory())) {
                radioPersonal.setChecked(true);
            } else if ("Work".equalsIgnoreCase(existingNote.getCategory())) {
                radioWork.setChecked(true);
            } else if ("Idea".equalsIgnoreCase(existingNote.getCategory())) {
                radioIdea.setChecked(true);
            }
        } else {
            dialogTitle.setText("Create Note");
        }

        dialogBtnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        dialogBtnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = dialogNoteTitle.getText().toString().trim();
                String content = dialogNoteContent.getText().toString().trim();

                if (TextUtils.isEmpty(title)) {
                    Toast.makeText(MainActivity.this, "Title cannot be empty!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(content)) {
                    Toast.makeText(MainActivity.this, "Note content cannot be empty!", Toast.LENGTH_SHORT).show();
                    return;
                }

                String category = "Personal";
                int checkedRadioId = dialogTagGroup.getCheckedRadioButtonId();
                if (checkedRadioId == R.id.radioWork) {
                    category = "Work";
                } else if (checkedRadioId == R.id.radioIdea) {
                    category = "Idea";
                }

                String dateStr = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(new Date());

                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.COLUMN_TITLE, title);
                values.put(DatabaseHelper.COLUMN_CONTENT, content);
                values.put(DatabaseHelper.COLUMN_CATEGORY, category);
                values.put(DatabaseHelper.COLUMN_TIMESTAMP, dateStr);

                if (existingNote == null) {
                    db.insert(DatabaseHelper.TABLE_NAME, null, values);
                    Toast.makeText(MainActivity.this, "Note added!", Toast.LENGTH_SHORT).show();
                } else {
                    db.update(DatabaseHelper.TABLE_NAME, values, DatabaseHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(existingNote.getId())});
                    Toast.makeText(MainActivity.this, "Note updated!", Toast.LENGTH_SHORT).show();
                }

                alertDialog.dismiss();
                refreshNotesList();
            }
        });

        alertDialog.show();
    }

    private void showDeleteConfirmation(final Note note) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Note");
        builder.setMessage("Are you sure you want to permanently delete \"" + note.getTitle() + "\"?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.delete(DatabaseHelper.TABLE_NAME, DatabaseHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(note.getId())});
                Toast.makeText(MainActivity.this, "Note deleted!", Toast.LENGTH_SHORT).show();
                refreshNotesList();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void shareNote(Note note) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String shareText = "[" + note.getCategory() + "] " + note.getTitle() + "\n\n" + note.getContent() + "\n\nSaved on: " + note.getTimestamp();
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, note.getTitle());
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Share note via:"));
    }
}