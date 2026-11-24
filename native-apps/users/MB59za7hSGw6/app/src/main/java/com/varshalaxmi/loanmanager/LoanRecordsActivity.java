package com.varshalaxmi.loanmanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LoanRecordsActivity extends Activity {

    private DatabaseHelper dbHelper;
    private ListView lvLoans;
    private LoanAdapter adapter;
    private List<LoanModel> loanList = new ArrayList<>();

    private Button btnActive, btnClosed, btnAll;
    private String currentFilter = "ACTIVE";

    private static class LoanModel {
        long id;
        long customerId;
        String customerName;
        String loanDate;
        String status;
        String method;
        double principal;
        double paid;
        double outstanding;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loan_records);

        dbHelper = new DatabaseHelper(this);
        lvLoans = (ListView) findViewById(R.id.lv_loan_records);

        btnActive = (Button) findViewById(R.id.btn_filter_active);
        btnClosed = (Button) findViewById(R.id.btn_filter_closed);
        btnAll = (Button) findViewById(R.id.btn_filter_all);

        adapter = new LoanAdapter(this, loanList);
        lvLoans.setAdapter(adapter);

        btnActive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentFilter = "ACTIVE";
                updateFilterButtonsUI();
                loadLoans();
            }
        });

        btnClosed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentFilter = "CLOSED";
                updateFilterButtonsUI();
                loadLoans();
            }
        });

        btnAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentFilter = "ALL";
                updateFilterButtonsUI();
                loadLoans();
            }
        });

        lvLoans.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                LoanModel selected = loanList.get(position);
                Intent intent = new Intent(LoanRecordsActivity.this, CustomerDetailActivity.class);
                intent.putExtra("customer_id", selected.customerId);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLoans();
    }

    private void updateFilterButtonsUI() {
        int darkGreen = 0xFF145A32;
        int gold = 0xFFD4AF37;
        int darkText = 0xFF0F4C3A;

        btnActive.setBackgroundColor("ACTIVE".equalsIgnoreCase(currentFilter) ? gold : darkGreen);
        btnActive.setTextColor("ACTIVE".equalsIgnoreCase(currentFilter) ? darkText : 0xFFFFFFFF);

        btnClosed.setBackgroundColor("CLOSED".equalsIgnoreCase(currentFilter) ? gold : darkGreen);
        btnClosed.setTextColor("CLOSED".equalsIgnoreCase(currentFilter) ? darkText : 0xFFFFFFFF);

        btnAll.setBackgroundColor("ALL".equalsIgnoreCase(currentFilter) ? gold : darkGreen);
        btnAll.setTextColor("ALL".equalsIgnoreCase(currentFilter) ? darkText : 0xFFFFFFFF);
    }

    private void loadLoans() {
        loanList.clear();
        Cursor cursor = dbHelper.getAllLoans(currentFilter);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    LoanModel lm = new LoanModel();
                    lm.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                    lm.customerId = cursor.getLong(cursor.getColumnIndexOrThrow("customer_id"));
                    lm.customerName = cursor.getString(cursor.getColumnIndexOrThrow("customer_name"));
                    lm.loanDate = cursor.getString(cursor.getColumnIndexOrThrow("loan_date"));
                    lm.status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
                    lm.method = cursor.getString(cursor.getColumnIndexOrThrow("interest_method"));

                    lm.principal = dbHelper.getTotalPrincipalGivenForLoan(lm.id);
                    double pPaid = dbHelper.getTotalPrincipalPaidForLoan(lm.id);
                    double iPaid = dbHelper.getTotalInterestPaidForLoan(lm.id);
                    lm.paid = pPaid + iPaid;
                    lm.outstanding = dbHelper.getLoanOutstandingAmount(lm.id);

                    loanList.add(lm);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        adapter.notifyDataSetChanged();
    }

    private static class LoanAdapter extends BaseAdapter {
        private Context context;
        private List<LoanModel> items;

        LoanAdapter(Context context, List<LoanModel> items) {
            this.context = context;
            this.items = items;
        }

        @Override
        public int getCount() { return items.size(); }

        @Override
        public Object getItem(int position) { return items.get(position); }

        @Override
        public long getItemId(int position) { return items.get(position).id; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_loan, parent, false);
            }
            LoanModel item = items.get(position);

            TextView tvHeader = (TextView) convertView.findViewById(R.id.tv_item_loan_header);
            TextView tvStatus = (TextView) convertView.findViewById(R.id.tv_item_loan_status);
            TextView tvDate = (TextView) convertView.findViewById(R.id.tv_item_loan_date);
            TextView tvMethod = (TextView) convertView.findViewById(R.id.tv_item_loan_method);
            TextView tvPrincipal = (TextView) convertView.findViewById(R.id.tv_item_loan_principal);
            TextView tvPaid = (TextView) convertView.findViewById(R.id.tv_item_loan_paid);
            TextView tvDue = (TextView) convertView.findViewById(R.id.tv_item_loan_due);

            tvHeader.setText(String.format(Locale.US, "Loan #%d - %s", item.id, item.customerName));
            tvStatus.setText(item.status);
            tvStatus.setTextColor("ACTIVE".equalsIgnoreCase(item.status) ? 0xFF1E8449 : 0xFF7F8C8D);

            tvDate.setText("Issue Date: " + item.loanDate);
            tvMethod.setText("Method: " + item.method);
            tvPrincipal.setText(String.format(Locale.US, "Total Principal Given: ₹%,.2f", item.principal));
            tvPaid.setText(String.format(Locale.US, "Total Collected: ₹%,.2f", item.paid));
            tvDue.setText(String.format(Locale.US, "Outstanding Balance: ₹%,.2f", item.outstanding));

            return convertView;
        }
    }
}