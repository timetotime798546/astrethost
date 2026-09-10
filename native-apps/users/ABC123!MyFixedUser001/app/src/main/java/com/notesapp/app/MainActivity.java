package com.notesapp.app;

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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;
    private ListView lvNotes;
    private TextView tvEmptyState;
    private EditText etSearch;
    private Spinner spinnerFilter;
    private RelativeLayout btnAddNote;
    private Button btnManageCategories;

    private List<DatabaseHelper.Note> notesList = new ArrayList<>();
    private List<DatabaseHelper.Category> categoryFilterList = new ArrayList<>();
    private NotesAdapter notesAdapter;
    private ArrayAdapter<String> filterSpinnerAdapter;

    private long currentCategoryFilterId = 0; // 0 means 'All'
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        lvNotes = (ListView) findViewById(R.id.lv_notes);
        tvEmptyState = (TextView) findViewById(R.id.tv_empty_state);
        etSearch = (EditText) findViewById(R.id.et_search);
        spinnerFilter = (Spinner) findViewById(R.id.spinner_filter);
        btnAddNote = (RelativeLayout) findViewById(R.id.btn_add_note);
        btnManageCategories = (Button) findViewById(R.id.btn_manage_categories);

        setupFilterSpinner();
        setupNotesList();
        setupSearch();

        btnAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNoteDialog(null);
            }
        });

        btnManageCategories.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCategoriesDialog();
            }
        });

        refreshData();
    }

    private void setupFilterSpinner() {
        updateFilterSpinnerItems();

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    currentCategoryFilterId = 0; // All Categories
                } else {
                    currentCategoryFilterId = categoryFilterList.get(position - 1).id;
                }
                refreshData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentCategoryFilterId = 0;
            }
        });
    }

    private void updateFilterSpinnerItems() {
        List<DatabaseHelper.Category> dbCategories = dbHelper.getAllCategories();
        categoryFilterList.clear();
        categoryFilterList.addAll(dbCategories);

        List<String> spinnerItems = new ArrayList<>();
        spinnerItems.add("All Categories");
        for (int i = 0; i < dbCategories.size(); i++) {
            spinnerItems.add(dbCategories.get(i).name);
        }

        filterSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerItems);
        filterSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(filterSpinnerAdapter);
    }

    private void setupNotesList() {
        notesAdapter = new NotesAdapter(this, notesList);
        lvNotes.setAdapter(notesAdapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                refreshData();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void refreshData() {
        List<DatabaseHelper.Note> updatedNotes = dbHelper.getAllNotes(currentSearchQuery, currentCategoryFilterId);
        notesList.clear();
        notesList.addAll(updatedNotes);
        notesAdapter.notifyDataSetChanged();

        if (notesList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    private void showNoteDialog(final DatabaseHelper.Note noteToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_note, null);
        builder.setView(dialogView);

        final TextView tvDialogTitle = (TextView) dialogView.findViewById(R.id.tv_dialog_title);
        final EditText etTitle = (EditText) dialogView.findViewById(R.id.et_dialog_title);
        final EditText etContent = (EditText) dialogView.findViewById(R.id.et_dialog_content);
        final Spinner spinnerCategory = (Spinner) dialogView.findViewById(R.id.spinner_dialog_category);
        Button btnCancel = (Button) dialogView.findViewById(R.id.btn_dialog_cancel);
        Button btnSave = (Button) dialogView.findViewById(R.id.btn_dialog_save);

        // Load Categories into spinner
        final List<DatabaseHelper.Category> categories = dbHelper.getAllCategories();
        List<String> categoryNames = new ArrayList<>();
        categoryNames.add("No Category");
        for (int i = 0; i < categories.size(); i++) {
            categoryNames.add(categories.get(i).name);
        }

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryNames);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(catAdapter);

        final AlertDialog dialog = builder.create();

        if (noteToEdit != null) {
            tvDialogTitle.setText("Edit Note");
            etTitle.setText(noteToEdit.title);
            etContent.setText(noteToEdit.content);
            
            // Set correct category in spinner
            for (int i = 0; i < categories.size(); i++) {
                if (categories.get(i).id == noteToEdit.categoryId) {
                    spinnerCategory.setSelection(i + 1); // +1 because of "No Category"
                    break;
                }
            }
        } else {
            tvDialogTitle.setText("Add Note");
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
                String title = etTitle.getText().toString().trim();
                String content = etContent.getText().toString().trim();

                if (title.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter a title", Toast.LENGTH_SHORT).show();
                    return;
                }

                int selectedPos = spinnerCategory.getSelectedItemPosition();
                long categoryId = 0;
                if (selectedPos > 0) {
                    categoryId = categories.get(selectedPos - 1).id;
                }

                if (noteToEdit != null) {
                    dbHelper.updateNote(noteToEdit.id, title, content, categoryId);
                    Toast.makeText(MainActivity.this, "Note updated", Toast.LENGTH_SHORT).show();
                } else {
                    dbHelper.addNote(title, content, categoryId);
                    Toast.makeText(MainActivity.this, "Note saved", Toast.LENGTH_SHORT).show();
                }

                refreshData();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void showCategoriesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_categories, null);
        builder.setView(dialogView);

        final EditText etNewCategory = (EditText) dialogView.findViewById(R.id.et_new_category);
        Button btnAdd = (Button) dialogView.findViewById(R.id.btn_add_category);
        ListView lvCategories = (ListView) dialogView.findViewById(R.id.lv_categories);
        Button btnClose = (Button) dialogView.findViewById(R.id.btn_close_categories);

        final List<DatabaseHelper.Category> categoryList = dbHelper.getAllCategories();
        final CategoryAdapter catAdapter = new CategoryAdapter(this, categoryList);
        lvCategories.setAdapter(catAdapter);

        final AlertDialog dialog = builder.create();

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = etNewCategory.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Enter a category name", Toast.LENGTH_SHORT).show();
                    return;
                }

                long id = dbHelper.addCategory(name);
                if (id == -1) {
                    Toast.makeText(MainActivity.this, "Category already exists", Toast.LENGTH_SHORT).show();
                } else {
                    etNewCategory.setText("");
                    categoryList.clear();
                    categoryList.addAll(dbHelper.getAllCategories());
                    catAdapter.notifyDataSetChanged();
                    updateFilterSpinnerItems();
                    refreshData();
                }
            }
        });

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    // Adapter for Notes List View
    private class NotesAdapter extends ArrayAdapter<DatabaseHelper.Note> {
        public NotesAdapter(Context context, List<DatabaseHelper.Note> notes) {
            super(context, 0, notes);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final DatabaseHelper.Note note = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.note_item, parent, false);
            }

            TextView tvTitle = (TextView) convertView.findViewById(R.id.tv_note_title);
            TextView tvContent = (TextView) convertView.findViewById(R.id.tv_note_content);
            TextView tvCategory = (TextView) convertView.findViewById(R.id.tv_note_category);
            TextView tvDate = (TextView) convertView.findViewById(R.id.tv_note_date);
            Button btnEdit = (Button) convertView.findViewById(R.id.btn_edit_note);
            Button btnDelete = (Button) convertView.findViewById(R.id.btn_delete_note);

            tvTitle.setText(note.title);
            tvContent.setText(note.content != null ? note.content : "");
            tvDate.setText(note.timestamp);

            if (note.categoryName != null && !note.categoryName.isEmpty()) {
                tvCategory.setVisibility(View.VISIBLE);
                tvCategory.setText(note.categoryName);
            } else {
                tvCategory.setVisibility(View.GONE);
            }

            btnEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showNoteDialog(note);
                }
            });

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(getContext())
                        .setTitle("Delete Note")
                        .setMessage("Are you sure you want to delete this note?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dbHelper.deleteNote(note.id);
                                refreshData();
                                Toast.makeText(getContext(), "Note deleted", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
                }
            });

            return convertView;
        }
    }

    // Adapter for Category List View inside management dialog
    private class CategoryAdapter extends ArrayAdapter<DatabaseHelper.Category> {
        public CategoryAdapter(Context context, List<DatabaseHelper.Category> categories) {
            super(context, 0, categories);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final DatabaseHelper.Category category = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.category_item, parent, false);
            }

            TextView tvName = (TextView) convertView.findViewById(R.id.tv_category_name);
            Button btnDelete = (Button) convertView.findViewById(R.id.btn_delete_category);

            tvName.setText(category.name);

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(getContext())
                        .setTitle("Delete Category")
                        .setMessage("Are you sure you want to delete this category? Notes in this category will be reset to no category.")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                dbHelper.deleteCategory(category.id);
                                // Refresh Dialog List
                                clear();
                                addAll(dbHelper.getAllCategories());
                                notifyDataSetChanged();
                                // Refresh Filter Spinner & Main list
                                updateFilterSpinnerItems();
                                refreshData();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
                }
            });

            return convertView;
        }
    }
}