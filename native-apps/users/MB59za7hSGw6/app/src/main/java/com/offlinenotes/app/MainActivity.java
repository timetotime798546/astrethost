package com.offlinenotes.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends Activity {
    private EditText etSearch;
    private TextView tvStats;
    private ListView lvNotes;
    private LinearLayout llEmpty;
    private Button fabAdd;

    private TextView tabAll;
    private TextView tabPersonal;
    private TextView tabWork;
    private TextView tabIdeas;
    private TextView tabTodo;
    private TextView tabStudy;

    private NotesManager notesManager;
    private NoteAdapter noteAdapter;
    private String selectedCategory = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        notesManager = new NotesManager(this);

        etSearch = (EditText) findViewById(R.id.et_search);
        tvStats = (TextView) findViewById(R.id.tv_stats);
        lvNotes = (ListView) findViewById(R.id.lv_notes);
        llEmpty = (LinearLayout) findViewById(R.id.ll_empty);
        fabAdd = (Button) findViewById(R.id.fab_add);

        tabAll = (TextView) findViewById(R.id.tab_all);
        tabPersonal = (TextView) findViewById(R.id.tab_personal);
        tabWork = (TextView) findViewById(R.id.tab_work);
        tabIdeas = (TextView) findViewById(R.id.tab_ideas);
        tabTodo = (TextView) findViewById(R.id.tab_todo);
        tabStudy = (TextView) findViewById(R.id.tab_study);

        setupCategoryTabs();

        noteAdapter = new NoteAdapter(this, notesManager.getAllNotes());
        lvNotes.setAdapter(noteAdapter);

        lvNotes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Note clickedNote = (Note) noteAdapter.getItem(position);
                Intent intent = new Intent(MainActivity.this, NoteEditorActivity.class);
                intent.putExtra("note_id", clickedNote.getId());
                startActivity(intent);
            }
        });

        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NoteEditorActivity.class);
                startActivity(intent);
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshNotesList();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshNotesList();
    }

    private void setupCategoryTabs() {
        tabAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectCategory("All", tabAll);
            }
        });
        tabPersonal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectCategory("Personal", tabPersonal);
            }
        });
        tabWork.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectCategory("Work", tabWork);
            }
        });
        tabIdeas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectCategory("Ideas", tabIdeas);
            }
        });
        tabTodo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectCategory("Tasks", tabTodo);
            }
        });
        tabStudy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectCategory("Study", tabStudy);
            }
        });
    }

    private void selectCategory(String category, TextView selectedView) {
        this.selectedCategory = category;

        // Reset all tabs
        resetTabStyle(tabAll);
        resetTabStyle(tabPersonal);
        resetTabStyle(tabWork);
        resetTabStyle(tabIdeas);
        resetTabStyle(tabTodo);
        resetTabStyle(tabStudy);

        // Highlight selected tab
        highlightTab(selectedView);

        refreshNotesList();
    }

    private void resetTabStyle(TextView tv) {
        tv.setBackgroundResource(R.drawable.chip_bg);
        tv.setTextColor(Color.parseColor("#64748B"));
    }

    private void highlightTab(TextView tv) {
        tv.setBackgroundResource(R.drawable.btn_primary);
        tv.setTextColor(Color.parseColor("#FFFFFF"));
    }

    private void refreshNotesList() {
        List<Note> allNotes = notesManager.getAllNotes();
        String query = etSearch.getText().toString();

        List<Note> filtered = notesManager.filterNotes(allNotes, query, selectedCategory);
        noteAdapter.updateList(filtered);

        // Mandatory offline calculations: count notes & total words
        int totalNotes = allNotes.size();
        int totalWords = notesManager.calculateTotalWords(allNotes);
        tvStats.setText(totalNotes + (totalNotes == 1 ? " note" : " notes") + " • " + totalWords + " words");

        if (filtered.isEmpty()) {
            llEmpty.setVisibility(View.VISIBLE);
            lvNotes.setVisibility(View.GONE);
        } else {
            llEmpty.setVisibility(View.GONE);
            lvNotes.setVisibility(View.VISIBLE);
        }
    }
}