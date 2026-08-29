package com.smartnotes.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private NotesDatabaseHelper dbHelper;
    private NoteAdapter adapter;
    private List<Note> notesList;
    
    private EditText editSearch;
    private ListView listNotes;
    private TextView textNoNotes;
    private Button btnAddNote;
    private LinearLayout layoutCategories;
    
    private String currentCategoryFilter = "All";
    private String currentSearchQuery = "";
    
    private final String[] categories = {"General", "Work", "Personal", "Ideas", "Todos"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new NotesDatabaseHelper(this);
        notesList = new ArrayList<Note>();
        
        editSearch = (EditText) findViewById(R.id.edit_search);
        listNotes = (ListView) findViewById(R.id.list_notes);
        textNoNotes = (TextView) findViewById(R.id.text_no_notes);
        btnAddNote = (Button) findViewById(R.id.btn_add_note);
        layoutCategories = (LinearLayout) findViewById(R.id.layout_categories);

        setupCategoryTabs();
        setupListView();
        setupSearch();
        
        btnAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNoteDialog(null);
            }
        });

        refreshNotes();
    }

    private void setupCategoryTabs() {
        layoutCategories.removeAllViews();
        addCategoryTab("All");
        for (int i = 0; i < categories.length; i++) {
            addCategoryTab(categories[i]);
        }
    }

    private void addCategoryTab(final String categoryName) {
        final TextView tab = new TextView(this);
        tab.setText(categoryName);
        tab.setPadding(32, 16, 32, 16);
        tab.setTextSize(14);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 0, 8, 0);
        tab.setLayoutParams(params);

        updateTabStyle(tab, categoryName.equals(currentCategoryFilter));

        tab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentCategoryFilter = categoryName;
                for (int i = 0; i < layoutCategories.getChildCount(); i++) {
                    View child = layoutCategories.getChildAt(i);
                    if (child instanceof TextView) {
                        TextView childTab = (TextView) child;
                        updateTabStyle(childTab, childTab.getText().toString().equals(currentCategoryFilter));
                    }
                }
                refreshNotes();
            } 
        });

        layoutCategories.addView(tab);
    }

    private void updateTabStyle(TextView tab, boolean isSelected) {
        if (isSelected) {
            tab.setBackgroundResource(R.drawable.tab_selected);
            tab.setTextColor(0xFFFFFFFF);
        } else {
            tab.setBackgroundResource(R.drawable.tab_unselected);
            tab.setTextColor(0xFF555555);
        }
    }

    private void setupListView() {
        adapter = new NoteAdapter(this, notesList);
        listNotes.setAdapter(adapter);

        listNotes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Note selectedNote = notesList.get(position);
                showNoteOptionsDialog(selectedNote);
            }
        });
    }

    private void setupSearch() {
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                refreshNotes();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void refreshNotes() {
        notesList = dbHelper.getAllNotes(currentSearchQuery, currentCategoryFilter);
        adapter.setNotes(notesList);
        
        if (notesList.isEmpty()) {
            textNoNotes.setVisibility(View.VISIBLE);
            listNotes.setVisibility(View.GONE);
        } else {
            textNoNotes.setVisibility(View.GONE);
            listNotes.setVisibility(View.VISIBLE);
        }
    }

    private void showNoteOptionsDialog(final Note note) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(note.getTitle());
        String[] options = {"Edit Note", "Delete Note"};
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    showNoteDialog(note);
                } else if (which == 1) {
                    confirmDeleteNote(note);
                }
            }
        });
        builder.show();
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
                refreshNotes();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showNoteDialog(final Note note) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_note, null);
        builder.setView(dialogView);

        final EditText editTitle = (EditText) dialogView.findViewById(R.id.edit_dialog_title);
        final EditText editContent = (EditText) dialogView.findViewById(R.id.edit_dialog_content);
        final Spinner spinnerCategory = (Spinner) dialogView.findViewById(R.id.spinner_dialog_category);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(spinnerAdapter);

        final boolean isEdit = (note != null);
        if (isEdit) {
            builder.setTitle("Edit Note");
            editTitle.setText(note.getTitle());
            editContent.setText(note.getContent());
            
            for (int i = 0; i < categories.length; i++) {
                if (categories[i].equals(note.getCategory())) {
                    spinnerCategory.setSelection(i);
                    break;
                }
            }
        } else {
            builder.setTitle("New Note");
        }

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String title = editTitle.getText().toString().trim();
                String content = editContent.getText().toString().trim();
                String category = spinnerCategory.getSelectedItem().toString();

                if (title.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (isEdit) {
                    dbHelper.updateNote(note.getId(), title, content, category);
                    Toast.makeText(MainActivity.this, "Note updated", Toast.LENGTH_SHORT).show();
                } else {
                    dbHelper.insertNote(title, content, category);
                    Toast.makeText(MainActivity.this, "Note saved", Toast.LENGTH_SHORT).show();
                }
                refreshNotes();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}