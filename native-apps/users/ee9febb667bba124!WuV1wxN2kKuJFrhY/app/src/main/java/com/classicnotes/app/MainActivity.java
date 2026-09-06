package com.classicnotes.app;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;
    private ListView notesListView;
    private NoteAdapter noteAdapter;
    private TextView emptyNotesView;
    private EditText searchEditText;
    private Spinner categoryFilterSpinner;
    private Button btnManageCategories;
    private ImageButton fabAddNote;

    private List<Category> filterCategories = new ArrayList<>();
    private List<Note> displayedNotes = new ArrayList<>();
    
    private int selectedFilterCategoryId = -1;
    private String currentSearchQuery = "";

    private static final int REQUEST_CODE_EDIT_NOTE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        notesListView = (ListView) findViewById(R.id.notes_list_view);
        emptyNotesView = (TextView) findViewById(R.id.empty_notes_view);
        searchEditText = (EditText) findViewById(R.id.search_edit_text);
        categoryFilterSpinner = (Spinner) findViewById(R.id.category_filter_spinner);
        btnManageCategories = (Button) findViewById(R.id.btn_manage_categories);
        fabAddNote = (ImageButton) findViewById(R.id.fab_add_note);

        noteAdapter = new NoteAdapter(this, displayedNotes);
        notesListView.setAdapter(noteAdapter);

        notesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                Note selectedNote = displayedNotes.get(position);
                Intent intent = new Intent(MainActivity.this, EditNoteActivity.class);
                intent.putExtra("note_id", selectedNote.getId());
                intent.putExtra("note_title", selectedNote.getTitle());
                intent.putExtra("note_content", selectedNote.getContent());
                intent.putExtra("note_category_id", selectedNote.getCategoryId());
                startActivityForResult(intent, REQUEST_CODE_EDIT_NOTE);
            }
        });

        fabAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, EditNoteActivity.class);
                startActivityForResult(intent, REQUEST_CODE_EDIT_NOTE);
            }
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                refreshNotesList();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        categoryFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView parent, View view, int position, long id) {
                Category selectedCat = filterCategories.get(position);
                selectedFilterCategoryId = selectedCat.getId();
                refreshNotesList();
            }

            @Override
            public void onNothingSelected(AdapterView parent) {}
        });

        btnManageCategories.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showManageCategoriesDialog();
            }
        });

        refreshCategorySpinner();
        refreshNotesList();
    }

    private void refreshCategorySpinner() {
        filterCategories.clear();
        filterCategories.add(new Category(-1, "All Categories"));
        filterCategories.add(new Category(-2, "Uncategorized"));
        
        List<Category> dbCategories = dbHelper.getAllCategories();
        filterCategories.addAll(dbCategories);

        List<String> categoryNames = new ArrayList<>();
        for (int i = 0; i < filterCategories.size(); i++) {
            categoryNames.add(filterCategories.get(i).getName());
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categoryNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categoryFilterSpinner.setAdapter(spinnerAdapter);

        int selectedIndex = 0;
        for (int i = 0; i < filterCategories.size(); i++) {
            if (filterCategories.get(i).getId() == selectedFilterCategoryId) {
                selectedIndex = i;
                break;
            }
        }
        categoryFilterSpinner.setSelection(selectedIndex);
    }

    private void refreshNotesList() {
        List<Note> notes = dbHelper.getFilteredNotes(currentSearchQuery, selectedFilterCategoryId);
        displayedNotes = notes;
        noteAdapter.updateData(displayedNotes);

        if (displayedNotes.isEmpty()) {
            emptyNotesView.setVisibility(View.VISIBLE);
        } else {
            emptyNotesView.setVisibility(View.GONE);
        }
    }

    private void showManageCategoriesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Manage Categories");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        LinearLayout addLayout = new LinearLayout(this);
        addLayout.setOrientation(LinearLayout.HORIZONTAL);

        final EditText newCatInput = new EditText(this);
        newCatInput.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        newCatInput.setHint("New category name");
        newCatInput.setSingleLine(true);
        addLayout.addView(newCatInput);

        Button btnAdd = new Button(this);
        btnAdd.setText("Add");
        btnAdd.setBackgroundResource(R.drawable.btn_primary_bg);
        btnAdd.setTextColor(0xFFFFFFFF);
        addLayout.addView(btnAdd);

        layout.addView(addLayout);

        View div = new View(this);
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2);
        divParams.setMargins(0, 16, 0, 16);
        div.setLayoutParams(divParams);
        div.setBackgroundColor(0xFFE0E0E0);
        layout.addView(div);

        final ListView dialogListView = new ListView(this);
        dialogListView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 300));
        layout.addView(dialogListView);

        builder.setView(layout);
        builder.setPositiveButton("Done", null);
        
        final AlertDialog dialog = builder.create();
        dialog.show();

        final List<Category> dialogCategories = new ArrayList<>();
        
        final BaseAdapter dialogAdapter = new BaseAdapter() {
            @Override
            public int getCount() { return dialogCategories.size(); }
            @Override
            public Object getItem(int position) { return dialogCategories.get(position); }
            @Override
            public long getItemId(int position) { return dialogCategories.get(position).getId(); }
            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(MainActivity.this).inflate(android.R.layout.simple_list_item_1, parent, false);
                }
                TextView text = (TextView) convertView.findViewById(android.R.id.text1);
                Category cat = dialogCategories.get(position);
                text.setText(cat.getName() + " (Tap to delete)");
                text.setTextColor(0xFFD32F2F);
                return convertView;
            }
        };

        dialogListView.setAdapter(dialogAdapter);

        final Runnable reloadDialogCategories = new Runnable() {
            @Override
            public void run() {
                dialogCategories.clear();
                dialogCategories.addAll(dbHelper.getAllCategories());
                dialogAdapter.notifyDataSetChanged();
            }
        };

        reloadDialogCategories.run();

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = newCatInput.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Category name cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                long insertedId = dbHelper.addCategory(name);
                if (insertedId == -1) {
                    Toast.makeText(MainActivity.this, "Category already exists", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Category added", Toast.LENGTH_SHORT).show();
                    newCatInput.setText("");
                    reloadDialogCategories.run();
                    refreshCategorySpinner();
                }
            }
        });

        dialogListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                final Category catToDelete = dialogCategories.get(position);
                
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete Category")
                        .setMessage("Are you sure you want to delete '" + catToDelete.getName() + "'? Notes in this category will become uncategorized.")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {
                                dbHelper.deleteCategory(catToDelete.getId());
                                Toast.makeText(MainActivity.this, "Category deleted", Toast.LENGTH_SHORT).show();
                                reloadDialogCategories.run();
                                refreshCategorySpinner();
                                refreshNotesList();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_EDIT_NOTE) {
            refreshNotesList();
        }
    }
}