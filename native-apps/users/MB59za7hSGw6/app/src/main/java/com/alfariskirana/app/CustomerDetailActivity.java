package com.alfariskirana.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
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
    private Button btnTabSales;
    private LinearLayout layoutSectionUdhaar;
    private LinearLayout layoutSectionJama;
    private LinearLayout layoutSectionSales;

    private ListView lvUdhaarHistory;
    private ListView lvJamaHistory;
    private ListView lvSalesHistory;

    private List<UdhaarTransaction> udhaarList = new ArrayList<UdhaarTransaction>();
    private List<UdhaarTransaction> jamaList = new ArrayList<UdhaarTransaction>();
    private List<SaleRecord> salesList = new ArrayList<SaleRecord>();

    private UdhaarAdapter udhaarAdapter;
    private JamaAdapter jamaAdapter;
    private SalesAdapter salesAdapter;

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
        btnTabSales = (Button) findViewById(R.id.btn_tab_sales);

        layoutSectionUdhaar = (LinearLayout) findViewById(R.id.layout_section_udhaar);
        layoutSectionJama = (LinearLayout) findViewById(R.id.layout_section_jama);
        layoutSectionSales = (LinearLayout) findViewById(R.id.layout_section_sales);

        lvUdhaarHistory = (ListView) findViewById(R.id.lv_udhaar_history);
        lvJamaHistory = (ListView) findViewById(R.id.lv_jama_history);
        lvSalesHistory = (ListView) findViewById(R.id.lv_sales_history);

        Button btnActionAddUdhaar = (Button) findViewById(R.id.btn_action_add_udhaar);
        Button btnActionAddJama = (Button) findViewById(R.id.btn_action_add_jama);
        Button btnShareStatement = (Button) findViewById(R.id.btn_share_statement);
        Button btnShareReminder = (Button) findViewById(R.id.btn_share_reminder);

        udhaarAdapter = new UdhaarAdapter();
        lvUdhaarHistory.setAdapter(udhaarAdapter);

        jamaAdapter = new JamaAdapter();
        lvJamaHistory.setAdapter(jamaAdapter);

        salesAdapter = new SalesAdapter();
        lvSalesHistory.setAdapter(salesAdapter);

        btnTabUdhaar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(0);
            }
        });

        btnTabJama.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(1);
            }
        });

        btnTabSales.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
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

        btnShareStatement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareCustomerStatement();
            }
        });

        btnShareReminder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sharePaymentReminder();
            }
        });

        loadCustomerDetails();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCustomerDetails();
    }

    private void switchTab(int tabIndex) {
        layoutSectionUdhaar.setVisibility(tabIndex == 0 ? View.VISIBLE : View.GONE);
        layoutSectionJama.setVisibility(tabIndex == 1 ? View.VISIBLE : View.GONE);
        layoutSectionSales.setVisibility(tabIndex == 2 ? View.VISIBLE : View.GONE);

        btnTabUdhaar.setTextColor(tabIndex == 0 ? getResources().getColor(R.color.danger) : getResources().getColor(R.color.text_secondary));
        btnTabJama.setTextColor(tabIndex == 1 ? getResources().getColor(R.color.success) : getResources().getColor(R.color.text_secondary));
        btnTabSales.setTextColor(tabIndex == 2 ? getResources().getColor(R.color.primary) : getResources().getColor(R.color.text_secondary));
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
            loadSalesList();
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

    private void loadSalesList() {
        salesList = dbHelper.getCustomerSales(customerId);
        salesAdapter.notifyDataSetChanged();
    }

    private void shareCustomerStatement() {
        if (customer == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append("🛒 *AL FARIS KIRANA STORE* 🛒\n");
        sb.append("----------------------------------\n");
        sb.append("👤 *GRAHAK KHATA STATEMENT (ग्राहक खाता विवरण)*\n");
        sb.append("Name: ").append(customer.getName()).append("\n");
        if (customer.getMobile() != null && !customer.getMobile().isEmpty()) {
            sb.append("Mobile: ").append(customer.getMobile()).append("\n");
        }
        sb.append("\n📊 *SUMMARY / सारांश:*\n");
        sb.append("• Total Udhaar (कुल उधार): ₹ ").append(String.format(Locale.getDefault(), "%.2f", customer.getTotalUdhaar())).append("\n");
        sb.append("• Total Jama (कुल जमा): ₹ ").append(String.format(Locale.getDefault(), "%.2f", customer.getTotalJama())).append("\n");
        if (customer.getPendingBalance() > 0) {
            sb.append("• *Remaining Udhaar (बाकी बकाया): ₹ ").append(String.format(Locale.getDefault(), "%.2f", customer.getPendingBalance())).append("*\n");
        } else if (customer.getAdvanceBalance() > 0) {
            sb.append("• *Advance Credit (अग्रिम जमा): ₹ ").append(String.format(Locale.getDefault(), "%.2f", customer.getAdvanceBalance())).append("*\n");
        } else {
            sb.append("• *Account Status: Settled (खाता चुकता ₹0)*\n");
        }

        if (udhaarList != null && !udhaarList.isEmpty()) {
            sb.append("\n🔴 *Recent Udhaar Transactions (उधार लेन-देन):*\n");
            int count = Math.min(udhaarList.size(), 5);
            for (int i = 0; i < count; i++) {
                UdhaarTransaction tx = udhaarList.get(i);
                sb.append("• ").append(tx.getDateTime()).append(" - ₹ ").append(String.format(Locale.getDefault(), "%.2f", tx.getAmount()));
                if (tx.getNotes() != null && !tx.getNotes().isEmpty()) {
                    sb.append(" (").append(tx.getNotes()).append(")");
                }
                sb.append("\n");
            }
        }

        if (jamaList != null && !jamaList.isEmpty()) {
            sb.append("\n🟢 *Recent Jama Payments (प्राप्त जमा):*\n");
            int count = Math.min(jamaList.size(), 5);
            for (int i = 0; i < count; i++) {
                UdhaarTransaction tx = jamaList.get(i);
                sb.append("• ").append(tx.getDateTime()).append(" - ₹ ").append(String.format(Locale.getDefault(), "%.2f", tx.getAmount()));
                if (tx.getPaymentMethod() != null) {
                    sb.append(" [").append(tx.getPaymentMethod()).append("]");
                }
                sb.append("\n");
            }
        }

        sb.append("\n----------------------------------\n");
        sb.append("Shukriya! / धन्यवाद!\nAl Faris Kirana Store");

        WhatsAppShareHelper.shareText(this, sb.toString(), "Share Statement via WhatsApp");
    }

    private void sharePaymentReminder() {
        if (customer == null) return;
        if (customer.getPendingBalance() <= 0) {
            Toast.makeText(this, "इस ग्राहक का कोई उधार बाकी नहीं है!", Toast.LENGTH_SHORT).show();
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("🙏 *AL FARIS KIRANA STORE* 🙏\n\n");
        sb.append("आदरणीय *").append(customer.getName()).append("* जी,\n\n");
        sb.append("आपके अल फारिस किराना स्टोर के खाते में *₹ ").append(String.format(Locale.getDefault(), "%.2f", customer.getPendingBalance())).append("* की उधार राशि बकाया है।\n\n");
        sb.append("कृपया यथाशीघ्र बकाया राशि जमा करने का कष्ट करें।\n\n");
        sb.append("📋 *खाता विवरण:*\n");
        sb.append("• कुल उधार: ₹ ").append(String.format(Locale.getDefault(), "%.2f", customer.getTotalUdhaar())).append("\n");
        sb.append("• कुल जमा: ₹ ").append(String.format(Locale.getDefault(), "%.2f", customer.getTotalJama())).append("\n");
        sb.append("• *बाकी बकाया: ₹ ").append(String.format(Locale.getDefault(), "%.2f", customer.getPendingBalance())).append("*\n\n");
        sb.append("धन्यवाद!\nAl Faris Kirana Store");

        WhatsAppShareHelper.shareText(this, sb.toString(), "Send Payment Reminder via WhatsApp");
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

                if (amount <= 0) {
                    Toast.makeText(CustomerDetailActivity.this, "जमा राशि 0 से अधिक होनी चाहिए!", Toast.LENGTH_SHORT).show();
                    return;
                }

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

    private class SalesAdapter extends BaseAdapter {
        @Override
        public int getCount() { return salesList.size(); }

        @Override
        public Object getItem(int position) { return salesList.get(position); }

        @Override
        public long getItemId(int position) { return salesList.get(position).getId(); }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(CustomerDetailActivity.this).inflate(R.layout.item_sale, parent, false);
            }

            final SaleRecord sale = salesList.get(position);

            TextView tvBillNo = (TextView) convertView.findViewById(R.id.tv_sale_bill_no);
            TextView tvModeBadge = (TextView) convertView.findViewById(R.id.tv_sale_mode_badge);
            TextView tvAmount = (TextView) convertView.findViewById(R.id.tv_sale_amount);
            TextView tvDate = (TextView) convertView.findViewById(R.id.tv_sale_date);
            Button btnViewDetail = (Button) convertView.findViewById(R.id.btn_view_bill_detail);

            tvBillNo.setText("Bill #" + sale.getId());
            boolean isCredit = "Udhaar".equalsIgnoreCase(sale.getPaymentMode()) || "Credit".equalsIgnoreCase(sale.getPaymentMode());
            if (isCredit) {
                tvModeBadge.setText("🔴 उधार (Udhaar)");
                tvModeBadge.setBackgroundResource(R.drawable.bg_badge_pill_red);
                tvModeBadge.setTextColor(getResources().getColor(R.color.danger));
            } else {
                tvModeBadge.setText("🟢 नगद (Cash)");
                tvModeBadge.setBackgroundResource(R.drawable.bg_badge_pill_green);
                tvModeBadge.setTextColor(getResources().getColor(R.color.success));
            }

            tvAmount.setText(String.format(Locale.getDefault(), "₹ %.2f", sale.getFinalAmount()));
            tvDate.setText(sale.getDateTime());

            btnViewDetail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(CustomerDetailActivity.this, SaleBillDetailActivity.class);
                    intent.putExtra("sale_id", sale.getId());
                    startActivity(intent);
                }
            });

            return convertView;
        }
    }
}