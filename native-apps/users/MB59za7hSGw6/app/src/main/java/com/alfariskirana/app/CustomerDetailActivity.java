package com.alfariskirana.app;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
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
    private TextView tvTotalUdhaar;
    private TextView tvTotalJama;
    private TextView tvPendingUdhaar;
    private TextView tvAdvanceBadge;

    private Button btnTabUdhaar;
    private Button btnTabJama;
    private LinearLayout layoutSectionUdhaar;
    private LinearLayout layoutSectionJama;

    private ListView lvUdhaarHistory;
    private ListView lvJamaHistory;

    private List<UdhaarTransaction> udhaarList = new ArrayList<UdhaarTransaction>();
    private List<UdhaarTransaction> jamaList = new ArrayList<UdhaarTransaction>();

    private UdhaarAdapter udhaarAdapter;
    private JamaAdapter jamaAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_detail);

        dbHelper = new DatabaseHelper(this);
        customerId = getIntent().getLongExtra("customer_id", -1);

        tvName = (TextView) findViewById(R.id.tv_detail_name);
        tvMobile = (TextView) findViewById(R.id.tv_detail_mobile);
        tvTotalUdhaar = (TextView) findViewById(R.id.tv_detail_total_udhaar);
        tvTotalJama = (TextView) findViewById(R.id.tv_detail_total_jama);
        tvPendingUdhaar = (TextView) findViewById(R.id.tv_detail_pending_udhaar);
        tvAdvanceBadge = (TextView) findViewById(R.id.tv_advance_badge);

        btnTabUdhaar = (Button) findViewById(R.id.btn_tab_udhaar);
        btnTabJama = (Button) findViewById(R.id.btn_tab_jama);
        layoutSectionUdhaar = (LinearLayout) findViewById(R.id.layout_section_udhaar);
        layoutSectionJama = (LinearLayout) findViewById(R.id.layout_section_jama);

        lvUdhaarHistory = (ListView) findViewById(R.id.lv_udhaar_history);
        lvJamaHistory = (ListView) findViewById(R.id.lv_jama_history);

        Button btnActionAddUdhaar = (Button) findViewById(R.id.btn_action_add_udhaar);
        Button btnActionAddJama = (Button) findViewById(R.id.btn_action_add_jama);

        udhaarAdapter = new UdhaarAdapter();
        lvUdhaarHistory.setAdapter(udhaarAdapter);

        jamaAdapter = new JamaAdapter();
        lvJamaHistory.setAdapter(jamaAdapter);

        btnTabUdhaar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(true);
            }
        });

        btnTabJama.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(false);
            }
        });

        btnActionAddUdhaar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddUdhaarDialog();
            }
        });

        btnActionAddJama.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddJamaDialog();
            }
        });

        loadCustomerDetails();
    }

    private void switchTab(boolean isUdhaarTab) {
        if (isUdhaarTab) {
            layoutSectionUdhaar.setVisibility(View.VISIBLE);
            layoutSectionJama.setVisibility(View.GONE);
            btnTabUdhaar.setTextColor(getResources().getColor(R.color.danger));
            btnTabJama.setTextColor(getResources().getColor(R.color.text_secondary));
        } else {
            layoutSectionUdhaar.setVisibility(View.GONE);
            layoutSectionJama.setVisibility(View.VISIBLE);
            btnTabUdhaar.setTextColor(getResources().getColor(R.color.text_secondary));
            btnTabJama.setTextColor(getResources().getColor(R.color.success));
        }
    }

    private void loadCustomerDetails() {
        customer = dbHelper.getCustomerById(customerId);
        if (customer != null) {
            tvName.setText(customer.getName());
            tvMobile.setText("Mobile: " + (customer.getMobile() == null || customer.getMobile().isEmpty() ? "N/A" : customer.getMobile()));

            tvTotalUdhaar.setText(String.format(Locale.getDefault(), "₹ %.2f", customer.getTotalUdhaar()));
            tvTotalJama.setText(String.format(Locale.getDefault(), "₹ %.2f", customer.getTotalJama()));
            tvPendingUdhaar.setText(String.format(Locale.getDefault(), "₹ %.2f", customer.getPendingBalance()));

            if (customer.getAdvanceBalance() > 0) {
                tvAdvanceBadge.setVisibility(View.VISIBLE);
                tvAdvanceBadge.setText(String.format(Locale.getDefault(), "Customer Advance: ₹ %.2f", customer.getAdvanceBalance()));
            } else {
                tvAdvanceBadge.setVisibility(View.GONE);
            }

            loadUdhaarList();
            loadJamaList();
        }
    }

    private void loadUdhaarList() {
        udhaarList = dbHelper.getUdhaarTransactions(customerId);
        udhaarAdapter.notifyDataSetChanged();
    }

    private void loadJamaList() {
        jamaList = dbHelper.getJamaTransactions(customerId);
        jamaAdapter.notifyDataSetChanged();
    }

    private void showAddUdhaarDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_udhaar);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        final EditText etAmount = (EditText) dialog.findViewById(R.id.et_dialog_udhaar_amount);
        final EditText etNotes = (EditText) dialog.findViewById(R.id.et_dialog_udhaar_notes);
        final EditText etDate = (EditText) dialog.findViewById(R.id.et_dialog_udhaar_date);
        Button btnCancel = (Button) dialog.findViewById(R.id.btn_dialog_udhaar_cancel);
        Button btnSave = (Button) dialog.findViewById(R.id.btn_dialog_udhaar_save);

        etDate.setText(DatabaseHelper.getCurrentDateTime());

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String amtStr = etAmount.getText().toString().trim();
                if (amtStr.isEmpty()) {
                    Toast.makeText(CustomerDetailActivity.this, "उधार राशि दर्ज करें", Toast.LENGTH_SHORT).show();
                    return;
                }

                double amount = Double.parseDouble(amtStr);
                if (amount <= 0) {
                    Toast.makeText(CustomerDetailActivity.this, "वैध राशि दर्ज करें", Toast.LENGTH_SHORT).show();
                    return;
                }

                String notes = etNotes.getText().toString().trim();
                String dateTime = etDate.getText().toString().trim();

                dbHelper.addUdhaarTransaction(customerId, amount, notes, dateTime);
                Toast.makeText(CustomerDetailActivity.this, "नया उधार सफलतापूर्वक जोड़ा गया", Toast.LENGTH_SHORT).show();
                dialog.dismiss();

                loadCustomerDetails();
            }
        });

        dialog.show();
    }

    private void showAddJamaDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_jama);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        final EditText etAmount = (EditText) dialog.findViewById(R.id.et_dialog_jama_amount);
        final Spinner spMethod = (Spinner) dialog.findViewById(R.id.sp_dialog_jama_method);
        final EditText etNotes = (EditText) dialog.findViewById(R.id.et_dialog_jama_notes);
        final EditText etDate = (EditText) dialog.findViewById(R.id.et_dialog_jama_date);
        Button btnCancel = (Button) dialog.findViewById(R.id.btn_dialog_jama_cancel);
        Button btnSave = (Button) dialog.findViewById(R.id.btn_dialog_jama_save);

        String[] methods = {"Cash", "GPay", "PhonePe", "Paytm / UPI", "Other"};
        ArrayAdapter<String> methodAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, methods);
        spMethod.setAdapter(methodAdapter);

        etDate.setText(DatabaseHelper.getCurrentDateTime());

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String amtStr = etAmount.getText().toString().trim();
                if (amtStr.isEmpty()) {
                    Toast.makeText(CustomerDetailActivity.this, "जमा राशि दर्ज करें", Toast.LENGTH_SHORT).show();
                    return;
                }

                double amount = Double.parseDouble(amtStr);
                if (amount <= 0) {
                    Toast.makeText(CustomerDetailActivity.this, "वैध राशि दर्ज करें", Toast.LENGTH_SHORT).show();
                    return;
                }

                String method = spMethod.getSelectedItem().toString();
                String notes = etNotes.getText().toString().trim();
                String dateTime = etDate.getText().toString().trim();

                dbHelper.addJamaTransaction(customerId, amount, method, notes, dateTime);
                Toast.makeText(CustomerDetailActivity.this, "पैसा जमा सफलतापूर्वक दर्ज किया गया", Toast.LENGTH_SHORT).show();
                dialog.dismiss();

                loadCustomerDetails();
            }
        });

        dialog.show();
    }

    // --- UDHAAR LIST ADAPTER ---
    private class UdhaarAdapter extends BaseAdapter {
        @Override
        public int getCount() { return udhaarList.size(); }

        @Override
        public Object getItem(int position) { return udhaarList.get(position); }

        @Override
        public long getItemId(int position) { return udhaarList.get(position).getId(); }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(CustomerDetailActivity.this).inflate(R.layout.item_udhaar, parent, false);
            }

            UdhaarTransaction tx = udhaarList.get(position);

            TextView tvNotes = (TextView) convertView.findViewById(R.id.tv_udhaar_notes);
            TextView tvDate = (TextView) convertView.findViewById(R.id.tv_udhaar_date);
            TextView tvAmount = (TextView) convertView.findViewById(R.id.tv_udhaar_amount);
            TextView tvRunningPending = (TextView) convertView.findViewById(R.id.tv_udhaar_running_pending);

            tvNotes.setText(tx.getNotes() == null || tx.getNotes().isEmpty() ? "सामान उधार लिया" : tx.getNotes());
            tvDate.setText(tx.getDateTime());
            tvAmount.setText(String.format(Locale.getDefault(), "+ ₹ %.2f", tx.getAmount()));
            tvRunningPending.setText(String.format(Locale.getDefault(), "बाकी उधार: ₹ %.2f", customer.getPendingBalance()));

            return convertView;
        }
    }

    // --- JAMA LIST ADAPTER (NEVER DISPLAYS PENDING/REMAINING BALANCES) ---
    private class JamaAdapter extends BaseAdapter {
        @Override
        public int getCount() { return jamaList.size(); }

        @Override
        public Object getItem(int position) { return jamaList.get(position); }

        @Override
        public long getItemId(int position) { return jamaList.get(position).getId(); }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(CustomerDetailActivity.this).inflate(R.layout.item_jama, parent, false);
            }

            UdhaarTransaction tx = jamaList.get(position);

            TextView tvMethodBadge = (TextView) convertView.findViewById(R.id.tv_jama_method_badge);
            TextView tvNotes = (TextView) convertView.findViewById(R.id.tv_jama_notes);
            TextView tvDate = (TextView) convertView.findViewById(R.id.tv_jama_date);
            TextView tvAmount = (TextView) convertView.findViewById(R.id.tv_jama_amount);

            tvMethodBadge.setText("🟢 जमा (" + (tx.getPaymentMethod() == null ? "Cash" : tx.getPaymentMethod()) + ")");
            tvNotes.setText(tx.getNotes() == null || tx.getNotes().isEmpty() ? "पैसा प्राप्त हुआ" : tx.getNotes());
            tvDate.setText(tx.getDateTime());
            tvAmount.setText(String.format(Locale.getDefault(), "₹ %.2f", tx.getAmount()));

            return convertView;
        }
    }
}