package com.cloudnotes.app;

import android.app.Activity;
import android.content.Intent;
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
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private TextView tvStatTotalNotes;
    private TextView tvStatTotalChars;
    private EditText etSearch;
    private ListView lvNotes;
    private TextView tvEmpty;
    private ProgressBar pbLoading;
    private Button btnAddNote;
    private Button btnLogout;

    private BackendApi backendApi;
    private List<Note> allNotes = new ArrayList<>();
    private List<Note> filteredNotes = new ArrayList<>();
    private String selectedCategory = "All";
    private String searchQuery = "";
    private NoteAdapter adapter;

    private TextView tabAll, tabWork, tabPersonal, tabIdeas, tabTodo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(android.R.style.Theme_Material_Light_NoActionBar);
        setContentView(R.layout.activity_main);

        backendApi = new BackendApi(this);

        if (backendApi.getToken() == null || backendApi.getToken().isEmpty()) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        tvStatTotalNotes = (TextView) findViewById(R.id.tv_stat_total_notes);
        tvStatTotalChars = (TextView) findViewById(R.id.tv_stat_total_chars);
        etSearch = (EditText) findViewById(R.id.et_search);
        lvNotes = (ListView) findViewById(R.id.lv_notes);
        tvEmpty = (TextView) findViewById(R.id.tv_empty);
        pbLoading = (ProgressBar) findViewById(R.id.pb_notes_loading);
        btnAddNote = (Button) findViewById(R.id.btn_add_note);
        btnLogout = (Button) findViewById(R.id.btn_logout);

        tabAll = (TextView) findViewById(R.id.tab_all);
        tabWork = (TextView) findViewById(R.id.tab_work);
        tabPersonal = (TextView) findViewById(R.id.tab_personal);
        tabIdeas = (TextView) findViewById(R.id.tab_ideas);
        tabTodo = (TextView) findViewById(R.id.tab_todo);

        adapter = new NoteAdapter();
        lvNotes.setAdapter(adapter);

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });

        btnAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NoteActivity.class);
                startActivity(intent);
            }
        });

        lvNotes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Note clickedNote = filteredNotes.get(position);
                Intent intent = new Intent(MainActivity.this, NoteActivity.class);
                intent.putExtra("note", clickedNote);
                startActivity(intent);
            }
        });

        setupCategoryTabs();
        setupSearchBox();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes();
    }

    private void logout() {
        backendApi.clearToken();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }

    private void loadNotes() {
        pbLoading.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        backendApi.getNotes(new BackendApi.ApiCallback<List<Note>>() {
            @Override
            public void onSuccess(List<Note> notes) {
                pbLoading.setVisibility(View.GONE);
                allNotes.clear();
                allNotes.addAll(notes);

                calculateOfflineStatistics();
                applyFilter();
            }

            @Override
            public void onError(String error) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Failed to load notes: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void calculateOfflineStatistics() {
        int notesCount = allNotes.size();
        long totalChars = 0;
        for (int i = 0; i < allNotes.size(); i++) {
            Note n = allNotes.get(i);
            if (n.content != null) {
                totalChars += n.content.length();
            }
            if (n.title != null) {
                totalChars += n.title.length();
            }
        }
        tvStatTotalNotes.setText(String.valueOf(notesCount));
        tvStatTotalChars.setText(String.valueOf(totalChars));
    }

    private void applyFilter() {
        filteredNotes.clear();
        for (int i = 0; i < allNotes.size(); i++) {
            Note note = allNotes.get(i);

            boolean matchesCategory = selectedCategory.equals("All") ||
                    (note.category != null && note.category.equalsIgnoreCase(selectedCategory));

            boolean matchesSearch = searchQuery.isEmpty() ||
                    (note.title != null && note.title.toLowerCase().contains(searchQuery)) ||
                    (note.content != null && note.content.toLowerCase().contains(searchQuery));

            if (matchesCategory && matchesSearch) {
                filteredNotes.add(note);
            }
        }

        adapter.notifyDataSetChanged();

        if (filteredNotes.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void setupSearchBox() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase();
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupCategoryTabs() {
        View.OnClickListener tabClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetTabStyle(tabAll);
                resetTabStyle(tabWork);
                resetTabStyle(tabPersonal);
                resetTabStyle(tabIdeas);
                resetTabStyle(tabTodo);

                TextView selectedTab = (TextView) v;
                selectedTab.setBackgroundColor(0xFF2196F3);
                selectedTab.setTextColor(0xFFFFFFFF);

                selectedCategory = selectedTab.getText().toString();
                applyFilter();
            }
        };

        tabAll.setOnClickListener(tabClickListener);
        tabWork.setOnClickListener(tabClickListener);
        tabPersonal.setOnClickListener(tabClickListener);
        tabIdeas.setOnClickListener(tabClickListener);
        tabTodo.setOnClickListener(tabClickListener);
    }

    private void resetTabStyle(TextView tv) {
        tv.setBackgroundColor(0xFFE0E0E0);
        tv.setTextColor(0xFF555555);
    }

    private class NoteAdapter extends BaseAdapter {
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

            TextView tvTitle = (TextView) convertView.findViewById(R.id.tv_item_title);
            TextView tvCategory = (TextView) convertView.findViewById(R.id.tv_item_category);
            TextView tvContent = (TextView) convertView.findViewById(R.id.tv_item_content);

            Note note = filteredNotes.get(position);

            tvTitle.setText(note.title != null && !note.title.isEmpty() ? note.title : "Untitled Note");
            tvContent.setText(note.content != null ? note.content : "");

            String category = note.category != null ? note.category : "General";
            tvCategory.setText(category);

            if (category.equalsIgnoreCase("Work")) {
                tvCategory.setBackgroundColor(0xFF4CAF50);
            } else if (category.equalsIgnoreCase("Personal")) {
                tvCategory.setBackgroundColor(0xFFFF9800);
            } else if (category.equalsIgnoreCase("Ideas")) {
                tvCategory.setBackgroundColor(0xFF9C27B0);
            } else if (category.equalsIgnoreCase("Todo")) {
                tvCategory.setBackgroundColor(0xFFE91E63);
            } else {
                tvCategory.setBackgroundColor(0xFF2196F3);
            }

            return convertView;
        }
    }
}