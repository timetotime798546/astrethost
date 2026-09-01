package com.notecraft.app;

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
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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

    private NotesDatabaseHelper dbHelper;

    // Main Panels
    private View panelList;
    private View panelEditor;
    private View panelCategories;

    // List Panel Views
    private EditText editSearch;
    private Spinner spinnerFilter;
    private ListView listNotes;
    private Button btnManageCats;
    private Button btnAddNote;

    // Editor Panel Views
    private TextView lblEditorTitle;
    private Button btnEditorClose;
    private EditText editNoteTitle;
    private EditText editNoteContent;
    private Spinner spinnerNoteCat;
    private Button btnDeleteNote;
    private Button btnSaveNote;

    // Category Panel Views
    private Button btnCatsClose;
    private EditText editNewCat;
    private Button btnCreateCat;
    private ListView listCategories;

    // State Tracking
    private List<Category> allCategories = new ArrayList<>();
    private List<Note> currentNotesList = new ArrayList<>();
    private long editingNoteId = -1; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new NotesDatabaseHelper(this);

        initViews();
        setupListeners();
        refreshCategoryPickers();
        refreshNotesList();
    }

    private void initViews() {
        panelList = findViewById(R.id.panel_list);
        panelEditor = findViewById(R.id.panel_editor);
        panelCategories = findViewById(R.id.panel_categories);

        // Main List elements
        editSearch = (EditText) findViewById(R.id.edit_search);
        spinnerFilter = (Spinner) findViewById(R.id.spinner_filter);
        listNotes = (ListView) findViewById(R.id.list_notes);
        btnManageCats = (Button) findViewById(R.id.btn_manage_cats);
        btnAddNote = (Button) findViewById(R.id.btn_add_note);

        // Editor elements
        lblEditorTitle = (TextView) findViewById(R.id.lbl_editor_title);
        btnEditorClose = (Button) findViewById(R.id.btn_editor_close);
        editNoteTitle = (EditText) findViewById(R.id.edit_note_title);
        editNoteContent = (EditText) findViewById(R.id.edit_note_content);
        spinnerNoteCat = (Spinner) findViewById(R.id.spinner_note_cat);
        btnDeleteNote = (Button) findViewById(R.id.btn_delete_note);
        btnSaveNote = (Button) findViewById(R.id.btn_save_note);

        // Categories UI elements
        btnCatsClose = (Button) findViewById(R.id.btn_cats_close);
        editNewCat = (EditText) findViewById(R.id.edit_new_cat);
        btnCreateCat = (Button) findViewById(R.id.btn_create_cat);
        listCategories = (ListView) findViewById(R.id.list_categories);
    }

    private void setupListeners() {
        // Navigation Panel switches
        btnManageCats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPanel(panelCategories);
                refreshCategoriesList();
            }
        });

        btnCatsClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPanel(panelList);
                refreshCategoryPickers();
                refreshNotesList();
            }
        });

        btnAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openEditor(-1);
            }
        });

        btnEditorClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
                showPanel(panelList);
            }
        });

        // Save note actions
        btnSaveNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNoteAction();
            }
        });

        // Delete note option
        btnDeleteNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteNoteAction();
            }
        });

        // New Category actions
        btnCreateCat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String catName = editNewCat.getText().toString().trim();
                if (!catName.isEmpty()) {
                    long id = dbHelper.insertCategory(catName);
                    if (id != -1) {
                        editNewCat.setText("");
                        hideKeyboard();
                        refreshCategoriesList();
                    } else {
                        Toast.makeText(MainActivity.this, "Category already exists!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // Filter & Search Watchers
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshNotesList();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshNotesList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Note selection handler
        listNotes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Note note = currentNotesList.get(position);
                openEditor(note.getId());
            }
        });
    }

    private void showPanel(View activePanel) {
        panelList.setVisibility(activePanel == panelList ? View.VISIBLE : View.GONE);
        panelEditor.setVisibility(activePanel == panelEditor ? View.VISIBLE : View.GONE);
        panelCategories.setVisibility(activePanel == panelCategories ? View.VISIBLE : View.GONE);
    }

    private void refreshCategoryPickers() {
        allCategories = dbHelper.getAllCategories();

        // 1. Fill filter dropdown (includes 'All' option)
        List<String> filterList = new ArrayList<>();
        filterList.add("All Categories");
        for (int i = 0; i < allCategories.size(); i++) {
            filterList.add(allCategories.get(i).getName());
        }

        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, filterList);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(filterAdapter);

        // 2. Fill editor dropdown
        ArrayAdapter<Category> editorCatAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, allCategories);
        editorCatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerNoteCat.setAdapter(editorCatAdapter);
    }

    private void refreshNotesList() {
        String query = editSearch.getText().toString().trim();
        long filterCatId = 0;

        int selectedIdx = spinnerFilter.getSelectedItemPosition();
        if (selectedIdx > 0 && (selectedIdx - 1) < allCategories.size()) {
            filterCatId = allCategories.get(selectedIdx - 1).getId();
        }

        currentNotesList = dbHelper.getFilteredNotes(query, filterCatId);
        NotesAdapter adapter = new NotesAdapter(this, currentNotesList);
        listNotes.setAdapter(adapter);
    }

    private void refreshCategoriesList() {
        final List<Category> cats = dbHelper.getAllCategories();
        CategoryManagerAdapter adapter = new CategoryManagerAdapter(this, cats);
        listCategories.setAdapter(adapter);
    }

    private void openEditor(long noteId) {
        editingNoteId = noteId;
        refreshCategoryPickers();

        if (noteId == -1) {
            // Create mode
            lblEditorTitle.setText("Create Note");
            editNoteTitle.setText("");
            editNoteContent.setText("");
            btnDeleteNote.setVisibility(View.GONE);
            if (spinnerNoteCat.getCount() > 0) {
                spinnerNoteCat.setSelection(0);
            }
        } else {
            // Edit mode
            lblEditorTitle.setText("Edit Note");
            btnDeleteNote.setVisibility(View.VISIBLE);

            Note targetNote = null;
            for (int i = 0; i < currentNotesList.size(); i++) {
                if (currentNotesList.get(i).getId() == noteId) {
                    targetNote = currentNotesList.get(i);
                    break;
                }
            }

            if (targetNote != null) {
                editNoteTitle.setText(targetNote.getTitle());
                editNoteContent.setText(targetNote.getContent());

                // Find category spinner selection position
                int selectionPos = 0;
                for (int i = 0; i < allCategories.size(); i++) {
                    if (allCategories.get(i).getId() == targetNote.getCategoryId()) {
                        selectionPos = i;
                        break;
                    }
                }
                spinnerNoteCat.setSelection(selectionPos);
            }
        }
        showPanel(panelEditor);
    }

    private void saveNoteAction() {
        String title = editNoteTitle.getText().toString().trim();
        String content = editNoteContent.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
            return;
        }

        if (spinnerNoteCat.getSelectedItem() == null) {
            Toast.makeText(this, "Please create at least one category first", Toast.LENGTH_SHORT).show();
            return;
        }

        Category selectedCat = (Category) spinnerNoteCat.getSelectedItem();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

        if (editingNoteId == -1) {
            // New insert
            dbHelper.insertNote(title, content, selectedCat.getId(), timestamp);
            Toast.makeText(this, "Note saved successfully", Toast.LENGTH_SHORT).show();
        } else {
            // Edit update
            dbHelper.updateNote(editingNoteId, title, content, selectedCat.getId(), timestamp);
            Toast.makeText(this, "Note updated successfully", Toast.LENGTH_SHORT).show();
        }

        hideKeyboard();
        showPanel(panelList);
        refreshNotesList();
    }

    private void deleteNoteAction() {
        if (editingNoteId == -1) return;

        new AlertDialog.Builder(this)
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dbHelper.deleteNote(editingNoteId);
                        Toast.makeText(MainActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                        hideKeyboard();
                        showPanel(panelList);
                        refreshNotesList();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (panelEditor.getVisibility() == View.VISIBLE) {
            showPanel(panelList);
        } else if (panelCategories.getVisibility() == View.VISIBLE) {
            showPanel(panelList);
            refreshCategoryPickers();
            refreshNotesList();
        } else {
            super.onBackPressed();
        }
    }

    // Custom Note List Adapter
    private static class NotesAdapter extends ArrayAdapter<Note> {
        private final Context context;
        private final List<Note> values;

        public NotesAdapter(Context context, List<Note> values) {
            super(context, 0, values);
            this.context = context;
            this.values = values;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                // Build card row programmatically for simplified custom styled lists
                LinearLayout row = new LinearLayout(context);
                row.setOrientation(LinearLayout.VERTICAL);
                row.setPadding(16, 16, 16, 16);

                TextView titleView = new TextView(context);
                titleView.setId(Character.getNumericValue('t'));
                titleView.setTextSize(16spToPx(16));
                titleView.setTextColor(Color.parseColor("#212121"));
                titleView.setSingleLine(true);

                TextView descView = new TextView(context);
                descView.setId(Character.getNumericValue('d'));
                descView.setTextSize(14spToPx(14));
                descView.setTextColor(Color.parseColor("#757575"));
                descView.setMaxLines(2);
                descView.setPadding(0, 4, 0, 4);

                LinearLayout subRow = new LinearLayout(context);
                subRow.setOrientation(LinearLayout.HORIZONTAL);

                TextView catTag = new TextView(context);
                catTag.setId(Character.getNumericValue('c'));
                catTag.setTextSize(11spToPx(11));
                catTag.setTextColor(Color.parseColor("#2196F3"));

                TextView timeTag = new TextView(context);
                timeTag.setId(Character.getNumericValue('m'));
                timeTag.setTextSize(11spToPx(11));
                timeTag.setTextColor(Color.parseColor("#BDBDBD"));
                timeTag.setPadding(16, 0, 0, 0);

                subRow.addView(catTag);
                subRow.addView(timeTag);

                row.addView(titleView);
                row.addView(descView);
                row.addView(subRow);
                convertView = row;
            }

            Note item = values.get(position);

            TextView t = (TextView) convertView.findViewById(Character.getNumericValue('t'));
            TextView d = (TextView) convertView.findViewById(Character.getNumericValue('d'));
            TextView c = (TextView) convertView.findViewById(Character.getNumericValue('c'));
            TextView m = (TextView) convertView.findViewById(Character.getNumericValue('m'));

            t.setText(item.getTitle());
            d.setText(item.getContent().trim().isEmpty() ? "(Empty note)" : item.getContent());
            c.setText("[" + item.getCategoryName() + "]");
            m.setText(item.getTimestamp());

            return convertView;
        }

        private int spToPx(float sp) {
            return (int) (sp * context.getResources().getDisplayMetrics().scaledDensity);
        }
    }

    // Custom Category Management List Adapter with Inline Deletion
    private class CategoryManagerAdapter extends ArrayAdapter<Category> {
        private final Context context;
        private final List<Category> values;

        public CategoryManagerAdapter(Context context, List<Category> values) {
            super(context, 0, values);
            this.context = context;
            this.values = values;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                // Build programmatic category list item layout containing deletion option
                RelativeLayout row = new RelativeLayout(context);
                row.setPadding(16, 12, 16, 12);

                TextView catName = new TextView(context);
                catName.setId(Character.getNumericValue('n'));
                catName.setTextSize(16);
                catName.setTextColor(Color.parseColor("#212121"));
                RelativeLayout.LayoutParams lpTxt = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                lpTxt.addRule(RelativeLayout.ALIGN_PARENT_START);
                lpTxt.addRule(RelativeLayout.CENTER_VERTICAL);
                row.addView(catName, lpTxt);

                Button delBtn = new Button(context);
                delBtn.setId(Character.getNumericValue('b'));
                delBtn.setText("Delete");
                delBtn.setTextColor(Color.parseColor("#D32F2F"));
                delBtn.setBackgroundColor(Color.TRANSPARENT);
                RelativeLayout.LayoutParams lpBtn = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                lpBtn.addRule(RelativeLayout.ALIGN_PARENT_END);
                lpBtn.addRule(RelativeLayout.CENTER_VERTICAL);
                row.addView(delBtn, lpBtn);

                convertView = row;
            }

            final Category item = values.get(position);

            TextView name = (TextView) convertView.findViewById(Character.getNumericValue('n'));
            Button del = (Button) convertView.findViewById(Character.getNumericValue('b'));

            name.setText(item.getName());
            del.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(context)
                            .setTitle("Delete Category")
                            .setMessage("Deleting '" + item.getName() + "' will also un-link notes belonging to it. Continue?")
                            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dbHelper.deleteCategory(item.getId());
                                    refreshCategoriesList();
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            });

            return convertView;
        }
    }
}
