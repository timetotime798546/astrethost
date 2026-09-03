package com.categorizednotes.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;
    private ListView notesListView;
    private EditText searchBar;
    private Spinner categoryFilterSpinner;
    private TextView emptyStateView;
    private Button btnAddNote;
    private Button btnManageCategories;

    private List<Note> displayedNotesList;
    private NotesCustomAdapter listAdapter;
    private List<String> spinnerCategories;
    private ArrayAdapter<String> spinnerAdapter;

    private String currentSelectedCategory = "All Categories";
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        // UI Mapping
        notesListView = (ListView) findViewById(R.id.notes_list_view);
        searchBar = (EditText) findViewById(R.id.search_bar);
        categoryFilterSpinner = (Spinner) findViewById(R.id.category_filter_spinner);
        emptyStateView = (TextView) findViewById(R.id.empty_state_view);
        btnAddNote = (Button) findViewById(R.id.btn_add_note);
        btnManageCategories = (Button) findViewById(R.id.btn_manage_categories);

        displayedNotesList = new ArrayList<>();
        spinnerCategories = new ArrayList<>();

        setupCategorySpinner();
        setupNotesListView();

        // Search bar Text Change Listener (Java 8 compatibility - anonymous class)
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                refreshNotesDisplay();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Add Note Action Trigger
        btnAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NoteEditorActivity.class);
                startActivity(intent);
            }
        });

        // Manage/Add Categories Dialog Action
        btnManageCategories.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddCategoryDialog();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCategoryList();
        refreshNotesDisplay();
    }

    private void setupCategorySpinner() {
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerCategories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categoryFilterSpinner.setAdapter(spinnerAdapter);

        categoryFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentSelectedCategory = spinnerCategories.get(position);
                refreshNotesDisplay();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupNotesListView() {
        listAdapter = new NotesCustomAdapter(this, displayedNotesList);
        notesListView.setAdapter(listAdapter);

        // Opening detailed View/Editing interface on Click
        notesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Note selectedNote = displayedNotesList.get(position);
                Intent editIntent = new Intent(MainActivity.this, NoteEditorActivity.class);
                editIntent.putExtra("NOTE_ID", selectedNote.getId());
                startActivity(editIntent);
            }
        });
    }

    private void refreshCategoryList() {
        List<Category> realCategories = dbHelper.getAllCategories();
        spinnerCategories.clear();
        spinnerCategories.add("All Categories");
        for (int i = 0; i < realCategories.size(); i++) {
            spinnerCategories.add(realCategories.get(i).getName());
        }
        spinnerAdapter.notifyDataSetChanged();
    }

    private void refreshNotesDisplay() {
        displayedNotesList.clear();
        List<Note> fetched = dbHelper.searchAndFilterNotes(currentSearchQuery, currentSelectedCategory);
        displayedNotesList.addAll(fetched);
        listAdapter.notifyDataSetChanged();

        if (displayedNotesList.isEmpty()) {
            emptyStateView.setVisibility(View.VISIBLE);
            notesListView.setVisibility(View.GONE);
        } else {
            emptyStateView.setVisibility(View.GONE);
            notesListView.setVisibility(View.VISIBLE);
        }
    }

    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New Category");

        final EditText categoryInput = new EditText(this);
        categoryInput.setHint("Category name");
        categoryInput.setSingleLine(true);
        builder.setView(categoryInput);

        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String val = categoryInput.getText().toString().trim();
                if (!val.isEmpty()) {
                    boolean success = dbHelper.insertCategory(val);
                    if (success) {
                        Toast.makeText(MainActivity.this, "Category Added!", Toast.LENGTH_SHORT).show();
                        refreshCategoryList();
                    } else {
                        Toast.makeText(MainActivity.this, "Category already exists or invalid", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Name cannot be empty!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    // Inner Adapter Class designed for list styling custom layouts without external recyclerview dependencies
    private static class NotesCustomAdapter extends ArrayAdapter<Note> {
        private final Context context;
        private final List<Note> notes;

        public NotesCustomAdapter(Context context, List<Note> notes) {
            super(context, R.layout.note_list_item, notes);
            this.context = context;
            this.notes = notes;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.note_list_item, parent, false);
            }

            Note current = notes.get(position);

            TextView itemTitle = (TextView) convertView.findViewById(R.id.item_title);
            TextView itemCategory = (TextView) convertView.findViewById(R.id.item_category_tag);
            TextView itemPreview = (TextView) convertView.findViewById(R.id.item_content_preview);
            TextView itemDate = (TextView) convertView.findViewById(R.id.item_date);

            itemTitle.setText(current.getTitle());
            itemCategory.setText(current.getCategory());
            itemPreview.setText(current.getContent());
            itemDate.setText(current.getUpdatedAt());

            return convertView;
        }
    }
}