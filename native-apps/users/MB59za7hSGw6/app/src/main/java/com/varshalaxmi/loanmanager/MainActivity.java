package com.varshalaxmi.loanmanager;

import android.app.Activity;
import android.app.AlertDialog;

import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.varshalaxmi.loanmanager.db.DatabaseHelper;
import com.varshalaxmi.loanmanager.model.Customer;
import com.varshalaxmi.loanmanager.model.Loan;
import com.varshalaxmi.loanmanager.model.Payment;
import com.varshalaxmi.loanmanager.util.BackupRestoreHelper;
import com.varshalaxmi.loanmanager.util.InterestCalculator;

import java.io.File;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;
    private FrameLayout containerContent;
    private TextView tvHeaderTitle;

    private LinearLayout tabDashboard, tabCustomers, tabLoans, tabReports, tabSettings;

    private String currentLanguage = "hi";
    private int currentTab = 0; // 0: Dash, 1: Cust, 2: Loans, 3: Reports, 4: Settings

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DatabaseHelper(this);
        currentLanguage = dbHelper.getSetting("language", "hi");
        setAppLocale(currentLanguage);

        setContentView(R.layout.activity_main);

        tvHeaderTitle = findViewById(R.id.tv_header_title);
        containerContent = findViewById(R.id.container_content);

        tabDashboard = findViewById(R.id.tab_dashboard);
        tabCustomers = findViewById(R.id.tab_customers);
        tabLoans = findViewById(R.id.tab_loans);
        tabReports = findViewById(R.id.tab_reports);
        tabSettings = findViewById(R.id.tab_settings);

        findViewById(R.id.btn_switch_language).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleLanguage();
            }
        });

        tabDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(0);
            }
        });

        tabCustomers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(1);
            }
        });

        tabLoans.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
            }
        });

        tabReports.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(3);
            }
        });

        tabSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(4);
            }
        });

        switchTab(0);
    }

    private void toggleLanguage() {
        if ("hi".equalsIgnoreCase(currentLanguage)) {
            currentLanguage = "en";
        } else {
            currentLanguage = "hi";
        }
        dbHelper.setSetting("language", currentLanguage);
        setAppLocale(currentLanguage);
        Toast.makeText(this, "Language updated", Toast.LENGTH_SHORT).show();
        recreate();
    }

    private void setAppLocale(String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    private void switchTab(int tabIndex) {
        currentTab = tabIndex;
        containerContent.removeAllViews();

        if (tabIndex == 0) {
            tvHeaderTitle.setText("VarshaLaxmi - Dashboard");
            renderDashboardView();
        } else if (tabIndex == 1) {
            tvHeaderTitle.setText("VarshaLaxmi - Customers");
            renderCustomersView();
        } else if (tabIndex == 2) {
            tvHeaderTitle.setText("VarshaLaxmi - Loans");
            renderLoansView();
        } else if (tabIndex == 3) {
            tvHeaderTitle.setText("VarshaLaxmi - Reports");
            renderReportsView();
        } else if (tabIndex == 4) {
            tvHeaderTitle.setText("VarshaLaxmi - Settings");
            renderSettingsView();
        }
    }

    // --- DASHBOARD SCREEN ---
    private void renderDashboardView() {
        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        // Stats Card Grid
        double given = dbHelper.getTotalMoneyGiven();
        double collected = dbHelper.getTotalMoneyCollected();
        double todayColl = dbHelper.getTodayCollection();
        int totalCust = dbHelper.getCustomerCount();

        List<Loan> allLoans = dbHelper.getAllLoans();
        double totalOutstanding = 0;
        for (Loan l : allLoans) {
            totalOutstanding += l.getRemainingBalance();
        }

        root.addView(createStatCard("👥 Total Customers", String.valueOf(totalCust), "#0F5132"));
        root.addView(createStatCard("💸 Total Money Given", "₹ " + String.format("%.2f", given), "#1E293B"));
        root.addView(createStatCard("💰 Total Money Collected", "₹ " + String.format("%.2f", collected), "#16A34A"));
        root.addView(createStatCard("⚠️ Total Outstanding", "₹ " + String.format("%.2f", totalOutstanding), "#DC2626"));
        root.addView(createStatCard("📅 Today's Collection", "₹ " + String.format("%.2f", todayColl), "#D4AF37"));

        // Quick Action Buttons
        LinearLayout btnBox = new LinearLayout(this);
        btnBox.setOrientation(LinearLayout.HORIZONTAL);
        btnBox.setPadding(0, 16, 0, 16);

        Button btnAddCust = new Button(this);
        btnAddCust.setText("+ Add Customer");
        btnAddCust.setBackgroundColor(0xFF0F5132);
        btnAddCust.setTextColor(0xFFFFFFFF);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        lp.rightMargin = 8;
        btnAddCust.setLayoutParams(lp);
        btnAddCust.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddCustomerDialog();
            }
        });

        Button btnAddLoan = new Button(this);
        btnAddLoan.setText("+ Give Loan");
        btnAddLoan.setBackgroundColor(0xFFD4AF37);
        btnAddLoan.setTextColor(0xFF0F172A);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        lp2.leftMargin = 8;
        btnAddLoan.setLayoutParams(lp2);
        btnAddLoan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddLoanDialog(0);
            }
        });

        btnBox.addView(btnAddCust);
        btnBox.addView(btnAddLoan);
        root.addView(btnBox);

        sv.addView(root);
        containerContent.addView(sv);
    }

    private View createStatCard(String title, String val, String colorHex) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(16, 16, 16, 16);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = 10;
        card.setLayoutParams(lp);
        card.setBackgroundColor(0xFFFFFFFF);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(0xFF64748B);
        tvTitle.setTextSize(13);

        TextView tvVal = new TextView(this);
        tvVal.setText(val);
        tvVal.setTextColor(android.graphics.Color.parseColor(colorHex));
        tvVal.setTextSize(20);
        tvVal.setTypeface(null, android.graphics.Typeface.BOLD);
        tvVal.setPadding(0, 4, 0, 0);

        card.addView(tvTitle);
        card.addView(tvVal);
        return card;
    }

    // --- CUSTOMERS SCREEN ---
    private void renderCustomersView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        EditText etSearch = new EditText(this);
        etSearch.setHint("🔍 Search customer name or mobile...");
        etSearch.setBackgroundColor(0xFFFFFFFF);
        etSearch.setPadding(16, 12, 16, 12);
        root.addView(etSearch);

        final LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(0, 10, 0, 0);

        ScrollView sv = new ScrollView(this);
        sv.addView(listContainer);
        root.addView(sv);

        loadCustomerList(listContainer, null);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadCustomerList(listContainer, s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        containerContent.addView(root);
    }

    private void loadCustomerList(LinearLayout container, String query) {
        container.removeAllViews();
        List<Customer> list = dbHelper.getAllCustomers(query);
        for (final Customer c : list) {
            View view = LayoutInflater.from(this).inflate(R.layout.item_customer, container, false);
            TextView tvName = view.findViewById(R.id.tv_cust_name);
            TextView tvMobile = view.findViewById(R.id.tv_cust_mobile);
            TextView tvBalance = view.findViewById(R.id.tv_cust_balance);

            Button btnLoan = view.findViewById(R.id.btn_action_loan);
            Button btnPayment = view.findViewById(R.id.btn_action_payment);

            tvName.setText(c.getName());
            tvMobile.setText("📞 " + (c.getMobile() != null ? c.getMobile() : "N/A") + " | 📍 " + (c.getAddress() != null ? c.getAddress() : "N/A"));
            tvBalance.setText("Due: ₹ " + String.format("%.2f", c.getOutstandingBalance()));

            btnLoan.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAddLoanDialog(c.getId());
                }
            });

            btnPayment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAddPaymentDialog(c.getId());
                }
            });

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showCustomerDetailsDialog(c);
                }
            });

            container.addView(view);
        }
    }

    // --- LOANS SCREEN ---
    private void renderLoansView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        ScrollView sv = new ScrollView(this);
        LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);

        List<Loan> loans = dbHelper.getAllLoans();
        for (final Loan l : loans) {
            View v = LayoutInflater.from(this).inflate(R.layout.item_loan, listContainer, false);
            TextView tvCust = v.findViewById(R.id.tv_loan_customer);
            TextView tvPrincipal = v.findViewById(R.id.tv_loan_principal);
            TextView tvMethod = v.findViewById(R.id.tv_loan_method_rate);
            TextView tvDate = v.findViewById(R.id.tv_loan_date);
            TextView tvRem = v.findViewById(R.id.tv_loan_remaining);
            Button btnPay = v.findViewById(R.id.btn_collect_loan_pay);

            tvCust.setText(l.getCustomerName());
            tvPrincipal.setText("Principal: ₹ " + String.format("%.2f", l.getPrincipalAmount()));
            tvMethod.setText("Rate: " + l.getInterestRate() + "% (" + l.getInterestMethod() + ")");
            tvDate.setText(l.getIssueDate());
            tvRem.setText("Due + Interest: ₹ " + String.format("%.2f", l.getRemainingBalance()));

            btnPay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAddPaymentDialogForLoan(l);
                }
            });

            listContainer.addView(v);
        }

        sv.addView(listContainer);
        root.addView(sv);
        containerContent.addView(root);
    }

    // --- REPORTS SCREEN ---
    private void renderReportsView() {
        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("📊 Business Ledger Summary");
        tvTitle.setTextSize(18);
        tvTitle.setTextColor(0xFF0F5132);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, 16);
        root.addView(tvTitle);

        List<Payment> payments = dbHelper.getAllPayments();
        root.addView(createStatCard("Recent Payment Collections", "Total Records: " + payments.size(), "#0F5132"));

        for (Payment p : payments) {
            View pv = LayoutInflater.from(this).inflate(R.layout.item_payment, root, false);
            ((TextView) pv.findViewById(R.id.tv_pay_customer)).setText(p.getCustomerName());
            ((TextView) pv.findViewById(R.id.tv_pay_amount)).setText("₹ " + String.format("%.2f", p.getAmount()));
            ((TextView) pv.findViewById(R.id.tv_pay_note)).setText(p.getNote() != null ? p.getNote() : "Regular Collection");
            ((TextView) pv.findViewById(R.id.tv_pay_date)).setText(p.getPaymentDate());
            root.addView(pv);
        }

        sv.addView(root);
        containerContent.addView(sv);
    }

    // --- SETTINGS SCREEN ---
    private void renderSettingsView() {
        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        Button btnBackup = new Button(this);
        btnBackup.setText("📦 " + getString(R.string.btn_backup));
        btnBackup.setBackgroundColor(0xFF0F5132);
        btnBackup.setTextColor(0xFFFFFFFF);
        btnBackup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    File file = BackupRestoreHelper.createZipBackup(MainActivity.this, dbHelper);
                    Toast.makeText(MainActivity.this, "Backup saved to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Backup failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        root.addView(btnBackup);

        Button btnRestore = new Button(this);
        btnRestore.setText("🔄 " + getString(R.string.btn_restore));
        btnRestore.setBackgroundColor(0xFFD4AF37);
        btnRestore.setTextColor(0xFF0F172A);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 16;
        btnRestore.setLayoutParams(lp);
        btnRestore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File backupDir = new File(getExternalFilesDir(null), "backups");
                File zipFile = new File(backupDir, "VarshaLaxmi_Backup.zip");
                if (!zipFile.exists()) {
                    Toast.makeText(MainActivity.this, "No backup file found!", Toast.LENGTH_SHORT).show();
                    return;
                }
                boolean success = BackupRestoreHelper.restoreFromZip(MainActivity.this, zipFile, dbHelper);
                if (success) {
                    Toast.makeText(MainActivity.this, "Data restored successfully!", Toast.LENGTH_SHORT).show();
                    switchTab(0);
                } else {
                    Toast.makeText(MainActivity.this, "Restore failed!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        root.addView(btnRestore);

        sv.addView(root);
        containerContent.addView(sv);
    }

    // --- DIALOG BUILDERS ---
    private void showAddCustomerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_customer, null);
        final EditText etName = view.findViewById(R.id.et_cust_name);
        final EditText etMobile = view.findViewById(R.id.et_cust_mobile);
        final EditText etAddress = view.findViewById(R.id.et_cust_address);

        builder.setView(view)
                .setPositiveButton(R.string.btn_save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = etName.getText().toString().trim();
                        String mobile = etMobile.getText().toString().trim();
                        String address = etAddress.getText().toString().trim();

                        if (name.isEmpty()) {
                            Toast.makeText(MainActivity.this, "Name is required!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        dbHelper.addCustomer(name, mobile, address);
                        Toast.makeText(MainActivity.this, "Customer added!", Toast.LENGTH_SHORT).show();
                        switchTab(currentTab);
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void showAddLoanDialog(final long targetCustomerId) {
        List<Customer> customers = dbHelper.getAllCustomers(null);
        if (customers.isEmpty()) {
            Toast.makeText(this, "Please add a customer first!", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_loan, null);
        final EditText etAmount = view.findViewById(R.id.et_loan_amount);
        final EditText etRate = view.findViewById(R.id.et_loan_rate);
        final Spinner spMethod = view.findViewById(R.id.sp_interest_method);
        final EditText etNotes = view.findViewById(R.id.et_loan_notes);

        String[] methods = new String[]{"No Interest", "Daily Interest", "Monthly Interest", "Yearly Interest", "Simple Interest", "Flat Interest", "Reducing Balance"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, methods);
        spMethod.setAdapter(adapter);

        final Spinner spCust = new Spinner(this);
        String[] custNames = new String[customers.size()];
        final long[] custIds = new long[customers.size()];
        int selectedIndex = 0;
        for (int i = 0; i < customers.size(); i++) {
            custNames[i] = customers.get(i).getName();
            custIds[i] = customers.get(i).getId();
            if (targetCustomerId == customers.get(i).getId()) {
                selectedIndex = i;
            }
        }
        ArrayAdapter<String> custAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, custNames);
        spCust.setAdapter(custAdapter);
        spCust.setSelection(selectedIndex);

        ((LinearLayout) view).addView(spCust, 1);

        builder.setView(view)
                .setPositiveButton(R.string.btn_save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            double amount = Double.parseDouble(etAmount.getText().toString().trim());
                            double rate = Double.parseDouble(etRate.getText().toString().trim());
                            String method = spMethod.getSelectedItem().toString();
                            String notes = etNotes.getText().toString().trim();
                            long cId = custIds[spCust.getSelectedItemPosition()];

                            dbHelper.addLoan(cId, amount, rate, method, null, notes);
                            Toast.makeText(MainActivity.this, "Loan issued successfully!", Toast.LENGTH_SHORT).show();
                            switchTab(currentTab);
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "Invalid number inputs!", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void showAddPaymentDialog(long customerId) {
        List<Loan> loans = dbHelper.getLoansForCustomer(customerId);
        if (loans.isEmpty()) {
            Toast.makeText(this, "No active loan found for this customer!", Toast.LENGTH_SHORT).show();
            return;
        }
        showAddPaymentDialogForLoan(loans.get(0));
    }

    private void showAddPaymentDialogForLoan(final Loan loan) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_payment, null);
        final EditText etAmount = view.findViewById(R.id.et_pay_amount);
        final EditText etNote = view.findViewById(R.id.et_pay_note);

        builder.setView(view)
                .setPositiveButton(R.string.btn_save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            double amt = Double.parseDouble(etAmount.getText().toString().trim());
                            String note = etNote.getText().toString().trim();

                            dbHelper.addPayment(loan.getId(), loan.getCustomerId(), amt, note);
                            Toast.makeText(MainActivity.this, "Payment collected!", Toast.LENGTH_SHORT).show();
                            switchTab(currentTab);
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "Invalid payment amount!", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void showCustomerDetailsDialog(final Customer c) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(c.getName())
                .setMessage("📞 Mobile: " + c.getMobile() + "\n📍 Address: " + c.getAddress() + "\n\nTotal Given: ₹ " + c.getTotalGiven() + "\nTotal Paid: ₹ " + c.getTotalPaid() + "\nOutstanding: ₹ " + String.format("%.2f", c.getOutstandingBalance()))
                .setPositiveButton("OK", null)
                .setNegativeButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean deleted = dbHelper.deleteCustomer(c.getId());
                        if (deleted) {
                            Toast.makeText(MainActivity.this, "Customer deleted", Toast.LENGTH_SHORT).show();
                            switchTab(currentTab);
                        } else {
                            Toast.makeText(MainActivity.this, "Cannot delete customer with active loans!", Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .show();
    }
}