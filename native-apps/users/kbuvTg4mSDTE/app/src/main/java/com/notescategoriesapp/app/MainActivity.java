package com.notescategoriesapp.app;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {

    private ListView noteListView;
    private NoteDatabaseHelper db;
    private NoteAdapter noteAdapter;
    private List<Note> currentNotes;

    private static final int REQUEST_CODE_ADD_NOTE = 1;
    private static final int REQUEST_CODE_EDIT_NOTE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        noteListView = (ListView) findViewById(R.id.noteListView);
        db = new NoteDatabaseHelper(this);

        currentNotes = new ArrayList<Note>();
        noteAdapter = new NoteAdapter(this, currentNotes);
        noteListView.setAdapter(noteAdapter);

        noteListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Note selectedNote = (Note) parent.getItemAtPosition(position);
                Intent intent = new Intent(MainActivity.this, NoteDetailActivity.class);
                intent.putExtra("note_id", selectedNote.getId());
                startActivityForResult(intent, REQUEST_CODE_EDIT_NOTE);
            }
        });

        loadNotes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes(); // Refresh the list when returning to MainActivity
    }

    private void loadNotes() {
        currentNotes.clear();
        currentNotes.addAll(db.getAllNotes());
        sortNotesByMostRecent();
        noteAdapter.notifyDataSetChanged();
    }

    private void sortNotesByMostRecent() {
        Collections.sort(currentNotes, new Comparator<Note>() {
            @Override
            public int compare(Note n1, Note n2) {
                // Sort in descending order of timestamp (most recent first)
                return Long.compare(n2.getTimestamp(), n1.getTimestamp());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add_note) {
            Intent intent = new Intent(MainActivity.this, NoteDetailActivity.class);
            startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
            return true;
        } else if (id == R.id.action_search) {
            showSearchDialog();
            return true;
        } else if (id == R.id.action_filter_category) {
            showCategoryFilterDialog();
            return true;
        } else if (id == R.id.action_show_all_notes) {
            loadNotes(); // Show all notes
            Toast.makeText(MainActivity.this, "Showing all notes", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Search Notes");

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_search, null);
        final EditText searchEditText = (EditText) dialogView.findViewById(R.id.search_edit_text);
        builder.setView(dialogView);

        builder.setPositiveButton("Search", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String query = searchEditText.getText().toString();
                if (query.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter a search term", Toast.LENGTH_SHORT).show();
                    return;
                }
                searchNotes(query);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    private void searchNotes(String query) {
        currentNotes.clear();
        currentNotes.addAll(db.searchNotes(query));
        sortNotesByMostRecent();
        noteAdapter.notifyDataSetChanged();
        if (currentNotes.isEmpty()) {
            Toast.makeText(MainActivity.this, "No notes found for '" + query + "'", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this, "Found " + currentNotes.size() + " notes", Toast.LENGTH_SHORT).show();
        }
    }

    private void showCategoryFilterDialog() {
        final List<String> categories = db.getAllCategories();
        if (categories.isEmpty()) {
            Toast.makeText(MainActivity.this, "No categories found", Toast.LENGTH_SHORT).show();
            return;
        }
        // Add "All Categories" option
        categories.add(0, "All Categories");

        final ArrayAdapter<String> categoryAdapter = new ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_1,
            categories
        );

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Filter by Category");
        builder.setAdapter(categoryAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String selectedCategory = categories.get(which);
                if ("All Categories".equals(selectedCategory)) {
                    loadNotes();
                    Toast.makeText(MainActivity.this, "Showing all notes", Toast.LENGTH_SHORT).show();
                } else {
                    filterNotesByCategory(selectedCategory);
                    Toast.makeText(MainActivity.this, "Filtered by '" + selectedCategory + "'", Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    private void filterNotesByCategory(String category) {
        currentNotes.clear();
        currentNotes.addAll(db.getNotesByCategory(category));
        sortNotesByMostRecent();
        noteAdapter.notifyDataSetChanged();
        if (currentNotes.isEmpty()) {
            Toast.makeText(MainActivity.this, "No notes found in category '" + category + "'", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            loadNotes(); // Reload notes if an update or addition occurred
            Toast.makeText(this, "Note saved!", Toast.LENGTH_SHORT).show();
        } else if (resultCode == NoteDetailActivity.RESULT_NOTE_DELETED) {
            loadNotes();
            Toast.makeText(this, "Note deleted!", Toast.LENGTH_SHORT).show();
        }
    }
}