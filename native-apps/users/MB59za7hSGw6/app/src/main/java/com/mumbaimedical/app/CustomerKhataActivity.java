package com.mumbaimedical.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CustomerKhataActivity extends Activity {

    private DatabaseHelper dbHelper;
    private long customerId = -1;

    private TextView tvCustomerName, tvCustomerPhone, tvCustomerAddress;
    private TextView tvTotalUdhar, tvTotalJama, tvPendingBalanceDetail, tvBalanceStatus;
    private Button btnAddUdhar, btnAddJama, btnEditCustomerInfo;
    private ListView lvKhataTransactions;
    private SimpleCursorAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_khata);

        dbHelper = new DatabaseHelper(this);

        if (getIntent().hasExtra("customer_id")) {
            customerId = getIntent().getLongExtra("customer_id", -1);
        }

        if (customerId == -1) {
            Toast.makeText(this, "Invalid Customer Selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvCustomerName = findViewById(R.id.tvKhataCustomerName);
        tvCustomerPhone = findViewById(R.id.tvKhataCustomerPhone);
        tvCustomerAddress = findViewById(R.id.tvKhataCustomerAddress);

        tvTotalUdhar = findViewById(R.id.tvTotalUdhar);
        tvTotalJama = findViewById(R.id.tvTotalJama);
        tvPendingBalanceDetail = findViewById(R.id.tvPendingBalanceDetail);
        tvBalanceStatus = findViewById(R.id.tvBalanceStatus);

        btnAddUdhar = findViewById(R.id.btnAddUdhar);
        btnAddJama = findViewById(R.id.btnAddJama);
        btnEditCustomerInfo = findViewById(R.id.btnEditCustomerInfo);
        lvKhataTransactions = findViewById(R.id.lvKhataTransactions);

        btnEditCustomerInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CustomerKhataActivity.this, AddCustomerActivity.class);
                intent.putExtra("customer_id", customerId);
                startActivity(intent);
            }
        });

        btnAddUdhar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddEntryDialog("UDHAR");
            }
        });

        btnAddJama.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddEntryDialog("JAMA");
            }
        });

        lvKhataTransactions.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, final long txId) {
                new AlertDialog.Builder(CustomerKhataActivity.this)
                        .setTitle("Delete Ledger Entry")
                        .setMessage("Are you sure you want to delete this Khata transaction? The customer's balance will be recalculated automatically.")
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dbHelper.deleteKhataTransaction(txId, customerId);
                                Toast.makeText(CustomerKhataActivity.this, "Transaction deleted", Toast.LENGTH_SHORT).show();
                                refreshKhataData();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshKhataData();
    }

    private void refreshKhataData() {
        // Load Customer Header
        Cursor c = dbHelper.getCustomerById(customerId);
        if (c != null && c.moveToFirst()) {
            String name = c.getString(c.getColumnIndexOrThrow("name"));
            String phone = c.getString(c.getColumnIndexOrThrow("phone"));
            String address = c.getString(c.getColumnIndexOrThrow("address"));

            tvCustomerName.setText(name);
            tvCustomerPhone.setText("Phone: " + (phone != null && !phone.isEmpty() ? phone : "-"));
            tvCustomerAddress.setText("Address: " + (address != null && !address.isEmpty() ? address : "-"));
            c.close();
        }

        // Load Udhar & Jama totals
        double udhar = dbHelper.getCustomerTotalUdhar(customerId);
        double jama = dbHelper.getCustomerTotalJama(customerId);
        double pending = udhar - jama;

        tvTotalUdhar.setText(String.format(Locale.getDefault(), "₹ %.2f", udhar));
        tvTotalJama.setText(String.format(Locale.getDefault(), "₹ %.2f", jama));
        tvPendingBalanceDetail.setText(String.format(Locale.getDefault(), "₹ %.2f", Math.abs(pending)));

        if (pending > 0) {
            tvPendingBalanceDetail.setTextColor(getResources().getColor(R.color.warning_red));
            tvBalanceStatus.setText("Customer owes ₹ " + String.format(Locale.getDefault(), "%.2f", pending));
            tvBalanceStatus.setTextColor(getResources().getColor(R.color.warning_red));
        } else if (pending < 0) {
            tvPendingBalanceDetail.setTextColor(getResources().getColor(R.color.success_green));
            tvBalanceStatus.setText("Advance balance available: ₹ " + String.format(Locale.getDefault(), "%.2f", Math.abs(pending)));
            tvBalanceStatus.setTextColor(getResources().getColor(R.color.success_green));
        } else {
            tvPendingBalanceDetail.setTextColor(getResources().getColor(R.color.text_dark));
            tvBalanceStatus.setText("Account is fully clear (₹ 0.00)");
            tvBalanceStatus.setTextColor(getResources().getColor(R.color.text_muted));
        }

        // Load Ledger list
        Cursor historyCursor = dbHelper.getCustomerKhataHistory(customerId);

        String[] from = new String[]{"type", "date", "amount", "description", "payment_mode"};
        int[] to = new int[]{R.id.tvKhataType, R.id.tvKhataDate, R.id.tvKhataAmount, R.id.tvKhataNotes, R.id.tvKhataPaymentMode};

        adapter = new SimpleCursorAdapter(
                this,
                R.layout.item_khata,
                historyCursor,
                from,
                to,
                0
        );

        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                int id = view.getId();
                if (id == R.id.tvKhataType) {
                    String type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
                    TextView tv = (TextView) view;
                    tv.setText(type);
                    if ("UDHAR".equalsIgnoreCase(type)) {
                        tv.setTextColor(getResources().getColor(R.color.warning_red));
                        tv.setBackgroundColor(0xFFFFEBEE);
                    } else {
                        tv.setTextColor(getResources().getColor(R.color.success_green));
                        tv.setBackgroundColor(0xFFE8F5E9);
                    }
                    return true;
                } else if (id == R.id.tvKhataAmount) {
                    double amt = cursor.getDouble(cursor.getColumnIndexOrThrow("amount"));
                    String type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
                    TextView tv = (TextView) view;
                    tv.setText(String.format(Locale.getDefault(), "₹ %.2f", amt));
                    if ("UDHAR".equalsIgnoreCase(type)) {
                        tv.setTextColor(getResources().getColor(R.color.warning_red));
                    } else {
                        tv.setTextColor(getResources().getColor(R.color.success_green));
                    }
                    return true;
                } else if (id == R.id.tvKhataNotes) {
                    String notes = cursor.getString(cursor.getColumnIndexOrThrow("description"));
                    String ref = cursor.getString(cursor.getColumnIndexOrThrow("reference"));
                    String text = (notes != null && !notes.isEmpty() ? notes : "No description");
                    if (ref != null && !ref.isEmpty()) {
                        text += " (" + ref + ")";
                    }
                    ((TextView) view).setText(text);
                    return true;
                } else if (id == R.id.tvKhataPaymentMode) {
                    String mode = cursor.getString(cursor.getColumnIndexOrThrow("payment_mode"));
                    ((TextView) view).setText("Mode: " + (mode != null && !mode.isEmpty() ? mode : "N/A"));
                    return true;
                }
                return false;
            }
        });

        lvKhataTransactions.setAdapter(adapter);
    }

    private void showAddEntryDialog(final String entryType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(entryType.equals("UDHAR") ? "Record Customer Udhar (Credit)" : "Record Customer Jama (Payment)");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(40, 20, 40, 20);

        final EditText etAmount = new EditText(this);
        etAmount.setHint("Amount (₹) *");
        etAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        container.addView(etAmount);

        final EditText etNotes = new EditText(this);
        etNotes.setHint("Description / Note (Optional)");
        etNotes.setPadding(0, 20, 0, 0);
        container.addView(etNotes);

        final Spinner spMode = new Spinner(this);
        String[] modes = new String[]{"Cash", "UPI / GPay", "Card / NetBanking", "Other"};
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, modes);
        spMode.setAdapter(modeAdapter);
        if (entryType.equals("JAMA")) {
            TextView tvLabel = new TextView(this);
            tvLabel.setText("Payment Mode:");
            tvLabel.setPadding(0, 20, 0, 5);
            container.addView(tvLabel);
            container.addView(spMode);
        }

        builder.setView(container);

        builder.setPositiveButton("Save " + entryType, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String amtStr = etAmount.getText().toString().trim();
                if (amtStr.isEmpty()) {
                    Toast.makeText(CustomerKhataActivity.this, "Please enter valid amount", Toast.LENGTH_SHORT).show();
                    return;
                }

                double amount;
                try {
                    amount = Double.parseDouble(amtStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(CustomerKhataActivity.this, "Invalid amount format", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (amount <= 0) {
                    Toast.makeText(CustomerKhataActivity.this, "Amount must be greater than zero", Toast.LENGTH_SHORT).show();
                    return;
                }

                String notes = etNotes.getText().toString().trim();
                String paymentMode = entryType.equals("JAMA") ? spMode.getSelectedItem().toString() : "Credit";
                String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

                dbHelper.addKhataTransaction(customerId, entryType, amount, currentDate, notes, paymentMode, "Manual Entry");
                Toast.makeText(CustomerKhataActivity.this, entryType + " Entry Recorded", Toast.LENGTH_SHORT).show();
                refreshKhataData();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}