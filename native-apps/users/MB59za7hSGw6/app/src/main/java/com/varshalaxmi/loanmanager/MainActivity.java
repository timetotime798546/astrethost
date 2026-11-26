package com.varshalaxmi.loanmanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
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
import com.varshalaxmi.loanmanager.model.PeriodBreakdownItem;
import com.varshalaxmi.loanmanager.model.PeriodBreakdownResult;
import com.varshalaxmi.loanmanager.util.BackupRestoreHelper;
import com.varshalaxmi.loanmanager.util.InterestCalculator;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int REQUEST_CODE_CREATE_BACKUP = 1001;
    private static final int REQUEST_CODE_RESTORE_BACKUP = 1002;
    private static final String RUPEE = "\u20B9";

    private DatabaseHelper dbHelper;
    private FrameLayout containerContent;
    private TextView tvHeaderTitle;

    private LinearLayout tabDashboard, tabCustomers, tabLoans, tabReports, tabSettings;
    private TextView textDash, textCust, textLoans, textRep, textSet;

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

        textDash = findViewById(R.id.text_dashboard);
        textCust = findViewById(R.id.text_customers);
        textLoans = findViewById(R.id.text_loans);
        textRep = findViewById(R.id.text_reports);
        textSet = findViewById(R.id.text_settings);

        findViewById(R.id.btn_switch_language).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleLanguage();
            }
        });

        tabDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { switchTab(0); }
        });

        tabCustomers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { switchTab(1); }
        });

        tabLoans.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { switchTab(2); }
        });

        tabReports.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { switchTab(3); }
        });

        tabSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { switchTab(4); }
        });

        switchTab(0);
    }

    private void toggleLanguage() {
        currentLanguage = "hi".equalsIgnoreCase(currentLanguage) ? "en" : "hi";
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

        applyFooterIcons(tabIndex, activeColor, inactiveColor);
    }

    private void applyFooterIcons(int activeTabIndex, int activeColor, int inactiveColor) {
        Drawable baseIcon = Drawable.createFromStream(
                getResources().openRawResource(R.drawable.Icon), null);
        if (baseIcon == null || baseIcon.getConstantState() == null) {
            return;
        }

        TextView[] tabs = {textDash, textCust, textLoans, textRep, textSet};
        float density = getResources().getDisplayMetrics().density;
        int iconSize = (int) (20 * density);
        int padding = (int) (2 * density);

        for (int i = 0; i < tabs.length; i++) {
            Drawable d = baseIcon.getConstantState().newDrawable().mutate();
            d.setBounds(0, 0, iconSize, iconSize);
            d.setColorFilter(i == activeTabIndex ? activeColor : inactiveColor, PorterDuff.Mode.SRC_IN);
            tabs[i].setCompoundDrawables(null, d, null, null);
            tabs[i].setCompoundDrawablePadding(padding);
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
            tvHeaderTitle.setText("Customers Ledger");
            renderCustomersView();
        } else if (tabIndex == 2) {
            tvHeaderTitle.setText("Active Loans");
            renderLoansView();
        } else if (tabIndex == 3) {
            tvHeaderTitle.setText("Payment Reports");
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
            public void onClick(View v) { showAddCustomerDialog(); }
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
            public void onClick(View v) { showAddLoanDialog(0); }
        });

        btnBox.addView(btnAddCust);
        btnBox.addView(btnAddLoan);
        root.addView(btnBox);

        root.addView(createGradientStatCard("Total Registered Clients", totalCust + " Clients", R.drawable.bg_stat_emerald));
        root.addView(createGradientStatCard("Total Outstanding Balance", RUPEE + " " + String.format(Locale.getDefault(), "%,.2f", totalOutstanding), R.drawable.bg_stat_red));
        root.addView(createGradientStatCard("Today's Collection", RUPEE + " " + String.format(Locale.getDefault(), "%,.2f", todayColl), R.drawable.bg_stat_gold));
        root.addView(createCardStatView("Total Capital Disbursed", RUPEE + " " + String.format(Locale.getDefault(), "%,.2f", given), "#0F172A"));
        root.addView(createCardStatView("Total Collections Received", RUPEE + " " + String.format(Locale.getDefault(), "%,.2f", collected), "#16A34A"));

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
        etSearch.setHint("Search customer name or mobile...");
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
            tvEmpty.setText("No customers found matching query.");
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
            String mob = (c.getMobile() != null && !c.getMobile().isEmpty()) ? c.getMobile() : "N/A";
            String addr = (c.getAddress() != null && !c.getAddress().isEmpty()) ? c.getAddress() : "N/A";
            tvMobile.setText("Mob: " + mob + " | " + addr);
            tvBalance.setText(RUPEE + " " + String.format(Locale.getDefault(), "%,.2f", c.getOutstandingBalance()));

            btnLoan.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { showAddLoanDialog(c.getId()); }
            });

            btnPayment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { showAddPaymentDialog(c.getId()); }
            });

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { showCustomerDetailsDialog(c); }
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
                TextView tvAccrued = v.findViewById(R.id.tv_loan_accrued);
                TextView tvMethod = v.findViewById(R.id.tv_loan_method_rate);
                TextView tvDate = v.findViewById(R.id.tv_loan_date);
                TextView tvPaid = v.findViewById(R.id.tv_loan_paid);
                TextView tvRem = v.findViewById(R.id.tv_loan_remaining);

                Button btnBreakdown = v.findViewById(R.id.btn_loan_breakdown);
                Button btnTopUp = v.findViewById(R.id.btn_topup_loan_add);
                Button btnPay = v.findViewById(R.id.btn_collect_loan_pay);

                tvCust.setText(l.getCustomerName());
                tvPrincipal.setText(RUPEE + " " + String.format(Locale.getDefault(), "%,.2f", l.getCombinedPrincipal()));
                tvAccrued.setText(RUPEE + " " + String.format(Locale.getDefault(), "%,.2f", l.getAccruedInterest()));
                tvMethod.setText(l.getInterestRate() + "% (" + l.getInterestMethod() + ")");
                tvDate.setText("Issued: " + l.getIssueDate());
                tvPaid.setText("Paid: " + RUPEE + " " + String.format(Locale.getDefault(), "%,.2f", l.getTotalPaid()));
                tvRem.setText(RUPEE + " " + String.format(Locale.getDefault(), "%,.2f", l.getRemainingBalance()));

                btnBreakdown.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) { showInterestBreakdownDialog(l); }
                });

                btnTopUp.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) { showTopUpDialog(l); }
                });

                btnPay.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) { showAddPaymentDialogForLoan(l); }
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
        tvTitle.setText("Payment Collections Ledger");
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
                ((TextView) pv.findViewById(R.id.tv_pay_type)).setText(p.getPaymentType() != null ? p.getPaymentType() : "Combined Payment");
                ((TextView) pv.findViewById(R.id.tv_pay_amount)).setText(RUPEE + " " + String.format(Locale.getDefault(), "%,.2f", p.getAmount()));
                ((TextView) pv.findViewById(R.id.tv_pay_note)).setText(p.getNote() != null && !p.getNote().isEmpty() ? p.getNote() : "Repayment Collection");
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
        tvTitle.setText("App Settings & Data Backup");
        tvTitle.setTextSize(18);
        tvTitle.setTextColor(Color.parseColor("#0B4632"));
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, 16);
        root.addView(tvTitle);

        Button btnBackup = new Button(this);
        btnBackup.setText("Export Backup Archive (ZIP)");
        btnBackup.setBackgroundResource(R.drawable.bg_btn_primary);
        btnBackup.setTextColor(Color.WHITE);
        btnBackup.setTextSize(14);
        btnBackup.setTypeface(null, Typeface.BOLD);
        btnBackup.setPadding(0, 14, 0, 14);
        btnBackup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/zip");
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                intent.putExtra(Intent.EXTRA_TITLE, "VarshaLaxmi_Backup_" + timeStamp + ".zip");
                try {
                    startActivityForResult(intent, REQUEST_CODE_CREATE_BACKUP);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "File picker unavailable: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        root.addView(btnBackup);

        Button btnRestore = new Button(this);
        btnRestore.setText("Restore Data from Backup");
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
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                String[] mimeTypes = {"application/zip", "application/x-zip-compressed", "application/octet-stream"};
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
                try {
                    startActivityForResult(intent, REQUEST_CODE_RESTORE_BACKUP);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "File picker unavailable: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        root.addView(btnRestore);

        sv.addView(root);
        containerContent.addView(sv);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            if (requestCode == REQUEST_CODE_CREATE_BACKUP) {
                try {
                    OutputStream os = getContentResolver().openOutputStream(uri);
                    if (os != null) {
                        boolean success = BackupRestoreHelper.writeZipBackupToStream(this, dbHelper, os);
                        if (success) {
                            Toast.makeText(this, "Backup saved successfully!", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Failed to save backup file!", Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Error saving backup: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else if (requestCode == REQUEST_CODE_RESTORE_BACKUP) {
                try {
                    InputStream is = getContentResolver().openInputStream(uri);
                    if (is != null) {
                        boolean success = BackupRestoreHelper.restoreFromInputStream(this, is, dbHelper);
                        if (success) {
                            Toast.makeText(this, "Data restored successfully!", Toast.LENGTH_LONG).show();
                            switchTab(0);
                        } else {
                            Toast.makeText(this, "Failed to restore backup file!", Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Error restoring backup: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    // --- HELPER FOR "FROM TODAY / CUSTOM DATE" PICKER ---
    private void bindDateModeSelector(
            final Button btnToday,
            final Button btnCustom,
            final TextView tvDateLabel,
            final String prefixText,
            final String[] selectedDateHolder,
            final boolean[] isCustomModeHolder) {

        final Runnable renderDateSelector = new Runnable() {
            @Override
            public void run() {
                if (!isCustomModeHolder[0]) {
                    selectedDateHolder[0] = dbHelper.getCurrentDate();
                    btnToday.setBackgroundResource(R.drawable.bg_nav_tab_active);
                    btnToday.setTextColor(Color.parseColor("#0B4632"));
                    btnToday.setTypeface(null, Typeface.BOLD);

                    btnCustom.setBackgroundResource(R.drawable.bg_edit_text);
                    btnCustom.setTextColor(Color.parseColor("#475569"));
                    btnCustom.setTypeface(null, Typeface.NORMAL);
                } else {
                    btnCustom.setBackgroundResource(R.drawable.bg_nav_tab_active);
                    btnCustom.setTextColor(Color.parseColor("#0B4632"));
                    btnCustom.setTypeface(null, Typeface.BOLD);

                    btnToday.setBackgroundResource(R.drawable.bg_edit_text);
                    btnToday.setTextColor(Color.parseColor("#475569"));
                    btnToday.setTypeface(null, Typeface.NORMAL);
                }
                tvDateLabel.setText(prefixText + ": " + selectedDateHolder[0] + (isCustomModeHolder[0] ? " (Tap to change)" : " (Today)"));
            }
        };

        btnToday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isCustomModeHolder[0] = false;
                selectedDateHolder[0] = dbHelper.getCurrentDate();
                renderDateSelector.run();
            }
        });

        btnCustom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isCustomModeHolder[0] = true;
                renderDateSelector.run();
                showDatePickerDialog(selectedDateHolder[0], new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        selectedDateHolder[0] = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                        renderDateSelector.run();
                    }
                });
            }
        });

        tvDateLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isCustomModeHolder[0] = true;
                renderDateSelector.run();
                showDatePickerDialog(selectedDateHolder[0], new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        selectedDateHolder[0] = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                        renderDateSelector.run();
                    }
                });
            }
        });

        renderDateSelector.run();
    }

    // --- INTEREST PERIOD BREAKDOWN DIALOG & LIVE PREVIEW CONTROLLER ---
    private String selectedBreakdownDuration = "1 Month";
    private String calcStartDateStr = "";
    private String calcEndDateStr = "";

    private void showInterestBreakdownDialog(final Loan loan) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final View view = LayoutInflater.from(this).inflate(R.layout.dialog_interest_breakdown, null);

        TextView tvName = view.findViewById(R.id.tv_breakdown_customer_name);
        tvName.setText("Client: " + loan.getCustomerName() + " | Rate: " + loan.getInterestRate() + "% (" + loan.getInterestMethod() + ")");

        final TextView tvLoanStart = view.findViewById(R.id.tv_bd_loan_start);
        final TextView tvCalcStart = view.findViewById(R.id.tv_bd_calc_start);
        final TextView tvCalcEnd = view.findViewById(R.id.tv_bd_calc_end);
        final TextView tvSelectedDuration = view.findViewById(R.id.tv_bd_selected_duration);

        final LinearLayout itemsContainer = view.findViewById(R.id.container_breakdown_items);
        final TextView tvSumTotalInt = view.findViewById(R.id.tv_sum_total_interest);
        final TextView tvSumPaidInt = view.findViewById(R.id.tv_sum_paid_interest);
        final TextView tvSumUnpaidInt = view.findViewById(R.id.tv_sum_unpaid_interest);
        final TextView tvSumOutstandingPrin = view.findViewById(R.id.tv_sum_outstanding_principal);
        final TextView tvSumTotalPayable = view.findViewById(R.id.tv_sum_total_payable);

        final Button btn1M = view.findViewById(R.id.btn_dur_1m);
        final Button btn2M = view.findViewById(R.id.btn_dur_2m);
        final Button btn3M = view.findViewById(R.id.btn_dur_3m);
        final Button btn6M = view.findViewById(R.id.btn_dur_6m);
        final Button btn1Y = view.findViewById(R.id.btn_dur_1y);
        final Button btnCustom = view.findViewById(R.id.btn_dur_custom);

        selectedBreakdownDuration = "1 Month";
        calcStartDateStr = (loan.getIssueDate() != null && !loan.getIssueDate().isEmpty())
                ? loan.getIssueDate() : InterestCalculator.getTodayDate();
        calcEndDateStr = InterestCalculator.calculateEndDateForDuration(calcStartDateStr, "1 Month");

        tvLoanStart.setText(loan.getIssueDate());

        final Runnable refreshBreakdownUI = new Runnable() {
            @Override
            public void run() {
                tvCalcStart.setText(calcStartDateStr);
                tvCalcEnd.setText(calcEndDateStr);
                tvSelectedDuration.setText(selectedBreakdownDuration);

                btn1M.setBackgroundResource("1 Month".equals(selectedBreakdownDuration) ? R.drawable.bg_nav_tab_active : R.drawable.bg_edit_text);
                btn2M.setBackgroundResource("2 Months".equals(selectedBreakdownDuration) ? R.drawable.bg_nav_tab_active : R.drawable.bg_edit_text);
                btn3M.setBackgroundResource("3 Months".equals(selectedBreakdownDuration) ? R.drawable.bg_nav_tab_active : R.drawable.bg_edit_text);
                btn6M.setBackgroundResource("6 Months".equals(selectedBreakdownDuration) ? R.drawable.bg_nav_tab_active : R.drawable.bg_edit_text);
                btn1Y.setBackgroundResource("1 Year".equals(selectedBreakdownDuration) ? R.drawable.bg_nav_tab_active : R.drawable.bg_edit_text);
                btnCustom.setBackgroundResource("Custom Date".equals(selectedBreakdownDuration) ? R.drawable.bg_nav_tab_active : R.drawable.bg_edit_text);

                PeriodBreakdownResult result = InterestCalculator.computePeriodBreakdown(
                        loan.getPrincipalAmount(),
                        loan.getInterestRate(),
                        loan.getInterestMethod(),
                        loan.getIssueDate(),
                        loan.getAdditions(),
                        loan.getPayments(),
                        calcStartDateStr,
                        calcEndDateStr,
                        selectedBreakdownDuration
                );

                itemsContainer.removeAllViews();
                List<PeriodBreakdownItem> items = result.getItems();
                if (items.isEmpty()) {
                    TextView tvEmpty = new TextView(MainActivity.this);
                    tvEmpty.setText("No active period breakdown.");
                    tvEmpty.setTextColor(Color.parseColor("#94A3B8"));
                    tvEmpty.setPadding(0, 10, 0, 10);
                    itemsContainer.addView(tvEmpty);
                } else {
                    for (PeriodBreakdownItem item : items) {
                        View itemView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_period_breakdown, itemsContainer, false);
                        ((TextView) itemView.findViewById(R.id.tv_pb_period)).setText(item.getPeriodLabel());
                        ((TextView) itemView.findViewById(R.id.tv_pb_principal)).setText(RUPEE + String.format(Locale.getDefault(), "%,.2f", item.getPrincipal()));
                        ((TextView) itemView.findViewById(R.id.tv_pb_days)).setText(item.getDays() + " days");
                        ((TextView) itemView.findViewById(R.id.tv_pb_interest)).setText(RUPEE + String.format(Locale.getDefault(), "%,.2f", item.getInterest()));

                        TextView tvNote = itemView.findViewById(R.id.tv_pb_note);
                        if (item.getNote() != null && !item.getNote().isEmpty() && !item.getNote().contains("Standard")) {
                            tvNote.setVisibility(View.VISIBLE);
                            tvNote.setText(item.getNote());
                        } else {
                            tvNote.setVisibility(View.GONE);
                        }
                        itemsContainer.addView(itemView);
                    }
                }

                tvSumTotalInt.setText(RUPEE + " " + String.format(Locale.getDefault(), "%,.2f", result.getTotalCalculatedInterest()));
                tvSumPaidInt.setText(RUPEE + " " + String.format(Locale.getDefault(), "%,.2f", result.getInterestAlreadyPaid()));
                tvSumUnpaidInt.setText(RUPEE + " " + String.format(Locale.getDefault(), "%,.2f", result.getRemainingUnpaidInterest()));
                tvSumOutstandingPrin.setText(RUPEE + " " + String.format(Locale.getDefault(), "%,.2f", result.getCurrentOutstandingPrincipal()));
                tvSumTotalPayable.setText(RUPEE + " " + String.format(Locale.getDefault(), "%,.2f", result.getExpectedTotalPayable()));
            }
        };

        View.OnClickListener durationClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();
                if (id == R.id.btn_dur_1m) selectedBreakdownDuration = "1 Month";
                else if (id == R.id.btn_dur_2m) selectedBreakdownDuration = "2 Months";
                else if (id == R.id.btn_dur_3m) selectedBreakdownDuration = "3 Months";
                else if (id == R.id.btn_dur_6m) selectedBreakdownDuration = "6 Months";
                else if (id == R.id.btn_dur_1y) selectedBreakdownDuration = "1 Year";
                else if (id == R.id.btn_dur_custom) selectedBreakdownDuration = "Custom Date";

                if (!"Custom Date".equals(selectedBreakdownDuration)) {
                    calcEndDateStr = InterestCalculator.calculateEndDateForDuration(calcStartDateStr, selectedBreakdownDuration);
                }
                refreshBreakdownUI.run();
            }
        };

        btn1M.setOnClickListener(durationClickListener);
        btn2M.setOnClickListener(durationClickListener);
        btn3M.setOnClickListener(durationClickListener);
        btn6M.setOnClickListener(durationClickListener);
        btn1Y.setOnClickListener(durationClickListener);
        btnCustom.setOnClickListener(durationClickListener);

        tvCalcStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(calcStartDateStr, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        calcStartDateStr = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                        if (!"Custom Date".equals(selectedBreakdownDuration)) {
                            calcEndDateStr = InterestCalculator.calculateEndDateForDuration(calcStartDateStr, selectedBreakdownDuration);
                        }
                        refreshBreakdownUI.run();
                    }
                });
            }
        });

        tvCalcEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(calcEndDateStr, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        calcEndDateStr = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                        selectedBreakdownDuration = "Custom Date";
                        refreshBreakdownUI.run();
                    }
                });
            }
        });

        refreshBreakdownUI.run();

        builder.setView(view)
                .setPositiveButton("Close Preview", null)
                .show();
    }

    private void showDatePickerDialog(String currentDateStr, DatePickerDialog.OnDateSetListener listener) {
        Calendar cal = Calendar.getInstance();
        try {
            if (currentDateStr != null && !currentDateStr.isEmpty()) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                Date date = sdf.parse(currentDateStr);
                if (date != null) {
                    cal.setTime(date);
                }
            }
        } catch (Exception ignored) {}

        int y = cal.get(Calendar.YEAR);
        int m = cal.get(Calendar.MONTH);
        int d = cal.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(this, listener, y, m, d).show();
    }

    // --- OTHER DIALOG HANDLERS ---
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
        final List<Customer> customers = dbHelper.getAllCustomers(null);
        if (customers.isEmpty()) {
            Toast.makeText(this, "Please add a customer profile first!", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_loan, null);
        final Spinner spCust = view.findViewById(R.id.sp_loan_customer);
        final EditText etAmount = view.findViewById(R.id.et_loan_amount);
        final EditText etRate = view.findViewById(R.id.et_loan_rate);
        final Spinner spMethod = view.findViewById(R.id.sp_interest_method);
        final Button btnDateToday = view.findViewById(R.id.btn_loan_date_today);
        final Button btnDateCustom = view.findViewById(R.id.btn_loan_date_custom);
        final TextView tvSelectedDate = view.findViewById(R.id.tv_loan_selected_date);
        final CheckBox cbAddToExisting = view.findViewById(R.id.cb_add_to_existing);
        final EditText etNotes = view.findViewById(R.id.et_loan_notes);

        final String[] selectedDate = new String[]{dbHelper.getCurrentDate()};
        final boolean[] isCustomDate = new boolean[]{false};
        bindDateModeSelector(btnDateToday, btnDateCustom, tvSelectedDate, "Issue Date", selectedDate, isCustomDate);

        final String[] methods = new String[]{"Daily Interest", "Monthly Interest", "No Interest"};
        ArrayAdapter<String> methodAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, methods);
        spMethod.setAdapter(methodAdapter);
        spMethod.setSelection(1);

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

        spCust.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                long cId = custIds[position];
                List<Loan> activeLoans = dbHelper.getLoansForCustomer(cId);
                if (!activeLoans.isEmpty()) {
                    cbAddToExisting.setVisibility(View.VISIBLE);
                    cbAddToExisting.setChecked(true);
                } else {
                    cbAddToExisting.setVisibility(View.GONE);
                    cbAddToExisting.setChecked(false);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        builder.setView(view)
                .setPositiveButton(R.string.btn_save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            double amount = Double.parseDouble(etAmount.getText().toString().trim());
                            if (amount <= 0) {
                                Toast.makeText(MainActivity.this, "Please enter a valid positive principal amount!", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            double rate = 0.0;
                            String rateStr = etRate.getText().toString().trim();
                            if (!rateStr.isEmpty()) {
                                rate = Double.parseDouble(rateStr);
                            }
                            String method = spMethod.getSelectedItem().toString();
                            String notes = etNotes.getText().toString().trim();
                            long cId = custIds[spCust.getSelectedItemPosition()];
                            String issueDate = selectedDate[0];

                            List<Loan> existingLoans = dbHelper.getLoansForCustomer(cId);
                            if (cbAddToExisting.isChecked() && !existingLoans.isEmpty()) {
                                Loan existingLoan = existingLoans.get(0);
                                dbHelper.addLoanAddition(existingLoan.getId(), amount, issueDate, notes);
                                Toast.makeText(MainActivity.this, "Added " + RUPEE + amount + " on " + issueDate, Toast.LENGTH_LONG).show();
                            } else {
                                dbHelper.addLoan(cId, amount, rate, method, issueDate, notes);
                                Toast.makeText(MainActivity.this, "Loan issued for " + issueDate, Toast.LENGTH_SHORT).show();
                            }
                            switchTab(currentTab);
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "Please enter valid numeric principal amount!", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void showTopUpDialog(final Loan loan) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_topup, null);
        TextView tvSubtitle = view.findViewById(R.id.tv_topup_target_info);
        final EditText etAmount = view.findViewById(R.id.et_topup_amount);
        final Button btnDateToday = view.findViewById(R.id.btn_topup_date_today);
        final Button btnDateCustom = view.findViewById(R.id.btn_topup_date_custom);
        final TextView tvSelectedDate = view.findViewById(R.id.tv_topup_selected_date);
        final EditText etNote = view.findViewById(R.id.et_topup_note);

        tvSubtitle.setText("Customer: " + loan.getCustomerName() + "\nCurrent Principal: " + RUPEE + String.format(Locale.getDefault(), "%,.2f", loan.getCombinedPrincipal()));

        final String[] selectedDate = new String[]{dbHelper.getCurrentDate()};
        final boolean[] isCustomDate = new boolean[]{false};
        bindDateModeSelector(btnDateToday, btnDateCustom, tvSelectedDate, "Top-Up Date", selectedDate, isCustomDate);

        builder.setView(view)
                .setPositiveButton(R.string.btn_save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            double amt = Double.parseDouble(etAmount.getText().toString().trim());
                            if (amt <= 0) {
                                Toast.makeText(MainActivity.this, "Please enter a valid positive top-up amount!", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            String note = etNote.getText().toString().trim();
                            String topUpDate = selectedDate[0];

                            dbHelper.addLoanAddition(loan.getId(), amt, topUpDate, note);
                            Toast.makeText(MainActivity.this, "Top-up added for " + topUpDate, Toast.LENGTH_SHORT).show();
                            switchTab(currentTab);
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "Please enter a valid top-up amount!", Toast.LENGTH_SHORT).show();
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
        TextView tvSubtitle = view.findViewById(R.id.tv_pay_dialog_subtitle);
        final Spinner spAllocation = view.findViewById(R.id.sp_payment_allocation);
        final EditText etAmount = view.findViewById(R.id.et_pay_amount);
        final Button btnDateToday = view.findViewById(R.id.btn_pay_date_today);
        final Button btnDateCustom = view.findViewById(R.id.btn_pay_date_custom);
        final TextView tvSelectedDate = view.findViewById(R.id.tv_pay_selected_date);
        final EditText etNote = view.findViewById(R.id.et_pay_note);

        tvSubtitle.setText("Customer: " + loan.getCustomerName() + " | Due: " + RUPEE + String.format(Locale.getDefault(), "%,.2f", loan.getRemainingBalance()));

        final String[] selectedDate = new String[]{dbHelper.getCurrentDate()};
        final boolean[] isCustomDate = new boolean[]{false};
        bindDateModeSelector(btnDateToday, btnDateCustom, tvSelectedDate, "Payment Date", selectedDate, isCustomDate);

        final String[] allocations = new String[]{"Combined Payment", "Principal Payment", "Interest Payment"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, allocations);
        spAllocation.setAdapter(adapter);

        builder.setView(view)
                .setPositiveButton(R.string.btn_save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            double amt = Double.parseDouble(etAmount.getText().toString().trim());
                            if (amt <= 0) {
                                Toast.makeText(MainActivity.this, "Please enter a valid positive payment amount!", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            String allocationType = spAllocation.getSelectedItem().toString();
                            String note = etNote.getText().toString().trim();
                            String payDate = selectedDate[0];

                            dbHelper.addPayment(loan.getId(), loan.getCustomerId(), amt, allocationType, payDate, note);
                            Toast.makeText(MainActivity.this, "Payment recorded on " + payDate + " as " + allocationType, Toast.LENGTH_SHORT).show();
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
                .setMessage("Mobile: " + (c.getMobile() != null ? c.getMobile() : "N/A") +
                        "\nAddress: " + (c.getAddress() != null ? c.getAddress() : "N/A") +
                        "\n\nTotal Capital Disbursed: " + RUPEE + " " + String.format(Locale.getDefault(), "%,.2f", c.getTotalGiven()) +
                        "\nTotal Collected: " + RUPEE + " " + String.format(Locale.getDefault(), "%,.2f", c.getTotalPaid()) +
                        "\nOutstanding Due: " + RUPEE + " " + String.format(Locale.getDefault(), "%,.2f", c.getOutstandingBalance()))
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
