package com.alfariskirana.app;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CustomerDetailActivity extends Activity {

    private DatabaseHelper dbHelper;
    private long customerId = -1;
    private Customer customer;

    private TextView tvName;
    private TextView tvMobile;
    private TextView tvBalance;
    private EditText etPaymentAmount;
    private EditText etPaymentNotes;
    private ListView lvLedger;

    private List<LedgerItem> ledgerList = new ArrayList<LedgerItem>();
    private LedgerAdapter adapter;

    private static class LedgerItem {
        String type;
        double amount;
        String date;
        String notes;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_detail);

        dbHelper = new DatabaseHelper(this);
        customerId = getIntent().getLongExtra("customer_id", -1);

        tvName = (TextView) findViewById(R.id.tv_detail_name);
        tvMobile = (TextView) findViewById(R.id.tv_detail_mobile);
        tvBalance = (TextView) findViewById(R.id.tv_detail_balance);
        etPaymentAmount = (EditText) findViewById(R.id.et_payment_amount);
        etPaymentNotes = (EditText) findViewById(R.id.et_payment_notes);
        lvLedger = (ListView) findViewById(R.id.lv_ledger);
        Button btnReceive = (Button) findViewById(R.id.btn_record_payment);

        adapter = new LedgerAdapter();
        lvLedger.setAdapter(adapter);

        btnReceive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recordPayment();
            }
        });

        loadCustomerDetails();
    }

    private void loadCustomerDetails() {
        customer = dbHelper.getCustomerById(customerId);
        if (customer != null) {
            tvName.setText(customer.getName());
            tvMobile.setText("Mobile: " + (customer.getMobile() == null || customer.getMobile().isEmpty() ? "N/A" : customer.getMobile()));
            tvBalance.setText(String.format(Locale.getDefault(), "₹ %.2f", customer.getPendingBalance()));
            loadLedger();
        }
    }

    private void loadLedger() {
        ledgerList.clear();
        Cursor c = dbHelper.getCustomerLedger(customerId);
        if (c.moveToFirst()) {
            do {
                LedgerItem item = new LedgerItem();
                item.type = c.getString(c.getColumnIndexOrThrow("type"));
                item.amount = c.getDouble(c.getColumnIndexOrThrow("amount"));
                item.date = c.getString(c.getColumnIndexOrThrow("date_time"));
                item.notes = c.getString(c.getColumnIndexOrThrow("notes"));
                ledgerList.add(item);
            } while (c.moveToNext());
        }
        c.close();
        adapter.notifyDataSetChanged();
    }

    private void recordPayment() {
        String amtStr = etPaymentAmount.getText().toString().trim();
        if (amtStr.isEmpty()) {
            Toast.makeText(this, "Enter repayment amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amtStr);
        String notes = etPaymentNotes.getText().toString().trim();
        dbHelper.recordCustomerPayment(customerId, amount, notes.isEmpty() ? "Cash received" : notes);

        etPaymentAmount.setText("");
        etPaymentNotes.setText("");
        Toast.makeText(this, "Payment recorded successfully", Toast.LENGTH_SHORT).show();

        loadCustomerDetails();
    }

    private class LedgerAdapter extends BaseAdapter {
        @Override
        public int getCount() { return ledgerList.size(); }

        @Override
        public Object getItem(int position) { return ledgerList.get(position); }

        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(CustomerDetailActivity.this).inflate(R.layout.item_udhaar, parent, false);
            }

            LedgerItem item = ledgerList.get(position);
            TextView tvType = (TextView) convertView.findViewById(R.id.tv_udhaar_type);
            TextView tvNotes = (TextView) convertView.findViewById(R.id.tv_udhaar_notes);
            TextView tvDate = (TextView) convertView.findViewById(R.id.tv_udhaar_date);
            TextView tvAmount = (TextView) convertView.findViewById(R.id.tv_udhaar_amount);

            tvNotes.setText(item.notes);
            tvDate.setText(item.date);

            if ("PAYMENT".equalsIgnoreCase(item.type)) {
                tvType.setText("🟢 PAYMENT RECEIVED");
                tvType.setTextColor(getResources().getColor(R.color.success));
                tvAmount.setText(String.format(Locale.getDefault(), "- ₹ %.2f", item.amount));
                tvAmount.setTextColor(getResources().getColor(R.color.success));
            } else {
                tvType.setText("🔴 UDHAAR (GIVEN)");
                tvType.setTextColor(getResources().getColor(R.color.danger));
                tvAmount.setText(String.format(Locale.getDefault(), "+ ₹ %.2f", item.amount));
                tvAmount.setTextColor(getResources().getColor(R.color.danger));
            }

            return convertView;
        }
    }
}