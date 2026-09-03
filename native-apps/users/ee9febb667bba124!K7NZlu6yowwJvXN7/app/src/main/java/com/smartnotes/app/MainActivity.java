package com.smartnotes.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private NoteDbHelper dbHelper;
    private ListView lvNotes;
    private TextView tvEmpty;
    private EditText etSearch;
    private Spinner spinnerFilter;
    private Button btnAdd;

    private List<Note> allNotesList = new ArrayList<>();
    private NoteAdapter adapter;

    private String[] categories = {"All Categories", "Personal", "Work", "Study", "Ideas", "Urgent", "Others"};
    private String currentCategoryFilter = "All Categories";
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new NoteDbHelper(this);

        lvNotes = (ListView) findViewById(R.id.lv_notes);
        tvEmpty = (TextView) findViewById(R.id.tv_empty);
        etSearch = (EditText) findViewById(R.id.et_search);
        spinnerFilter = (Spinner) findViewById(R.id.spinner_filter);
        btnAdd = (Button) findViewById(R.id.btn_add);

        // Spinner Setup
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(spinnerAdapter);

        // Setup Adapter
        adapter = new NoteAdapter(this, new ArrayList<Note>());
        lvNotes.setAdapter(adapter);

        // Search text watcher
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim().toLowerCase();
                filterNotes();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Spinner category selection
        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentCategoryFilter = categories[position];
                filterNotes();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Add Note Action
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NoteActivity.class);
                startActivity(intent);
            }
        });

        // Click Note Action
        lvNotes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Note selectedNote = (Note) adapter.getItem(position);
                Intent intent = new Intent(MainActivity.this, NoteActivity.class);
                intent.putExtra("NOTE_ID", selectedNote.getId());
                intent.putExtra("NOTE_TITLE", selectedNote.getTitle());
                intent.putExtra("NOTE_CONTENT", selectedNote.getContent());
                intent.putExtra("NOTE_CATEGORY", selectedNote.getCategory());
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAllNotes();
    }

    private void loadAllNotes() {
        allNotesList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                NoteDbHelper.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                NoteDbHelper.COLUMN_ID + " DESC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(NoteDbHelper.COLUMN_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(NoteDbHelper.COLUMN_TITLE));
                String content = cursor.getString(cursor.getColumnIndexOrThrow(NoteDbHelper.COLUMN_CONTENT));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(NoteDbHelper.COLUMN_CATEGORY));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(NoteDbHelper.COLUMN_DATE));

                allNotesList.add(new Note(id, title, content, category, date));
            }
            cursor.close();
        }
        filterNotes();
    }

    private void filterNotes() {
        List<Note> filteredList = new ArrayList<>();
        for (int i = 0; i < allNotesList.size(); i++) {
            Note note = allNotesList.get(i);
            boolean matchesCategory = currentCategoryFilter.equals("All Categories") || note.getCategory().equalsIgnoreCase(currentCategoryFilter);
            boolean matchesSearch = note.getTitle().toLowerCase().contains(currentSearchQuery) || note.getContent().toLowerCase().contains(currentSearchQuery);

            if (matchesCategory && matchesSearch) {
                filteredList.add(note);
            }
        }

        adapter.updateList(filteredList);

        if (filteredList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            lvNotes.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            lvNotes.setVisibility(View.VISIBLE);
        }
    }

    // Custom Adapter for Notes Listing
    private class NoteAdapter extends BaseAdapter {
        private Context context;
        private List<Note> notes;

        public NoteAdapter(Context context, List<Note> notes) {
            this.context = context;
            this.notes = notes;
        }

        public void updateList(List<Note> newNotes) {
            this.notes = newNotes;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return notes.size();
        }

        @Override
        public Object getItem(int position) {
            return notes.get(position);
        }

        @Override
        public long getItemId(int position) {
            return notes.get(position).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_note, parent, false);
            }

            Note note = notes.get(position);

            TextView tvTitle = (TextView) convertView.findViewById(R.id.tv_note_title);
            TextView tvCategory = (TextView) convertView.findViewById(R.id.tv_note_category);
            TextView tvSnippet = (TextView) convertView.findViewById(R.id.tv_note_snippet);
            TextView tvDate = (TextView) convertView.findViewById(R.id.tv_note_date);

            tvTitle.setText(note.getTitle());
            tvCategory.setText(note.getCategory());
            tvSnippet.setText(note.getContent());
            tvDate.setText(note.getDate());

            // Dynamically assign pill shapes with categorical colors
            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setShape(GradientDrawable.RECTANGLE);
            badgeBg.setCornerRadius(24);
            badgeBg.setColor(getCategoryColor(note.getCategory()));
            tvCategory.setBackground(badgeBg);

            return convertView;
        }

        private int getCategoryColor(String category) {
            if (category == null) return Color.parseColor("#757575");
            switch (category.toLowerCase()) {
                case "personal":
                    return Color.parseColor("#4CAF50"); // Green
                case "work":
                    return Color.parseColor("#2196F3"); // Blue
                case "study":
                    return Color.parseColor("#9C27B0"); // Purple
                case "ideas":
                    return Color.parseColor("#FF9800"); // Orange
                case "urgent":
                    return Color.parseColor("#F44336"); // Red
                default:
                    return Color.parseColor("#757575"); // Grey
            }
        }
    }
}