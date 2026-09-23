package com.smartnotes.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;

    // View Panels
    private View panelListView, panelEditorView, panelCategoriesView;
    private Button fabAddNote;

    // List Panel Elements
    private EditText etSearchQuery;
    private Spinner spinnerFilterCategory;
    private LinearLayout containerNotesList;
    private TextView tvEmptyState;

    // Editor Panel Elements
    private TextView tvEditorModeTitle;
    private EditText etNoteTitle;
    private Spinner spinnerNoteCategory;
    private EditText etNoteContent;
    private Button btnCancelEditor, btnSaveNote;

    // Category Manager Elements
    private Button btnManageCategories;
    private EditText etNewCategory;
    private Button btnAddCategory, btnCloseCategories;
    private LinearLayout containerCategoriesList;

    // State properties
    private boolean isEditing = false;
    private int editingNoteId = -1;

    // dynamic database category lists
    private ArrayList<String> categoryNames = new ArrayList<>();
    private ArrayList<Integer> categoryIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        // Bind main containers
        panelListView = findViewById(R.id.panel_list_view);
        panelEditorView = findViewById(R.id.panel_editor_view);
        panelCategoriesView = findViewById(R.id.panel_categories_view);
        fabAddNote = findViewById(R.id.fab_add_note);

        // Bind listing elements
        etSearchQuery = findViewById(R.id.et_search_query);
        spinnerFilterCategory = findViewById(R.id.spinner_filter_category);
        containerNotesList = findViewById(R.id.container_notes_list);
        tvEmptyState = findViewById(R.id.tv_empty_state);

        // Bind editor elements
        tvEditorModeTitle = findViewById(R.id.tv_editor_mode_title);
        etNoteTitle = findViewById(R.id.et_note_title);
        spinnerNoteCategory = findViewById(R.id.spinner_note_category);
        etNoteContent = findViewById(R.id.et_note_content);
        btnCancelEditor = findViewById(R.id.btn_cancel_editor);
        btnSaveNote = findViewById(R.id.btn_save_note);

        // Bind category view elements
        btnManageCategories = findViewById(R.id.btn_manage_categories);
        etNewCategory = findViewById(R.id.et_new_category);
        btnAddCategory = findViewById(R.id.btn_add_category);
        btnCloseCategories = findViewById(R.id.btn_close_categories);
        containerCategoriesList = findViewById(R.id.container_categories_list);

        // Trigger loading of categories and lists
        refreshAllSpinnersAndFilters();
        refreshNotesList();

        // Setup FAB click - Launch Editor in Create Mode
        fabAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchCreateMode();
            }
        });

        // Setup Cancel Editor Click
        btnCancelEditor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchViewPanel(panelListView);
            }
        });

        // Save Note Form Submission
        btnSaveNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveForm();
            }
        });

        // Category Panel Management Toggle Buttons
        btnManageCategories.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchViewPanel(panelCategoriesView);
                refreshCategoriesList();
            }
        });

        btnCloseCategories.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshAllSpinnersAndFilters();
                switchViewPanel(panelListView);
            }
        });

        btnAddCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewCategory();
            }
        });

        // Search Input Watcher
        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshNotesList();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Category Filter Spinner selection triggers
        spinnerFilterCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshNotesList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void switchViewPanel(View targetPanel) {
        panelListView.setVisibility(View.GONE);
        panelEditorView.setVisibility(View.GONE);
        panelCategoriesView.setVisibility(View.GONE);

        targetPanel.setVisibility(View.VISIBLE);

        // Hide FAB on non-list views
        if (targetPanel == panelListView) {
            fabAddNote.setVisibility(View.VISIBLE);
        } else {
            fabAddNote.setVisibility(View.GONE);
        }
    }

    private void launchCreateMode() {
        isEditing = false;
        editingNoteId = -1;
        tvEditorModeTitle.setText("Create New Note");
        etNoteTitle.setText("");
        etNoteContent.setText("");
        if (spinnerNoteCategory.getAdapter() != null && spinnerNoteCategory.getAdapter().getCount() > 0) {
            spinnerNoteCategory.setSelection(0);
        }
        switchViewPanel(panelEditorView);
    }

    private void launchEditMode(int id, String title, String content, int currentCategoryId) {
        isEditing = true;
        editingNoteId = id;
        tvEditorModeTitle.setText("Edit Note Details");
        etNoteTitle.setText(title);
        etNoteContent.setText(content);

        // Map Category ID index back to spinner selection list
        int spinnerIndex = 0;
        for (int i = 0; i < categoryIds.size(); i++) {
            if (categoryIds.get(i) == currentCategoryId) {
                spinnerIndex = i;
                break;
            }
        }
        spinnerNoteCategory.setSelection(spinnerIndex);
        switchViewPanel(panelEditorView);
    }

    private void refreshAllSpinnersAndFilters() {
        categoryNames.clear();
        categoryIds.clear();

        Cursor cursor = dbHelper.getAllCategories();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_NAME));
                categoryNames.add(name);
                categoryIds.add(id);
            }
            cursor.close();
        }

        // Form selection setup
        ArrayAdapter<String> editorAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categoryNames);
        editorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerNoteCategory.setAdapter(editorAdapter);

        // Filter Dropdown setup (includes "All Categories" as top index)
        ArrayList<String> filterNames = new ArrayList<>();
        filterNames.add("All Categories");
        for (int i = 0; i < categoryNames.size(); i++) {
            filterNames.add(categoryNames.get(i));
        }

        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, filterNames);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterCategory.setAdapter(filterAdapter);
    }

    private void refreshNotesList() {
        containerNotesList.removeAllViews();

        int selectedFilterCatId = -1; // Default - All categories
        int filterPos = spinnerFilterCategory.getSelectedItemPosition();
        if (filterPos > 0) {
            selectedFilterCatId = categoryIds.get(filterPos - 1);
        }

        String searchVal = etSearchQuery.getText().toString();

        Cursor cursor = dbHelper.getNotes(selectedFilterCatId, searchVal);
        if (cursor != null && cursor.getCount() > 0) {
            tvEmptyState.setVisibility(View.GONE);
            while (cursor.moveToNext()) {
                final int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTE_ID));
                final String title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTE_TITLE));
                final String content = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTE_CONTENT));
                final int categoryId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTE_CATEGORY_ID));
                String categoryName = dbHelper.getCategoryName(categoryId);

                // Note item card
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(16, 16, 16, 16);
                card.setBackgroundColor(Color.WHITE);
                LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                cardLp.setMargins(0, 4, 0, 12);
                card.setLayoutParams(cardLp);

                // Header block: Title and Category badge
                LinearLayout headerLine = new LinearLayout(this);
                headerLine.setOrientation(LinearLayout.HORIZONTAL);
                headerLine.setGravity(Gravity.CENTER_VERTICAL);

                TextView tvTitle = new TextView(this);
                tvTitle.setText(title);
                tvTitle.setTextSize(16f);
                tvTitle.setTypeface(null, Typeface.BOLD);
                tvTitle.setTextColor(Color.parseColor("#212121"));
                tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                TextView tvTag = new TextView(this);
                tvTag.setText(categoryName);
                tvTag.setTextSize(10f);
                tvTag.setPadding(8, 4, 8, 4);
                tvTag.setTextColor(Color.WHITE);
                tvTag.setBackgroundColor(Color.parseColor("#3F51B5"));
                tvTag.setGravity(Gravity.CENTER);

                headerLine.addView(tvTitle);
                headerLine.addView(tvTag);

                // Body excerpt text
                TextView tvBody = new TextView(this);
                String preview = content;
                if (preview.length() > 100) {
                    preview = preview.substring(0, 100) + "...";
                }
                tvBody.setText(preview);
                tvBody.setTextSize(13f);
                tvBody.setTextColor(Color.parseColor("#616161"));
                tvBody.setPadding(0, 8, 0, 12);

                // Control actions footer
                LinearLayout controlsLine = new LinearLayout(this);
                controlsLine.setOrientation(LinearLayout.HORIZONTAL);
                controlsLine.setGravity(Gravity.RIGHT);

                Button btnEdit = new Button(this);
                btnEdit.setText("Edit");
                btnEdit.setTextSize(11f);
                btnEdit.setTextColor(Color.parseColor("#3F51B5"));
                btnEdit.setBackgroundColor(Color.parseColor("#E8EAF6"));
                btnEdit.setMinWidth(60);
                btnEdit.setMinimumHeight(32);
                LinearLayout.LayoutParams editLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                editLp.setMargins(0, 0, 8, 0);
                btnEdit.setLayoutParams(editLp);
                btnEdit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        launchEditMode(id, title, content, categoryId);
                    }
                });

                Button btnDelete = new Button(this);
                btnDelete.setText("Delete");
                btnDelete.setTextSize(11f);
                btnDelete.setTextColor(Color.WHITE);
                btnDelete.setBackgroundColor(Color.parseColor("#F44336"));
                btnDelete.setMinWidth(60);
                btnDelete.setMinimumHeight(32);
                btnDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showDeleteConfirmation(id);
                    }
                });

                controlsLine.addView(btnEdit);
                controlsLine.addView(btnDelete);

                card.addView(headerLine);
                card.addView(tvBody);
                card.addView(controlsLine);

                containerNotesList.addView(card);
            }
            cursor.close();
        } else {
            tvEmptyState.setVisibility(View.VISIBLE);
        }
    }

    private void refreshCategoriesList() {
        containerCategoriesList.removeAllViews();
        Cursor cursor = dbHelper.getAllCategories();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                final int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_ID));
                final String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_NAME));

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(12, 8, 12, 8);
                row.setBackgroundColor(Color.WHITE);
                row.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 2, 0, 6);
                row.setLayoutParams(lp);

                TextView tvName = new TextView(this);
                tvName.setText(name);
                tvName.setTextSize(14f);
                tvName.setTextColor(Color.BLACK);
                tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                Button btnDelete = new Button(this);
                btnDelete.setText("Delete");
                btnDelete.setTextSize(10f);
                btnDelete.setBackgroundColor(Color.parseColor("#F44336"));
                btnDelete.setTextColor(Color.WHITE);

                // Prevent deleting 'Uncategorized' default record (ID 1)
                if (id == 1) {
                    btnDelete.setEnabled(false);
                    btnDelete.setBackgroundColor(Color.parseColor("#BDBDBD"));
                }

                btnDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dbHelper.deleteCategory(id);
                        refreshCategoriesList();
                    }
                });

                row.addView(tvName);
                row.addView(btnDelete);
                containerCategoriesList.addView(row);
            }
            cursor.close();
        }
    }

    private void addNewCategory() {
        String newCatName = etNewCategory.getText().toString().trim();
        if (!newCatName.isEmpty()) {
            long result = dbHelper.insertCategory(newCatName);
            if (result != -1) {
                etNewCategory.setText("");
                Toast.makeText(this, "Category created!", Toast.LENGTH_SHORT).show();
                refreshCategoriesList();
            } else {
                Toast.makeText(this, "Category name already exists!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Enter a valid category name!", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteConfirmation(final int noteId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Deletion");
        builder.setMessage("Are you sure you want to permanently delete this note?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dbHelper.deleteNote(noteId);
                Toast.makeText(MainActivity.this, "Note Deleted", Toast.LENGTH_SHORT).show();
                refreshNotesList();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void saveForm() {
        String title = etNoteTitle.getText().toString().trim();
        String content = etNoteContent.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Title cannot be empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (spinnerNoteCategory.getSelectedItem() == null) {
            Toast.makeText(this, "Create a category first!", Toast.LENGTH_SHORT).show();
            return;
        }

        int catPos = spinnerNoteCategory.getSelectedItemPosition();
        int selectedCategoryId = categoryIds.get(catPos);

        if (isEditing) {
            dbHelper.updateNote(editingNoteId, title, content, selectedCategoryId);
            Toast.makeText(this, "Note Updated", Toast.LENGTH_SHORT).show();
        } else {
            dbHelper.insertNote(title, content, selectedCategoryId);
            Toast.makeText(this, "Note Created Successfully", Toast.LENGTH_SHORT).show();
        }

        switchViewPanel(panelListView);
        refreshNotesList();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.close();
    }
}