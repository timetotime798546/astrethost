package com.quicknotes.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;
    private ListView listViewNotes;
    private TextView emptyTextView;
    private EditText etSearch;
    private Spinner spFilterCategory;
    private Button btnAddNewNote;

    private NoteAdapter noteAdapter;
    private List<Note> noteList;

    private String currentSearchQuery = "";
    private String currentCategoryFilter = "All";

    private final String[] categories = {"Personal", "Work", "Ideas", "Todo", "Others"};
    private final String[] filterCategories = {"All", "Personal", "Work", "Ideas", "Todo", "Others"};

    @Override
    protected void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        noteList = new ArrayList<>();

        listViewNotes = (ListView) findViewById(R.id.listViewNotes);
        emptyTextView = (TextView) findViewById(R.id.emptyTextView);
        etSearch = (EditText) findViewById(R.id.etSearch);
        spFilterCategory = (Spinner) findViewById(R.id.spFilterCategory);
        btnAddNewNote = (Button) findViewById(R.id.btnAddNewNote);

        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filterCategories);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFilterCategory.setAdapter(filterAdapter);

        refreshNoteList();

        btnAddNewNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNoteDialog(null);
            }
        });

        listViewNotes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Note selectedNote = noteList.get(position);
                showNoteDialog(selectedNote);
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                refreshNoteList();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        spFilterCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentCategoryFilter = filterCategories[position];
                refreshNoteList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void refreshNoteList() {
        noteList.clear();
        noteList.addAll(dbHelper.getAllNotes(currentSearchQuery, currentCategoryFilter));

        if (noteAdapter == null) {
            noteAdapter = new NoteAdapter(this, noteList);
            listViewNotes.setAdapter(noteAdapter);
        } else {
            noteAdapter.notifyDataSetChanged();
        }

        if (noteList.isEmpty()) {
            emptyTextView.setVisibility(View.VISIBLE);
        } else {
            emptyTextView.setVisibility(View.GONE);
        }
    }

    private void showNoteDialog(final Note noteToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_note, null);
        builder.setView(dialogView);

        final TextView tvDialogTitle = (TextView) dialogView.findViewById(R.id.tvDialogTitle);
        final EditText etNoteTitle = (EditText) dialogView.findViewById(R.id.etNoteTitle);
        final Spinner spNoteCategory = (Spinner) dialogView.findViewById(R.id.spNoteCategory);
        final EditText etNoteContent = (EditText) dialogView.findViewById(R.id.etNoteContent);
        final Button btnDelete = (Button) dialogView.findViewById(R.id.btnDeleteNote);
        final Button btnCancel = (Button) dialogView.findViewById(R.id.btnCancelNote);
        final Button btnSave = (Button) dialogView.findViewById(R.id.btnSaveNote);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spNoteCategory.setAdapter(categoryAdapter);

        final AlertDialog dialog = builder.create();
        final boolean isEditMode = (noteToEdit != null);

        if (isEditMode) {
            tvDialogTitle.setText("Edit Note");
            etNoteTitle.setText(noteToEdit.getTitle());
            etNoteContent.setText(noteToEdit.getContent());
            btnDelete.setVisibility(View.VISIBLE);

            for (int i = 0; i < categories.length; i++) {
                if (categories[i].equals(noteToEdit.getCategory())) {
                    spNoteCategory.setSelection(i);
                    break;
                }
            }
        } else {
            tvDialogTitle.setText("Create Note");
            btnDelete.setVisibility(View.GONE);
        }

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = etNoteTitle.getText().toString().trim();
                String content = etNoteContent.getText().toString().trim();
                String category = spNoteCategory.getSelectedItem().toString();

                if (title.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter a title", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (isEditMode) {
                    dbHelper.updateNote(noteToEdit.getId(), title, content, category);
                    Toast.makeText(MainActivity.this, "Note updated", Toast.LENGTH_SHORT).show();
                } else {
                    dbHelper.insertNote(title, content, category);
                    Toast.makeText(MainActivity.this, "Note saved", Toast.LENGTH_SHORT).show();
                }

                refreshNoteList();
                dialog.dismiss();
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete Note")
                        .setMessage("Are you sure you want to delete this note?")
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {
                                dbHelper.deleteNote(noteToEdit.getId());
                                Toast.makeText(MainActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                                refreshNoteList();
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        dialog.show();
    }

    private class NoteAdapter extends ArrayAdapter<Note> {
        private Context context;
        private List<Note> notes;

        public NoteAdapter(Context context, List<Note> notes) {
            super(context, R.layout.note_list_item, notes);
            this.context = context;
            this.notes = notes;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.note_list_item, parent, false);
            }

            Note note = notes.get(position);

            TextView tvTitle = (TextView) convertView.findViewById(R.id.tvNoteTitle);
            TextView tvContent = (TextView) convertView.findViewById(R.id.tvNoteContentSnippet);
            TextView tvCategory = (TextView) convertView.findViewById(R.id.tvNoteCategory);
            TextView tvDate = (TextView) convertView.findViewById(R.id.tvNoteDate);

            tvTitle.setText(note.getTitle());
            tvContent.setText(note.getContent());
            tvCategory.setText(note.getCategory());

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            tvDate.setText(sdf.format(new Date(note.getTimestamp())));

            int badgeColor;
            int textColor;
            String category = note.getCategory();
            if ("Personal".equals(category)) {
                badgeColor = Color.parseColor("#E0F2FE");
                textColor = Color.parseColor("#0369A1");
            } else if ("Work".equals(category)) {
                badgeColor = Color.parseColor("#FEF3C7");
                textColor = Color.parseColor("#B45309");
            } else if ("Ideas".equals(category)) {
                badgeColor = Color.parseColor("#F3E8FF");
                textColor = Color.parseColor("#6B21A8");
            } else if ("Todo".equals(category)) {
                badgeColor = Color.parseColor("#D1FAE5");
                textColor = Color.parseColor("#065F46");
            } else {
                badgeColor = Color.parseColor("#F1F5F9");
                textColor = Color.parseColor("#475569");
            }

            android.graphics.drawable.Drawable background = tvCategory.getBackground();
            if (background != null) {
                android.graphics.drawable.Drawable mutate = background.mutate();
                if (mutate instanceof android.graphics.drawable.GradientDrawable) {
                    ((android.graphics.drawable.GradientDrawable) mutate).setColor(badgeColor);
                }
            }
            tvCategory.setTextColor(textColor);

            return convertView;
        }
    }
}