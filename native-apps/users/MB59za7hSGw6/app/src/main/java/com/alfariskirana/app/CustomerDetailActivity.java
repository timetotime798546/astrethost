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
                showJamaDialogForUdhaarRecord(null);
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

    private void showJamaDialogForUdhaarRecord(final UdhaarTransaction targetUdhaar) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_jama);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView tvCustInfo = (TextView) dialog.findViewById(R.id.tv_dialog_customer_info);
        TextView tvOrigAmt = (TextView) dialog.findViewById(R.id.tv_dialog_original_amount);
        TextView tvPendAmt = (TextView) dialog.findViewById(R.id.tv_dialog_pending_amount);

        final EditText etAmount = (EditText) dialog.findViewById(R.id.et_dialog_jama_amount);
        final Spinner spMethod = (Spinner) dialog.findViewById(R.id.sp_dialog_jama_method);
        final EditText etNotes = (EditText) dialog.findViewById(R.id.et_dialog_jama_notes);
        final EditText etDate = (EditText) dialog.findViewById(R.id.et_dialog_jama_date);
        Button btnCancel = (Button) dialog.findViewById(R.id.btn_dialog_jama_cancel);
        Button btnSave = (Button) dialog.findViewById(R.id.btn_dialog_jama_save);

        tvCustInfo.setText("ग्राहक: " + customer.getName());

        final double maxAllowedPending;
        if (targetUdhaar != null) {
            maxAllowedPending = targetUdhaar.getPendingAmount();
            tvOrigAmt.setText(String.format(Locale.getDefault(), "मूल उधार (Original Udhaar): ₹ %.2f", targetUdhaar.getAmount()));
            tvPendAmt.setText(String.format(Locale.getDefault(), "इस उधार की बाकी (Pending): ₹ %.2f", targetUdhaar.getPendingAmount()));
            etAmount.setText(String.format(Locale.getDefault(), "%.2f", targetUdhaar.getPendingAmount()));
        } else {
            maxAllowedPending = customer.getPendingBalance();
            tvOrigAmt.setText(String.format(Locale.getDefault(), "कुल उधार (Total Udhaar): ₹ %.2f", customer.getTotalUdhaar()));
            tvPendAmt.setText(String.format(Locale.getDefault(), "कुल बाकी (Total Pending): ₹ %.2f", customer.getPendingBalance()));
            etAmount.setText(String.format(Locale.getDefault(), "%.2f", customer.getPendingBalance()));
        }

        String[] methods = {"Cash", "GPay", "UPI", "Other"};
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
                    Toast.makeText(CustomerDetailActivity.this, "प्राप्त राशि दर्ज करें", Toast.LENGTH_SHORT).show();
                    return;
                }

                double amount = Double.parseDouble(amtStr);

                // Validation Rule: Reject zero or negative payment
                if (amount <= 0) {
                    Toast.makeText(CustomerDetailActivity.this, "जमा राशि 0 से अधिक होनी चाहिए!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Validation Rule: Do not allow payment greater than pending amount
                if (amount > maxAllowedPending + 0.001) {
                    Toast.makeText(CustomerDetailActivity.this,
                            String.format(Locale.getDefault(), "जमा राशि बाकी बकाया (₹ %.2f) से अधिक नहीं हो सकती!", maxAllowedPending),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                String method = spMethod.getSelectedItem().toString();
                String notes = etNotes.getText().toString().trim();
                String dateTime = etDate.getText().toString().trim();

                boolean success = false;
                if (targetUdhaar != null) {
                    success = dbHelper.addJamaForUdhaar(targetUdhaar.getId(), customerId, amount, method, notes, dateTime);
                } else {
                    success = dbHelper.addGeneralJama(customerId, amount, method, notes, dateTime);
                }

                if (success) {
                    Toast.makeText(CustomerDetailActivity.this, "जमा राशि सफलतापूर्वक दर्ज की गई!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadCustomerDetails();
                } else {
                    Toast.makeText(CustomerDetailActivity.this, "भुगतान प्रक्रिया में त्रुटि", Toast.LENGTH_SHORT).show();
                }
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

            final UdhaarTransaction tx = udhaarList.get(position);

            TextView tvNotes = (TextView) convertView.findViewById(R.id.tv_udhaar_notes);
            TextView tvDate = (TextView) convertView.findViewById(R.id.tv_udhaar_date);
            TextView tvAmount = (TextView) convertView.findViewById(R.id.tv_udhaar_amount);
            TextView tvRunningPending = (TextView) convertView.findViewById(R.id.tv_udhaar_running_pending);
            Button btnJamaKarein = (Button) convertView.findViewById(R.id.btn_item_jama_karein);

            tvNotes.setText(tx.getNotes() == null || tx.getNotes().isEmpty() ? "सामान उधार लिया" : tx.getNotes());
            tvDate.setText(tx.getDateTime());
            tvAmount.setText(String.format(Locale.getDefault(), "मूल उधार: ₹ %.2f", tx.getAmount()));
            tvRunningPending.setText(String.format(Locale.getDefault(), "बाकी उधार: ₹ %.2f", tx.getPendingAmount()));

            if (tx.getPendingAmount() <= 0) {
                btnJamaKarein.setText("✓ पूरा जमा");
                btnJamaKarein.setEnabled(false);
                btnJamaKarein.setBackgroundResource(R.drawable.bg_badge_pill_green);
                btnJamaKarein.setTextColor(getResources().getColor(R.color.primary_dark));
            } else {
                btnJamaKarein.setText("जमा करें (Jama Karein)");
                btnJamaKarein.setEnabled(true);
                btnJamaKarein.setBackgroundResource(R.drawable.bg_button_primary);
                btnJamaKarein.setTextColor(0xFFFFFFFF);

                btnJamaKarein.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showJamaDialogForUdhaarRecord(tx);
                    }
                });
            }

            return convertView;
        }
    }

    // --- JAMA LIST ADAPTER (STRICT NO PENDING BALANCE RULE) ---
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
            tvNotes.setText(tx.getNotes() == null || tx.getNotes().isEmpty() ? "पैसा जमा किया" : tx.getNotes());
            tvDate.setText(tx.getDateTime());
            tvAmount.setText(String.format(Locale.getDefault(), "₹ %.2f", tx.getAmount()));

            return convertView;
        }
    }
}