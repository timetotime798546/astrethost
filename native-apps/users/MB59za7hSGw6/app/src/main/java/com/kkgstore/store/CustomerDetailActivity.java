package com.kkgstore.store;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CustomerDetailActivity extends Activity {

    private static final int REQUEST_CAMERA = 101;
    private static final int REQUEST_GALLERY = 102;

    private long customerId;
    private String customerName, customerMobile;
    private DatabaseHelper dbHelper;

    private TextView tvName, tvMobile, tvTotalUdhaar, tvTotalJama, tvPendingUdhaar;
    private LinearLayout layoutUdhaar, layoutJama, layoutSales;
    private ListView lvUdhaar, lvJama, lvSales;

    private List<UdhaarModel> udhaarList = new ArrayList<>();
    private List<JamaModel> jamaList = new ArrayList<>();
    private List<SaleModel> salesList = new ArrayList<>();

    private UdhaarAdapter udhaarAdapter;
    private JamaAdapter jamaAdapter;
    private SalesAdapter salesAdapter;

    // Active Dialog Photo References
    private String currentPhotoFilename = null;
    private ImageView currentDialogPreviewIv = null;
    private RelativeLayout currentDialogPreviewBox = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DemoLockManager.checkAndEnforceLock(this)) return;

        setContentView(R.layout.activity_customer_detail);
        dbHelper = new DatabaseHelper(this);

        customerId = getIntent().getLongExtra("customer_id", 0);
        customerName = getIntent().getStringExtra("customer_name");
        customerMobile = getIntent().getStringExtra("customer_mobile");

        tvName = (TextView) findViewById(R.id.tv_detail_name);
        tvMobile = (TextView) findViewById(R.id.tv_detail_mobile);
        tvTotalUdhaar = (TextView) findViewById(R.id.tv_detail_total_udhaar);
        tvTotalJama = (TextView) findViewById(R.id.tv_detail_total_jama);
        tvPendingUdhaar = (TextView) findViewById(R.id.tv_detail_pending_udhaar);

        tvName.setText(customerName);
        tvMobile.setText("Mobile: " + (customerMobile != null && !customerMobile.isEmpty() ? customerMobile : "N/A"));

        layoutUdhaar = (LinearLayout) findViewById(R.id.layout_section_udhaar);
        layoutJama = (LinearLayout) findViewById(R.id.layout_section_jama);
        layoutSales = (LinearLayout) findViewById(R.id.layout_section_sales);

        lvUdhaar = (ListView) findViewById(R.id.lv_udhaar_history);
        lvJama = (ListView) findViewById(R.id.lv_jama_history);
        lvSales = (ListView) findViewById(R.id.lv_sales_history);

        Button btnTabUdhaar = (Button) findViewById(R.id.btn_tab_udhaar);
        Button btnTabJama = (Button) findViewById(R.id.btn_tab_jama);
        Button btnTabSales = (Button) findViewById(R.id.btn_tab_sales);

        Button btnAddUdhaar = (Button) findViewById(R.id.btn_action_add_udhaar);
        Button btnAddJama = (Button) findViewById(R.id.btn_action_add_jama);
        Button btnShareStatement = (Button) findViewById(R.id.btn_share_statement);
        Button btnShareReminder = (Button) findViewById(R.id.btn_share_reminder);
        Button btnDeleteCust = (Button) findViewById(R.id.btn_delete_customer);

        udhaarAdapter = new UdhaarAdapter();
        lvUdhaar.setAdapter(udhaarAdapter);

        jamaAdapter = new JamaAdapter();
        lvJama.setAdapter(jamaAdapter);

        salesAdapter = new SalesAdapter();
        lvSales.setAdapter(salesAdapter);

        btnTabUdhaar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { showTab(1); }
        });

        btnTabJama.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { showTab(2); }
        });

        btnTabSales.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { showTab(3); }
        });

        btnAddUdhaar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { showAddUdhaarDialog(); }
        });

        btnAddJama.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { showAddJamaDialog(0); }
        });

        btnShareStatement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { shareStatement(); }
        });

        btnShareReminder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { shareReminder(); }
        });

        btnDeleteCust.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDeleteCustomer();
            }
        });

        showTab(1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DemoLockManager.checkAndEnforceLock(this)) return;
        loadCustomerData();
    }

    private void showTab(int tab) {
        layoutUdhaar.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        layoutJama.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);
        layoutSales.setVisibility(tab == 3 ? View.VISIBLE : View.GONE);
    }

    private void loadCustomerData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        double opening = 0;
        Cursor cCust = db.rawQuery("SELECT opening_balance FROM customers WHERE id=" + customerId, null);
        if (cCust.moveToFirst()) opening = cCust.getDouble(0);
        cCust.close();

        udhaarList.clear();
        double totalUdhaar = opening;
        Cursor cU = db.rawQuery("SELECT id, amount, notes, entry_date, photo_path FROM udhaar_entries WHERE customer_id=" + customerId + " ORDER BY id DESC", null);
        while (cU.moveToNext()) {
            double amt = cU.getDouble(1);
            totalUdhaar += amt;
            String photoPath = cU.getColumnCount() > 4 ? cU.getString(4) : null;
            udhaarList.add(new UdhaarModel(cU.getLong(0), amt, cU.getString(2), cU.getString(3), photoPath));
        }
        cU.close();

        jamaList.clear();
        double totalJama = 0;
        Cursor cJ = db.rawQuery("SELECT id, amount, method, notes, entry_date FROM jama_entries WHERE customer_id=" + customerId + " ORDER BY id DESC", null);
        while (cJ.moveToNext()) {
            double amt = cJ.getDouble(1);
            totalJama += amt;
            jamaList.add(new JamaModel(cJ.getLong(0), amt, cJ.getString(2), cJ.getString(3), cJ.getString(4)));
        }
        cJ.close();

        salesList.clear();
        Cursor cS = db.rawQuery("SELECT id, bill_no, grand_total, payment_mode, sale_date FROM sales WHERE customer_id=" + customerId + " ORDER BY id DESC", null);
        while (cS.moveToNext()) {
            salesList.add(new SaleModel(cS.getLong(0), cS.getInt(1), cS.getDouble(2), cS.getString(3), cS.getString(4)));
        }
        cS.close();

        double pending = Math.max(0, totalUdhaar - totalJama);

        tvTotalUdhaar.setText(String.format("₹ %.2f", totalUdhaar));
        tvTotalJama.setText(String.format("₹ %.2f", totalJama));
        tvPendingUdhaar.setText(String.format("₹ %.2f", pending));

        udhaarAdapter.notifyDataSetChanged();
        jamaAdapter.notifyDataSetChanged();
        salesAdapter.notifyDataSetChanged();
    }

    private void showAddUdhaarDialog() {
        currentPhotoFilename = null;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_udhaar, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        final EditText etAmount = (EditText) dialogView.findViewById(R.id.et_dialog_udhaar_amount);
        final EditText etNotes = (EditText) dialogView.findViewById(R.id.et_dialog_udhaar_notes);
        final EditText etDate = (EditText) dialogView.findViewById(R.id.et_dialog_udhaar_date);
        Button btnCamera = (Button) dialogView.findViewById(R.id.btn_dialog_udhaar_camera);
        Button btnGallery = (Button) dialogView.findViewById(R.id.btn_dialog_udhaar_gallery);
        Button btnRemovePhoto = (Button) dialogView.findViewById(R.id.btn_remove_photo);

        currentDialogPreviewIv = (ImageView) dialogView.findViewById(R.id.iv_dialog_udhaar_preview);
        currentDialogPreviewBox = (RelativeLayout) dialogView.findViewById(R.id.layout_photo_preview_box);

        Button btnSave = (Button) dialogView.findViewById(R.id.btn_dialog_udhaar_save);
        Button btnCancel = (Button) dialogView.findViewById(R.id.btn_dialog_udhaar_cancel);

        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
        etDate.setText(now);

        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchCameraIntent();
            }
        });

        btnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchGalleryIntent();
            }
        });

        btnRemovePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentPhotoFilename = null;
                if (currentDialogPreviewBox != null) currentDialogPreviewBox.setVisibility(View.GONE);
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { dialog.dismiss(); }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String amtStr = etAmount.getText().toString().trim();
                if (amtStr.isEmpty()) {
                    Toast.makeText(CustomerDetailActivity.this, "उधार राशि दर्ज करें", Toast.LENGTH_SHORT).show();
                    return;
                }
                double amt = Double.parseDouble(amtStr);
                String notes = etNotes.getText().toString().trim();
                String date = etDate.getText().toString().trim();
                String photoPath = currentPhotoFilename != null ? currentPhotoFilename : "";

                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.execSQL("INSERT INTO udhaar_entries (customer_id, amount, notes, entry_date, photo_path) VALUES ("
                        + customerId + ", " + amt + ", '" + notes + "', '" + date + "', '" + photoPath + "')");

                dialog.dismiss();
                loadCustomerData();
                Toast.makeText(CustomerDetailActivity.this, "उधार दर्ज कर दिया गया", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void launchCameraIntent() {
        try {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, REQUEST_CAMERA);
        } catch (Exception e) {
            Toast.makeText(this, "कैमरा खोलने में समस्या: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void launchGalleryIntent() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_GALLERY);
        } catch (Exception e) {
            Toast.makeText(this, "गैलरी खोलने में समस्या", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CAMERA) {
                if (data != null && data.getExtras() != null && data.getExtras().get("data") != null) {
                    Bitmap bmp = (Bitmap) data.getExtras().get("data");
                    String savedFile = ImageUtils.saveCompressedBitmap(this, bmp);
                    if (savedFile != null) {
                        currentPhotoFilename = savedFile;
                        updateDialogPhotoPreview();
                    }
                } else if (data != null && data.getData() != null) {
                    String savedFile = ImageUtils.saveCompressedImageFromUri(this, data.getData());
                    if (savedFile != null) {
                        currentPhotoFilename = savedFile;
                        updateDialogPhotoPreview();
                    }
                }
            } else if (requestCode == REQUEST_GALLERY && data != null && data.getData() != null) {
                String savedFile = ImageUtils.saveCompressedImageFromUri(this, data.getData());
                if (savedFile != null) {
                    currentPhotoFilename = savedFile;
                    updateDialogPhotoPreview();
                }
            }
        }
    }

    private void updateDialogPhotoPreview() {
        if (currentPhotoFilename != null && currentDialogPreviewIv != null && currentDialogPreviewBox != null) {
            Bitmap thumb = ImageUtils.loadThumbnail(this, currentPhotoFilename, 200, 200);
            if (thumb != null) {
                currentDialogPreviewIv.setImageBitmap(thumb);
                currentDialogPreviewBox.setVisibility(View.VISIBLE);
            }
        }
    }

    private void showAddJamaDialog(double defaultAmount) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_jama, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        TextView tvCust = (TextView) dialogView.findViewById(R.id.tv_dialog_customer_info);
        TextView tvOrig = (TextView) dialogView.findViewById(R.id.tv_dialog_original_amount);
        TextView tvPend = (TextView) dialogView.findViewById(R.id.tv_dialog_pending_amount);
        final EditText etAmount = (EditText) dialogView.findViewById(R.id.et_dialog_jama_amount);
        final Spinner spMethod = (Spinner) dialogView.findViewById(R.id.sp_dialog_jama_method);
        final EditText etNotes = (EditText) dialogView.findViewById(R.id.et_dialog_jama_notes);
        final EditText etDate = (EditText) dialogView.findViewById(R.id.et_dialog_jama_date);
        Button btnSave = (Button) dialogView.findViewById(R.id.btn_dialog_jama_save);
        Button btnCancel = (Button) dialogView.findViewById(R.id.btn_dialog_jama_cancel);

        tvCust.setText("ग्राहक: " + customerName);
        tvOrig.setText("कुल उधार दर्ज: " + tvTotalUdhaar.getText().toString());
        tvPend.setText("वर्तमान बाकी उधार: " + tvPendingUdhaar.getText().toString());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Cash (नगद)", "UPI (GPay/PhonePe)", "Bank Transfer", "Cheque"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMethod.setAdapter(adapter);

        if (defaultAmount > 0) {
            etAmount.setText(String.valueOf(defaultAmount));
        }

        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
        etDate.setText(now);

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { dialog.dismiss(); }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String amtStr = etAmount.getText().toString().trim();
                if (amtStr.isEmpty()) {
                    Toast.makeText(CustomerDetailActivity.this, "जमा राशि दर्ज करें", Toast.LENGTH_SHORT).show();
                    return;
                }
                double amt = Double.parseDouble(amtStr);
                String method = spMethod.getSelectedItem().toString();
                String notes = etNotes.getText().toString().trim();
                String date = etDate.getText().toString().trim();

                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.execSQL("INSERT INTO jama_entries (customer_id, amount, method, notes, entry_date) VALUES (" + customerId + ", " + amt + ", '" + method + "', '" + notes + "', '" + date + "')");

                dialog.dismiss();
                loadCustomerData();
                Toast.makeText(CustomerDetailActivity.this, "पैसा जमा कर दिया गया", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void confirmDeleteCustomer() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("ग्राहक खाता हटाएं");
        builder.setMessage("क्या आप निश्चित रूप से " + customerName + " का खाता हटाना चाहते हैं?");
        builder.setPositiveButton("हां, हटाएं", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.execSQL("UPDATE customers SET is_active=0 WHERE id=" + customerId);
                Toast.makeText(CustomerDetailActivity.this, "ग्राहक खाता हटा दिया गया है", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        builder.setNegativeButton("रद्द करें", null);
        builder.show();
    }

    private void shareStatement() {
        String msg = "📄 *KKG Store Khata Statement*\n"
                + "ग्राहक: " + customerName + "\n"
                + "कुल उधार: " + tvTotalUdhaar.getText().toString() + "\n"
                + "कुल जमा: " + tvTotalJama.getText().toString() + "\n"
                + "बाकी बकाया: " + tvPendingUdhaar.getText().toString() + "\n\n"
                + "कृपया बकाया राशि का भुगतान करें। धन्यवाद!";

        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, msg);
            startActivity(Intent.createChooser(intent, "Share Statement"));
        } catch (Exception e) {
            Toast.makeText(this, "शेयर सुविधा उपलब्ध नहीं है", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareReminder() {
        String msg = "⏰ *KKG Store Payment Reminder*\n\n"
                + "नमस्ते " + customerName + " जी,\n"
                + "आपका KKG Store पर कुल बाकी उधार: " + tvPendingUdhaar.getText().toString() + " है।\n"
                + "कृपया समय पर भुगतान करने का कष्ट करें।\n\n"
                + "धन्यवाद! - KKG Store";

        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, msg);
            startActivity(Intent.createChooser(intent, "Send Reminder"));
        } catch (Exception e) {
            Toast.makeText(this, "शेयर सुविधा उपलब्ध नहीं है", Toast.LENGTH_SHORT).show();
        }
    }

    private static class UdhaarModel {
        long id; double amount; String notes, date, photoPath;
        UdhaarModel(long id, double amount, String notes, String date, String photoPath) {
            this.id = id; this.amount = amount; this.notes = notes; this.date = date; this.photoPath = photoPath;
        }
    }

    private static class JamaModel {
        long id; double amount; String method, notes, date;
        JamaModel(long id, double amount, String method, String notes, String date) {
            this.id = id; this.amount = amount; this.method = method; this.notes = notes; this.date = date;
        }
    }

    private static class SaleModel {
        long id; int billNo; double grandTotal; String paymentMode, date;
        SaleModel(long id, int billNo, double grandTotal, String paymentMode, String date) {
            this.id = id; this.billNo = billNo; this.grandTotal = grandTotal; this.paymentMode = paymentMode; this.date = date;
        }
    }

    private class UdhaarAdapter extends BaseAdapter {
        @Override
        public int getCount() { return udhaarList.size(); }
        @Override
        public Object getItem(int position) { return udhaarList.get(position); }
        @Override
        public long getItemId(int position) { return udhaarList.get(position).id; }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(CustomerDetailActivity.this).inflate(R.layout.item_udhaar, parent, false);
            }
            final UdhaarModel m = udhaarList.get(position);
            TextView tvAmt = (TextView) convertView.findViewById(R.id.tv_udhaar_amount);
            TextView tvNotes = (TextView) convertView.findViewById(R.id.tv_udhaar_notes);
            TextView tvDate = (TextView) convertView.findViewById(R.id.tv_udhaar_date);
            ImageView ivPhotoThumb = (ImageView) convertView.findViewById(R.id.iv_udhaar_photo_thumb);
            Button btnJama = (Button) convertView.findViewById(R.id.btn_item_jama_karein);

            tvAmt.setText(String.format("उधार: ₹ %.2f", m.amount));
            tvNotes.setText(m.notes == null || m.notes.isEmpty() ? "उधार लेनदेन" : m.notes);
            tvDate.setText(m.date);

            if (m.photoPath != null && !m.photoPath.isEmpty()) {
                File photoFile = ImageUtils.getPhotoFile(CustomerDetailActivity.this, m.photoPath);
                if (photoFile != null && photoFile.exists()) {
                    Bitmap thumb = ImageUtils.loadThumbnail(CustomerDetailActivity.this, m.photoPath, 150, 150);
                    if (thumb != null) {
                        ivPhotoThumb.setImageBitmap(thumb);
                        ivPhotoThumb.setVisibility(View.VISIBLE);
                        ivPhotoThumb.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(CustomerDetailActivity.this, FullScreenImageActivity.class);
                                intent.putExtra("photo_path", m.photoPath);
                                startActivity(intent);
                            }
                        });
                    } else {
                        ivPhotoThumb.setVisibility(View.GONE);
                    }
                } else {
                    ivPhotoThumb.setVisibility(View.GONE);
                }
            } else {
                ivPhotoThumb.setVisibility(View.GONE);
            }

            btnJama.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAddJamaDialog(m.amount);
                }
            });
            return convertView;
        }
    }

    private class JamaAdapter extends BaseAdapter {
        @Override
        public int getCount() { return jamaList.size(); }
        @Override
        public Object getItem(int position) { return jamaList.get(position); }
        @Override
        public long getItemId(int position) { return jamaList.get(position).id; }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(CustomerDetailActivity.this).inflate(R.layout.item_jama, parent, false);
            }
            JamaModel m = jamaList.get(position);
            TextView tvBadge = (TextView) convertView.findViewById(R.id.tv_jama_method_badge);
            TextView tvAmt = (TextView) convertView.findViewById(R.id.tv_jama_amount);
            TextView tvNotes = (TextView) convertView.findViewById(R.id.tv_jama_notes);
            TextView tvDate = (TextView) convertView.findViewById(R.id.tv_jama_date);

            tvBadge.setText("🟢 " + (m.method == null ? "जमा" : m.method));
            tvAmt.setText(String.format("₹ %.2f", m.amount));
            tvNotes.setText(m.notes == null || m.notes.isEmpty() ? "पैसा जमा हुआ" : m.notes);
            tvDate.setText(m.date);
            return convertView;
        }
    }

    private class SalesAdapter extends BaseAdapter {
        @Override
        public int getCount() { return salesList.size(); }
        @Override
        public Object getItem(int position) { return salesList.get(position); }
        @Override
        public long getItemId(int position) { return salesList.get(position).id; }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(CustomerDetailActivity.this).inflate(R.layout.item_sale, parent, false);
            }
            final SaleModel m = salesList.get(position);
            TextView tvBillNo = (TextView) convertView.findViewById(R.id.tv_sale_bill_no);
            TextView tvBadge = (TextView) convertView.findViewById(R.id.tv_sale_mode_badge);
            TextView tvAmt = (TextView) convertView.findViewById(R.id.tv_sale_amount);
            TextView tvDate = (TextView) convertView.findViewById(R.id.tv_sale_date);
            Button btnView = (Button) convertView.findViewById(R.id.btn_view_bill_detail);

            tvBillNo.setText("Bill #" + m.billNo);
            tvBadge.setText(m.paymentMode);
            tvAmt.setText(String.format("₹ %.2f", m.grandTotal));
            tvDate.setText(m.date);

            btnView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(CustomerDetailActivity.this, SaleBillDetailActivity.class);
                    intent.putExtra("sale_id", m.id);
                    startActivity(intent);
                }
            });
            return convertView;
        }
    }
}