package com.smartkhata.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPrefs;

    // Active preferences variables
    private String shopName;
    private String keeperName;
    private boolean isSoundEnabled;

    // View components
    private RelativeLayout splashOverlay;
    private TextView headerShopSubtitle;
    private TextView homeGreeting;

    // Tab sections
    private ScrollView sectionHome;
    private LinearLayout sectionCustomers;
    private LinearLayout sectionTransactions;
    private ScrollView sectionSettings;

    // Tab buttons
    private LinearLayout tabHome;
    private LinearLayout tabCustomers;
    private LinearLayout tabTransactions;
    private LinearLayout tabSettings;

    private TextView tabHomeIcon, tabHomeText;
    private TextView tabCustomersIcon, tabCustomersText;
    private TextView tabTransactionsIcon, tabTransactionsText;
    private TextView tabSettingsIcon, tabSettingsText;

    // Dashboard dynamic items
    private TextView cardValCustomers;
    private TextView cardValOutstanding;
    private TextView cardValUdhaar;
    private TextView cardValJama;
    private LinearLayout recentTxContainer;
    private TextView recentEmptyState;

    // Customers List items
    private EditText customerSearchInput;
    private ListView customersListView;
    private TextView customersEmptyState;
    private CustomerAdapter customerAdapter;
    private List<DatabaseHelper.Customer> customerList = new ArrayList<>();

    // All Transactions items
    private Button filterAll, filterUdhaar, filterJama;
    private EditText txSearchInput;
    private ListView transactionsListView;
    private TextView transactionsEmptyState;
    private TransactionAdapter transactionAdapter;
    private List<DatabaseHelper.Transaction> globalTransactionList = new ArrayList<>();
    private String activeTxFilter = "ALL";

    // Settings elements
    private TextView settingShopDisplay;
    private TextView settingKeeperDisplay;
    private CheckBox toggleSounds;

    // Customer profile overlay screen
    private RelativeLayout profileViewOverlay;
    private TextView profileAvatarBig;
    private TextView profileNameTitle;
    private TextView profilePhoneSub;
    private TextView profileMetaSub;
    private TextView profileSumUdhaar;
    private TextView profileSumJama;
    private TextView profileBalanceVal;
    private TextView profileBalanceDesc;
    private LinearLayout profileTransactionsContainer;
    private TextView profileTxEmptyState;

    private int activeTab = 0; // 0=Home, 1=Customers, 2=Transactions, 3=Settings
    private int selectedCustomerId = -1; // -1 if no customer profile is active

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        sharedPrefs = getSharedPreferences("smart_khata_prefs", MODE_PRIVATE);

        // Load configuration or set default values
        shopName = sharedPrefs.getString("shop_name", "My Smart Store");
        keeperName = sharedPrefs.getString("keeper_name", "Shopkeeper");
        isSoundEnabled = sharedPrefs.getBoolean("sound_enabled", true);

        // First launch check - populates fictional data for premium looking dashboard on demo
        if (dbHelper.isDatabaseEmpty() && sharedPrefs.getBoolean("is_first_launch", true)) {
            dbHelper.populateDemoData();
            sharedPrefs.edit().putBoolean("is_first_launch", false).apply();
        }

        initializeViews();
        setupNavigationListeners();
        setupActionListeners();

        // If instance state was saved (e.g. screen rotation), restore current layout view
        if (savedInstanceState != null) {
            activeTab = savedInstanceState.getInt("active_tab", 0);
            selectedCustomerId = savedInstanceState.getInt("selected_customer_id", -1);
            splashOverlay.setVisibility(View.GONE);
            switchToTab(activeTab);
            if (selectedCustomerId != -1) {
                openCustomerProfile(selectedCustomerId);
            }
        } else {
            // Run Splash Screen loader
            splashOverlay.setVisibility(View.VISIBLE);
            splashOverlay.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Smooth transition to dashboard
                    splashOverlay.animate().alpha(0f).setDuration(500).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            splashOverlay.setVisibility(View.GONE);
                        }
                    });
                }
            }, 1800);

            switchToTab(0);
        }

        updateUI();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("active_tab", activeTab);
        outState.putInt("selected_customer_id", selectedCustomerId);
    }

    private void initializeViews() {
        splashOverlay = findViewById(R.id.splash_overlay);
        headerShopSubtitle = findViewById(R.id.header_shop_subtitle);
        homeGreeting = findViewById(R.id.home_greeting);

        // Sections
        sectionHome = findViewById(R.id.section_home);
        sectionCustomers = findViewById(R.id.section_customers);
        sectionTransactions = findViewById(R.id.section_transactions);
        sectionSettings = findViewById(R.id.section_settings);

        // Tab buttons
        tabHome = findViewById(R.id.tab_home);
        tabCustomers = findViewById(R.id.tab_customers);
        tabTransactions = findViewById(R.id.tab_transactions);
        tabSettings = findViewById(R.id.tab_settings);

        tabHomeIcon = findViewById(R.id.tab_home_icon);
        tabHomeText = findViewById(R.id.tab_home_text);
        tabCustomersIcon = findViewById(R.id.tab_customers_icon);
        tabCustomersText = findViewById(R.id.tab_customers_text);
        tabTransactionsIcon = findViewById(R.id.tab_transactions_icon);
        tabTransactionsText = findViewById(R.id.tab_transactions_text);
        tabSettingsIcon = findViewById(R.id.tab_settings_icon);
        tabSettingsText = findViewById(R.id.tab_settings_text);

        // Home dashboard variables
        cardValCustomers = findViewById(R.id.card_val_customers);
        cardValOutstanding = findViewById(R.id.card_val_outstanding);
        cardValUdhaar = findViewById(R.id.card_val_udhaar);
        cardValJama = findViewById(R.id.card_val_jama);
        recentTxContainer = findViewById(R.id.recent_transactions_container);
        recentEmptyState = findViewById(R.id.recent_empty_state);

        // Customer tab items
        customerSearchInput = findViewById(R.id.customer_search_input);
        customersListView = findViewById(R.id.customers_list_view);
        customersEmptyState = findViewById(R.id.customers_empty_state);

        // Transaction tab items
        filterAll = findViewById(R.id.filter_all);
        filterUdhaar = findViewById(R.id.filter_udhaar);
        filterJama = findViewById(R.id.filter_jama);
        txSearchInput = findViewById(R.id.tx_search_input);
        transactionsListView = findViewById(R.id.transactions_list_view);
        transactionsEmptyState = findViewById(R.id.transactions_empty_state);

        // Settings items
        settingShopDisplay = findViewById(R.id.setting_shop_display);
        settingKeeperDisplay = findViewById(R.id.setting_keeper_display);
        toggleSounds = findViewById(R.id.toggle_sounds);
        toggleSounds.setChecked(isSoundEnabled);

        // Profile overlay screen elements
        profileViewOverlay = findViewById(R.id.profile_view_overlay);
        profileAvatarBig = findViewById(R.id.profile_avatar_big);
        profileNameTitle = findViewById(R.id.profile_name_title);
        profilePhoneSub = findViewById(R.id.profile_phone_sub);
        profileMetaSub = findViewById(R.id.profile_meta_sub);
        profileSumUdhaar = findViewById(R.id.profile_sum_udhaar);
        profileSumJama = findViewById(R.id.profile_sum_jama);
        profileBalanceVal = findViewById(R.id.profile_balance_val);
        profileBalanceDesc = findViewById(R.id.profile_balance_desc);
        profileTransactionsContainer = findViewById(R.id.profile_transactions_container);
        profileTxEmptyState = findViewById(R.id.profile_tx_empty_state);

        // Set static displayed properties
        headerShopSubtitle.setText(shopName);
        homeGreeting.setText("Namaste, " + keeperName + "!");
        settingShopDisplay.setText(shopName);
        settingKeeperDisplay.setText("Keeper: " + keeperName);
    }

    private void playSuccessSound() {
        if (!isSoundEnabled) return;
        try {
            ToneGenerator tg = new ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 90);
            tg.startTone(ToneGenerator.TONE_PROP_BEEP, 100);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String formatCurrency(double amount) {
        return String.format(Locale.getDefault(), "₹%,.2f", amount);
    }

    private GradientDrawable getAvatarDrawable(String name) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        int hash = name.hashCode();
        int[] palette = {0xFF3B82F6, 0xFF10B981, 0xFFF59E0B, 0xFFEF4444, 0xFF8B5CF6, 0xFFEC4899, 0xFF14B8A6};
        int selectedColor = palette[Math.abs(hash) % palette.length];
        drawable.setColor(selectedColor);
        return drawable;
    }

    private void switchToTab(int index) {
        activeTab = index;

        // Visual feedback configuration on navigation bar
        int activeColor = Color.parseColor("#1E3A8A");
        int inactiveColor = Color.parseColor("#6B7280");

        tabHomeText.setTextColor(index == 0 ? activeColor : inactiveColor);
        tabHomeText.setTypeface(null, index == 0 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        tabCustomersText.setTextColor(index == 1 ? activeColor : inactiveColor);
        tabCustomersText.setTypeface(null, index == 1 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        tabTransactionsText.setTextColor(index == 2 ? activeColor : inactiveColor);
        tabTransactionsText.setTypeface(null, index == 2 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        tabSettingsText.setTextColor(index == 3 ? activeColor : inactiveColor);
        tabSettingsText.setTypeface(null, index == 3 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        sectionHome.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        sectionCustomers.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        sectionTransactions.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        sectionSettings.setVisibility(index == 3 ? View.VISIBLE : View.GONE);

        if (index == 1) {
            customerSearchInput.setText("");
            updateCustomersList("");
        } else if (index == 2) {
            txSearchInput.setText("");
            updateTransactionsList("ALL", "");
        }
    }

    private void setupNavigationListeners() {
        tabHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToTab(0);
            }
        });

        tabCustomers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToTab(1);
            }
        });

        tabTransactions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToTab(2);
            }
        });

        tabSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToTab(3);
            }
        });
    }

    private void setupActionListeners() {
        // Dashboard Add Customer
        findViewById(R.id.home_btn_add_customer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddCustomerDialog();
            }
        });

        // Customers list Add Customer
        findViewById(R.id.cust_btn_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddCustomerDialog();
            }
        });

        // Search text watcher in Customer list
        customerSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateCustomersList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Search text watcher in Transaction list
        txSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateTransactionsList(activeTxFilter, s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Filters listeners inside transaction list
        filterAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setActiveFilter("ALL");
            }
        });

        filterUdhaar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setActiveFilter("UDHAAR");
            }
        });

        filterJama.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setActiveFilter("JAMA");
            }
        });

        // Customers List Item trigger
        customersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DatabaseHelper.Customer selected = customerList.get(position);
                openCustomerProfile(selected.id);
            }
        });

        // Profile back navigation
        findViewById(R.id.profile_btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeCustomerProfile();
            }
        });

        // Delete customer
        findViewById(R.id.profile_btn_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteConfirmationDialog(selectedCustomerId);
            }
        });

        // Edit customer info
        findViewById(R.id.profile_btn_edit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditCustomerDialog(selectedCustomerId);
            }
        });

        // Record Udhaar inside profile
        findViewById(R.id.profile_btn_add_udhaar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddTransactionDialog("UDHAAR");
            }
        });

        // Record Jama payment inside profile
        findViewById(R.id.profile_btn_add_jama).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddTransactionDialog("JAMA");
            }
        });

        // Send WhatsApp Reminder
        findViewById(R.id.profile_btn_whatsapp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseHelper.Customer cust = dbHelper.getCustomerById(selectedCustomerId);
                if (cust != null) {
                    dispatchWhatsAppReminder(cust.name, cust.phone, cust.balance);
                }
            }
        });

        // Edit Shop metadata inside Settings
        findViewById(R.id.btn_edit_shop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditShopDialog();
            }
        });

        // Sound preferences toggle checkbox listener
        toggleSounds.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isSoundEnabled = isChecked;
                sharedPrefs.edit().putBoolean("sound_enabled", isChecked).apply();
            }
        });

        // Backup export share button
        findViewById(R.id.btn_share_export).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchBackupTextShare();
            }
        });

        // Clear DB reset trigger
        findViewById(R.id.btn_reset_data).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showResetDataConfirmation();
            }
        });
    }

    private void setActiveFilter(String type) {
        activeTxFilter = type;

        // Button background visual toggling
        int selectedBg = R.drawable.button_primary;
        int unselectedBg = R.drawable.search_bg;
        int selectedColor = Color.WHITE;
        int unselectedColor = Color.parseColor("#1F2937");

        filterAll.setBackgroundResource(type.equals("ALL") ? selectedBg : unselectedBg);
        filterAll.setTextColor(type.equals("ALL") ? selectedColor : unselectedColor);

        filterUdhaar.setBackgroundResource(type.equals("UDHAAR") ? selectedBg : unselectedBg);
        filterUdhaar.setTextColor(type.equals("UDHAAR") ? selectedColor : unselectedColor);

        filterJama.setBackgroundResource(type.equals("JAMA") ? selectedBg : unselectedBg);
        filterJama.setTextColor(type.equals("JAMA") ? selectedColor : unselectedColor);

        updateTransactionsList(type, txSearchInput.getText().toString());
    }

    private void updateUI() {
        Bundle summary = dbHelper.getSummary();
        int count = summary.getInt("cust_count", 0);
        double udhaar = summary.getDouble("total_udhaar", 0.0);
        double jama = summary.getDouble("total_jama", 0.0);
        double outstanding = summary.getDouble("outstanding", 0.0);

        cardValCustomers.setText(String.valueOf(count));
        cardValUdhaar.setText(formatCurrency(udhaar));
        cardValJama.setText(formatCurrency(jama));
        cardValOutstanding.setText(formatCurrency(outstanding));

        if (outstanding > 0) {
            cardValOutstanding.setTextColor(Color.parseColor("#EF4444")); // Red Udhaar
        } else if (outstanding < 0) {
            cardValOutstanding.setTextColor(Color.parseColor("#10B981")); // Green Advance (negative outstanding means advance payment)
        } else {
            cardValOutstanding.setTextColor(Color.parseColor("#1F2937")); // Neutral Dark
        }

        updateRecentTransactionsList();
    }

    private void updateRecentTransactionsList() {
        recentTxContainer.removeAllViews();
        List<DatabaseHelper.Transaction> recentList = dbHelper.getTransactions(0, "ALL", "");
        
        if (recentList.isEmpty()) {
            recentEmptyState.setVisibility(View.VISIBLE);
        } else {
            recentEmptyState.setVisibility(View.GONE);
            // Dynamic generation of recent top 5 list inside dashboard scroll layout
            int displayCount = Math.min(recentList.size(), 5);
            LayoutInflater inflater = LayoutInflater.from(this);
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());

            for (int i = 0; i < displayCount; i++) {
                DatabaseHelper.Transaction tx = recentList.get(i);
                View row = inflater.inflate(R.layout.item_transaction, recentTxContainer, false);

                TextView name = row.findViewById(R.id.tx_customer_name);
                TextView note = row.findViewById(R.id.tx_note);
                TextView date = row.findViewById(R.id.tx_date);
                TextView amount = row.findViewById(R.id.tx_amount);
                TextView badge = row.findViewById(R.id.tx_type_badge);

                name.setText(tx.customerName);
                note.setText(tx.note == null || tx.note.trim().isEmpty() ? "No dynamic remark" : tx.note);
                date.setText(sdf.format(new Date(tx.timestamp)));
                amount.setText(formatCurrency(tx.amount));

                if ("UDHAAR".equals(tx.type)) {
                    amount.setTextColor(Color.parseColor("#EF4444"));
                    badge.setText("UDHAAR");
                    badge.setTextColor(Color.parseColor("#EF4444"));
                    badge.setBackgroundResource(R.drawable.badge_udhaar);
                } else {
                    amount.setTextColor(Color.parseColor("#10B981"));
                    badge.setText("JAMA / PAY");
                    badge.setTextColor(Color.parseColor("#10B981"));
                    badge.setBackgroundResource(R.drawable.badge_jama);
                }

                recentTxContainer.addView(row);
            }
        }
    }

    private void updateCustomersList(String searchWord) {
        customerList = dbHelper.getCustomers(searchWord);
        if (customerList.isEmpty()) {
            customersEmptyState.setVisibility(View.VISIBLE);
        } else {
            customersEmptyState.setVisibility(View.GONE);
        }

        if (customerAdapter == null) {
            customerAdapter = new CustomerAdapter(this, customerList);
            customersListView.setAdapter(customerAdapter);
        } else {
            customerAdapter.updateData(customerList);
        }
    }

    private void updateTransactionsList(String type, String searchWord) {
        globalTransactionList = dbHelper.getTransactions(0, type, searchWord);
        if (globalTransactionList.isEmpty()) {
            transactionsEmptyState.setVisibility(View.VISIBLE);
        } else {
            transactionsEmptyState.setVisibility(View.GONE);
        }

        if (transactionAdapter == null) {
            transactionAdapter = new TransactionAdapter(this, globalTransactionList);
            transactionsListView.setAdapter(transactionAdapter);
        } else {
            transactionAdapter.updateData(globalTransactionList);
        }
    }

    private void openCustomerProfile(int id) {
        selectedCustomerId = id;
        DatabaseHelper.Customer cust = dbHelper.getCustomerById(id);
        if (cust == null) return;

        profileViewOverlay.setVisibility(View.VISIBLE);

        // Header and names
        profileNameTitle.setText(cust.name);
        profilePhoneSub.setText(cust.phone == null || cust.phone.trim().isEmpty() ? "No Mobile Number Added" : "+91 " + cust.phone);
        
        String addressLine = (cust.address != null && !cust.address.trim().isEmpty()) ? cust.address : "";
        String notesLine = (cust.notes != null && !cust.notes.trim().isEmpty()) ? cust.notes : "";
        String fullMeta = "";
        if (!addressLine.isEmpty()) fullMeta += "📍 " + addressLine;
        if (!notesLine.isEmpty()) {
            if (!fullMeta.isEmpty()) fullMeta += "  •  ";
            fullMeta += "📝 " + notesLine;
        }
        profileMetaSub.setText(fullMeta.isEmpty() ? "No additional details added." : fullMeta);

        // Circular avatar representation
        profileAvatarBig.setText(cust.name.substring(0, 1).toUpperCase());
        profileAvatarBig.setBackground(getAvatarDrawable(cust.name));

        updateProfileTransactions(cust);
    }

    private void updateProfileTransactions(DatabaseHelper.Customer cust) {
        // Double check customer balances dynamically
        List<DatabaseHelper.Transaction> list = dbHelper.getTransactions(cust.id, "ALL", "");
        double totalUdhaar = 0;
        double totalJama = 0;

        for (DatabaseHelper.Transaction tx : list) {
            if ("UDHAAR".equals(tx.type)) {
                totalUdhaar += tx.amount;
            } else {
                totalJama += tx.amount;
            }
        }

        double finalBalance = totalUdhaar - totalJama;
        cust.balance = finalBalance;

        profileSumUdhaar.setText(formatCurrency(totalUdhaar));
        profileSumJama.setText(formatCurrency(totalJama));
        profileBalanceVal.setText(formatCurrency(Math.abs(finalBalance)));

        if (finalBalance > 0) {
            profileBalanceVal.setTextColor(Color.parseColor("#EF4444"));
            profileBalanceDesc.setText("Customer owes you this money");
            profileBalanceDesc.setTextColor(Color.parseColor("#EF4444"));
        } else if (finalBalance < 0) {
            profileBalanceVal.setTextColor(Color.parseColor("#10B981"));
            profileBalanceDesc.setText("Advance payments recorded");
            profileBalanceDesc.setTextColor(Color.parseColor("#10B981"));
        } else {
            profileBalanceVal.setTextColor(Color.parseColor("#1F2937"));
            profileBalanceDesc.setText("Accounts fully balanced");
            profileBalanceDesc.setTextColor(Color.parseColor("#6B7280"));
        }

        // Dynamically insert rows
        profileTransactionsContainer.removeAllViews();
        if (list.isEmpty()) {
            profileTxEmptyState.setVisibility(View.VISIBLE);
        } else {
            profileTxEmptyState.setVisibility(View.GONE);
            LayoutInflater inflater = LayoutInflater.from(this);
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

            for (DatabaseHelper.Transaction tx : list) {
                View row = inflater.inflate(R.layout.item_transaction, profileTransactionsContainer, false);

                TextView name = row.findViewById(R.id.tx_customer_name);
                TextView note = row.findViewById(R.id.tx_note);
                TextView date = row.findViewById(R.id.tx_date);
                TextView amount = row.findViewById(R.id.tx_amount);
                TextView badge = row.findViewById(R.id.tx_type_badge);

                name.setText(cust.name);
                note.setText(tx.note == null || tx.note.trim().isEmpty() ? "No additional details" : tx.note);
                date.setText(sdf.format(new Date(tx.timestamp)));
                amount.setText(formatCurrency(tx.amount));

                if ("UDHAAR".equals(tx.type)) {
                    amount.setTextColor(Color.parseColor("#EF4444"));
                    badge.setText("UDHAAR");
                    badge.setTextColor(Color.parseColor("#EF4444"));
                    badge.setBackgroundResource(R.drawable.badge_udhaar);
                } else {
                    amount.setTextColor(Color.parseColor("#10B981"));
                    badge.setText("JAMA / PAY");
                    badge.setTextColor(Color.parseColor("#10B981"));
                    badge.setBackgroundResource(R.drawable.badge_jama);
                }

                profileTransactionsContainer.addView(row);
            }
        }
    }

    private void closeCustomerProfile() {
        selectedCustomerId = -1;
        profileViewOverlay.setVisibility(View.GONE);
        updateUI();
        if (activeTab == 1) {
            updateCustomersList(customerSearchInput.getText().toString());
        }
    }

    private void showAddCustomerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_customer, null);
        builder.setView(dialogView);

        final EditText editName = dialogView.findViewById(R.id.edit_name);
        final EditText editPhone = dialogView.findViewById(R.id.edit_phone);
        final EditText editAddress = dialogView.findViewById(R.id.edit_address);
        final EditText editNotes = dialogView.findViewById(R.id.edit_notes);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnSave = dialogView.findViewById(R.id.btn_save);

        final AlertDialog dialog = builder.create();
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nameStr = editName.getText().toString().trim();
                String phoneStr = editPhone.getText().toString().trim();
                String addressStr = editAddress.getText().toString().trim();
                String notesStr = editNotes.getText().toString().trim();

                if (nameStr.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter customer name", Toast.LENGTH_SHORT).show();
                    return;
                }

                long statusId = dbHelper.addCustomer(nameStr, phoneStr, addressStr, notesStr);
                if (statusId > 0) {
                    playSuccessSound();
                    Toast.makeText(MainActivity.this, "Customer added successfully", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    updateUI();
                    if (activeTab == 1) {
                        updateCustomersList(customerSearchInput.getText().toString());
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showEditCustomerDialog(final int customerId) {
        final DatabaseHelper.Customer cust = dbHelper.getCustomerById(customerId);
        if (cust == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_customer, null);
        builder.setView(dialogView);

        TextView title = dialogView.findViewById(R.id.dialog_title);
        title.setText("Edit Customer Info");

        final EditText editName = dialogView.findViewById(R.id.edit_name);
        final EditText editPhone = dialogView.findViewById(R.id.edit_phone);
        final EditText editAddress = dialogView.findViewById(R.id.edit_address);
        final EditText editNotes = dialogView.findViewById(R.id.edit_notes);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnSave = dialogView.findViewById(R.id.btn_save);
        btnSave.setText("Update Details");

        // Fill current info
        editName.setText(cust.name);
        editPhone.setText(cust.phone);
        editAddress.setText(cust.address);
        editNotes.setText(cust.notes);

        final AlertDialog dialog = builder.create();
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nameStr = editName.getText().toString().trim();
                String phoneStr = editPhone.getText().toString().trim();
                String addressStr = editAddress.getText().toString().trim();
                String notesStr = editNotes.getText().toString().trim();

                if (nameStr.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter customer name", Toast.LENGTH_SHORT).show();
                    return;
                }

                int successCount = dbHelper.updateCustomer(customerId, nameStr, phoneStr, addressStr, notesStr);
                if (successCount > 0) {
                    playSuccessSound();
                    Toast.makeText(MainActivity.this, "Customer updated successfully", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    openCustomerProfile(customerId); // refresh profile view
                } else {
                    Toast.makeText(MainActivity.this, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showDeleteConfirmationDialog(final int customerId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Customer?");
        builder.setMessage("Are you sure? All transactions associated with this customer may also be permanently removed.");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dbHelper.deleteCustomer(customerId);
                Toast.makeText(MainActivity.this, "Customer deleted successfully", Toast.LENGTH_SHORT).show();
                closeCustomerProfile();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showAddTransactionDialog(final String type) {
        final DatabaseHelper.Customer cust = dbHelper.getCustomerById(selectedCustomerId);
        if (cust == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_transaction, null);
        builder.setView(dialogView);

        TextView title = dialogView.findViewById(R.id.tx_dialog_title);
        if ("UDHAAR".equals(type)) {
            title.setText("Record Udhaar (₹ Debit)");
            title.setTextColor(Color.parseColor("#EF4444"));
        } else {
            title.setText("Record Payment (₹ Credit)");
            title.setTextColor(Color.parseColor("#10B981"));
        }

        final EditText editAmount = dialogView.findViewById(R.id.edit_amount);
        final EditText editNote = dialogView.findViewById(R.id.edit_tx_note);
        Button btnCancel = dialogView.findViewById(R.id.btn_tx_cancel);
        Button btnSave = dialogView.findViewById(R.id.btn_tx_save);

        final AlertDialog dialog = builder.create();
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String amtStr = editAmount.getText().toString().trim();
                String noteStr = editNote.getText().toString().trim();

                if (amtStr.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter a valid amount", Toast.LENGTH_SHORT).show();
                    return;
                }

                double amtValue;
                try {
                    amtValue = Double.parseDouble(amtStr);
                } catch (NumberFormatException nfe) {
                    Toast.makeText(MainActivity.this, "Please enter a valid decimal number", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (amtValue <= 0) {
                    Toast.makeText(MainActivity.this, "Amount must be greater than zero", Toast.LENGTH_SHORT).show();
                    return;
                }

                long txId = dbHelper.addTransaction(cust.id, type, amtValue, noteStr, System.currentTimeMillis());
                if (txId > 0) {
                    playSuccessSound();
                    String msg = "UDHAAR".equals(type) ? "Udhaar added successfully" : "Payment recorded successfully";
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    openCustomerProfile(cust.id); // Reload
                } else {
                    Toast.makeText(MainActivity.this, "Unexpected problem. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void dispatchWhatsAppReminder(String name, String phone, double balance) {
        if (phone == null || phone.trim().isEmpty()) {
            Toast.makeText(this, "Please update customer mobile number to send reminder", Toast.LENGTH_SHORT).show();
            return;
        }

        String formattedBalance = formatCurrency(balance);
        String message = "Namaste *" + name + "*, aapke khate mein *" + formattedBalance + "* baki hai. Kripya suvidha anusar payment kar dein. Dhanyavaad.\n\n- Powered by Smart Khata App.";

        try {
            String cleanPhone = phone.replaceAll("[^0-9]", "");
            if (cleanPhone.length() == 10) {
                cleanPhone = "91" + cleanPhone;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            String url = "https://api.whatsapp.com/send?phone=" + cleanPhone + "&text=" + Uri.encode(message);
            intent.setData(Uri.parse(url));
            intent.setPackage("com.whatsapp");
            startActivity(intent);
        } catch (Exception e) {
            // Fallback to plain Intent sharing or standard SMS
            try {
                Intent backupIntent = new Intent(Intent.ACTION_SENDTO);
                backupIntent.setData(Uri.parse("smsto:" + phone));
                backupIntent.putExtra("sms_body", message);
                startActivity(backupIntent);
                Toast.makeText(this, "Opening default SMS messenger as WhatsApp is not installed.", Toast.LENGTH_SHORT).show();
            } catch (Exception ex) {
                Toast.makeText(this, "WhatsApp and standard messaging services not available.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showEditShopDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_settings_profile, null);
        builder.setView(dialogView);

        final EditText editShopName = dialogView.findViewById(R.id.edit_shop_name);
        final EditText editKeeperName = dialogView.findViewById(R.id.edit_keeper_name);
        Button btnCancel = dialogView.findViewById(R.id.btn_profile_cancel);
        Button btnSave = dialogView.findViewById(R.id.btn_profile_save);

        editShopName.setText(shopName);
        editKeeperName.setText(keeperName);

        final AlertDialog dialog = builder.create();
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String sName = editShopName.getText().toString().trim();
                String kName = editKeeperName.getText().toString().trim();

                if (sName.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Shop name is required", Toast.LENGTH_SHORT).show();
                    return;
                }

                shopName = sName;
                keeperName = kName.isEmpty() ? "Shopkeeper" : kName;

                sharedPrefs.edit()
                        .putString("shop_name", shopName)
                        .putString("keeper_name", keeperName)
                        .apply();

                playSuccessSound();
                Toast.makeText(MainActivity.this, "Saved successfully", Toast.LENGTH_SHORT).show();
                dialog.dismiss();

                // Update text view elements immediately
                headerShopSubtitle.setText(shopName);
                homeGreeting.setText("Namaste, " + keeperName + "!");
                settingShopDisplay.setText(shopName);
                settingKeeperDisplay.setText("Keeper: " + keeperName);
            }
        });
    }

    private void dispatchBackupTextShare() {
        List<DatabaseHelper.Customer> allCustomers = dbHelper.getCustomers("");
        if (allCustomers.isEmpty()) {
            Toast.makeText(this, "No customer details recorded to backup", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(shopName.toUpperCase()).append(" KHATA BACKUP ===\n");
        sb.append("Managed by: ").append(keeperName).append("\n");
        sb.append("Generated on: ").append(new SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault()).format(new Date())).append("\n\n");

        double totalOutstanding = 0;
        for (DatabaseHelper.Customer c : allCustomers) {
            sb.append("• ").append(c.name);
            if (c.phone != null && !c.phone.trim().isEmpty()) {
                sb.append(" (Phone: ").append(c.phone).append(")");
            }
            sb.append("\n  Balance: ").append(formatCurrency(c.balance)).append("\n");
            totalOutstanding += c.balance;
        }

        sb.append("\n============================\n");
        sb.append("Total Outstanding Book: ").append(formatCurrency(totalOutstanding));

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Backup Text Export Share via:"));
    }

    private void showResetDataConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Hard Reset?");
        builder.setMessage("This action will completely wipe out your existing ledger transactions and reload demo test data. Are you sure?");
        builder.setPositiveButton("Reset & Load Demo", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dbHelper.populateDemoData();
                playSuccessSound();
                Toast.makeText(MainActivity.this, "Database reloaded with fictional demo customers", Toast.LENGTH_SHORT).show();
                updateUI();
                switchToTab(0);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // Traditional non-generic adapter classes strictly avoiding lambdas and streams
    private static class CustomerAdapter extends BaseAdapter {

        private final Context context;
        private List<DatabaseHelper.Customer> list;
        private final LayoutInflater inflater;

        public CustomerAdapter(Context context, List<DatabaseHelper.Customer> list) {
            this.context = context;
            this.list = list;
            this.inflater = LayoutInflater.from(context);
        }

        public void updateData(List<DatabaseHelper.Customer> newList) {
            this.list = newList;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return list.get(position).id;
        }

        private GradientDrawable getOvalBackground(String name) {
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            int[] colors = {0xFF3B82F6, 0xFF10B981, 0xFFF59E0B, 0xFFEF4444, 0xFF8B5CF6, 0xFF14B8A6};
            int index = Math.abs(name.hashCode()) % colors.length;
            gd.setColor(colors[index]);
            return gd;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_customer, parent, false);
            }

            DatabaseHelper.Customer cust = list.get(position);

            TextView avatar = convertView.findViewById(R.id.customer_avatar);
            TextView name = convertView.findViewById(R.id.customer_name);
            TextView phone = convertView.findViewById(R.id.customer_phone);
            TextView balance = convertView.findViewById(R.id.customer_balance_val);
            TextView badge = convertView.findViewById(R.id.customer_status_tag);

            name.setText(cust.name);
            phone.setText(cust.phone == null || cust.phone.trim().isEmpty() ? "No Mobile Number" : "+91 " + cust.phone);

            avatar.setText(cust.name.substring(0, 1).toUpperCase());
            avatar.setBackground(getOvalBackground(cust.name));

            String formattedBal = String.format(Locale.getDefault(), "₹%,.2f", Math.abs(cust.balance));
            balance.setText(formattedBal);

            if (cust.balance > 0) {
                balance.setTextColor(Color.parseColor("#EF4444")); // owes you money
                badge.setText("UDHAAR");
                badge.setTextColor(Color.parseColor("#EF4444"));
                badge.setBackgroundResource(R.drawable.badge_udhaar);
            } else if (cust.balance < 0) {
                balance.setTextColor(Color.parseColor("#10B981")); // paid advance
                badge.setText("ADVANCE");
                badge.setTextColor(Color.parseColor("#10B981"));
                badge.setBackgroundResource(R.drawable.badge_jama);
            } else {
                balance.setTextColor(Color.parseColor("#1F2937")); // clear
                badge.setText("SETTLED");
                badge.setTextColor(Color.parseColor("#6B7280"));
                badge.setBackgroundResource(R.drawable.search_bg);
            }

            return convertView;
        }
    }

    private static class TransactionAdapter extends BaseAdapter {

        private final Context context;
        private List<DatabaseHelper.Transaction> list;
        private final LayoutInflater inflater;
        private final SimpleDateFormat sdf;

        public TransactionAdapter(Context context, List<DatabaseHelper.Transaction> list) {
            this.context = context;
            this.list = list;
            this.inflater = LayoutInflater.from(context);
            this.sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        }

        public void updateData(List<DatabaseHelper.Transaction> newList) {
            this.list = newList;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return list.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_transaction, parent, false);
            }

            DatabaseHelper.Transaction tx = list.get(position);

            TextView cName = convertView.findViewById(R.id.tx_customer_name);
            TextView note = convertView.findViewById(R.id.tx_note);
            TextView date = convertView.findViewById(R.id.tx_date);
            TextView amount = convertView.findViewById(R.id.tx_amount);
            TextView badge = convertView.findViewById(R.id.tx_type_badge);

            cName.setText(tx.customerName);
            note.setText(tx.note == null || tx.note.trim().isEmpty() ? "No additional details" : tx.note);
            date.setText(sdf.format(new Date(tx.timestamp)));

            String formattedAmt = String.format(Locale.getDefault(), "₹%,.2f", tx.amount);
            amount.setText(formattedAmt);

            if ("UDHAAR".equals(tx.type)) {
                amount.setTextColor(Color.parseColor("#EF4444"));
                badge.setText("UDHAAR");
                badge.setTextColor(Color.parseColor("#EF4444"));
                badge.setBackgroundResource(R.drawable.badge_udhaar);
            } else {
                amount.setTextColor(Color.parseColor("#10B981"));
                badge.setText("JAMA / PAY");
                badge.setTextColor(Color.parseColor("#10B981"));
                badge.setBackgroundResource(R.drawable.badge_jama);
            }

            return convertView;
        }
    }
}