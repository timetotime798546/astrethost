package com.mumbaimedical.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
    private ImageView ivKhataCustomerPhoto;
    private Button btnAddUdhar, btnAddJama, btnEditCustomerInfo, btnShareWhatsappKhata;
    private ListView lvKhataTransactions;
    private SimpleCursorAdapter adapter;
    private String currentPhotoPath = null;
    private String currentCustomerName = "";
    private String currentCustomerPhone = "";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

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
        ivKhataCustomerPhoto = findViewById(R.id.ivKhataCustomerPhoto);

        tvTotalUdhar = findViewById(R.id.tvTotalUdhar);
        tvTotalJama = findViewById(R.id.tvTotalJama);
        tvPendingBalanceDetail = findViewById(R.id.tvPendingBalanceDetail);
        tvBalanceStatus = findViewById(R.id.tvBalanceStatus);

        btnAddUdhar = findViewById(R.id.btnAddUdhar);
        btnAddJama = findViewById(R.id.btnAddJama);
        btnEditCustomerInfo = findViewById(R.id.btnEditCustomerInfo);
        btnShareWhatsappKhata = findViewById(R.id.btnShareWhatsappKhata);
        lvKhataTransactions = findViewById(R.id.lvKhataTransactions);

        ivKhataCustomerPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPhotoPath != null) {
                    PhotoHelper.showFullScreenImage(CustomerKhataActivity.this, currentPhotoPath);
                }
            }
        });

        btnEditCustomerInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CustomerKhataActivity.this, AddCustomerActivity.class);
                intent.putExtra("customer_id", customerId);
                startActivity(intent);
            }
        });

        btnShareWhatsappKhata.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double udhar = dbHelper.getCustomerTotalUdhar(customerId);
                double jama = dbHelper.getCustomerTotalJama(customerId);
                CustomerActivity.shareCustomerStatementOnWhatsApp(CustomerKhataActivity.this, currentCustomerName, currentCustomerPhone, udhar, jama);
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
                        .setTitle(R.string.delete)
                        .setMessage("Are you sure you want to delete this Khata transaction? Balance will be recalculated automatically.")
                        .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dbHelper.deleteKhataTransaction(txId, customerId);
                                Toast.makeText(CustomerKhataActivity.this, "Transaction deleted", Toast.LENGTH_SHORT).show();
                                refreshKhataData();
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
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
        Cursor c = dbHelper.getCustomerById(customerId);
        if (c != null && c.moveToFirst()) {
            currentCustomerName = c.getString(c.getColumnIndexOrThrow("name"));
            currentCustomerPhone = c.getString(c.getColumnIndexOrThrow("phone"));
            String address = c.getString(c.getColumnIndexOrThrow("address"));
            currentPhotoPath = c.getString(c.getColumnIndexOrThrow("image_path"));

            tvCustomerName.setText(currentCustomerName);
            tvCustomerPhone.setText("Phone: " + (currentCustomerPhone != null && !currentCustomerPhone.isEmpty() ? currentCustomerPhone : "-"));
            tvCustomerAddress.setText("Address: " + (address != null && !address.isEmpty() ? address : "-"));
            
            PhotoHelper.displayThumbnail(this, ivKhataCustomerPhoto, currentPhotoPath, 120, 120);
            c.close();
        }

        double udhar = dbHelper.getCustomerTotalUdhar(customerId);
        double jama = dbHelper.getCustomerTotalJama(customerId);
        double pending = udhar - jama;

        tvTotalUdhar.setText(String.format(Locale.getDefault(), "₹ %.2f", udhar));
        tvTotalJama.setText(String.format(Locale.getDefault(), "₹ %.2f", jama));
        tvPendingBalanceDetail.setText(String.format(Locale.getDefault(), "₹ %.2f", Math.abs(pending)));

        if (pending > 0) {
            tvPendingBalanceDetail.setTextColor(getResources().getColor(R.color.warning_red));
            tvBalanceStatus.setText(getString(R.string.customer_owes) + " ₹ " + String.format(Locale.getDefault(), "%.2f", pending));
            tvBalanceStatus.setTextColor(getResources().getColor(R.color.warning_red));
        } else if (pending < 0) {
            tvPendingBalanceDetail.setTextColor(getResources().getColor(R.color.success_green));
            tvBalanceStatus.setText(getString(R.string.advance_balance) + ": ₹ " + String.format(Locale.getDefault(), "%.2f", Math.abs(pending)));
            tvBalanceStatus.setTextColor(getResources().getColor(R.color.success_green));
        } else {
            tvPendingBalanceDetail.setTextColor(getResources().getColor(R.color.text_dark));
            tvBalanceStatus.setText(getString(R.string.account_clear));
            tvBalanceStatus.setTextColor(getResources().getColor(R.color.text_muted));
        }

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
                    if ("UDHAR".equalsIgnoreCase(type)) {
                        tv.setText(getString(R.string.udhar).toUpperCase());
                        tv.setTextColor(getResources().getColor(R.color.warning_red));
                        tv.setBackgroundColor(0xFFFFEBEE);
                    } else {
                        tv.setText(getString(R.string.jama).toUpperCase());
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
                    String text = (notes != null && !notes.isEmpty() ? notes : "-");
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
        boolean isUdhar = "UDHAR".equals(entryType);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isUdhar ? getString(R.string.add_udhar) : getString(R.string.add_jama));

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(40, 20, 40, 20);

        final EditText etAmount = new EditText(this);
        etAmount.setHint(getString(R.string.amount_required));
        etAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        container.addView(etAmount);

        final EditText etDate = new EditText(this);
        String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
        etDate.setText(currentDate);
        etDate.setHint(getString(R.string.date));
        container.addView(etDate);

        final Spinner spMode = new Spinner(this);
        if (!isUdhar) {
            TextView tvLabel = new TextView(this);
            tvLabel.setText(getString(R.string.payment_mode) + ":");
            tvLabel.setPadding(0, 15, 0, 5);
            container.addView(tvLabel);

            String[] modes = new String[]{"Cash", "UPI / GPay", "Card / NetBanking", "Other"};
            ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, modes);
            spMode.setAdapter(modeAdapter);
            container.addView(spMode);
        }

        final EditText etNotes = new EditText(this);
        etNotes.setHint(getString(R.string.description_notes));
        container.addView(etNotes);

        builder.setView(container);

        builder.setPositiveButton(getString(R.string.save), new DialogInterface.OnClickListener() {
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

                String dateStr = etDate.getText().toString().trim();
                if (dateStr.isEmpty()) dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

                String notes = etNotes.getText().toString().trim();
                String paymentMode = !entryType.equals("UDHAR") ? spMode.getSelectedItem().toString() : "Credit";

                dbHelper.addKhataTransaction(customerId, entryType, amount, dateStr, notes, paymentMode, "Manual Entry");
                Toast.makeText(CustomerKhataActivity.this, entryType + " Entry Recorded", Toast.LENGTH_SHORT).show();
                refreshKhataData();
            }
        });

        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.show();
    }
}