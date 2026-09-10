package com.smartnotes.app;

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
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;
    private ArrayList<HashMap<String, String>> notesList;
    private ArrayList<HashMap<String, String>> categoryList;
    private NotesAdapter notesAdapter;

    // List and form UI elements
    private LinearLayout layoutListContainer;
    private LinearLayout layoutFormContainer;
    private ListView notesListView;
    private TextView txtEmptyView;
    private EditText editSearch;
    private Spinner spinnerCategoryFilter;
    private Button btnAddCategoryQuick;
    private Button fabAddNote;

    // Form inputs
    private TextView txtFormTitle;
    private EditText editNoteTitle;
    private EditText editNoteContent;
    private Spinner spinnerNoteCategory;
    private Button btnCancelNote;
    private Button btnSaveNote;

    // Application state trackers
    private boolean isEditingMode = false;
    private long targetEditingNoteId = -1;
    private long selectedFilterCategoryId = -1; // -1 means "All Categories"
    private String currentSearchString = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        notesList = new ArrayList<HashMap<String, String>>();
        categoryList = new ArrayList<HashMap<String, String>>();

        initializeUiReferences();
        setupFilterCategorySpinner();
        setupFormCategorySpinner();
        refreshNotesListView();

        setupListeners();
    }

    private void initializeUiReferences() {
        // Master containers
        layoutListContainer = (LinearLayout) findViewById(R.id.layout_list_container);
        layoutFormContainer = (LinearLayout) findViewById(R.id.layout_form_container);
        
        // Listing UI elements
        notesListView = (ListView) findViewById(R.id.notes_list_view);
        txtEmptyView = (TextView) findViewById(R.id.txt_empty_view);
        editSearch = (EditText) findViewById(R.id.edit_search);
        spinnerCategoryFilter = (Spinner) findViewById(R.id.spinner_category_filter);
        btnAddCategoryQuick = (Button) findViewById(R.id.btn_add_category_quick);
        fabAddNote = (Button) findViewById(R.id.fab_add_note);

        // Form Editor components
        txtFormTitle = (TextView) findViewById(R.id.txt_form_title);
        editNoteTitle = (EditText) findViewById(R.id.edit_note_title);
        editNoteContent = (EditText) findViewById(R.id.edit_note_content);
        spinnerNoteCategory = (Spinner) findViewById(R.id.spinner_note_category);
        btnCancelNote = (Button) findViewById(R.id.btn_cancel_note);
        btnSaveNote = (Button) findViewById(R.id.btn_save_note);

        // Initial setup for empty text
        notesListView.setEmptyView(txtEmptyView);
    }

    private void setupFilterCategorySpinner() {
        // Pull updated categories list from database and populate
        ArrayList<HashMap<String, String>> dbCategories = dbHelper.getAllCategories();
        categoryList.clear();

        // Create virtual element for filtering "All Categories"
        HashMap<String, String> allOption = new HashMap<String, String>();
        allOption.put("id", "-1");
        allOption.put("name", getString(R.string.category_all));
        categoryList.add(allOption);
        categoryList.addAll(dbCategories);

        CategorySpinnerAdapter adapter = new CategorySpinnerAdapter(this, categoryList);
        spinnerCategoryFilter.setAdapter(adapter);
    }

    private void setupFormCategorySpinner() {
        ArrayList<HashMap<String, String>> dbCategories = dbHelper.getAllCategories();
        CategorySpinnerAdapter adapter = new CategorySpinnerAdapter(this, dbCategories);
        spinnerNoteCategory.setAdapter(adapter);
    }

    private void refreshNotesListView() {
        notesList = dbHelper.getFilteredNotes(currentSearchString, selectedFilterCategoryId);
        if (notesAdapter == null) {
            notesAdapter = new NotesAdapter(this, notesList);
            notesListView.setAdapter(notesAdapter);
        } else {
            notesAdapter.updateData(notesList);
        }
    }

    private void setupListeners() {
        // Quick dialog to add category on click
        btnAddCategoryQuick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddCategoryDialog();
            }
        });

        // Floating Action button triggers form mode to insert new note
        fabAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openNoteForm(false, -1, "", "", -1);
            }
        });

        // Search text watcher triggers database filtering on typing events
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchString = s.toString();
                refreshNotesListView();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Spinner triggers search list modification on filter selection modification
        spinnerCategoryFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                HashMap<String, String> selection = categoryList.get(position);
                selectedFilterCategoryId = Long.parseLong(selection.get("id"));
                refreshNotesListView();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Form cancel action closes inputs container and displays lists once more
        btnCancelNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeNoteForm();
            }
        });

        // Save note submission handler validates input fields, writes updates/inserts and refreshes
        btnSaveNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNoteFormInput();
            }
        });
    }

    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.add_category));

        final EditText inputField = new EditText(this);
        inputField.setHint(getString(R.string.enter_category_name));
        inputField.setSingleLine(true);
        builder.setView(inputField);

        builder.setPositiveButton(getString(R.string.save), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = inputField.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Category name cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                long id = dbHelper.insertCategory(name);
                if (id == -1) {
                    Toast.makeText(MainActivity.this, "Category name already exists", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Category created successfully", Toast.LENGTH_SHORT).show();
                    // Sync Spinners UI
                    setupFilterCategorySpinner();
                    setupFormCategorySpinner();
                }
            }
        });

        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void openNoteForm(boolean editing, long noteId, String title, String content, long currentCatId) {
        isEditingMode = editing;
        targetEditingNoteId = noteId;

        if (editing) {
            txtFormTitle.setText(getString(R.string.edit_note));
            editNoteTitle.setText(title);
            editNoteContent.setText(content);

            // Dynamically select proper assigned category inside form dropdown
            ArrayList<HashMap<String, String>> categories = dbHelper.getAllCategories();
            int selectIndex = 0;
            for (int i = 0; i < categories.size(); i++) {
                if (Long.parseLong(categories.get(i).get("id")) == currentCatId) {
                    selectIndex = i;
                    break;
                }
            }
            if (selectIndex < spinnerNoteCategory.getCount()) {
                spinnerNoteCategory.setSelection(selectIndex);
            }
        } else {
            txtFormTitle.setText(getString(R.string.add_note));
            editNoteTitle.setText("");
            editNoteContent.setText("");
            if (spinnerNoteCategory.getCount() > 0) {
                spinnerNoteCategory.setSelection(0);
            }
        }

        // Slide view structures transitions UI dynamically
        layoutListContainer.setVisibility(View.GONE);
        layoutFormContainer.setVisibility(View.PIVOT_X_DOCUMENT_BOUNDS | View.VISIBLE);
        fabAddNote.setVisibility(View.GONE);
    }

    private void closeNoteForm() {
        layoutFormContainer.setVisibility(View.GONE);
        layoutListContainer.setVisibility(View.VISIBLE);
        fabAddNote.setVisibility(View.VISIBLE);
    }

    private void saveNoteFormInput() {
        String title = editNoteTitle.getText().toString().trim();
        String content = editNoteContent.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Title field is mandatory", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get Category selection from spinner
        long selectedCatId = -1;
        if (spinnerNoteCategory.getSelectedItem() != null) {
            @SuppressWarnings("unchecked")
            HashMap<String, String> item = (HashMap<String, String>) spinnerNoteCategory.getSelectedItem();
            selectedCatId = Long.parseLong(item.get("id"));
        } else {
            Toast.makeText(this, "Please select or create a category first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isEditingMode) {
            dbHelper.updateNote(targetEditingNoteId, title, content, selectedCatId);
            Toast.makeText(this, "Note modified successfully", Toast.LENGTH_SHORT).show();
        } else {
            dbHelper.insertNote(title, content, selectedCatId);
            Toast.makeText(this, "Note saved successfully", Toast.LENGTH_SHORT).show();
        }

        closeNoteForm();
        refreshNotesListView();
    }

    private void triggerNoteDeletion(final long id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.delete_confirm_title));
        builder.setMessage(getString(R.string.delete_confirm_message));
        builder.setPositiveButton(getString(R.string.delete), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dbHelper.deleteNote(id);
                Toast.makeText(MainActivity.this, "Note deleted successfully", Toast.LENGTH_SHORT).show();
                refreshNotesListView();
            }
        });
        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.show();
    }

    // --- INNER CLASS LIST ADAPTER FOR NOTES DISPLAY ---

    private class NotesAdapter extends BaseAdapter {

        private Context context;
        private ArrayList<HashMap<String, String>> list;
        private LayoutInflater inflater;

        public NotesAdapter(Context context, ArrayList<HashMap<String, String>> list) {
            this.context = context;
            this.list = list;
            this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void updateData(ArrayList<HashMap<String, String>> newList) {
            this.list = newList;
            notifyDataSetChanged();
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
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(R.layout.note_item, parent, false);
            }

            final HashMap<String, String> item = list.get(position);

            TextView title = (TextView) view.findViewById(R.id.note_title);
            TextView snippet = (TextView) view.findViewById(R.id.note_content_snippet);
            TextView timestamp = (TextView) view.findViewById(R.id.note_timestamp);
            TextView badge = (TextView) view.findViewById(R.id.note_category_badge);
            Button btnEdit = (Button) view.findViewById(R.id.btn_item_edit);
            Button btnDelete = (Button) view.findViewById(R.id.btn_item_delete);

            title.setText(item.get("title"));
            snippet.setText(item.get("content"));
            timestamp.setText(item.get("timestamp"));
            
            String catName = item.get("category_name");
            badge.setText(catName != null ? catName : "General");

            // Wire individual cell triggers to trigger dynamic edit / delete
            btnEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    long noteId = Long.parseLong(item.get("id"));
                    long catId = Long.parseLong(item.get("category_id"));
                    openNoteForm(true, noteId, item.get("title"), item.get("content"), catId);
                }
            });

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    long noteId = Long.parseLong(item.get("id"));
                    triggerNoteDeletion(noteId);
                }
            });

            return view;
        }
    }

    // --- INNER CLASS ADAPTER FOR CATEGORY SPINNER ---

    private class CategorySpinnerAdapter extends BaseAdapter {

        private Context context;
        private ArrayList<HashMap<String, String>> list;
        private LayoutInflater inflater;

        public CategorySpinnerAdapter(Context context, ArrayList<HashMap<String, String>> list) {
            this.context = context;
            this.list = list;
            this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(android.R.layout.simple_spinner_item, parent, false);
            }
            TextView txt = (TextView) view.findViewById(android.R.id.text1);
            txt.setText(list.get(position).get("name"));
            txt.setTextColor(0xFF2C3E50); // Deep slate grey text color matching theme colors
            txt.setPadding(8, 8, 8, 8);
            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
            }
            TextView txt = (TextView) view.findViewById(android.R.id.text1);
            txt.setText(list.get(position).get("name"));
            txt.setTextColor(0xFF2C3E50);
            txt.setPadding(16, 16, 16, 16);
            return view;
        }
    }
}