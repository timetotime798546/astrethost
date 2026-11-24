package com.alfariskirana.app;

import android.app.Activity;
import android.content.Intent;
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
import java.util.Locale;

public class ExpenseListActivity extends Activity {

    private DatabaseHelper dbHelper;
    private ListView lvExpenses;
    private List<Expense> expenseList = new ArrayList<Expense>();
    private ExpenseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_list);

        dbHelper = new DatabaseHelper(this);
        lvExpenses = (ListView) findViewById(R.id.lv_expenses);
        Button btnAdd = (Button) findViewById(R.id.btn_add_expense);

        adapter = new ExpenseAdapter();
        lvExpenses.setAdapter(adapter);

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ExpenseListActivity.this, AddExpenseActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        expenseList = dbHelper.getAllExpenses();
        adapter.notifyDataSetChanged();
    }

    private class ExpenseAdapter extends BaseAdapter {
        @Override
        public int getCount() { return expenseList.size(); }

        @Override
        public Object getItem(int position) { return expenseList.get(position); }

        @Override
        public long getItemId(int position) { return expenseList.get(position).getId(); }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(ExpenseListActivity.this).inflate(R.layout.item_expense, parent, false);
            }

            Expense e = expenseList.get(position);
            TextView tvCat = (TextView) convertView.findViewById(R.id.tv_expense_category);
            TextView tvNotes = (TextView) convertView.findViewById(R.id.tv_expense_notes);
            TextView tvDate = (TextView) convertView.findViewById(R.id.tv_expense_date);
            TextView tvAmt = (TextView) convertView.findViewById(R.id.tv_expense_amount);

            tvCat.setText(e.getCategory());
            tvNotes.setText(e.getNotes() == null || e.getNotes().isEmpty() ? "No notes" : e.getNotes());
            tvDate.setText(e.getExpenseDate());
            tvAmt.setText(String.format(Locale.getDefault(), "₹ %.2f", e.getAmount()));

            return convertView;
        }
    }
}