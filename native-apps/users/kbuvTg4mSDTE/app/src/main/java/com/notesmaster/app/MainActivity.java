package com.notesmaster.app;

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

    private DatabaseHelper dbHelper;
    private List<Note> currentNotesList;
    private NotesListAdapter notesAdapter;

    private ListView lvNotes;
    private LinearLayout layoutEmptyState;
    private EditText etSearch;
    private LinearLayout layoutCategoriesContainer;
    private Button btnAddNote;

    private String selectedCategory = "All";
    private String currentSearchQuery = "";

    // Preset list of standard categories
    private final String[] categories = {"All", "Personal", "Work", "Ideas", "Study", "Finance", "Others"};
    // Categories for spinner dialog (without "All")
    private final String[] spinnerCategories = {"Personal", "Work", "Ideas", "Study", "Finance", "Others"};

    private final List<TextView> categoryChipViews = new ArrayList<TextView>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        currentNotesList = new ArrayList<Note>();

        // Find UI components
        lvNotes = (ListView) findViewById(R.id.lv_notes);
        layoutEmptyState = (LinearLayout) findViewById(R.id.layout_empty_state);
        etSearch = (EditText) findViewById(R.id.et_search);
        layoutCategoriesContainer = (LinearLayout) findViewById(R.id.layout_categories_container);
        btnAddNote = (Button) findViewById(R.id.btn_add_note);

        // Populate preset category filter UI chips
        setupCategoryFilters();

        // Setup ListView adapter
        notesAdapter = new NotesListAdapter();
        lvNotes.setAdapter(notesAdapter);

        // Setup Listeners
        btnAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddEditNoteDialog(null);
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
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

        // Load initial notes data
        refreshNotesList();
    }

    private void setupCategoryFilters() {
        layoutCategoriesContainer.removeAllViews();
        categoryChipViews.clear();

        for (int i = 0; i < categories.length; i++) {
            final String catName = categories[i];
            final TextView chip = new TextView(this);
            chip.setText(catName);
            chip.setTextSize(14);
            chip.setPadding(32, 16, 32, 16);

            // Layout Parameters for chip with spacing
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 16, 0);
            chip.setLayoutParams(params);

            // Apply style depending on selected state
            if (catName.equals(selectedCategory)) {
                chip.setBackgroundResource(R.drawable.chip_active);
                chip.setTextColor(0xFFFFFFFF);
            } else {
                chip.setBackgroundResource(R.drawable.chip_inactive);
                chip.setTextColor(0xFF37474F);
            }

            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedCategory = catName;
                    updateCategoryChipsUI();
                    refreshNotesList();
                }
            });

            layoutCategoriesContainer.addView(chip);
            categoryChipViews.add(chip);
        }
    }

    private void updateCategoryChipsUI() {
        for (int i = 0; i < categories.length; i++) {
            String catName = categories[i];
            TextView chip = categoryChipViews.get(i);
            if (catName.equals(selectedCategory)) {
                chip.setBackgroundResource(R.drawable.chip_active);
                chip.setTextColor(0xFFFFFFFF);
            } else {
                chip.setBackgroundResource(R.drawable.chip_inactive);
                chip.setTextColor(0xFF37474F);
            }
        }
    }

    private void refreshNotesList() {
        currentNotesList = dbHelper.getNotes(selectedCategory, currentSearchQuery);
        notesAdapter.notifyDataSetChanged();

        if (currentNotesList.isEmpty()) {
            lvNotes.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            lvNotes.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }

    // Unified Dialog logic for Add and Edit Note operations
    private void showAddEditNoteDialog(final Note noteToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_note, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        // Bind Dialog components
        TextView tvDialogTitle = (TextView) dialogView.findViewById(R.id.tv_dialog_title);
        final EditText etNoteTitle = (EditText) dialogView.findViewById(R.id.et_note_title);
        final EditText etNoteContent = (EditText) dialogView.findViewById(R.id.et_note_content);
        final Spinner spNoteCategory = (Spinner) dialogView.findViewById(R.id.sp_note_category);
        Button btnDialogCancel = (Button) dialogView.findViewById(R.id.btn_dialog_cancel);
        Button btnDialogSave = (Button) dialogView.findViewById(R.id.btn_dialog_save);

        // Bind spinner values
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, spinnerCategories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spNoteCategory.setAdapter(spinnerAdapter);

        final boolean isEditMode = (noteToEdit != null);

        if (isEditMode) {
            tvDialogTitle.setText("Edit Note");
            etNoteTitle.setText(noteToEdit.getTitle());
            etNoteContent.setText(noteToEdit.getContent());
            // Set spinner category selection
            int spinnerIndex = 0;
            for (int i = 0; i < spinnerCategories.length; i++) {
                if (spinnerCategories[i].equalsIgnoreCase(noteToEdit.getCategory())) {
                    spinnerIndex = i;
                    break;
                }
            }
            spNoteCategory.setSelection(spinnerIndex);
            btnDialogSave.setText("Update Note");
        } else {
            tvDialogTitle.setText("Create Note");
            btnDialogSave.setText("Save Note");
        }

        btnDialogCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnDialogSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = etNoteTitle.getText().toString().trim();
                String content = etNoteContent.getText().toString().trim();
                String category = spNoteCategory.getSelectedItem().toString();

                if (title.isEmpty()) {
                    etNoteTitle.setError("Title is required");
                    return;
                }

                if (content.isEmpty()) {
                    etNoteContent.setError("Content is required");
                    return;
                }

                // Get current formatted date string
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());
                String timestamp = sdf.format(new Date());

                if (isEditMode) {
                    noteToEdit.setTitle(title);
                    noteToEdit.setContent(content);
                    noteToEdit.setCategory(category);
                    noteToEdit.setTimestamp(timestamp);

                    dbHelper.updateNote(noteToEdit);
                    Toast.makeText(MainActivity.this, "Note updated successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Note newNote = new Note(title, content, category, timestamp);
                    dbHelper.insertNote(newNote);
                    Toast.makeText(MainActivity.this, "Note saved successfully", Toast.LENGTH_SHORT).show();
                }

                dialog.dismiss();
                refreshNotesList();
            }
        });

        dialog.show();
    }

    // Custom Note List Adapter
    private class NotesListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return currentNotesList.size();
        }

        @Override
        public Object getItem(int position) {
            return currentNotesList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return currentNotesList.get(position).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.item_note, parent, false);
            }

            final Note note = currentNotesList.get(position);

            TextView tvTitle = (TextView) convertView.findViewById(R.id.tv_note_title);
            TextView tvCategory = (TextView) convertView.findViewById(R.id.tv_note_category);
            TextView tvContent = (TextView) convertView.findViewById(R.id.tv_note_content);
            TextView tvDate = (TextView) convertView.findViewById(R.id.tv_note_date);
            TextView btnEdit = (TextView) convertView.findViewById(R.id.btn_edit_note);
            TextView btnDelete = (TextView) convertView.findViewById(R.id.btn_delete_note);

            tvTitle.setText(note.getTitle());
            tvCategory.setText(note.getCategory());
            tvContent.setText(note.getContent());
            tvDate.setText(note.getTimestamp());

            btnEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAddEditNoteDialog(note);
                }
            });

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
                    alertBuilder.setTitle("Delete Note");
                    alertBuilder.setMessage("Are you sure you want to delete this note?");
                    alertBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dbHelper.deleteNote(note.getId());
                            Toast.makeText(MainActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                            refreshNotesList();
                        }
                    });
                    alertBuilder.setNegativeButton("No", null);
                    alertBuilder.show();
                }
            });

            return convertView;
        }
    }
}