package com.digitalkhatabilling.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CustomerDetailActivity extends Activity {
    public static class Transaction {
        public String id;
        public double amount;
        public String type;
        public String date;
        public String notes;
    }

    private String customerName, customerPhone, customerId;
    private BackendApi backendApi;
    private List<Transaction> transactions = new ArrayList<>();
    
    private TextView tvName, tvPhone, tvBalance, tvTotalGot, tvTotalGave;
    private ListView lvTransactions;
    private Button btnGave, btnGot, btnPdf, btnCsv, btnBack;
    private ProgressBar progressBar;
    private TransactionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_detail);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        backendApi = new BackendApi(this);

        customerName = getIntent().getStringExtra("customer_name");
        customerPhone = getIntent().getStringExtra("customer_phone");
        customerId = getIntent().getStringExtra("customer_id");

        tvName = findViewById(R.id.detail_tv_name);
        tvPhone = findViewById(R.id.detail_tv_phone);
        tvBalance = findViewById(R.id.detail_tv_balance);
        tvTotalGot = findViewById(R.id.detail_tv_total_got);
        tvTotalGave = findViewById(R.id.detail_tv_total_gave);
        lvTransactions = findViewById(R.id.detail_lv_transactions);
        btnGave = findViewById(R.id.btn_gave);
        btnGot = findViewById(R.id.btn_got);
        btnPdf = findViewById(R.id.btn_pdf);
        btnCsv = findViewById(R.id.btn_csv);
        btnBack = findViewById(R.id.btn_back);
        progressBar = findViewById(R.id.detail_progress_bar);

        tvName.setText(customerName);
        tvPhone.setText(customerPhone);

        adapter = new TransactionAdapter(this, transactions);
        lvTransactions.setAdapter(adapter);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnGave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddTransactionDialog("gave");
            }
        });

        btnGot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddTransactionDialog("got");
            }
        });

        btnPdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateAndSharePDF();
            }
        });

        btnCsv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateAndShareCSV();
            }
        });

        loadTransactions();
    }

    private void loadTransactions() {
        progressBar.setVisibility(View.VISIBLE);
        backendApi.getRecords("khata_entries", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(final String response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        parseAndDisplayTransactions(response);
                    }
                });
            }

            @Override
            public void onFailure(final String errorMessage) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(CustomerDetailActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void parseAndDisplayTransactions(String jsonStr) {
        try {
            transactions.clear();
            JSONObject obj = new JSONObject(jsonStr);
            if (obj.optBoolean("success")) {
                JSONArray records = obj.optJSONArray("records");
                if (records != null) {
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject rec = records.getJSONObject(i);
                        JSONObject data = rec.getJSONObject("data");
                        String phone = data.optString("customer_phone");
                        if (customerPhone.equals(phone)) {
                            Transaction t = new Transaction();
                            t.id = rec.getString("id");
                            t.amount = data.optDouble("amount");
                            t.type = data.optString("type");
                            t.date = data.optString("date");
                            t.notes = data.optString("notes");
                            transactions.add(t);
                        }
                    }
                }
            }

            double gotSum = 0;
            double gaveSum = 0;
            for (Transaction t : transactions) {
                if ("got".equals(t.type)) {
                    gotSum += t.amount;
                } else if ("gave".equals(t.type)) {
                    gaveSum += t.amount;
                }
            }

            double netBalance = gotSum - gaveSum;
            tvTotalGot.setText("\u20B9" + String.format("%.2f", gotSum));
            tvTotalGave.setText("\u20B9" + String.format("%.2f", gaveSum));

            if (netBalance > 0) {
                tvBalance.setText("Jama: \u20B9" + String.format("%.2f", netBalance));
                tvBalance.setTextColor(Color.parseColor("#00E676"));
            } else if (netBalance < 0) {
                tvBalance.setText("Udhaar: \u20B9" + String.format("%.2f", Math.abs(netBalance)));
                tvBalance.setTextColor(Color.parseColor("#FF1744"));
            } else {
                tvBalance.setText("Settled: \u20B90.00");
                tvBalance.setTextColor(Color.WHITE);
            }

            adapter.notifyDataSetChanged();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to calculate transactions", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddTransactionDialog(final String type) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_transaction, null);
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView tvTitle = view.findViewById(R.id.dialog_trans_title);
        final EditText etAmount = view.findViewById(R.id.dialog_et_amount);
        final EditText etNotes = view.findViewById(R.id.dialog_et_notes);
        Button btnSave = view.findViewById(R.id.dialog_trans_btn_save);
        Button btnCancel = view.findViewById(R.id.dialog_trans_btn_cancel);

        if ("gave".equals(type)) {
            tvTitle.setText("Add Udhaar (Gave)");
            tvTitle.setTextColor(Color.parseColor("#FF1744"));
        } else {
            tvTitle.setText("Add Jama (Got)");
            tvTitle.setTextColor(Color.parseColor("#00E676"));
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
                String amountStr = etAmount.getText().toString().trim();
                String notes = etNotes.getText().toString().trim();

                if (amountStr.isEmpty()) {
                    Toast.makeText(CustomerDetailActivity.this, "Please enter amount", Toast.LENGTH_SHORT).show();
                    return;
                }

                double amount;
                try {
                    amount = Double.parseDouble(amountStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(CustomerDetailActivity.this, "Invalid amount", Toast.LENGTH_SHORT).show();
                    return;
                }

                String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

                JSONObject fields = new JSONObject();
                try {
                    fields.put("customer_phone", customerPhone);
                    fields.put("amount", amount);
                    fields.put("type", type);
                    fields.put("date", currentDate);
                    fields.put("notes", notes.isEmpty() ? "-" : notes);
                } catch (Exception ignored) {}

                dialog.dismiss();
                progressBar.setVisibility(View.VISIBLE);

                backendApi.createRecord("khata_entries", fields, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                loadTransactions();
                            }
                        });
                    }

                    @Override
                    public void onFailure(final String errorMessage) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(CustomerDetailActivity.this, "Transaction failed: " + errorMessage, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            }
        });

        dialog.show();
    }

    private void generateAndSharePDF() {
        if (transactions.isEmpty()) {
            Toast.makeText(this, "No transactions to export", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();

        paint.setColor(Color.parseColor("#090514"));
        canvas.drawRect(0, 0, 595, 120, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(24f);
        paint.setFakeBoldText(true);
        canvas.drawText("DIGITAL KHATA STATEMENT", 40, 60, paint);

        paint.setTextSize(12f);
        paint.setFakeBoldText(false);
        String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
        canvas.drawText("Date Generated: " + currentDate, 40, 90, paint);

        paint.setColor(Color.BLACK);
        paint.setTextSize(14f);
        paint.setFakeBoldText(true);
        canvas.drawText("CUSTOMER DETAILS", 40, 160, paint);

        paint.setFakeBoldText(false);
        paint.setTextSize(12f);
        canvas.drawText("Name: " + customerName, 40, 185, paint);
        canvas.drawText("Phone: " + customerPhone, 40, 205, paint);

        double totalGot = 0;
        double totalGave = 0;
        for (Transaction t : transactions) {
            if ("got".equals(t.type)) totalGot += t.amount;
            else totalGave += t.amount;
        }
        double net = totalGot - totalGave;

        canvas.drawText("Total Got (Jama): Rs. " + String.format("%.2f", totalGot), 350, 185, paint);
        canvas.drawText("Total Gave (Udhaar): Rs. " + String.format("%.2f", totalGave), 350, 205, paint);
        
        paint.setFakeBoldText(true);
        canvas.drawText("Net Balance: Rs. " + String.format("%.2f", net), 350, 230, paint);
        paint.setFakeBoldText(false);

        canvas.drawRect(40, 260, 555, 262, paint);

        paint.setFakeBoldText(true);
        canvas.drawText("Date", 50, 280, paint);
        canvas.drawText("Type", 200, 280, paint);
        canvas.drawText("Notes", 300, 280, paint);
        canvas.drawText("Amount", 480, 280, paint);
        paint.setFakeBoldText(false);

        canvas.drawRect(40, 290, 555, 292, paint);

        int y = 315;
        for (Transaction t : transactions) {
            if (y > 800) break;
            canvas.drawText(t.date, 50, y, paint);
            canvas.drawText(t.type.toUpperCase(), 200, y, paint);
            
            String noteCut = t.notes.length() > 20 ? t.notes.substring(0, 18) + ".." : t.notes;
            canvas.drawText(noteCut, 300, y, paint);
            
            canvas.drawText("Rs. " + String.format("%.2f", t.amount), 480, y, paint);
            y += 25;
        }

        document.finishPage(page);

        File pdfFile = new File(getExternalCacheDir(), "Khata_" + customerPhone + ".pdf");
        try {
            document.writeTo(new FileOutputStream(pdfFile));
            Toast.makeText(this, "PDF Generated Successfully", Toast.LENGTH_SHORT).show();
            
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(pdfFile));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Digital Khata Receipt - " + customerName);
            intent.putExtra(Intent.EXTRA_TEXT, "Here is the Digital Khata statement for " + customerName + " (" + customerPhone + ").");
            startActivity(Intent.createChooser(intent, "Share Bill/Receipt via"));

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save PDF file", Toast.LENGTH_SHORT).show();
        } finally {
            document.close();
        }
    }

    private void generateAndShareCSV() {
        if (transactions.isEmpty()) {
            Toast.makeText(this, "No transactions to export", Toast.LENGTH_SHORT).show();
            return;
        }

        File csvFile = new File(getExternalCacheDir(), "Khata_" + customerPhone + ".csv");
        try {
            FileWriter writer = new FileWriter(csvFile);
            writer.write("Customer Name,Customer Phone,Date,Type,Notes,Amount\n");
            for (Transaction t : transactions) {
                writer.write(customerName + "," + customerPhone + "," + t.date + "," + t.type.toUpperCase() + "," + t.notes.replace(",", " ") + "," + t.amount + "\n");
            }
            writer.flush();
            writer.close();

            Toast.makeText(this, "CSV Data Exported", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(csvFile));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Khata CSV Export - " + customerName);
            startActivity(Intent.createChooser(intent, "Share CSV via"));

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to export CSV file", Toast.LENGTH_SHORT).show();
        }
    }

    private static class TransactionAdapter extends BaseAdapter {
        private Context context;
        private List<Transaction> list;

        public TransactionAdapter(Context context, List<Transaction> list) {
            this.context = context;
            this.list = list;
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
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.list_item_transaction, parent, false);
            }

            TextView tvDate = convertView.findViewById(R.id.item_trans_date);
            TextView tvNotes = convertView.findViewById(R.id.item_trans_notes);
            TextView tvAmount = convertView.findViewById(R.id.item_trans_amount);
            TextView tvTypePill = convertView.findViewById(R.id.item_trans_type_pill);

            Transaction t = list.get(position);
            tvDate.setText(t.date);
            tvNotes.setText(t.notes);
            tvAmount.setText("\u20B9" + String.format("%.2f", t.amount));

            if ("got".equals(t.type)) {
                tvTypePill.setText("Got (Jama)");
                tvTypePill.setBackgroundResource(R.drawable.glass_card_green);
                tvTypePill.setTextColor(Color.parseColor("#00E676"));
                tvAmount.setTextColor(Color.parseColor("#00E676"));
            } else {
                tvTypePill.setText("Gave (Udhaar)");
                tvTypePill.setBackgroundResource(R.drawable.glass_card_red);
                tvTypePill.setTextColor(Color.parseColor("#FF1744"));
                tvAmount.setTextColor(Color.parseColor("#FF1744"));
            }

            return convertView;
        }
    }
}