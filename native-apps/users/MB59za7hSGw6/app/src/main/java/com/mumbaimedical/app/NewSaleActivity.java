package com.mumbaimedical.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NewSaleActivity extends Activity {

    private static final int REQ_CAPTURE_CAMERA = 1005;
    private static final int REQ_PICK_GALLERY = 1006;

    private DatabaseHelper dbHelper;
    private Spinner spMedicines, spCustomerSelect, spPaymentType;
    private EditText etSaleQty, etDiscount;
    private ImageView ivSaleAttachment;
    private TextView tvSalePhotoStatus, tvSubtotal, tvGrandTotal;
    private Button btnAddCart, btnCompleteSale, btnAttachSalePhoto;
    private ListView lvCart;

    private List<Long> customerIds = new ArrayList<>();
    private List<String> customerNames = new ArrayList<>();

    private List<Long> medicineIds = new ArrayList<>();
    private List<String> medicineNames = new ArrayList<>();
    private List<Double> medicinePrices = new ArrayList<>();
    private List<Integer> medicineStocks = new ArrayList<>();

    private List<Map<String, String>> cartItems = new ArrayList<>();
    private List<CartModel> cartModels = new ArrayList<>();

    private SimpleAdapter cartAdapter;
    private double subtotal = 0.0;
    private double grandTotal = 0.0;
    private String salePhotoPath = null;

    private static class CartModel {
        long medicineId;
        String name;
        int qty;
        double price;
        double total;

        CartModel(long id, String name, int qty, double price, double total) {
            this.medicineId = id;
            this.name = name;
            this.qty = qty;
            this.price = price;
            this.total = total;
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_sale);

        dbHelper = new DatabaseHelper(this);

        spCustomerSelect = findViewById(R.id.spCustomerSelect);
        spPaymentType = findViewById(R.id.spPaymentType);
        spMedicines = findViewById(R.id.spMedicines);
        etSaleQty = findViewById(R.id.etSaleQty);
        etDiscount = findViewById(R.id.etDiscount);
        ivSaleAttachment = findViewById(R.id.ivSaleAttachment);
        tvSalePhotoStatus = findViewById(R.id.tvSalePhotoStatus);
        btnAttachSalePhoto = findViewById(R.id.btnAttachSalePhoto);
        btnAddCart = findViewById(R.id.btnAddCart);
        btnCompleteSale = findViewById(R.id.btnCompleteSale);
        lvCart = findViewById(R.id.lvCart);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvGrandTotal = findViewById(R.id.tvGrandTotal);

        cartAdapter = new SimpleAdapter(
                this,
                cartItems,
                R.layout.item_cart,
                new String[]{"name", "qty", "total"},
                new int[]{R.id.tvCartName, R.id.tvCartQty, R.id.tvCartTotal}
        );
        lvCart.setAdapter(cartAdapter);

        setupPaymentTypeSpinner();
        loadCustomerSpinner();
        loadMedicineSpinner();

        btnAttachSalePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PhotoHelper.showPhotoOptionsDialog(NewSaleActivity.this, salePhotoPath != null, new PhotoHelper.PhotoPickCallback() {
                    @Override
                    public void onOptionSelected(int option) {
                        if (option == 1) {
                            PhotoHelper.disableStrictModeVmPolicy();
                            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            startActivityForResult(intent, REQ_CAPTURE_CAMERA);
                        } else if (option == 2) {
                            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            startActivityForResult(intent, REQ_PICK_GALLERY);
                        } else if (option == 3) {
                            salePhotoPath = null;
                            ivSaleAttachment.setVisibility(View.GONE);
                            tvSalePhotoStatus.setText("Optional Bill / Rx Attachment");
                            btnAttachSalePhoto.setText("+ Photo");
                            Toast.makeText(NewSaleActivity.this, R.string.photo_removed, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        ivSaleAttachment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (salePhotoPath != null) {
                    PhotoHelper.showFullScreenImage(NewSaleActivity.this, salePhotoPath);
                }
            }
        });

        btnAddCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addToCart();
            }
        });

        etDiscount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                recalculateTotals();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnCompleteSale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                completeSale();
            }
        });
    }

    private void setupPaymentTypeSpinner() {
        String[] paymentModes = new String[]{"Cash / UPI", "Udhar (Credit)"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, paymentModes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPaymentType.setAdapter(adapter);
    }

    private void loadCustomerSpinner() {
        Cursor cursor = dbHelper.searchCustomers("");
        customerIds.clear();
        customerNames.clear();

        customerIds.add(-1L);
        customerNames.add("Walk-in Customer (Cash)");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                customerIds.add(cursor.getLong(cursor.getColumnIndexOrThrow("_id")));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
                customerNames.add(name + (phone != null && !phone.isEmpty() ? " (" + phone + ")" : ""));
            }
            cursor.close();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, customerNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCustomerSelect.setAdapter(adapter);
    }

    private void loadMedicineSpinner() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, name, selling_price, quantity FROM " + DatabaseHelper.TABLE_MEDICINES + " WHERE quantity > 0 ORDER BY name ASC", null);

        medicineIds.clear();
        medicineNames.clear();
        medicinePrices.clear();
        medicineStocks.clear();

        while (cursor.moveToNext()) {
            long id = cursor.getLong(0);
            String name = cursor.getString(1);
            double price = cursor.getDouble(2);
            int stock = cursor.getInt(3);

            medicineIds.add(id);
            medicineNames.add(name + " (₹" + price + " | Stock: " + stock + ")");
            medicinePrices.add(price);
            medicineStocks.add(stock);
        }
        cursor.close();

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, medicineNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMedicines.setAdapter(spinnerAdapter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PhotoHelper.REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                PhotoHelper.disableStrictModeVmPolicy();
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, REQ_CAPTURE_CAMERA);
            } else {
                Toast.makeText(this, R.string.camera_permission_denied_msg, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQ_CAPTURE_CAMERA && data != null && data.getExtras() != null) {
                Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                if (bitmap != null) {
                    salePhotoPath = PhotoHelper.saveBitmapImage(this, bitmap, "sales");
                }
            } else if (requestCode == REQ_PICK_GALLERY && data != null && data.getData() != null) {
                Uri selectedImageUri = data.getData();
                salePhotoPath = PhotoHelper.saveImageFromUri(this, selectedImageUri, "sales");
            }

            if (salePhotoPath != null) {
                PhotoHelper.displayThumbnail(this, ivSaleAttachment, salePhotoPath, 100, 100);
                tvSalePhotoStatus.setText(getString(R.string.tap_to_view_full));
                btnAttachSalePhoto.setText(getString(R.string.btn_change_photo));
            }
        }
    }

    private void addToCart() {
        int selectedIndex = spMedicines.getSelectedItemPosition();
        if (selectedIndex < 0 || selectedIndex >= medicineIds.size()) {
            Toast.makeText(this, "Select a medicine", Toast.LENGTH_SHORT).show();
            return;
        }

        String qtyStr = etSaleQty.getText().toString().trim();
        if (qtyStr.isEmpty()) {
            Toast.makeText(this, "Enter quantity", Toast.LENGTH_SHORT).show();
            return;
        }

        int qty = Integer.parseInt(qtyStr);
        int availableStock = medicineStocks.get(selectedIndex);

        if (qty <= 0) {
            Toast.makeText(this, "Invalid quantity", Toast.LENGTH_SHORT).show();
            return;
        }

        if (qty > availableStock) {
            Toast.makeText(this, "Cannot sell more than available stock (" + availableStock + ")", Toast.LENGTH_SHORT).show();
            return;
        }

        long medId = medicineIds.get(selectedIndex);
        String rawName = medicineNames.get(selectedIndex).split(" \\(")[0];
        double price = medicinePrices.get(selectedIndex);
        double itemTotal = price * qty;

        cartModels.add(new CartModel(medId, rawName, qty, price, itemTotal));

        Map<String, String> map = new HashMap<>();
        map.put("name", rawName);
        map.put("qty", "x" + qty);
        map.put("total", String.format(Locale.getDefault(), "₹ %.2f", itemTotal));
        cartItems.add(map);

        cartAdapter.notifyDataSetChanged();
        etSaleQty.setText("");

        recalculateTotals();
    }

    private void recalculateTotals() {
        subtotal = 0.0;
        for (CartModel item : cartModels) {
            subtotal += item.total;
        }

        String discountStr = etDiscount.getText().toString().trim();
        double discount = discountStr.isEmpty() ? 0.0 : Double.parseDouble(discountStr);

        grandTotal = subtotal - discount;
        if (grandTotal < 0) grandTotal = 0.0;

        tvSubtotal.setText(String.format(Locale.getDefault(), "₹ %.2f", subtotal));
        tvGrandTotal.setText(String.format(Locale.getDefault(), "₹ %.2f", grandTotal));
    }

    private void completeSale() {
        if (cartModels.isEmpty()) {
            Toast.makeText(this, "Cart is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        int custPos = spCustomerSelect.getSelectedItemPosition();
        long selectedCustomerId = (custPos >= 0 && custPos < customerIds.size()) ? customerIds.get(custPos) : -1L;
        String customerDisplayName = (custPos >= 0 && custPos < customerNames.size()) ? customerNames.get(custPos) : "Walk-in Customer";

        String paymentMode = spPaymentType.getSelectedItem().toString();
        boolean isCreditSale = paymentMode.contains("Udhar");

        if (isCreditSale && selectedCustomerId == -1L) {
            Toast.makeText(this, "Please select a registered customer to record a Credit (Udhar) sale", Toast.LENGTH_LONG).show();
            return;
        }

        String discountStr = etDiscount.getText().toString().trim();
        double discount = discountStr.isEmpty() ? 0.0 : Double.parseDouble(discountStr);
        String saleDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues saleCv = new ContentValues();
            saleCv.put("sale_date", saleDate);
            saleCv.put("customer_id", selectedCustomerId);
            saleCv.put("customer_name", customerDisplayName);
            saleCv.put("total_amount", subtotal);
            saleCv.put("discount", discount);
            saleCv.put("grand_total", grandTotal);
            saleCv.put("payment_method", paymentMode);
            saleCv.put("image_path", salePhotoPath);

            long saleId = db.insert(DatabaseHelper.TABLE_SALES, null, saleCv);

            for (CartModel item : cartModels) {
                ContentValues itemCv = new ContentValues();
                itemCv.put("sale_id", saleId);
                itemCv.put("medicine_id", item.medicineId);
                itemCv.put("medicine_name", item.name);
                itemCv.put("quantity", item.qty);
                itemCv.put("price", item.price);
                itemCv.put("total", item.total);

                db.insert(DatabaseHelper.TABLE_SALE_ITEMS, null, itemCv);

                db.execSQL("UPDATE " + DatabaseHelper.TABLE_MEDICINES + " SET quantity = quantity - " + item.qty + " WHERE id = " + item.medicineId);
            }

            if (isCreditSale && selectedCustomerId > 0) {
                dbHelper.addKhataTransaction(
                        selectedCustomerId,
                        "UDHAR",
                        grandTotal,
                        saleDate,
                        "Credit Sale Bill #" + saleId,
                        "Credit",
                        "Bill #" + saleId
                );
            }

            db.setTransactionSuccessful();

            String msg = "Bill generated for " + customerDisplayName + "\nTotal: ₹ " + grandTotal;
            if (isCreditSale) {
                msg += "\n(Added to Customer Udhar Khata Ledger)";
            }

            new AlertDialog.Builder(this)
                    .setTitle("Sale Completed!")
                    .setMessage(msg)
                    .setPositiveButton("OK", null)
                    .show();

            cartModels.clear();
            cartItems.clear();
            cartAdapter.notifyDataSetChanged();
            recalculateTotals();
            loadMedicineSpinner();
            salePhotoPath = null;
            ivSaleAttachment.setVisibility(View.GONE);
            tvSalePhotoStatus.setText("Optional Bill / Rx Attachment");
            btnAttachSalePhoto.setText("+ Photo");

        } finally {
            db.endTransaction();
        }
    }
}