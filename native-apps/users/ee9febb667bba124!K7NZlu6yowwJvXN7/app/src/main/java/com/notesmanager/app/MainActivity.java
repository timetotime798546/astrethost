package com.notesmanager.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;
    private ListView listViewNotes;
    private TextView tvEmpty;
    private EditText etSearch;
    private Spinner spinnerFilter;
    private Button btnAddNote;

    private List<Note> currentNotesList = new ArrayList<>();
    private NoteAdapter noteAdapter;

    private final String[] filterCategories = {"All", "Personal", "Work", "Ideas", "Other"};
    private final String[] formCategories = {"Personal", "Work", "Ideas", "Other"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        listViewNotes = (ListView) findViewById(R.id.list_view_notes);
        tvEmpty = (TextView) findViewById(R.id.tv_empty);
        etSearch = (EditText) findViewById(R.id.et_search);
        spinnerFilter = (Spinner) findViewById(R.id.spinner_category_filter);
        btnAddNote = (Button) findViewById(R.id.btn_add_note);

        setupFilters();
        setupSearch();
        setupListView();

        btnAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNoteDialog(null);
            }
        });

        loadNotes();
    }

    private void setupFilters() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filterCategories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(adapter);

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadNotes();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadNotes();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupListView() {
        noteAdapter = new NoteAdapter(this, currentNotesList);
        listViewNotes.setAdapter(noteAdapter);

        listViewNotes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Note selectedNote = currentNotesList.get(position);
                showNoteDialog(selectedNote);
            }
        });
    }

    private void loadNotes() {
        String searchQuery = etSearch.getText().toString().trim();
        String selectedCategory = spinnerFilter.getSelectedItem().toString();

        currentNotesList.clear();
        currentNotesList.addAll(dbHelper.searchNotes(searchQuery, selectedCategory));
        noteAdapter.notifyDataSetChanged();

        if (currentNotesList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void showNoteDialog(final Note noteToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_note, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        TextView tvDialogTitle = (TextView) dialogView.findViewById(R.id.dialog_title);
        final EditText etNoteTitle = (EditText) dialogView.findViewById(R.id.et_note_title);
        final Spinner spinnerNoteCategory = (Spinner) dialogView.findViewById(R.id.spinner_note_category);
        final EditText etNoteContent = (EditText) dialogView.findViewById(R.id.et_note_content);

        Button btnDelete = (Button) dialogView.findViewById(R.id.btn_dialog_delete);
        Button btnCancel = (Button) dialogView.findViewById(R.id.btn_dialog_cancel);
        Button btnSave = (Button) dialogView.findViewById(R.id.btn_dialog_save);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, formCategories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerNoteCategory.setAdapter(categoryAdapter);

        if (noteToEdit != null) {
            tvDialogTitle.setText("Edit Note");
            etNoteTitle.setText(noteToEdit.getTitle());
            etNoteContent.setText(noteToEdit.getContent());
            btnDelete.setVisibility(View.VISIBLE);

            for (int i = 0; i < formCategories.length; i++) {
                if (formCategories[i].equalsIgnoreCase(noteToEdit.getCategory())) {
                    spinnerNoteCategory.setSelection(i);
                    break;
                } 
            }
        } else {
            tvDialogTitle.setText("Add New Note");
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
                String category = spinnerNoteCategory.getSelectedItem().toString();

                if (title.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter a title", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (noteToEdit == null) {
                    dbHelper.insertNote(title, content, category);
                    Toast.makeText(MainActivity.this, "Note saved", Toast.LENGTH_SHORT).show();
                } else {
                    dbHelper.updateNote(noteToEdit.getId(), title, content, category);
                    Toast.makeText(MainActivity.this, "Note updated", Toast.LENGTH_SHORT).show();
                }

                loadNotes();
                dialog.dismiss();
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete Note")
                        .setMessage("Are you sure you want to delete this note?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {
                                dbHelper.deleteNote(noteToEdit.getId());
                                Toast.makeText(MainActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                                loadNotes();
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });

        dialog.show();
    }

    private class NoteAdapter extends BaseAdapter {
        private Context context;
        private List<Note> notes;

        public NoteAdapter(Context context, List<Note> notes) {
            this.context = context;
            this.notes = notes;
        }

        @Override
        public int getCount() {
            return notes.size();
        }

        @Override
        public Object getItem(int position) {
            return notes.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.note_item, parent, false);
            }

            TextView tvTitle = (TextView) convertView.findViewById(R.id.tv_title);
            TextView tvCategory = (TextView) convertView.findViewById(R.id.tv_category);
            TextView tvContent = (TextView) convertView.findViewById(R.id.tv_content);
            TextView tvDate = (TextView) convertView.findViewById(R.id.tv_date);

            Note note = notes.get(position);

            tvTitle.setText(note.getTitle());
            tvContent.setText(note.getContent());
            tvDate.setText(note.getTimestamp());

            String category = note.getCategory();
            tvCategory.setText(category.toUpperCase());

            GradientDrawable background = (GradientDrawable) tvCategory.getBackground();
            if (background != null) {
                int color;
                if ("Personal".equalsIgnoreCase(category)) {
                    color = Color.parseColor("#2196F3");
                } else if ("Work".equalsIgnoreCase(category)) {
                    color = Color.parseColor("#FF9800");
                } else if ("Ideas".equalsIgnoreCase(category)) {
                    color = Color.parseColor("#9C27B0");
                } else {
                    color = Color.parseColor("#009688");
                }
                background.setColor(color);
            }

            return convertView;
        }
    }
}