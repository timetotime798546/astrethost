package com.bizkeep.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private static final String COLLECTION = "transactions";

    private TextView tvNetBalance, tvTotalIncome, tvTotalExpense;
    private ListView lvTransactions;
    private Button btnAddEntry, btnSync, btnLogout;
    private ProgressBar mainProgress;

    private BackendApi backendApi;
    private List<LedgerItem> transactionList;
    private LedgerAdapter adapter;

    private DecimalFormat df = new DecimalFormat("$#,##0.00");

    // Local ledger entity
    public static class LedgerItem {
        public String id;
        public String title;
        public String type; // "income" or "expense"
        public double amount;
        public String date;
        public String notes;

        public LedgerItem(String id, String title, String type, double amount, String date, String notes) {
            this.id = id;
            this.title = title;
            this.type = type;
            this.amount = amount;
            this.date = date;
            this.notes = notes;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        backendApi = new BackendApi(this);
        transactionList = new ArrayList<LedgerItem>();

        tvNetBalance = (TextView) findViewById(R.id.tv_net_balance);
        tvTotalIncome = (TextView) findViewById(R.id.tv_total_income);
        tvTotalExpense = (TextView) findViewById(R.id.tv_total_expense);
        lvTransactions = (ListView) findViewById(R.id.lv_transactions);
        btnAddEntry = (Button) findViewById(R.id.btn_add_entry);
        btnSync = (Button) findViewById(R.id.btn_sync);
        btnLogout = (Button) findViewById(R.id.btn_logout);
        mainProgress = (ProgressBar) findViewById(R.id.main_progress);

        adapter = new LedgerAdapter();
        lvTransactions.setAdapter(adapter);

        btnAddEntry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddEditDialog(null);
            }
        });

        btnSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadLedgerEntries();
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogout();
            }
        });

        loadLedgerEntries();
    }

    // MANDATORY: Native calculation of totals
    private void calculateAndDisplayTotals() {
        double totalIncome = 0.0;
        double totalExpense = 0.0;

        for (int i = 0; i < transactionList.size(); i++) {
            LedgerItem item = transactionList.get(i);
            if ("income".equalsIgnoreCase(item.type)) {
                totalIncome += item.amount;
            } else if ("expense".equalsIgnoreCase(item.type)) {
                totalExpense += item.amount;
            }
        }

        double netBalance = totalIncome - totalExpense;

        tvTotalIncome.setText(df.format(totalIncome));
        tvTotalExpense.setText(df.format(totalExpense));
        tvNetBalance.setText(df.format(netBalance));
        if (netBalance >= 0) {
            tvNetBalance.setTextColor(0xFF2E7D32); // Greenish
        } else {
            tvNetBalance.setTextColor(0xFFC62828); // Reddish
        }
    }

    private void loadLedgerEntries() {
        mainProgress.setVisibility(View.VISIBLE);
        backendApi.readRecords(COLLECTION, new BackendApi.ApiCallback<JSONArray>() {
            @Override
            public void onSuccess(final JSONArray records) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mainProgress.setVisibility(View.GONE);
                        transactionList.clear();
                        try {
                            for (int i = 0; i < records.length(); i++) {
                                JSONObject recordObj = records.getJSONObject(i);
                                String id = recordObj.getString("id");
                                JSONObject innerData = recordObj.getJSONObject("data");

                                String title = innerData.optString("title", "No Title");
                                String type = innerData.optString("type", "income");
                                double amount = innerData.optDouble("amount", 0.0);
                                String date = innerData.optString("date", "");
                                String notes = innerData.optString("notes", "");

                                transactionList.add(new LedgerItem(id, title, type, amount, date, notes));
                            }
                            adapter.notifyDataSetChanged();
                            calculateAndDisplayTotals();
                        } catch (Exception e) {
                            Log.e(TAG, "Parsing transaction JSON failed", e);
                            Toast.makeText(MainActivity.this, "Data formatting parsed with errors.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mainProgress.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, "Network Sync Failed: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void showAddEditDialog(final LedgerItem existingItem) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_transaction);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView tvTitle = (TextView) dialog.findViewById(R.id.tv_dialog_title);
        final EditText etTitle = (EditText) dialog.findViewById(R.id.et_dialog_title);
        final RadioGroup rgType = (RadioGroup) dialog.findViewById(R.id.rg_dialog_type);
        final RadioButton rbIncome = (RadioButton) dialog.findViewById(R.id.rb_income);
        final RadioButton rbExpense = (RadioButton) dialog.findViewById(R.id.rb_expense);
        final EditText etAmount = (EditText) dialog.findViewById(R.id.et_dialog_amount);
        final EditText etDate = (EditText) dialog.findViewById(R.id.et_dialog_date);
        final EditText etNotes = (EditText) dialog.findViewById(R.id.et_dialog_notes);

        Button btnCancel = (Button) dialog.findViewById(R.id.btn_dialog_cancel);
        Button btnSave = (Button) dialog.findViewById(R.id.btn_dialog_save);

        if (existingItem != null) {
            tvTitle.setText("Edit Entry Details");
            etTitle.setText(existingItem.title);
            if ("income".equalsIgnoreCase(existingItem.type)) {
                rbIncome.setChecked(true);
            } else {
                rbExpense.setChecked(true);
            }
            etAmount.setText(String.valueOf(existingItem.amount));
            etDate.setText(existingItem.date);
            etNotes.setText(existingItem.notes);
        } else {
            tvTitle.setText("Add Ledger Entry");
            etDate.setText(new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()));
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
                String title = etTitle.getText().toString().trim();
                String amountStr = etAmount.getText().toString().trim();
                String dateStr = etDate.getText().toString().trim();
                String notesStr = etNotes.getText().toString().trim();

                if (title.isEmpty() || amountStr.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Title and Amount are mandatory.", Toast.LENGTH_SHORT).show();
                    return;
                }

                double amount = 0.0;
                try {
                    amount = Double.parseDouble(amountStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(MainActivity.this, "Invalid entry for Amount field", Toast.LENGTH_SHORT).show();
                    return;
                }

                String type = rbIncome.isChecked() ? "income" : "expense";

                try {
                    JSONObject recordData = new JSONObject();
                    recordData.put("title", title);
                    recordData.put("type", type);
                    recordData.put("amount", amount);
                    recordData.put("date", dateStr);
                    recordData.put("notes", notesStr);

                    mainProgress.setVisibility(View.VISIBLE);
                    dialog.dismiss();

                    if (existingItem == null) {
                        // Create Action
                        backendApi.createRecord(COLLECTION, recordData, new BackendApi.ApiCallback<JSONObject>() {
                            @Override
                            public void onSuccess(JSONObject result) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MainActivity.this, "Created successfully", Toast.LENGTH_SHORT).show();
                                        loadLedgerEntries();
                                    }
                                });
                            }

                            @Override
                            public void onError(final String error) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mainProgress.setVisibility(View.GONE);
                                        Toast.makeText(MainActivity.this, "Error saving entry: " + error, Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        });
                    } else {
                        // Update Action
                        backendApi.updateRecord(existingItem.id, recordData, new BackendApi.ApiCallback<String>() {
                            @Override
                            public void onSuccess(String resultId) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MainActivity.this, "Updated successfully", Toast.LENGTH_SHORT).show();
                                        loadLedgerEntries();
                                    }
                                });
                            }

                            @Override
                            public void onError(final String error) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mainProgress.setVisibility(View.GONE);
                                        Toast.makeText(MainActivity.this, "Error editing entry: " + error, Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        });
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        dialog.show();
    }

    private void deleteItem(final LedgerItem item) {
        mainProgress.setVisibility(View.VISIBLE);
        backendApi.deleteRecord(item.id, new BackendApi.ApiCallback<String>() {
            @Override
            public void onSuccess(String resultId) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Record successfully purged", Toast.LENGTH_SHORT).show();
                        loadLedgerEntries();
                    }
                });
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mainProgress.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, "Purge failure: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void performLogout() {
        mainProgress.setVisibility(View.VISIBLE);
        backendApi.logout(new BackendApi.ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mainProgress.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, "Signed Out Successfully!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        finish();
                    }
                });
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mainProgress.setVisibility(View.GONE);
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        finish();
                    }
                });
            }
        });
    }

    private class LedgerAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return transactionList.size();
        }

        @Override
        public Object getItem(int position) {
            return transactionList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.list_item_transaction, parent, false);
            }

            final LedgerItem item = transactionList.get(position);

            View indicator = convertView.findViewById(R.id.view_indicator);
            TextView tvTitle = (TextView) convertView.findViewById(R.id.tv_item_title);
            TextView tvDate = (TextView) convertView.findViewById(R.id.tv_item_date);
            TextView tvNotes = (TextView) convertView.findViewById(R.id.tv_item_notes);
            TextView tvAmount = (TextView) convertView.findViewById(R.id.tv_item_amount);

            ImageView btnEdit = (ImageView) convertView.findViewById(R.id.img_item_edit);
            ImageView btnDelete = (ImageView) convertView.findViewById(R.id.img_item_delete);

            tvTitle.setText(item.title);
            tvDate.setText(item.date);
            tvNotes.setText(item.notes.isEmpty() ? "No Notes" : item.notes);

            if ("income".equalsIgnoreCase(item.type)) {
                indicator.setBackgroundColor(0xFF2E7D32); // Dark Green
                tvAmount.setTextColor(0xFF2E7D32);
                tvAmount.setText("+" + df.format(item.amount));
            } else {
                indicator.setBackgroundColor(0xFFC62828); // Dark Red
                tvAmount.setTextColor(0xFFC62828);
                tvAmount.setText("-" + df.format(item.amount));
            }

            btnEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAddEditDialog(item);
                }
            });

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteItem(item);
                }
            });

            return convertView;
        }
    }
}