package com.expensetracker.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class ManageCategoriesActivity extends Activity {

    private EditText newCategoryEditText;
    private Button addCategoryButton;
    private ListView categoriesListView;

    private DatabaseHelper dbHelper;
    private SimpleCursorAdapter cursorAdapter;
    private Cursor categoriesCursor;

    private static final int CONTEXT_MENU_EDIT = 1;
    private static final int CONTEXT_MENU_DELETE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_categories);

        dbHelper = new DatabaseHelper(this);

        newCategoryEditText = (EditText) findViewById(R.id.newCategoryEditText);
        addCategoryButton = (Button) findViewById(R.id.addCategoryButton);
        categoriesListView = (ListView) findViewById(R.id.categoriesListView);

        addCategoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addCategory();
            }
        });

        loadCategories();
        registerForContextMenu(categoriesListView);
    }

    private void loadCategories() {
        if (categoriesCursor != null) {
            categoriesCursor.close();
        }
        categoriesCursor = dbHelper.getAllCategories();

        String[] fromColumns = {DatabaseHelper.COLUMN_CATEGORY_NAME};
        int[] toViews = {android.R.id.text1};

        if (cursorAdapter == null) {
            cursorAdapter = new SimpleCursorAdapter(this,
                    android.R.layout.simple_list_item_1,
                    categoriesCursor,
                    fromColumns,
                    toViews,
                    0);
            categoriesListView.setAdapter(cursorAdapter);
        } else {
            cursorAdapter.changeCursor(categoriesCursor);
        }
    }

    private void addCategory() {
        String categoryName = newCategoryEditText.getText().toString().trim();
        if (categoryName.isEmpty()) {
            Toast.makeText(this, \"Category name cannot be empty\", Toast.LENGTH_SHORT).show();
            return;
        }

        long result = dbHelper.addCategory(categoryName);
        if (result > 0) {
            Toast.makeText(this, \"Category added!\", Toast.LENGTH_SHORT).show();
            newCategoryEditText.setText(\"\");
            loadCategories();
        } else if (result == -1) {
            Toast.makeText(this, \"Category already exists.\", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, \"Failed to add category.\", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.categoriesListView) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            menu.setHeaderTitle(\"Category Options\");
            menu.add(0, CONTEXT_MENU_EDIT, 0, \"Edit\");
            menu.add(0, CONTEXT_MENU_DELETE, 1, \"Delete\");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final long categoryId = info.id; // This is the _id from the database

        switch (item.getItemId()) {
            case CONTEXT_MENU_EDIT:
                showEditCategoryDialog(categoryId);
                return true;
            case CONTEXT_MENU_DELETE:
                showDeleteConfirmationDialog(categoryId);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void showEditCategoryDialog(final long categoryId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(\"Edit Category\");

        final EditText input = new EditText(this);
        String currentName = dbHelper.getCategoryName(categoryId);
        input.setText(currentName);
        builder.setView(input);

        builder.setPositiveButton(\"Save\", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newName = input.getText().toString().trim();
                if (!newName.isEmpty()) {
                    int rowsAffected = dbHelper.updateCategory(categoryId, newName);
                    if (rowsAffected > 0) {
                        Toast.makeText(ManageCategoriesActivity.this, \"Category updated!\", Toast.LENGTH_SHORT).show();
                        loadCategories();
                    } else {
                        Toast.makeText(ManageCategoriesActivity.this, \"Failed to update category or no changes.\", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ManageCategoriesActivity.this, \"Category name cannot be empty.\", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton(\"Cancel\", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    private void showDeleteConfirmationDialog(final long categoryId) {
        new AlertDialog.Builder(this)
                .setTitle(\"Delete Category\")
                .setMessage(\"Are you sure you want to delete this category? Transactions associated with this category will have their category cleared.\")
                .setPositiveButton(\"Delete\", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int rowsAffected = dbHelper.deleteCategory(categoryId);
                        if (rowsAffected > 0) {
                            Toast.makeText(ManageCategoriesActivity.this, \"Category deleted!\", Toast.LENGTH_SHORT).show();
                            loadCategories();
                        } else {
                            Toast.makeText(ManageCategoriesActivity.this, \"Failed to delete category.\", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(\"Cancel\", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (categoriesCursor != null) {
            categoriesCursor.close();
        }
        dbHelper.close();
    }
}