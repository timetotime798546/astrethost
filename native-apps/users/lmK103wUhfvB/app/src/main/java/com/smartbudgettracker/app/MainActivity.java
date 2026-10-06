package com.smartbudgettracker.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView tvNetBalance, tvTotalIncome, tvTotalExpenses, tvSavingsRate, tvRecordCount;
    private ListView listViewTransactions;
    private Button btnNewTransaction, btnLogout;
    private BackendApi backendApi;
    private List<Transaction> transactionList;
    private TransactionAdapter adapter;
    private ProgressDialog progressDialog;

    private static final String[] CATEGORIES = {"Food", "Transport", "Utilities", "Rent", "Entertainment", "Salary", "Bonus", "Others"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        backendApi = new BackendApi(this);

        tvNetBalance = (TextView) findViewById(R.id.tvNetBalance);
        tvTotalIncome = (TextView) findViewById(R.id.tvTotalIncome);
        tvTotalExpenses = (TextView) findViewById(R.id.tvTotalExpenses);
        tvSavingsRate = (TextView) findViewById(R.id.tvSavingsRate);
        tvRecordCount = (TextView) findViewById(R.id.tvRecordCount);
        listViewTransactions = (ListView) findViewById(R.id.listViewTransactions);
        btnNewTransaction = (Button) findViewById(R.id.btnNewTransaction);
        btnLogout = (Button) findViewById(R.id.btnLogout);

        transactionList = new ArrayList<>();
        adapter = new TransactionAdapter(this, transactionList);
        listViewTransactions.setAdapter(adapter);

        btnNewTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTransactionDialog(null);
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogout();
            }
        });

        listViewTransactions.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showActionDialog(transactionList.get(position));
            }
        });

        loadData();
    }

    private void performLogout() {
        showProgress("Logging out...");
        backendApi.logout(new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideProgress();
                        backendApi.clearSession();
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                });
            }

            @Override
            public void onError(final String errorMessage) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideProgress();
                        // Even if call fails on server, clear session locally to prevent lock out
                        backendApi.clearSession();
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                });
            }
        });
    }

    private void loadData() {
        showProgress("Syncing ledger entries...");
        backendApi.fetchTransactions(new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(final String response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideProgress();
                        try {
                            JSONObject root = new JSONObject(response);
                            if (root.optBoolean("success", false)) {
                                transactionList.clear();
                                JSONArray records = root.getJSONArray("records");
                                for (int i = 0; i < records.length(); i++) {
                                    JSONObject rec = records.getJSONObject(i);
                                    String id = rec.getString("id");
                                    JSONObject data = rec.getJSONObject("data");
                                    
                                    Transaction t = new Transaction(
                                            id,
                                            data.getString("title"),
                                            data.getDouble("amount"),
                                            data.getString("type"),
                                            data.getString("category"),
                                            data.optString("date", "Today")
                                    );
                                    transactionList.add(t);
                                }
                                adapter.notifyDataSetChanged();
                                calculateOfflineStatistics();
                            } else {
                                Toast.makeText(MainActivity.this, "Failed to load dashboard data.", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "Error updating UI models.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onError(final String errorMessage) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideProgress();
                        Toast.makeText(MainActivity.this, "Sync Error: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    // MANDATORY OFFLINE CALCULATIONS
    private void calculateOfflineStatistics() {
        double incomeTotal = 0.0;
        double expenseTotal = 0.0;

        for (Transaction t : transactionList) {
            if ("income".equalsIgnoreCase(t.getType())) {
                incomeTotal += t.getAmount();
            } else {
                expenseTotal += t.getAmount();
            }
        }

        double balance = incomeTotal - expenseTotal;
        double savingsRate = 0.0;
        if (incomeTotal > 0) {
            savingsRate = ((incomeTotal - expenseTotal) / incomeTotal) * 100.0;
        }

        // Apply formatted numbers to the local UI controls
        tvNetBalance.setText(String.format(Locale.getDefault(), "$%.2f", balance));
        tvTotalIncome.setText(String.format(Locale.getDefault(), "$%.2f", incomeTotal));
        tvTotalExpenses.setText(String.format(Locale.getDefault(), "$%.2f", expenseTotal));
        
        tvRecordCount.setText("Transactions: " + transactionList.size());
        if (incomeTotal > 0) {
            tvSavingsRate.setText(String.format(Locale.getDefault(), "Savings Rate: %.1f%%", savingsRate));
        } else {
            tvSavingsRate.setText("Savings Rate: N/A");
        }
    }

    private void showActionDialog(final Transaction transaction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(transaction.getTitle());
        builder.setItems(new String[]{"Edit Entry", "Delete Entry"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    showTransactionDialog(transaction);
                } else if (which == 1) {
                    confirmDelete(transaction);
                }
            }
        });
        builder.show();
    }

    private void confirmDelete(final Transaction transaction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Deletion");
        builder.setMessage("Are you sure you want to delete this ledger entry?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showProgress("Deleting ledger item...");
                backendApi.deleteTransaction(transaction.getId(), new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(final String response) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hideProgress();
                                Toast.makeText(MainActivity.this, "Entry removed.", Toast.LENGTH_SHORT).show();
                                loadData();
                            }
                        });
                    }

                    @Override
                    public void onError(final String errorMessage) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hideProgress();
                                Toast.makeText(MainActivity.this, "Deletion Failed: " + errorMessage, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showTransactionDialog(final Transaction editTransaction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_transaction, null);
        builder.setView(view);

        final TextView tvDialogTitle = (TextView) view.findViewById(R.id.tvDialogTitle);
        final EditText etTxTitle = (EditText) view.findViewById(R.id.etTxTitle);
        final EditText etTxAmount = (EditText) view.findViewById(R.id.etTxAmount);
        final RadioGroup rgTxType = (RadioGroup) view.findViewById(R.id.rgTxType);
        final RadioButton rbExpense = (RadioButton) view.findViewById(R.id.rbExpense);
        final RadioButton rbIncome = (RadioButton) view.findViewById(R.id.rbIncome);
        final Spinner spinnerCategory = (Spinner) view.findViewById(R.id.spinnerCategory);

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, CATEGORIES);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(catAdapter);

        if (editTransaction != null) {
            tvDialogTitle.setText("Edit Transaction");
            etTxTitle.setText(editTransaction.getTitle());
            etTxAmount.setText(String.valueOf(editTransaction.getAmount()));
            if ("income".equalsIgnoreCase(editTransaction.getType())) {
                rbIncome.setChecked(true);
            } else {
                rbExpense.setChecked(true);
            }
            int index = 0;
            for (int i = 0; i < CATEGORIES.length; i++) {
                if (CATEGORIES[i].equalsIgnoreCase(editTransaction.getCategory())) {
                    index = i;
                    break;
                }
            }
            spinnerCategory.setSelection(index);
        }

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String title = etTxTitle.getText().toString().trim();
                String amountStr = etTxAmount.getText().toString().trim();
                String type = rbIncome.isChecked() ? "income" : "expense";
                String category = spinnerCategory.getSelectedItem().toString();

                if (title.isEmpty() || amountStr.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please fill in all details.", Toast.LENGTH_SHORT).show();
                    return;
                }

                double amount;
                try {
                    amount = Double.parseDouble(amountStr);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Invalid amount value.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                showProgress("Saving transaction...");
                if (editTransaction == null) {
                    backendApi.createTransaction(title, amount, type, category, date, new BackendApi.ApiCallback() {
                        @Override
                        public void onSuccess(final String response) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    hideProgress();
                                    Toast.makeText(MainActivity.this, "Transaction logged.", Toast.LENGTH_SHORT).show();
                                    loadData();
                                }
                            });
                        }

                        @Override
                        public void onError(final String errorMessage) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    hideProgress();
                                    Toast.makeText(MainActivity.this, "Failed saving transaction: " + errorMessage, Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    });
                } else {
                    backendApi.updateTransaction(editTransaction.getId(), title, amount, type, category, editTransaction.getDate(), new BackendApi.ApiCallback() {
                        @Override
                        public void onSuccess(final String response) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    hideProgress();
                                    Toast.makeText(MainActivity.this, "Transaction modified.", Toast.LENGTH_SHORT).show();
                                    loadData();
                                }
                            });
                        }

                        @Override
                        public void onError(final String errorMessage) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    hideProgress();
                                    Toast.makeText(MainActivity.this, "Modification failed: " + errorMessage, Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    });
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showProgress(String message) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void hideProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private static class TransactionAdapter extends ArrayAdapter<Transaction> {
        public TransactionAdapter(Context context, List<Transaction> transactions) {
            super(context, 0, transactions);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Transaction t = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            }

            TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
            TextView text2 = (TextView) convertView.findViewById(android.R.id.text2);

            String prefix = "income".equalsIgnoreCase(t.getType()) ? "+" : "-";
            text1.setText(t.getTitle() + " (" + t.getCategory() + ")");
            text2.setText(prefix + String.format(Locale.getDefault(), "$%.2f", t.getAmount()) + "  |  " + t.getDate());

            if ("income".equalsIgnoreCase(t.getType())) {
                text2.setTextColor(getContext().getResources().getColor(R.color.income_green));
            } else {
                text2.setTextColor(getContext().getResources().getColor(R.color.expense_red));
            }

            return convertView;
        }
    }
}