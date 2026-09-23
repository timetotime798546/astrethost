package com.notewise.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
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
    private List<Note> notesList;
    private List<Category> categoriesList;
    private NoteAdapter noteAdapter;

    private ListView listViewNotes;
    private TextView tvEmptyNotes;
    private EditText etSearch;
    private Button btnClearSearch;
    private LinearLayout layoutCategoryFilters;

    private String currentSearchQuery = "";
    private long selectedCategoryId = -1; // -1 = ALL

    private final int[] noteColors = {
            Color.parseColor("#FFFFFF"), // 0 - White
            Color.parseColor("#FFD1DC"), // 1 - Pink
            Color.parseColor("#FFE5B4"), // 2 - Peach
            Color.parseColor("#FFF9A6"), // 3 - Yellow
            Color.parseColor("#C1E1C1"), // 4 - Green
            Color.parseColor("#AEC6CF"), // 5 - Blue
            Color.parseColor("#D6CADD")  // 6 - Purple
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        notesList = new ArrayList<>();
        categoriesList = new ArrayList<>();

        listViewNotes = (ListView) findViewById(R.id.listViewNotes);
        tvEmptyNotes = (TextView) findViewById(R.id.tvEmptyNotes);
        etSearch = (EditText) findViewById(R.id.etSearch);
        btnClearSearch = (Button) findViewById(R.id.btnClearSearch);
        layoutCategoryFilters = (LinearLayout) findViewById(R.id.layoutCategoryFilters);

        Button btnAddNote = (Button) findViewById(R.id.btnAddNote);
        Button btnManageCategories = (Button) findViewById(R.id.btnManageCategories);

        noteAdapter = new NoteAdapter(this, notesList);
        listViewNotes.setAdapter(noteAdapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                if (currentSearchQuery.length() > 0) {
                    btnClearSearch.setVisibility(View.VISIBLE);
                } else {
                    btnClearSearch.setVisibility(View.GONE);
                }
                refreshNotes();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etSearch.setText("");
            }
        });

        btnAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNoteDialog(null);
            }
        });

        btnManageCategories.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCategoryManagerDialog();
            }
        });

        refreshCategories();
        refreshNotes();
    }

    private void refreshNotes() {
        notesList.clear();
        notesList.addAll(dbHelper.getNotes(currentSearchQuery, selectedCategoryId));
        noteAdapter.notifyDataSetChanged();

        if (notesList.isEmpty()) {
            tvEmptyNotes.setVisibility(View.VISIBLE);
        } else {
            tvEmptyNotes.setVisibility(View.GONE);
        }
    }

    private void refreshCategories() {
        categoriesList.clear();
        categoriesList.addAll(dbHelper.getAllCategories());
        renderCategoryFilterBar();
    }

    private void renderCategoryFilterBar() {
        layoutCategoryFilters.removeAllViews();

        // Standard All Filter Chip
        addFilterTag(-1, "All");

        // Uncategorized Option Chip
        addFilterTag(0, "Uncategorized");

        for (int i = 0; i < categoriesList.size(); i++) {
            Category cat = categoriesList.get(i);
            addFilterTag(cat.getId(), cat.getName());
        }
    }

    private void addFilterTag(final long categoryId, String label) {
        final Button tagBtn = new Button(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 16, 0);
        tagBtn.setLayoutParams(params);
        tagBtn.setPadding(30, 16, 30, 16);
        tagBtn.setText(label);
        tagBtn.setTextSize(13.0f);
        tagBtn.setAllCaps(false);

        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(30);
        if (selectedCategoryId == categoryId) {
            gd.setColor(Color.parseColor("#1ABC9C"));
            tagBtn.setTextColor(Color.WHITE);
        } else {
            gd.setColor(Color.parseColor("#E2E8F0"));
            tagBtn.setTextColor(Color.parseColor("#4A5568"));
        }
        tagBtn.setBackground(gd);

        tagBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedCategoryId = categoryId;
                renderCategoryFilterBar();
                refreshNotes();
            }
        });

        layoutCategoryFilters.addView(tagBtn);
    }

    private void showNoteDialog(final Note noteToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_note, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        TextView tvDialogTitle = (TextView) dialogView.findViewById(R.id.tvDialogTitle);
        final EditText etNoteTitle = (EditText) dialogView.findViewById(R.id.etNoteTitle);
        final EditText etNoteContent = (EditText) dialogView.findViewById(R.id.etNoteContent);
        final Spinner spinnerCategory = (Spinner) dialogView.findViewById(R.id.spinnerCategory);
        Button btnSaveNote = (Button) dialogView.findViewById(R.id.btnSaveNote);
        Button btnCancelNote = (Button) dialogView.findViewById(R.id.btnCancelNote);

        final List<Category> spinnerList = new ArrayList<>();
        spinnerList.add(new Category(0, "Uncategorized"));
        spinnerList.addAll(dbHelper.getAllCategories());

        ArrayAdapter<Category> spinnerAdapter = new ArrayAdapter<Category>(
                this,
                android.R.layout.simple_spinner_item,
                spinnerList
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(Color.parseColor("#2C3E50"));
                view.setTextSize(14.0f);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setTextColor(Color.parseColor("#2C3E50"));
                view.setPadding(20, 20, 20, 20);
                return view;
            }
        };
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(spinnerAdapter);

        final int[] activeColorIndex = {0};

        final Button[] colorButtons = new Button[7];
        colorButtons[0] = (Button) dialogView.findViewById(R.id.color0);
        colorButtons[1] = (Button) dialogView.findViewById(R.id.color1);
        colorButtons[2] = (Button) dialogView.findViewById(R.id.color2);
        colorButtons[3] = (Button) dialogView.findViewById(R.id.color3);
        colorButtons[4] = (Button) dialogView.findViewById(R.id.color4);
        colorButtons[5] = (Button) dialogView.findViewById(R.id.color5);
        colorButtons[6] = (Button) dialogView.findViewById(R.id.color6);

        for (int i = 0; i < 7; i++) {
            final int index = i;
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(noteColors[i]);
            circle.setStroke(2, Color.parseColor("#BDC3C7"));
            colorButtons[i].setBackground(circle);

            colorButtons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activeColorIndex[0] = index;
                    highlightColorSelection(colorButtons, index);
                }
            });
        }

        if (noteToEdit != null) {
            tvDialogTitle.setText("Edit Note");
            etNoteTitle.setText(noteToEdit.getTitle());
            etNoteContent.setText(noteToEdit.getContent());

            activeColorIndex[0] = noteToEdit.getColorIndex();
            highlightColorSelection(colorButtons, activeColorIndex[0]);

            for (int i = 0; i < spinnerList.size(); i++) {
                if (spinnerList.get(i).getId() == noteToEdit.getCategoryId()) {
                    spinnerCategory.setSelection(i);
                    break;
                }
            }
        } else {
            tvDialogTitle.setText("Create Note");
            highlightColorSelection(colorButtons, 0);
        }

        btnCancelNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnSaveNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = etNoteTitle.getText().toString().trim();
                String content = etNoteContent.getText().toString().trim();
                Category selectedCat = (Category) spinnerCategory.getSelectedItem();

                if (title.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                long categoryId = (selectedCat != null) ? selectedCat.getId() : 0;

                if (noteToEdit != null) {
                    dbHelper.updateNote(noteToEdit.getId(), title, content, categoryId, activeColorIndex[0]);
                    Toast.makeText(MainActivity.this, "Note updated", Toast.LENGTH_SHORT).show();
                } else {
                    dbHelper.insertNote(title, content, categoryId, activeColorIndex[0]);
                    Toast.makeText(MainActivity.this, "Note saved", Toast.LENGTH_SHORT).show();
                }

                refreshNotes();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void highlightColorSelection(Button[] buttons, int activeIndex) {
        for (int i = 0; i < buttons.length; i++) {
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(noteColors[i]);
            if (i == activeIndex) {
                circle.setStroke(6, Color.parseColor("#2C3E50"));
            } else {
                circle.setStroke(2, Color.parseColor("#BDC3C7"));
            }
            buttons[i].setBackground(circle);
        }
    }

    private void showCategoryManagerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_categories, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        final EditText etNewCategory = (EditText) dialogView.findViewById(R.id.etNewCategory);
        Button btnAddCategory = (Button) dialogView.findViewById(R.id.btnAddCategory);
        Button btnCloseCategories = (Button) dialogView.findViewById(R.id.btnCloseCategories);
        final LinearLayout layoutCategoryList = (LinearLayout) dialogView.findViewById(R.id.layoutCategoryList);

        final Runnable reloadList = new Runnable() {
            @Override
            public void run() {
                layoutCategoryList.removeAllViews();
                final List<Category> cats = dbHelper.getAllCategories();
                final Runnable outerReload = this;

                for (int i = 0; i < cats.size(); i++) {
                    final Category category = cats.get(i);
                    View itemView = LayoutInflater.from(MainActivity.this).inflate(R.layout.category_item, null);

                    TextView tvName = (TextView) itemView.findViewById(R.id.tvCategoryName);
                    Button btnDel = (Button) itemView.findViewById(R.id.btnDeleteCategory);

                    tvName.setText(category.getName());

                    btnDel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Delete Category")
                                    .setMessage("Are you sure you want to delete '" + category.getName() + "'? Notes in this category will become Uncategorized.")
                                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface d, int which) {
                                            dbHelper.deleteCategory(category.getId());
                                            Toast.makeText(MainActivity.this, "Category deleted", Toast.LENGTH_SHORT).show();
                                            refreshCategories();
                                            refreshNotes();
                                            outerReload.run();
                                        }
                                    })
                                    .setNegativeButton("No", null)
                                    .show();
                        }
                    });
                    layoutCategoryList.addView(itemView);
                }
            }
        };

        reloadList.run();

        btnAddCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = etNewCategory.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Enter a valid category name", Toast.LENGTH_SHORT).show();
                    return;
                }

                long res = dbHelper.insertCategory(name);
                if (res == -1) {
                    Toast.makeText(MainActivity.this, "Category already exists", Toast.LENGTH_SHORT).show();
                } else {
                    etNewCategory.setText("");
                    Toast.makeText(MainActivity.this, "Category added", Toast.LENGTH_SHORT).show();
                    refreshCategories();
                    refreshNotes();
                    reloadList.run();
                }
            }
        });

        btnCloseCategories.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private class NoteAdapter extends BaseAdapter {
        private Context context;
        private List<Note> list;

        public NoteAdapter(Context context, List<Note> list) {
            this.context = context;
            this.list = list;
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

            LinearLayout noteCard = (LinearLayout) convertView.findViewById(R.id.noteCard);
            TextView tvTitle = (TextView) convertView.findViewById(R.id.tvNoteTitle);
            TextView tvCategory = (TextView) convertView.findViewById(R.id.tvNoteCategory);
            TextView tvContent = (TextView) convertView.findViewById(R.id.tvNoteContent);
            TextView tvDate = (TextView) convertView.findViewById(R.id.tvNoteDate);
            Button btnEdit = (Button) convertView.findViewById(R.id.btnEditNote);
            Button btnDelete = (Button) convertView.findViewById(R.id.btnDeleteNote);

            tvTitle.setText(note.getTitle());
            tvContent.setText(note.getContent());
            tvCategory.setText(note.getCategoryName());

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());
            String dateString = sdf.format(new Date(note.getTimestamp()));
            tvDate.setText(dateString);

            int colorIndex = note.getColorIndex();
            if (colorIndex < 0 || colorIndex >= noteColors.length) {
                colorIndex = 0;
            }

            GradientDrawable drawable = new GradientDrawable();
            drawable.setColor(noteColors[colorIndex]);
            drawable.setCornerRadius(12);
            drawable.setStroke(1, Color.parseColor("#E2E8F0"));
            noteCard.setBackground(drawable);

            btnEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showNoteDialog(note);
                }
            });

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(context)
                            .setTitle("Delete Note")
                            .setMessage("Are you sure you want to delete '" + note.getTitle() + "'?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dbHelper.deleteNote(note.getId());
                                    Toast.makeText(context, "Note deleted successfully", Toast.LENGTH_SHORT).show();
                                    refreshNotes();
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