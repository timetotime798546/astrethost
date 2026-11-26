package com.mumbaimedical.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class CustomerActivity extends Activity {

    private DatabaseHelper dbHelper;
    private ListView lvCustomers;
    private EditText etSearchCustomer;
    private Button btnAddCustomer;
    private TextView tvEmptyCustomers;
    private SimpleCursorAdapter adapter;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer);

        dbHelper = new DatabaseHelper(this);
        lvCustomers = findViewById(R.id.lvCustomers);
        etSearchCustomer = findViewById(R.id.etSearchCustomer);
        btnAddCustomer = findViewById(R.id.btnAddCustomer);
        tvEmptyCustomers = findViewById(R.id.tvEmptyCustomers);

        btnAddCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(CustomerActivity.this, AddCustomerActivity.class));
            }
        });

        etSearchCustomer.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadCustomers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        lvCustomers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(CustomerActivity.this, CustomerKhataActivity.class);
                intent.putExtra("customer_id", id);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCustomers(etSearchCustomer.getText().toString());
    }

    private void loadCustomers(String query) {
        Cursor cursor = dbHelper.searchCustomers(query);

        if (cursor == null || cursor.getCount() == 0) {
            tvEmptyCustomers.setVisibility(View.VISIBLE);
        } else {
            tvEmptyCustomers.setVisibility(View.GONE);
        }

        String[] from = new String[]{"name", "phone", "address", "pending_balance", "image_path", "name", "phone"};
        int[] to = new int[]{R.id.tvCustomerName, R.id.tvCustomerPhone, R.id.tvCustomerAddress, R.id.tvPendingBalance, R.id.ivCustomerItemThumb, R.id.btnDeleteCustomer, R.id.btnShareWhatsappCustomer};

        adapter = new SimpleCursorAdapter(
                this,
                R.layout.item_customer,
                cursor,
                from,
                to,
                0
        );

        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                int id = view.getId();
                if (id == R.id.ivCustomerItemThumb) {
                    String photoPath = cursor.getString(columnIndex);
                    PhotoHelper.displayThumbnail(CustomerActivity.this, (ImageView) view, photoPath, 100, 100);
                    return true;
                } else if (id == R.id.tvCustomerPhone) {
                    String phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
                    ((TextView) view).setText("Phone: " + (phone != null && !phone.isEmpty() ? phone : "N/A"));
                    return true;
                } else if (id == R.id.tvCustomerAddress) {
                    String addr = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                    ((TextView) view).setText("Address: " + (addr != null && !addr.isEmpty() ? addr : "N/A"));
                    return true;
                } else if (id == R.id.tvPendingBalance) {
                    double balance = cursor.getDouble(cursor.getColumnIndexOrThrow("pending_balance"));
                    TextView tv = (TextView) view;
                    if (balance > 0) {
                        tv.setText(String.format("Due: ₹ %.2f", balance));
                        tv.setTextColor(getResources().getColor(R.color.warning_red));
                    } else if (balance < 0) {
                        tv.setText(String.format("Advance: ₹ %.2f", Math.abs(balance)));
                        tv.setTextColor(getResources().getColor(R.color.success_green));
                    } else {
                        tv.setText("Clear (₹ 0.00)");
                        tv.setTextColor(getResources().getColor(R.color.text_muted));
                    }
                    return true;
                } else if (id == R.id.btnDeleteCustomer) {
                    final long custId = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
                    final String custName = cursor.getString(cursor.getColumnIndexOrThrow("name"));

                    view.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            confirmAndDeleteCustomer(custId, custName);
                        }
                    });
                    return true;
                } else if (id == R.id.btnShareWhatsappCustomer) {
                    final long custId = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
                    final String custName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                    final String custPhone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));

                    view.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            double totalUdhar = dbHelper.getCustomerTotalUdhar(custId);
                            double totalJama = dbHelper.getCustomerTotalJama(custId);
                            shareCustomerStatementOnWhatsApp(CustomerActivity.this, custName, custPhone, totalUdhar, totalJama);
                        }
                    });
                    return true;
                }
                return false;
            }
        });

        lvCustomers.setAdapter(adapter);
    }

    private void confirmAndDeleteCustomer(final long custId, final String custName) {
        String msg = getString(R.string.confirm_delete_customer, custName);
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_delete)
                .setMessage(msg)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean deleted = dbHelper.deleteCustomerSafely(custId);
                        if (deleted) {
                            Toast.makeText(CustomerActivity.this, custName + " deleted successfully", Toast.LENGTH_SHORT).show();
                            loadCustomers(etSearchCustomer.getText().toString());
                        } else {
                            new AlertDialog.Builder(CustomerActivity.this)
                                    .setTitle("Cannot Delete Customer")
                                    .setMessage(getString(R.string.cannot_delete_customer, custName))
                                    .setPositiveButton(R.string.ok, null)
                                    .show();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    public static void shareCustomerStatementOnWhatsApp(Context context, String customerName, String phone, double totalUdhar, double totalJama) {
        if (phone == null || phone.trim().isEmpty()) {
            Toast.makeText(context, R.string.no_mobile_available, Toast.LENGTH_SHORT).show();
            return;
        }

        String cleanPhone = phone.replaceAll("[^0-9]", "");
        if (cleanPhone.isEmpty()) {
            Toast.makeText(context, R.string.no_mobile_available, Toast.LENGTH_SHORT).show();
            return;
        }

        if (cleanPhone.length() == 10) {
            cleanPhone = "91" + cleanPhone;
        }

        double pendingBalance = totalUdhar - totalJama;
        String statusMsg;
        if (pendingBalance > 0) {
            statusMsg = "Aapka itna payment baki hai.";
        } else if (pendingBalance < 0) {
            statusMsg = "Aapka itna advance jama hai.";
        } else {
            statusMsg = "Aapka account clear hai.";
        }

        String message = "Namaste " + customerName + " ji,\n\n" +
                "Mumbai Medical – Account Statement\n\n" +
                "Customer: " + customerName + "\n" +
                "Mobile: " + phone + "\n\n" +
                "Total Udhar: ₹" + String.format(Locale.getDefault(), "%.2f", totalUdhar) + "\n" +
                "Total Jama: ₹" + String.format(Locale.getDefault(), "%.2f", totalJama) + "\n\n" +
                "Current Balance: ₹" + String.format(Locale.getDefault(), "%.2f", Math.abs(pendingBalance)) + "\n\n" +
                statusMsg + "\n\n" +
                "Thank you!\nMumbai Medical";

        try {
            String url = "https://api.whatsapp.com/send?phone=" + cleanPhone + "&text=" + Uri.encode(message);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            context.startActivity(intent);
        } catch (Exception e) {
            try {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, message);
                context.startActivity(Intent.createChooser(shareIntent, "Share Statement via"));
            } catch (Exception ex) {
                Toast.makeText(context, "WhatsApp application is not installed.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}