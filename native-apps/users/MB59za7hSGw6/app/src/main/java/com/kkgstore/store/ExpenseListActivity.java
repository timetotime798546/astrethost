package com.kkgstore.store;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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

public class ExpenseListActivity extends Activity {

    private DatabaseHelper dbHelper;
    private ListView lvExpenses;
    private List<ExpenseModel> expenseList = new ArrayList<>();
    private ExpenseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DemoLockManager.checkAndEnforceLock(this)) return;

        setContentView(R.layout.activity_expense_list);
        dbHelper = new DatabaseHelper(this);

        lvExpenses = (ListView) findViewById(R.id.lv_expenses);
        Button btnAdd = (Button) findViewById(R.id.btn_add_expense);

        adapter = new ExpenseAdapter(this, expenseList);
        lvExpenses.setAdapter(adapter);

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ExpenseListActivity.this, AddExpenseActivity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DemoLockManager.checkAndEnforceLock(this)) return;
        loadExpenses();
    }

    private void loadExpenses() {
        expenseList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, category, amount, notes, expense_date FROM expenses ORDER BY id DESC", null);
        while (c.moveToNext()) {
            expenseList.add(new ExpenseModel(c.getLong(0), c.getString(1), c.getDouble(2), c.getString(3), c.getString(4)));
        }
        c.close();
        adapter.notifyDataSetChanged();
    }

    private static class ExpenseModel {
        long id; String category, notes, date; double amount;
        ExpenseModel(long id, String category, double amount, String notes, String date) {
            this.id = id; this.category = category; this.amount = amount; this.notes = notes; this.date = date;
        }
    }

    private class ExpenseAdapter extends BaseAdapter {
        private Context context; private List<ExpenseModel> items;
        ExpenseAdapter(Context context, List<ExpenseModel> items) { this.context = context; this.items = items; }
        @Override public int getCount() { return items.size(); }
        @Override public Object getItem(int position) { return items.get(position); }
        @Override public long getItemId(int position) { return items.get(position).id; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_expense, parent, false);
            }
            ExpenseModel m = items.get(position);
            TextView tvCategory = (TextView) convertView.findViewById(R.id.tv_expense_category);
            TextView tvNotes = (TextView) convertView.findViewById(R.id.tv_expense_notes);
            TextView tvDate = (TextView) convertView.findViewById(R.id.tv_expense_date);
            TextView tvAmount = (TextView) convertView.findViewById(R.id.tv_expense_amount);

            tvCategory.setText(m.category);
            tvNotes.setText(m.notes.isEmpty() ? "Shop expense" : m.notes);
            tvDate.setText(m.date);
            tvAmount.setText(String.format("₹ %.2f", m.amount));
            return convertView;
        }
    }
}