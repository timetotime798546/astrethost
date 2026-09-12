package com.notecraft.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private NotesDbHelper dbHelper;
    private List<Note> allNotesList = new ArrayList<>();
    private List<Note> filteredNotesList = new ArrayList<>();
    
    private ListView listView;
    private NotesAdapter adapter;
    private EditText searchBar;
    private Spinner categoryFilterSpinner;
    private TextView emptyText;
    private TextView btnAddNote;

    private static final String[] FILTER_CATEGORIES = {"All", "Personal", "Work", "Ideas", "Tasks", "Other"};
    private static final String[] FORM_CATEGORIES = {"Personal", "Work", "Ideas", "Tasks", "Other"};

    private String currentSearchQuery = "";
    private String currentCategoryFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new NotesDbHelper(this);

        listView = (ListView) findViewById(R.id.notes_list_view);
        searchBar = (EditText) findViewById(R.id.search_bar);
        categoryFilterSpinner = (Spinner) findViewById(R.id.category_filter);
        emptyText = (TextView) findViewById(R.id.empty_text);
        btnAddNote = (TextView) findViewById(R.id.btn_add_note);

        setupCategoryFilterSpinner();
        setupNotesList();
        setupSearchBar();

        btnAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNoteDialog(null);
            }
        });

        loadNotesFromDatabase();
    }

    private void setupCategoryFilterSpinner() {
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                FILTER_CATEGORIES
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categoryFilterSpinner.setAdapter(spinnerAdapter);

        categoryFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentCategoryFilter = FILTER_CATEGORIES[position];
                applyFilter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void setupNotesList() {
        adapter = new NotesAdapter(this, filteredNotesList, new NotesAdapter.OnNoteInteractionListener() {
            @Override
            public void onNoteClick(Note note) {
                showNoteDialog(note);
            }

            @Override
            public void onNoteDelete(Note note) {
                confirmDeleteNote(note);
            }
        });
        listView.setAdapter(adapter);
    }

    private void setupSearchBar() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim().toLowerCase();
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Do nothing
            }
        });
    }

    private void loadNotesFromDatabase() {
        allNotesList.clear();
        allNotesList.addAll(dbHelper.getAllNotes());
        applyFilter();
    }

    private void applyFilter() {
        filteredNotesList.clear();
        for (int i = 0; i < allNotesList.size(); i++) {
            Note note = allNotesList.get(i);
            
            // Category filter check
            boolean matchesCategory = currentCategoryFilter.equals("All") 
                    || note.getCategory().equalsIgnoreCase(currentCategoryFilter);
            
            // Search criteria check
            boolean matchesSearch = currentSearchQuery.isEmpty()
                    || note.getTitle().toLowerCase().contains(currentSearchQuery)
                    || note.getContent().toLowerCase().contains(currentSearchQuery)
                    || note.getCategory().toLowerCase().contains(currentSearchQuery);

            if (matchesCategory && matchesSearch) {
                filteredNotesList.add(note);
            }
        }
        
        adapter.notifyDataSetChanged();

        if (filteredNotesList.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
        } else {
            emptyText.setVisibility(View.GONE);
        }
    }

    private void confirmDeleteNote(final Note note) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Note");
        builder.setMessage("Are you sure you want to delete this note?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dbHelper.deleteNote(note.getId());
                Toast.makeText(MainActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                loadNotesFromDatabase();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showNoteDialog(final Note existingNote) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.note_dialog, null);
        builder.setView(dialogView);

        final TextView dialogTitle = (TextView) dialogView.findViewById(R.id.dialog_title);
        final EditText inputTitle = (EditText) dialogView.findViewById(R.id.dialog_note_title);
        final Spinner spinnerCategory = (Spinner) dialogView.findViewById(R.id.dialog_category_spinner);
        final EditText inputContent = (EditText) dialogView.findViewById(R.id.dialog_note_content);
        Button btnCancel = (Button) dialogView.findViewById(R.id.dialog_btn_cancel);
        Button btnSave = (Button) dialogView.findViewById(R.id.dialog_btn_save);

        // Setup dialog form categories spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                FORM_CATEGORIES
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(spinnerAdapter);

        final AlertDialog dialog = builder.create();

        if (existingNote != null) {
            dialogTitle.setText("Edit Note");
            inputTitle.setText(existingNote.getTitle());
            inputContent.setText(existingNote.getContent());
            
            // Selection mapping
            for (int i = 0; i < FORM_CATEGORIES.length; i++) {
                if (FORM_CATEGORIES[i].equalsIgnoreCase(existingNote.getCategory())) {
                    spinnerCategory.setSelection(i);
                    break;
                }
            }
        } else {
            dialogTitle.setText("Create Note");
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
                String title = inputTitle.getText().toString().trim();
                String content = inputContent.getText().toString().trim();
                String category = spinnerCategory.getSelectedItem().toString();

                if (title.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter a title", Toast.LENGTH_SHORT).show();
                    return;
                }

                String timestamp = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(new Date());

                if (existingNote == null) {
                    dbHelper.insertNote(title, content, category, timestamp);
                    Toast.makeText(MainActivity.this, "Note saved", Toast.LENGTH_SHORT).show();
                } else {
                    dbHelper.updateNote(existingNote.getId(), title, content, category, timestamp);
                    Toast.makeText(MainActivity.this, "Note updated", Toast.LENGTH_SHORT).show();
                }

                dialog.dismiss();
                loadNotesFromDatabase();
            }
        });

        dialog.show();
    }

    // Custom base adapter to map the dynamic list views
    private static class NotesAdapter extends BaseAdapter {

        public interface OnNoteInteractionListener {
            void onNoteClick(Note note);
            void onNoteDelete(Note note);
        }

        private final Context context;
        private final List<Note> list;
        private final OnNoteInteractionListener listener;

        public NotesAdapter(Context context, List<Note> list, OnNoteInteractionListener listener) {
            this.context = context;
            this.list = list;
            this.listener = listener;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return list.get(position).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.note_item, parent, false);
            }

            final Note note = list.get(position);

            TextView txtTitle = (TextView) convertView.findViewById(R.id.note_title);
            TextView txtSnippet = (TextView) convertView.findViewById(R.id.note_snippet);
            TextView txtCategory = (TextView) convertView.findViewById(R.id.note_category);
            TextView txtDate = (TextView) convertView.findViewById(R.id.note_date);
            TextView btnDelete = (TextView) convertView.findViewById(R.id.btn_delete);

            txtTitle.setText(note.getTitle());
            txtSnippet.setText(note.getContent().isEmpty() ? "(No content)" : note.getContent());
            txtCategory.setText(note.getCategory());
            txtDate.setText(note.getTimestamp());

            // Add background color variation based on category
            String cat = note.getCategory().toLowerCase();
            int badgeBgColor;
            int badgeTextColor;
            if (cat.equals("work")) {
                badgeTextColor = 0xFFE65100;
                badgeBgColor = 0xFFFFE0B2;
            } else if (cat.equals("ideas")) {
                badgeTextColor = 0xFF4A148C;
                badgeBgColor = 0xFFE1BEE7;
            } else if (cat.equals("tasks")) {
                badgeTextColor = 0xFF1B5E20;
                badgeBgColor = 0xFFC8E6C9;
            } else if (cat.equals("personal")) {
                badgeTextColor = 0xFF0D47A1;
                badgeBgColor = 0xFFBBDEFB;
            } else {
                badgeTextColor = 0xFF37474F;
                badgeBgColor = 0xFFCFD8DC;
            }

            txtCategory.setTextColor(badgeTextColor);
            android.graphics.drawable.Drawable bg = txtCategory.getBackground();
            if (bg != null) {
                android.graphics.drawable.Drawable mutatedBg = bg.mutate();
                mutatedBg.setColorFilter(badgeBgColor, android.graphics.PorterDuff.Mode.SRC_IN);
                txtCategory.setBackground(mutatedBg);
            }

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onNoteClick(note);
                    }
                }
            });

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onNoteDelete(note);
                    }
                }
            });

            return convertView;
        }
    }
}