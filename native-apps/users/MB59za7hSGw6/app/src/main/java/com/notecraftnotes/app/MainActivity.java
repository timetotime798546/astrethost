package com.notecraftnotes.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int REQUEST_CODE_EDIT_NOTE = 101;

    private EditText etSearch;
    private Button btnAddCategory, btnCreateNote, btnLogout;
    private LinearLayout llCategoriesContainer;
    private ListView lvNotes;
    private ProgressBar pbNotesLoading;
    private TextView tvEmptyState;
    private TextView tvStatTotalNotes, tvStatFilteredNotes, tvStatWordCount;

    private BackendApi api;
    private List<NoteModel> allNotes = new ArrayList<>();
    private List<NoteModel> filteredNotes = new ArrayList<>();
    private List<String> categories = new ArrayList<>();
    private String selectedCategory = "All";
    private String currentSearchQuery = "";

    private NotesAdapter notesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        api = new BackendApi(this);

        if (!api.isLoggedIn()) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        etSearch = findViewById(R.id.etSearchNotes);
        btnAddCategory = findViewById(R.id.btnAddCategory);
        btnCreateNote = findViewById(R.id.btnCreateNote);
        btnLogout = findViewById(R.id.btnLogout);
        llCategoriesContainer = findViewById(R.id.llCategoriesContainer);
        lvNotes = findViewById(R.id.lvNotes);
        pbNotesLoading = findViewById(R.id.pbNotesLoading);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        tvStatTotalNotes = findViewById(R.id.tvStatTotalNotes);
        tvStatFilteredNotes = findViewById(R.id.tvStatFilteredNotes);
        tvStatWordCount = findViewById(R.id.tvStatWordCount);

        notesAdapter = new NotesAdapter();
        lvNotes.setAdapter(notesAdapter);

        // Default initial categories
        categories.add("All");
        categories.add("Personal");
        categories.add("Work");
        categories.add("Ideas");
        categories.add("Tasks");

        renderCategoryChips();

        // Search text listener
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim();
                applyFilterAndRecalculate();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnAddCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddCategoryDialog();
            }
        });

        btnCreateNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openEditNoteScreen(null);
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                api.logout(new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        finish();
                    }

                    @Override
                    public void onError(String error) {
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        finish();
                    }
                });
            }
        });

        lvNotes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                NoteModel note = filteredNotes.get(position);
                openEditNoteScreen(note);
            }
        });

        fetchCategoriesFromBackend();
        fetchNotesFromBackend();
    }

    private void fetchCategoriesFromBackend() {
        api.readData("categories", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    JSONArray records = response.optJSONArray("records");
                    if (records != null) {
                        for (int i = 0; i < records.length(); i++) {
                            JSONObject rec = records.getJSONObject(i);
                            CategoryModel cat = CategoryModel.fromRecordJson(rec);
                            if (cat != null && !cat.getName().isEmpty() && !categories.contains(cat.getName())) {
                                categories.add(cat.getName());
                            }
                        }
                        renderCategoryChips();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(String error) {
                // Keep default categories
            }
        });
    }

    private void fetchNotesFromBackend() {
        pbNotesLoading.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);

        api.readData("notes", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                pbNotesLoading.setVisibility(View.GONE);
                allNotes.clear();
                try {
                    JSONArray records = response.optJSONArray("records");
                    if (records != null) {
                        for (int i = 0; i < records.length(); i++) {
                            JSONObject rec = records.getJSONObject(i);
                            NoteModel note = NoteModel.fromRecordJson(rec);
                            if (note != null) {
                                allNotes.add(note);
                                // Dynamic auto-add missing note categories to chip filter
                                if (!note.getCategory().isEmpty() && !categories.contains(note.getCategory())) {
                                    categories.add(note.getCategory());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                renderCategoryChips();
                applyFilterAndRecalculate();
            }

            @Override
            public void onError(String error) {
                pbNotesLoading.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Failed to load notes: " + error, Toast.LENGTH_SHORT).show();
                applyFilterAndRecalculate();
            }
        });
    }

    private void renderCategoryChips() {
        llCategoriesContainer.removeAllViews();

        for (final String catName : categories) {
            Button btnChip = new Button(this);
            btnChip.setText(catName);
            btnChip.setTextSize(12);
            btnChip.setAllCaps(false);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dpToPx(36)
            );
            params.setMargins(0, 0, dpToPx(8), 0);
            btnChip.setLayoutParams(params);

            if (catName.equalsIgnoreCase(selectedCategory)) {
                btnChip.setBackgroundResource(R.drawable.bg_chip_selected);
                btnChip.setTextColor(Color.WHITE);
            } else {
                btnChip.setBackgroundResource(R.drawable.bg_chip);
                btnChip.setTextColor(Color.parseColor("#4F46E5"));
            }

            btnChip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedCategory = catName;
                    renderCategoryChips();
                    applyFilterAndRecalculate();
                }
            });

            llCategoriesContainer.addView(btnChip);
        }
    }

    // Mandatory Offline Native Java Logic and Statistics Calculation
    private void applyFilterAndRecalculate() {
        filteredNotes.clear();

        int totalWordsInFiltered = 0;

        for (NoteModel note : allNotes) {
            boolean matchesCategory = selectedCategory.equalsIgnoreCase("All")
                    || note.getCategory().equalsIgnoreCase(selectedCategory);

            boolean matchesSearch = currentSearchQuery.isEmpty()
                    || note.getTitle().toLowerCase().contains(currentSearchQuery.toLowerCase())
                    || note.getContent().toLowerCase().contains(currentSearchQuery.toLowerCase());

            if (matchesCategory && matchesSearch) {
                filteredNotes.add(note);
                // Calculate word count locally
                totalWordsInFiltered += note.getWordCount();
            }
        }

        notesAdapter.notifyDataSetChanged();

        tvEmptyState.setVisibility(filteredNotes.isEmpty() ? View.VISIBLE : View.GONE);

        // Update Native Calculated Dashboard Statistics
        tvStatTotalNotes.setText(String.valueOf(allNotes.size()));
        tvStatFilteredNotes.setText(String.valueOf(filteredNotes.size()));
        tvStatWordCount.setText(String.valueOf(totalWordsInFiltered));
    }

    private void openEditNoteScreen(NoteModel note) {
        Intent intent = new Intent(MainActivity.this, EditNoteActivity.class);
        if (note != null) {
            intent.putExtra(EditNoteActivity.EXTRA_NOTE_ID, note.getId());
            intent.putExtra(EditNoteActivity.EXTRA_TITLE, note.getTitle());
            intent.putExtra(EditNoteActivity.EXTRA_CONTENT, note.getContent());
            intent.putExtra(EditNoteActivity.EXTRA_CATEGORY, note.getCategory());
        }

        // Pass available categories excluding "All"
        ArrayList<String> editCats = new ArrayList<>(categories);
        editCats.remove("All");
        intent.putStringArrayListExtra(EditNoteActivity.EXTRA_CATEGORIES_LIST, editCats);

        startActivityForResult(intent, REQUEST_CODE_EDIT_NOTE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_EDIT_NOTE && resultCode == RESULT_OK) {
            fetchNotesFromBackend();
        }
    }

    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_category, null);
        final EditText etCatName = dialogView.findViewById(R.id.etDialogCategoryName);

        builder.setView(dialogView);
        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                final String catName = etCatName.getText().toString().trim();
                if (!catName.isEmpty()) {
                    if (!categories.contains(catName)) {
                        categories.add(catName);
                        renderCategoryChips();
                    }
                    // Persist category to backend
                    JSONObject data = new JSONObject();
                    try {
                        data.put("name", catName);
                        data.put("color", "#4F46E5");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    api.createData("categories", data, new BackendApi.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            Toast.makeText(MainActivity.this, "Category '" + catName + "' created", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String error) {
                            // Local chip remains added
                        }
                    });
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void deleteNote(final NoteModel note) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete '" + note.getTitle() + "'?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        api.deleteData(note.getId(), new BackendApi.ApiCallback() {
                            @Override
                            public void onSuccess(JSONObject response) {
                                Toast.makeText(MainActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                                allNotes.remove(note);
                                applyFilterAndRecalculate();
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(MainActivity.this, "Failed to delete: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private class NotesAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return filteredNotes.size();
        }

        @Override
        public Object getItem(int position) {
            return filteredNotes.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_note, parent, false);
            }

            final NoteModel note = filteredNotes.get(position);

            TextView tvTitle = convertView.findViewById(R.id.tvNoteTitle);
            TextView tvCategory = convertView.findViewById(R.id.tvNoteCategoryBadge);
            TextView tvSnippet = convertView.findViewById(R.id.tvNoteContentSnippet);
            TextView tvDate = convertView.findViewById(R.id.tvNoteDate);
            TextView tvStats = convertView.findViewById(R.id.tvNoteWordStats);
            Button btnDelete = convertView.findViewById(R.id.btnDeleteNoteItem);

            tvTitle.setText(note.getTitle());
            tvCategory.setText(note.getCategory());
            tvSnippet.setText(note.getContent().isEmpty() ? "(No content)" : note.getContent());
            tvDate.setText(note.getDate());

            // Native offline word and char calculation display per note
            tvStats.setText(note.getWordCount() + " words • " + note.getCharacterCount() + " chars");

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteNote(note);
                }
            });

            return convertView;
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
}