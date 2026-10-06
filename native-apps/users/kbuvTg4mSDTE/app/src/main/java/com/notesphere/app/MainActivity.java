package com.notesphere.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int REQUEST_CODE_EDIT_NOTE = 1001;

    private ListView listView;
    private EditText editSearch;
    private Spinner spinnerFilter;
    private TextView textEmptyState;
    private Button btnAddNote;

    private List<Note> allNotes;
    private List<Note> filteredNotes;
    private NoteAdapter adapter;

    private String currentSearchText = "";
    private String currentCategoryFilter = "All Categories";

    private final String[] filterCategories = {"All Categories", "General", "Personal", "Work", "Ideas", "Todo"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = (ListView) findViewById(R.id.list_notes);
        editSearch = (EditText) findViewById(R.id.edit_search);
        spinnerFilter = (Spinner) findViewById(R.id.spinner_category_filter);
        textEmptyState = (TextView) findViewById(R.id.text_empty_state);
        btnAddNote = (Button) findViewById(R.id.button_add_note);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, filterCategories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(spinnerAdapter);

        allNotes = NoteStorage.loadNotes(this);
        filteredNotes = new ArrayList<Note>();

        adapter = new NoteAdapter(this, filteredNotes);
        listView.setAdapter(adapter);

        applyFilterAndSearch();

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchText = s.toString().trim().toLowerCase();
                applyFilterAndSearch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentCategoryFilter = filterCategories[position];
                applyFilterAndSearch();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Note clickedNote = filteredNotes.get(position);
                Intent intent = new Intent(MainActivity.this, EditNoteActivity.class);
                intent.putExtra("note_id", clickedNote.getId());
                startActivityForResult(intent, REQUEST_CODE_EDIT_NOTE);
            }
        });

        btnAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, EditNoteActivity.class);
                startActivityForResult(intent, REQUEST_CODE_EDIT_NOTE);
            }
        });
    }

    private void applyFilterAndSearch() {
        filteredNotes.clear();
        for (int i = 0; i < allNotes.size(); i++) {
            Note note = allNotes.get(i);
            boolean matchesCategory = currentCategoryFilter.equals("All Categories") 
                    || note.getCategory().equalsIgnoreCase(currentCategoryFilter);
            
            boolean matchesSearch = currentSearchText.isEmpty() 
                    || note.getTitle().toLowerCase().contains(currentSearchText)
                    || note.getContent().toLowerCase().contains(currentSearchText);

            if (matchesCategory && matchesSearch) {
                filteredNotes.add(note);
            }
        }

        if (filteredNotes.isEmpty()) {
            textEmptyState.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
        } else {
            textEmptyState.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
        }

        adapter.updateData(filteredNotes);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_EDIT_NOTE && resultCode == RESULT_OK) {
            allNotes = NoteStorage.loadNotes(this);
            applyFilterAndSearch();
        }
    }
}