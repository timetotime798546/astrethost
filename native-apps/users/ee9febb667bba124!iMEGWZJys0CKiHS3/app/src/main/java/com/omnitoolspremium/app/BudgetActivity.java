package com.omnitoolspremium.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class BudgetActivity extends Activity {

    private TextView txtTotalBudget;
    private ListView listExpenses;
    private DatabaseHelper dbHelper;
    private ExpenseAdapter adapter;
    private List<ExpenseItem> expenseList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget);

        txtTotalBudget = (TextView) findViewById(R.id.txt_total_budget);
        listExpenses = (ListView) findViewById(R.id.list_expenses);
        Button btnAddExpense = (Button) findViewById(R.id.btn_add_expense);

        dbHelper = new DatabaseHelper(this);
        expenseList = new ArrayList<ExpenseItem>();
        adapter = new ExpenseAdapter(this, expenseList);
        listExpenses.setAdapter(adapter);

        btnAddExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(BudgetActivity.this, ExpenseAddActivity.class), 101);
            }
        });

        loadExpenses();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            loadExpenses();
        }
    }

    private void loadExpenses() {
        expenseList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("expenses", null, null, null, null, null, "id DESC");

        double total = 0;
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount"));
                String category = cursor.getString(cursor.getColumnIndexOrThrow("category"));
                
                expenseList.add(new ExpenseItem(id, title, amount, category));
                total += amount;
            }
            cursor.close();
        }

        txtTotalBudget.setText(String.format("$%.2f", total));
        adapter.notifyDataSetChanged();
    }

    public static class ExpenseItem {
        int id;
        String title;
        double amount;
        String category;

        public ExpenseItem(int id, String title, double amount, String category) {
            this.id = id;
            this.title = title;
            this.amount = amount;
            this.category = category;
        }
    }

    private class ExpenseAdapter extends BaseAdapter {
        private Context context;
        private List<ExpenseItem> items;

        public ExpenseAdapter(Context context, List<ExpenseItem> items) {
            this.context = context;
            this.items = items;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return items.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false);
            }

            ExpenseItem item = items.get(position);
            TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
            TextView text2 = (TextView) convertView.findViewById(android.R.id.text2);

            text1.setText(item.title + " (" + item.category + ")");
            text1.setTextColor(0xFF2C3E50);
            text1.setTextSize(16sp);

            text2.setText(String.format("-$%.2f", item.amount));
            text2.setTextColor(0xFFE74C3C);
            text2.setTextSize(14sp);

            return convertView;
        }
    }

    public static class DatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "budget_db";
        private static final int DATABASE_VERSION = 1;

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE expenses (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, amount REAL, category TEXT)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS expenses");
            onCreate(db);
        }
    }
}