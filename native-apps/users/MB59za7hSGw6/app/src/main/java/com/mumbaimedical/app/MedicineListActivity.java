package com.mumbaimedical.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class MedicineListActivity extends Activity {

    private DatabaseHelper dbHelper;
    private ListView lvMedicines;
    private EditText etSearch;
    private Button btnAddMedicine;
    private SimpleCursorAdapter adapter;
    private String currentFilter = "";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicine_list);

        dbHelper = new DatabaseHelper(this);
        lvMedicines = findViewById(R.id.lvMedicines);
        etSearch = findViewById(R.id.etSearch);
        btnAddMedicine = findViewById(R.id.btnAddMedicine);

        if (getIntent().hasExtra("filter")) {
            currentFilter = getIntent().getStringExtra("filter");
        }

        btnAddMedicine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MedicineListActivity.this, AddEditMedicineActivity.class));
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadMedicines(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        lvMedicines.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MedicineListActivity.this, AddEditMedicineActivity.class);
                intent.putExtra("medicine_id", id);
                startActivity(intent);
            }
        });

        lvMedicines.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, final long id) {
                new AlertDialog.Builder(MedicineListActivity.this)
                        .setTitle(R.string.delete)
                        .setMessage("Are you sure you want to delete this medicine record?")
                        .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SQLiteDatabase db = dbHelper.getWritableDatabase();
                                db.delete(DatabaseHelper.TABLE_MEDICINES, "id = ?", new String[]{String.valueOf(id)});
                                Toast.makeText(MedicineListActivity.this, "Medicine deleted", Toast.LENGTH_SHORT).show();
                                loadMedicines(etSearch.getText().toString().trim());
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMedicines(etSearch.getText().toString().trim());
    }

    private void loadMedicines(String query) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT id AS _id, name, company, batch_number, selling_price, quantity, min_stock, exp_date, rack_number FROM " + DatabaseHelper.TABLE_MEDICINES;

        if ("low_stock".equals(currentFilter)) {
            sql += " WHERE quantity <= min_stock";
        } else if (query.length() > 0) {
            sql += " WHERE name LIKE '%" + query + "%' OR batch_number LIKE '%" + query + "%'";
        }

        sql += " ORDER BY name ASC";

        Cursor cursor = db.rawQuery(sql, null);

        String[] from = new String[]{"name", "selling_price", "company", "quantity", "exp_date"};
        int[] to = new int[]{R.id.tvMedName, R.id.tvMedPrice, R.id.tvMedDetails, R.id.tvMedQty, R.id.tvExpiryInfo};

        adapter = new SimpleCursorAdapter(
                this,
                R.layout.item_medicine,
                cursor,
                from,
                to,
                0
        );

        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                int id = view.getId();
                if (id == R.id.tvMedPrice) {
                    double price = cursor.getDouble(cursor.getColumnIndexOrThrow("selling_price"));
                    ((TextView) view).setText(String.format("₹ %.2f", price));
                    return true;
                } else if (id == R.id.tvMedDetails) {
                    String company = cursor.getString(cursor.getColumnIndexOrThrow("company"));
                    String batch = cursor.getString(cursor.getColumnIndexOrThrow("batch_number"));
                    ((TextView) view).setText(company + " | Batch: " + batch);
                    return true;
                } else if (id == R.id.tvMedQty) {
                    int qty = cursor.getInt(cursor.getColumnIndexOrThrow("quantity"));
                    int minStock = cursor.getInt(cursor.getColumnIndexOrThrow("min_stock"));
                    TextView tv = (TextView) view;
                    tv.setText("Qty: " + qty);
                    if (qty <= minStock) {
                        tv.setTextColor(getResources().getColor(R.color.warning_red));
                    } else {
                        tv.setTextColor(getResources().getColor(R.color.success_green));
                    }
                    return true;
                } else if (id == R.id.tvExpiryInfo) {
                    String exp = cursor.getString(cursor.getColumnIndexOrThrow("exp_date"));
                    String rack = cursor.getString(cursor.getColumnIndexOrThrow("rack_number"));
                    ((TextView) view).setText("Exp: " + exp + " | Rack: " + (rack != null ? rack : "-"));
                    return true;
                }
                return false;
            }
        });

        lvMedicines.setAdapter(adapter);
    }
}