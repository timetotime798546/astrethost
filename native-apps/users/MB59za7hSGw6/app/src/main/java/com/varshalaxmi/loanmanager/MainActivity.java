package com.varshalaxmi.loanmanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
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

import java.io.File;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;
    private FrameLayout containerContent;
    private TextView tvHeaderTitle;

    private LinearLayout tabDashboard, tabCustomers, tabLoans, tabReports, tabSettings;
    private TextView iconDash, textDash, iconCust, textCust, iconLoans, textLoans, iconRep, textRep, iconSet, textSet;

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

        iconDash = findViewById(R.id.icon_dashboard);
        textDash = findViewById(R.id.text_dashboard);
        iconCust = findViewById(R.id.icon_customers);
        textCust = findViewById(R.id.text_customers);
        iconLoans = findViewById(R.id.icon_loans);
        textLoans = findViewById(R.id.text_loans);
        iconRep = findViewById(R.id.icon_reports);
        textRep = findViewById(R.id.text_reports);
        iconSet = findViewById(R.id.icon_settings);
        textSet = findViewById(R.id.text_settings);

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

    private void highlightActiveTab(int tabIndex) {
        // Reset all tabs
        tabDashboard.setBackgroundResource(0);
        tabCustomers.setBackgroundResource(0);
        tabLoans.setBackgroundResource(0);
        tabReports.setBackgroundResource(0);
        tabSettings.setBackgroundResource(0);

        int inactiveColor = Color.parseColor("#475569");
        int activeColor = Color.parseColor("#0B4632");

        textDash.setTextColor(inactiveColor);
        textCust.setTextColor(inactiveColor);
        textLoans.setTextColor(inactiveColor);
        textRep.setTextColor(inactiveColor);
        textSet.setTextColor(inactiveColor);

        textDash.setTypeface(null, Typeface.NORMAL);
        textCust.setTypeface(null, Typeface.NORMAL);
        textLoans.setTypeface(null, Typeface.NORMAL);
        textRep.setTypeface(null, Typeface.NORMAL);
        textSet.setTypeface(null, Typeface.NORMAL);

        if (tabIndex == 0) {
            tabDashboard.setBackgroundResource(R.drawable.bg_nav_tab_active);
            textDash.setTextColor(activeColor);
            textDash.setTypeface(null, Typeface.BOLD);
        } else if (tabIndex == 1) {
            tabCustomers.setBackgroundResource(R.drawable.bg_nav_tab_active);
            textCust.setTextColor(activeColor);
            textCust.setTypeface(null, Typeface.BOLD);
        } else if (tabIndex == 2) {
            tabLoans.setBackgroundResource(R.drawable.bg_nav_tab_active);
            textLoans.setTextColor(activeColor);
            textLoans.setTypeface(null, Typeface.BOLD);
        } else if (tabIndex == 3) {
            tabReports.setBackgroundResource(R.drawable.bg_nav_tab_active);
            textRep.setTextColor(activeColor);
            textRep.setTypeface(null, Typeface.BOLD);
        } else if (tabIndex == 4) {
            tabSettings.setBackgroundResource(R.drawable.bg_nav_tab_active);
            textSet.setTextColor(activeColor);
            textSet.setTypeface(null, Typeface.BOLD);
        }
    }

    private void switchTab(int tabIndex) {
        currentTab = tabIndex;
        highlightActiveTab(tabIndex);
        containerContent.removeAllViews();

        if (tabIndex == 0) {
            tvHeaderTitle.setText("VarshaLaxmi");
            renderDashboardView();
        } else if (tabIndex == 1) {
            tvHeaderTitle.setText("Customers");
            renderCustomersView();
        } else if (tabIndex == 2) {
            tvHeaderTitle.setText("Active Loans");
            renderLoansView();
        } else if (tabIndex == 3) {
            tvHeaderTitle.setText("Ledger Reports");
            renderReportsView();
        } else if (tabIndex == 4) {
            tvHeaderTitle.setText("Settings & Backup");
            renderSettingsView();
        }
    }

    // --- DASHBOARD VIEW ---
    private void renderDashboardView() {
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        double given = dbHelper.getTotalMoneyGiven();
        double collected = dbHelper.getTotalMoneyCollected();
        double todayColl = dbHelper.getTodayCollection();
        int totalCust = dbHelper.getCustomerCount();

        List<Loan> allLoans = dbHelper.getAllLoans();
        double totalOutstanding = 0;
        for (Loan l : allLoans) {
            totalOutstanding += l.getRemainingBalance();
        }

        // Action Buttons Row
        LinearLayout btnBox = new LinearLayout(this);
        btnBox.setOrientation(LinearLayout.HORIZONTAL);
        btnBox.setPadding(0, 0, 0, 16);

        Button btnAddCust = new Button(this);
        btnAddCust.setText("+ Add Customer");
        btnAddCust.setBackgroundResource(R.drawable.bg_btn_primary);
        btnAddCust.setTextColor(Color.WHITE);
        btnAddCust.setTextSize(13);
        btnAddCust.setTypeface(null, Typeface.BOLD);
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
        btnAddLoan.setText("+ Give New Loan");
        btnAddLoan.setBackgroundResource(R.drawable.bg_btn_gold);
        btnAddLoan.setTextColor(Color.parseColor("#0F172A"));
        btnAddLoan.setTextSize(13);
        btnAddLoan.setTypeface(null, Typeface.BOLD);
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

        // Stats Cards Grid
        root.addView(createGradientStatCard("👥 Total Registered Clients", String.valueOf(totalCust) + " Clients", R.drawable.bg_stat_emerald));
        root.addView(createGradientStatCard("⚠️ Total Outstanding Due", "₹ " + String.format(Locale.getDefault(), "%,.2f", totalOutstanding), R.drawable.bg_stat_red));
        root.addView(createGradientStatCard("📅 Today's Collection", "₹ " + String.format(Locale.getDefault(), "%,.2f", todayColl), R.drawable.bg_stat_gold));
        root.addView(createCardStatView("💸 Total Capital Disbursed", "₹ " + String.format(Locale.getDefault(), "%,.2f", given), "#0F172A"));
        root.addView(createCardStatView("💰 Total Interest & Capital Collected", "₹ " + String.format(Locale.getDefault(), "%,.2f", collected), "#16A34A"));

        sv.addView(root);
        containerContent.addView(sv);
    }

    private View createGradientStatCard(String title, String val, int bgDrawableRes) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(20, 18, 20, 18);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = 12;
        card.setLayoutParams(lp);
        card.setBackgroundResource(bgDrawableRes);
        card.setElevation(4f);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(Color.parseColor("#E2E8F0"));
        tvTitle.setTextSize(12);

        TextView tvVal = new TextView(this);
        tvVal.setText(val);
        tvVal.setTextColor(Color.WHITE);
        tvVal.setTextSize(22);
        tvVal.setTypeface(null, Typeface.BOLD);
        tvVal.setPadding(0, 4, 0, 0);

        card.addView(tvTitle);
        card.addView(tvVal);
        return card;
    }

    private View createCardStatView(String title, String val, String colorHex) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(18, 16, 18, 16);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = 12;
        card.setLayoutParams(lp);
        card.setBackgroundResource(R.drawable.bg_card_premium);
        card.setElevation(3f);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(Color.parseColor("#64748B"));
        tvTitle.setTextSize(12);

        TextView tvVal = new TextView(this);
        tvVal.setText(val);
        tvVal.setTextColor(Color.parseColor(colorHex));
        tvVal.setTextSize(20);
        tvVal.setTypeface(null, Typeface.BOLD);
        tvVal.setPadding(0, 4, 0, 0);

        card.addView(tvTitle);
        card.addView(tvVal);
        return card;
    }

    // --- CUSTOMERS VIEW ---
    private void renderCustomersView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        EditText etSearch = new EditText(this);
        etSearch.setHint("🔍 Search customer name or mobile...");
        etSearch.setBackgroundResource(R.drawable.bg_edit_text);
        etSearch.setPadding(16, 14, 16, 14);
        etSearch.setElevation(2f);
        root.addView(etSearch);

        final LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(0, 14, 0, 0);

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
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

        if (list.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No customers found matching your query.");
            tvEmpty.setTextColor(Color.parseColor("#94A3B8"));
            tvEmpty.setPadding(0, 40, 0, 0);
            tvEmpty.setGravity(android.view.Gravity.CENTER);
            container.addView(tvEmpty);
            return;
        }

        for (final Customer c : list) {
            View view = LayoutInflater.from(this).inflate(R.layout.item_customer, container, false);
            TextView tvAvatar = view.findViewById(R.id.tv_avatar_initial);
            TextView tvName = view.findViewById(R.id.tv_cust_name);
            TextView tvMobile = view.findViewById(R.id.tv_cust_mobile);
            TextView tvBalance = view.findViewById(R.id.tv_cust_balance);

            Button btnLoan = view.findViewById(R.id.btn_action_loan);
            Button btnPayment = view.findViewById(R.id.btn_action_payment);

            String initial = (c.getName() != null && c.getName().length() > 0) ? c.getName().substring(0, 1).toUpperCase() : "C";
            tvAvatar.setText(initial);

            tvName.setText(c.getName());
            tvMobile.setText("📞 " + (c.getMobile() != null && !c.getMobile().isEmpty() ? c.getMobile() : "N/A") + " | 📍 " + (c.getAddress() != null && !c.getAddress().isEmpty() ? c.getAddress() : "N/A"));
            tvBalance.setText("₹ " + String.format(Locale.getDefault(), "%,.2f", c.getOutstandingBalance()));

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

    // --- LOANS VIEW ---
    private void renderLoansView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);

        List<Loan> loans = dbHelper.getAllLoans();
        if (loans.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No active loans present in ledger.");
            tvEmpty.setTextColor(Color.parseColor("#94A3B8"));
            tvEmpty.setPadding(0, 40, 0, 0);
            tvEmpty.setGravity(android.view.Gravity.CENTER);
            listContainer.addView(tvEmpty);
        } else {
            for (final Loan l : loans) {
                View v = LayoutInflater.from(this).inflate(R.layout.item_loan, listContainer, false);
                TextView tvCust = v.findViewById(R.id.tv_loan_customer);
                TextView tvPrincipal = v.findViewById(R.id.tv_loan_principal);
                TextView tvMethod = v.findViewById(R.id.tv_loan_method_rate);
                TextView tvDate = v.findViewById(R.id.tv_loan_date);
                TextView tvRem = v.findViewById(R.id.tv_loan_remaining);
                Button btnPay = v.findViewById(R.id.btn_collect_loan_pay);

                tvCust.setText(l.getCustomerName());
                tvPrincipal.setText("Principal: ₹ " + String.format(Locale.getDefault(), "%,.2f", l.getPrincipalAmount()));
                tvMethod.setText("Rate: " + l.getInterestRate() + "% (" + l.getInterestMethod() + ")");
                tvDate.setText("Issued: " + l.getIssueDate());
                tvRem.setText("₹ " + String.format(Locale.getDefault(), "%,.2f", l.getRemainingBalance()));

                btnPay.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showAddPaymentDialogForLoan(l);
                    }
                });

                listContainer.addView(v);
            }
        }

        sv.addView(listContainer);
        root.addView(sv);
        containerContent.addView(root);
    }

    // --- REPORTS VIEW ---
    private void renderReportsView() {
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("📊 Payment Collections Ledger");
        tvTitle.setTextSize(18);
        tvTitle.setTextColor(Color.parseColor("#0B4632"));
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, 14);
        root.addView(tvTitle);

        List<Payment> payments = dbHelper.getAllPayments();
        root.addView(createCardStatView("Collection History Records", payments.size() + " Total Payments Processed", "#0B4632"));

        if (payments.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No collection records stored.");
            tvEmpty.setTextColor(Color.parseColor("#94A3B8"));
            tvEmpty.setPadding(0, 30, 0, 0);
            tvEmpty.setGravity(android.view.Gravity.CENTER);
            root.addView(tvEmpty);
        } else {
            for (Payment p : payments) {
                View pv = LayoutInflater.from(this).inflate(R.layout.item_payment, root, false);
                ((TextView) pv.findViewById(R.id.tv_pay_customer)).setText(p.getCustomerName());
                ((TextView) pv.findViewById(R.id.tv_pay_amount)).setText("₹ " + String.format(Locale.getDefault(), "%,.2f", p.getAmount()));
                ((TextView) pv.findViewById(R.id.tv_pay_note)).setText(p.getNote() != null && !p.getNote().isEmpty() ? p.getNote() : "Regular Repayment");
                ((TextView) pv.findViewById(R.id.tv_pay_date)).setText(p.getPaymentDate());
                root.addView(pv);
            }
        }

        sv.addView(root);
        containerContent.addView(sv);
    }

    // --- SETTINGS VIEW ---
    private void renderSettingsView() {
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("⚙️ App Settings & Data Backup");
        tvTitle.setTextSize(18);
        tvTitle.setTextColor(Color.parseColor("#0B4632"));
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, 16);
        root.addView(tvTitle);

        Button btnBackup = new Button(this);
        btnBackup.setText("📦 Export Backup (ZIP)");
        btnBackup.setBackgroundResource(R.drawable.bg_btn_primary);
        btnBackup.setTextColor(Color.WHITE);
        btnBackup.setTextSize(14);
        btnBackup.setTypeface(null, Typeface.BOLD);
        btnBackup.setPadding(0, 14, 0, 14);
        btnBackup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    File file = BackupRestoreHelper.createZipBackup(MainActivity.this, dbHelper);
                    Toast.makeText(MainActivity.this, "Backup archive saved: " + file.getName(), Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Backup failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        root.addView(btnBackup);

        Button btnRestore = new Button(this);
        btnRestore.setText("🔄 Restore Data from Backup");
        btnRestore.setBackgroundResource(R.drawable.bg_btn_gold);
        btnRestore.setTextColor(Color.parseColor("#0F172A"));
        btnRestore.setTextSize(14);
        btnRestore.setTypeface(null, Typeface.BOLD);
        btnRestore.setPadding(0, 14, 0, 14);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 14;
        btnRestore.setLayoutParams(lp);
        btnRestore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File backupDir = new File(getExternalFilesDir(null), "backups");
                File zipFile = new File(backupDir, "VarshaLaxmi_Backup.zip");
                if (!zipFile.exists()) {
                    Toast.makeText(MainActivity.this, "No ZIP backup file found to restore!", Toast.LENGTH_SHORT).show();
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

    // --- DIALOGS ---
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
                            Toast.makeText(MainActivity.this, "Customer name is required!", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Please add a customer profile first!", Toast.LENGTH_SHORT).show();
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
        spCust.setBackgroundResource(R.drawable.bg_edit_text);
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

        ((LinearLayout) view).addView(spCust, 2);

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
                            Toast.makeText(MainActivity.this, "Please enter valid numeric principal and rate!", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(MainActivity.this, "Please enter a valid payment amount!", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void showCustomerDetailsDialog(final Customer c) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(c.getName())
                .setMessage("📞 Mobile: " + (c.getMobile() != null ? c.getMobile() : "N/A") +
                        "\n📍 Address: " + (c.getAddress() != null ? c.getAddress() : "N/A") +
                        "\n\nTotal Disbursed: ₹ " + String.format(Locale.getDefault(), "%,.2f", c.getTotalGiven()) +
                        "\nTotal Paid: ₹ " + String.format(Locale.getDefault(), "%,.2f", c.getTotalPaid()) +
                        "\nOutstanding Due: ₹ " + String.format(Locale.getDefault(), "%,.2f", c.getOutstandingBalance()))
                .setPositiveButton("OK", null)
                .setNegativeButton("Delete Profile", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean deleted = dbHelper.deleteCustomer(c.getId());
                        if (deleted) {
                            Toast.makeText(MainActivity.this, "Customer profile deleted", Toast.LENGTH_SHORT).show();
                            switchTab(currentTab);
                        } else {
                            Toast.makeText(MainActivity.this, "Cannot delete customer with active financial loans!", Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .show();
    }
}