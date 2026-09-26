package com.notescategorized.app;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class CategoryManageActivity extends android.app.Activity {

    private NoteStorage noteStorage;
    private ListView categoriesListView;
    private EditText newCategoryEditText;
    private Button addCategoryButton;

    private List<String> categories;
    private ArrayAdapter<String> categoryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_manage);

        noteStorage = new NoteStorage(this);

        categoriesListView = (ListView) findViewById(R.id.categoriesListView);
        newCategoryEditText = (EditText) findViewById(R.id.newCategoryEditText);
        addCategoryButton = (Button) findViewById(R.id.addCategoryButton);

        registerForContextMenu(categoriesListView);

        addCategoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addCategory();
            }
        });

        loadCategories();
    }

    private void loadCategories() {
        // Filter out "All" as it's not a real category to manage
        List<String> allCategories = noteStorage.loadCategories();
        categories = new ArrayList<String>();
        for (String cat : allCategories) {
            if (!"All".equals(cat)) {
                categories.add(cat);
            }
        }

        categoryAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, categories);
        categoriesListView.setAdapter(categoryAdapter);
    }

    private void addCategory() {
        String categoryName = newCategoryEditText.getText().toString().trim();
        if (categoryName.isEmpty()) {
            Toast.makeText(this, "Category name cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (categories.contains(categoryName)) {
            Toast.makeText(this, "Category already exists.", Toast.LENGTH_SHORT).show();
            return;
        }

        noteStorage.addCategory(categoryName);
        newCategoryEditText.setText("");
        loadCategories(); // Reload and refresh the list
        Toast.makeText(this, "Category added.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.categoriesListView) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            String selectedCategory = categories.get(info.position);
            menu.setHeaderTitle("Category Options: " + selectedCategory);
            // Only allow deletion of user-defined categories, not "Uncategorized"
            if (!"Uncategorized".equals(selectedCategory)) {
                menu.add(0, 0, 0, "Delete");
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final String selectedCategory = categories.get(info.position);

        if (item.getItemId() == 0) { // Delete
            new AlertDialog.Builder(this)
                    .setTitle("Delete Category")
                    .setMessage("Are you sure you want to delete category '" + selectedCategory + "'? Notes in this category will be moved to 'Uncategorized'.")
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            noteStorage.deleteCategory(selectedCategory);
                            Toast.makeText(CategoryManageActivity.this, "Category deleted.", Toast.LENGTH_SHORT).show();
                            loadCategories(); // Reload and refresh the list
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        }
        return super.onContextItemSelected(item);
    }
}