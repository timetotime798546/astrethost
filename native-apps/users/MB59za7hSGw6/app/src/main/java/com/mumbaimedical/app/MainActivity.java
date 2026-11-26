package com.mumbaimedical.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;
    private TextView tvTotalMedicines, tvStockValue, tvTodaySales, tvTodayProfit, tvLowStockCount, tvExpiringCount;
    private Button btnNewSale, btnNewPurchase, btnMedicines, btnSalesHistory, btnCustomers, btnSuppliers, btnReports, btnBackupRestore, btnLanguage;
    private LinearLayout cardLowStock, cardExpiryAlerts;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!LocaleHelper.isLanguageSet(this)) {
            showLanguageSelectionDialog(true);
        }

        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        tvTotalMedicines = findViewById(R.id.tvTotalMedicines);
        tvStockValue = findViewById(R.id.tvStockValue);
        tvTodaySales = findViewById(R.id.tvTodaySales);
        tvTodayProfit = findViewById(R.id.tvTodayProfit);
        tvLowStockCount = findViewById(R.id.tvLowStockCount);
        tvExpiringCount = findViewById(R.id.tvExpiringCount);

        cardLowStock = findViewById(R.id.cardLowStock);
        cardExpiryAlerts = findViewById(R.id.cardExpiryAlerts);

        btnNewSale = findViewById(R.id.btnNewSale);
        btnNewPurchase = findViewById(R.id.btnNewPurchase);
        btnMedicines = findViewById(R.id.btnMedicines);
        btnSalesHistory = findViewById(R.id.btnSalesHistory);
        btnCustomers = findViewById(R.id.btnCustomers);
        btnSuppliers = findViewById(R.id.btnSuppliers);
        btnReports = findViewById(R.id.btnReports);
        btnBackupRestore = findViewById(R.id.btnBackupRestore);
        btnLanguage = findViewById(R.id.btnLanguage);

        btnLanguage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLanguageSelectionDialog(false);
            }
        });

        btnNewSale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, NewSaleActivity.class));
            }
        });

        btnNewPurchase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, NewPurchaseActivity.class));
            }
        });

        btnMedicines.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, MedicineListActivity.class));
            }
        });

        btnSalesHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SalesListActivity.class));
            }
        });

        btnCustomers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, CustomerActivity.class));
            }
        });

        btnSuppliers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SupplierActivity.class));
            }
        });

        btnReports.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ReportsActivity.class));
            }
        });

        btnBackupRestore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, BackupRestoreActivity.class));
            }
        });

        cardLowStock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MedicineListActivity.class);
                intent.putExtra("filter", "low_stock");
                startActivity(intent);
            }
        });

        cardExpiryAlerts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ExpiryAlertsActivity.class));
            }
        });
    }

    private void showLanguageSelectionDialog(final boolean firstLaunch) {
        String[] languages = new String[]{"English", "Hindi (हिंदी)"};
        int currentSelection = "hi".equals(LocaleHelper.getLanguage(this)) ? 1 : 0;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_language);
        builder.setSingleChoiceItems(languages, currentSelection, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String selectedLang = (which == 1) ? "hi" : "en";
                LocaleHelper.setLocale(MainActivity.this, selectedLang);
                dialog.dismiss();

                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });

        if (firstLaunch) {
            builder.setCancelable(false);
            builder.setNegativeButton("Default (English)", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    LocaleHelper.setLocale(MainActivity.this, "en");
                    dialog.dismiss();
                }
            });
        } else {
            builder.setNegativeButton(R.string.cancel, null);
        }

        builder.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
    }

    private void loadDashboardData() {
        int totalMedicines = dbHelper.getTotalMedicinesCount();
        double stockValue = dbHelper.getTotalStockValue();
        double todaySales = dbHelper.getTodaySalesTotal();
        double todayProfit = dbHelper.getTodayProfit();
        int lowStock = dbHelper.getLowStockCount();
        int expiring = dbHelper.getExpiringMedicinesCount();

        tvTotalMedicines.setText(String.valueOf(totalMedicines));
        tvStockValue.setText(String.format(Locale.getDefault(), "₹ %.2f", stockValue));
        tvTodaySales.setText(String.format(Locale.getDefault(), "₹ %.2f", todaySales));
        tvTodayProfit.setText(String.format(Locale.getDefault(), "₹ %.2f", todayProfit));
        tvLowStockCount.setText(lowStock + " " + getString(R.string.items));
        tvExpiringCount.setText(expiring + " " + getString(R.string.items));
    }
}