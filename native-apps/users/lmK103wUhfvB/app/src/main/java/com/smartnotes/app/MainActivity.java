package com.smartnotes.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
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

    private Button btnLogout, btnRefresh, btnAdd;
    private EditText etSearch;
    private Spinner spinnerFilter;
    private ListView lvNotes;
    private TextView tvEmpty, tvStats;

    private BackendApi api;
    private List<Note> allNotesList = new ArrayList<>();
    private List<Note> filteredNotesList = new ArrayList<>();
    private NotesAdapter adapter;
    private ProgressDialog progressDialog;

    private final String[] CATEGORIES = {"All", "Personal", "Work", "Study", "Ideas", "Other"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        api = new BackendApi(this);

        btnLogout = (Button) findViewById(R.id.btn_logout);
        btnRefresh = (Button) findViewById(R.id.btn_refresh);
        btnAdd = (Button) findViewById(R.id.btn_add);
        etSearch = (EditText) findViewById(R.id.et_search);
        spinnerFilter = (Spinner) findViewById(R.id.spinner_category_filter);
        lvNotes = (ListView) findViewById(R.id.lv_notes);
        tvEmpty = (TextView) findViewById(R.id.tv_empty);
        tvStats = (TextView) findViewById(R.id.tv_stats);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Syncing dynamic cloud notes...");
        progressDialog.setCancelable(false);

        // Populate dynamic category spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, CATEGORIES);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(spinnerAdapter);

        adapter = new NotesAdapter(this, filteredNotesList);
        lvNotes.setAdapter(adapter);

        // Click listeners
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog.show();
                api.logout(new BackendApi.ApiCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        progressDialog.dismiss();
                        Toast.makeText(MainActivity.this, "Session terminated securely.", Toast.LENGTH_SHORT).show();
                        launchLogin();
                    }

                    @Override
                    public void onError(String error) {
                        progressDialog.dismiss();
                        launchLogin();
                    }
                });
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchNotesFromCloud();
            }
        });

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NoteEditorActivity.class);
                startActivity(intent);
            }
        });

        // Search trigger
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Spinner dynamic change filter trigger
        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // On Note click (Edit note screen)
        lvNotes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Note selectedNote = filteredNotesList.get(position);
                Intent intent = new Intent(MainActivity.this, NoteEditorActivity.class);
                intent.putExtra("id", selectedNote.getId());
                intent.putExtra("title", selectedNote.getTitle());
                intent.putExtra("content", selectedNote.getContent());
                intent.putExtra("category", selectedNote.getCategory());
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchNotesFromCloud();
    }

    private void fetchNotesFromCloud() {
        progressDialog.show();
        api.readNotes(new BackendApi.ApiCallback<List<Note>>() {
            @Override
            public void onSuccess(List<Note> result) {
                progressDialog.dismiss();
                allNotesList.clear();
                allNotesList.addAll(result);
                applyFilters();
            }

            @Override
            public void onError(String error) {
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, "Failed sync lookup: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    // MANDATORY LOCAL CALCULATIONS FOR OFFLINE BUSINESS LOGIC
    private void calculateAndDisplayStats() {
        int total = allNotesList.size();
        int personal = 0;
        int work = 0;
        int study = 0;
        int ideas = 0;
        int other = 0;

        for (int i = 0; i < allNotesList.size(); i++) {
            Note note = allNotesList.get(i);
            String cat = note.getCategory();
            if ("Personal".equalsIgnoreCase(cat)) {
                personal++;
            } else if ("Work".equalsIgnoreCase(cat)) {
                work++;
            } else if ("Study".equalsIgnoreCase(cat)) {
                study++;
            } else if ("Ideas".equalsIgnoreCase(cat)) {
                ideas++;
            } else {
                other++;
            }
        }

        String statsText = "Stats Count: Total Notes: " + total + " | Personal: " + personal + " | Work: " + work + " | Study: " + study + " | Ideas: " + ideas;
        tvStats.setText(statsText);
    }

    private void applyFilters() {
        String searchQuery = etSearch.getText().toString().toLowerCase().trim();
        String selectedCategory = spinnerFilter.getSelectedItem().toString();

        filteredNotesList.clear();
        for (int i = 0; i < allNotesList.size(); i++) {
            Note note = allNotesList.get(i);
            boolean matchesSearch = note.getTitle().toLowerCase().contains(searchQuery) ||
                    note.getContent().toLowerCase().contains(searchQuery);
            boolean matchesCategory = selectedCategory.equals("All") ||
                    note.getCategory().equalsIgnoreCase(selectedCategory);

            if (matchesSearch && matchesCategory) {
                filteredNotesList.add(note);
            }
        }

        // Perform local business calculations on the data set
        calculateAndDisplayStats();

        adapter.notifyDataSetChanged();

        if (filteredNotesList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void launchLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    // Custom Adapter matching native list
    private static class NotesAdapter extends BaseAdapter {
        private final Context context;
        private final List<Note> notes;
        private final LayoutInflater inflater;

        public NotesAdapter(Context context, List<Note> notes) {
            this.context = context;
            this.notes = notes;
            this.inflater = LayoutInflater.from(context);
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
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.note_list_item, parent, false);
                holder = new ViewHolder();
                holder.tvTitle = (TextView) convertView.findViewById(R.id.tv_item_title);
                holder.tvCategory = (TextView) convertView.findViewById(R.id.tv_item_category);
                holder.tvPreview = (TextView) convertView.findViewById(R.id.tv_item_preview);
                holder.tvDate = (TextView) convertView.findViewById(R.id.tv_item_date);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            Note note = notes.get(position);
            holder.tvTitle.setText(note.getTitle());
            holder.tvCategory.setText(note.getCategory());
            holder.tvPreview.setText(note.getContent());

            // Format timestamp locally inside the app securely
            try {
                long timestamp = Long.parseLong(note.getUpdatedAt());
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                holder.tvDate.setText(sdf.format(new Date(timestamp)));
            } catch (Exception e) {
                holder.tvDate.setText(note.getUpdatedAt());
            }

            // Simple Dynamic Theme categorization visual differences
            if ("Personal".equalsIgnoreCase(note.getCategory())) {
                holder.tvCategory.setBackgroundColor(0xFFC8E6C9); // Light Green
                holder.tvCategory.setTextColor(0xFF1B5E20);
            } else if ("Work".equalsIgnoreCase(note.getCategory())) {
                holder.tvCategory.setBackgroundColor(0xFFFFCDD2); // Light Red
                holder.tvCategory.setTextColor(0xFFB71C1C);
            } else if ("Study".equalsIgnoreCase(note.getCategory())) {
                holder.tvCategory.setBackgroundColor(0xFFBBDEFB); // Light Blue
                holder.tvCategory.setTextColor(0xFF0D47A1);
            } else if ("Ideas".equalsIgnoreCase(note.getCategory())) {
                holder.tvCategory.setBackgroundColor(0xFFFFF9C4); // Yellow
                holder.tvCategory.setTextColor(0xFFF57F17);
            } else {
                holder.tvCategory.setBackgroundColor(0xFFE1BEE7); // Purple
                holder.tvCategory.setTextColor(0xFF4A148C);
            }

            return convertView;
        }

        private static class ViewHolder {
            TextView tvTitle;
            TextView tvCategory;
            TextView tvPreview;
            TextView tvDate;
        }
    }
}