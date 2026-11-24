package com.varshalaxmi.loanmanager;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PaymentHistoryActivity extends Activity {

    private DatabaseHelper dbHelper;
    private ListView lvPayments;
    private EditText etFilterDate;
    private PaymentAdapter adapter;
    private List<PaymentModel> paymentList = new ArrayList<>();

    private static class PaymentModel {
        long id;
        long customerId;
        long loanId;
        String customerName;
        String paymentDate;
        double amount;
        double principalPaid;
        double interestPaid;
        String method;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_history);

        dbHelper = new DatabaseHelper(this);
        lvPayments = (ListView) findViewById(R.id.lv_payment_history);
        etFilterDate = (EditText) findViewById(R.id.et_filter_payment_date);
        Button btnClear = (Button) findViewById(R.id.btn_clear_payment_filter);

        adapter = new PaymentAdapter(this, paymentList);
        lvPayments.setAdapter(adapter);

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etFilterDate.setText("");
                loadPayments("");
            }
        });

        etFilterDate.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadPayments(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPayments(etFilterDate.getText().toString().trim());
    }

    private void loadPayments(String filterDate) {
        paymentList.clear();
        Cursor cursor;
        if (filterDate.isEmpty()) {
            cursor = dbHelper.getAllPayments();
        } else {
            cursor = dbHelper.getPaymentsByDate(filterDate);
        }

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    PaymentModel pm = new PaymentModel();
                    pm.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                    pm.customerId = cursor.getLong(cursor.getColumnIndexOrThrow("customer_id"));
                    pm.loanId = cursor.getLong(cursor.getColumnIndexOrThrow("loan_id"));
                    pm.customerName = cursor.getString(cursor.getColumnIndexOrThrow("customer_name"));
                    pm.paymentDate = cursor.getString(cursor.getColumnIndexOrThrow("payment_date"));
                    pm.amount = cursor.getDouble(cursor.getColumnIndexOrThrow("payment_amount"));
                    pm.principalPaid = cursor.getDouble(cursor.getColumnIndexOrThrow("principal_paid"));
                    pm.interestPaid = cursor.getDouble(cursor.getColumnIndexOrThrow("interest_paid"));
                    pm.method = cursor.getString(cursor.getColumnIndexOrThrow("payment_method"));

                    paymentList.add(pm);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        adapter.notifyDataSetChanged();
    }

    private static class PaymentAdapter extends BaseAdapter {
        private Context context;
        private List<PaymentModel> items;

        PaymentAdapter(Context context, List<PaymentModel> items) {
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
                convertView = LayoutInflater.from(context).inflate(R.layout.item_payment, parent, false);
            }
            PaymentModel item = items.get(position);

            TextView tvCust = (TextView) convertView.findViewById(R.id.tv_item_pay_customer);
            TextView tvAmount = (TextView) convertView.findViewById(R.id.tv_item_pay_amount);
            TextView tvDetails = (TextView) convertView.findViewById(R.id.tv_item_pay_details);
            TextView tvDate = (TextView) convertView.findViewById(R.id.tv_item_pay_date);

            tvCust.setText(item.customerName);
            tvAmount.setText(String.format(Locale.US, "₹%,.2f", item.amount));
            tvDetails.setText(String.format(Locale.US, "Loan #%d (%s) | Principal: ₹%,.2f, Interest: ₹%,.2f",
                    item.loanId, item.method != null ? item.method : "Cash", item.principalPaid, item.interestPaid));
            tvDate.setText(item.paymentDate);

            return convertView;
        }
    }
}