package com.boxstockmanager.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("deprecation")
public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CAMERA = 101;
    private static final int REQUEST_PICK_IMAGE = 102;

    private DatabaseHelper dbHelper;
    private MultiFormatReader multiFormatReader;

    private View viewDashboard;
    private View viewProducts;
    private View viewStockIn;
    private View viewStockOut;
    private View viewStockHistory;

    private Button navHome, navProducts, navStockIn, navStockOut, navHistory;
    private Button btnTopScan, btnTopUpload;

    private TextView tvDashTotalProducts, tvDashTotalQty, tvDashStockInToday, tvDashStockOutToday, tvDashLowStock, tvDashOutOfStock;
    private ListView lvDashRecentBox;
    private BoxAdapter boxAdapter;
    private List<BoxItem> legacyBoxList = new ArrayList<>();

    private EditText etProdSearch;
    private Spinner spProdCategory, spProdStatus;
    private ListView lvProducts;
    private ProductAdapter productAdapter;
    private List<Product> productList = new ArrayList<>();
    private AdapterView.OnItemSelectedListener spinListener;
    private boolean isUpdatingSpinner = false;

    private Spinner spStockInProd, spStockOutProd;
    private List<Product> spinnerProdList = new ArrayList<>();

    private RadioGroup rgHistoryTab;
    private RadioButton rbHistoryAll, rbHistoryIn, rbHistoryOut, rbHistoryScanLog;
    private View containerTransactions, containerScanLog;
    private EditText etHistorySearch, etScanLogSearch;
    private ListView lvTransactions, lvScanLogs;
    private TransactionAdapter transactionAdapter;
    private List<StockTransaction> transactionList = new ArrayList<>();
    private ScanLogAdapter scanLogAdapter;
    private List<ScanLogItem> scanLogList = new ArrayList<>();
    private TextView tvScanLogHeaderCount;
    private Button btnClearScanLog;

    private String scannerTargetMode = "GENERAL";

    private Dialog scannerDialog;
    private SurfaceView dialogSurfaceView;
    private SurfaceHolder dialogSurfaceHolder;
    private Camera mCamera;
    private boolean isFlashOn = false;
    private boolean isFrameProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        initZXingReader();
        OcrScannerHelper.initTesseractIfNeeded(this);

        initViews();
        setupNavigation();
        setupDashboardControllers();
        setupProductControllers();
        setupStockControllers();
        setupHistoryControllers();

        checkPermissions();
        switchTab("HOME");
    }

    private void initZXingReader() {
        multiFormatReader = new MultiFormatReader();
        multiFormatReader.setHints(getUniversalDecodeHints());
    }

    private Map<DecodeHintType, Object> getUniversalDecodeHints() {
        Map<DecodeHintType, Object> hints = new HashMap<>();
        List<BarcodeFormat> formats = new ArrayList<>();

        formats.add(BarcodeFormat.QR_CODE);
        formats.add(BarcodeFormat.EAN_13);
        formats.add(BarcodeFormat.EAN_8);
        formats.add(BarcodeFormat.UPC_A);
        formats.add(BarcodeFormat.UPC_E);
        formats.add(BarcodeFormat.CODE_128);
        formats.add(BarcodeFormat.CODE_39);
        formats.add(BarcodeFormat.ITF);
        formats.add(BarcodeFormat.CODABAR);
        formats.add(BarcodeFormat.DATA_MATRIX);
        formats.add(BarcodeFormat.PDF_417);
        formats.add(BarcodeFormat.AZTEC);

        hints.put(DecodeHintType.POSSIBLE_FORMATS, formats);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        return hints;
    }

    private void initViews() {
        viewDashboard = findViewById(R.id.view_dashboard);
        viewProducts = findViewById(R.id.view_products);
        viewStockIn = findViewById(R.id.view_stock_in);
        viewStockOut = findViewById(R.id.view_stock_out);
        viewStockHistory = findViewById(R.id.view_stock_history);

        navHome = (Button) findViewById(R.id.nav_home);
        navProducts = (Button) findViewById(R.id.nav_products);
        navStockIn = (Button) findViewById(R.id.nav_stock_in);
        navStockOut = (Button) findViewById(R.id.nav_stock_out);
        navHistory = (Button) findViewById(R.id.nav_history);

        btnTopScan = (Button) findViewById(R.id.btn_top_scan);
        btnTopUpload = (Button) findViewById(R.id.btn_top_upload);

        btnTopScan.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openScannerOverlay("GENERAL"); }
        });

        btnTopUpload.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openGalleryPicker("GENERAL"); }
        });
    }

    private void setupNavigation() {
        View.OnClickListener navClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();
                if (id == R.id.nav_home) switchTab("HOME");
                else if (id == R.id.nav_products) switchTab("PRODUCTS");
                else if (id == R.id.nav_stock_in) switchTab("STOCK_IN");
                else if (id == R.id.nav_stock_out) switchTab("STOCK_OUT");
                else if (id == R.id.nav_history) switchTab("HISTORY");
            }
        };

        navHome.setOnClickListener(navClick);
        navProducts.setOnClickListener(navClick);
        navStockIn.setOnClickListener(navClick);
        navStockOut.setOnClickListener(navClick);
        navHistory.setOnClickListener(navClick);
    }

    private void switchTab(String tabKey) {
        viewDashboard.setVisibility(View.GONE);
        viewProducts.setVisibility(View.GONE);
        viewStockIn.setVisibility(View.GONE);
        viewStockOut.setVisibility(View.GONE);
        viewStockHistory.setVisibility(View.GONE);

        navHome.setTextColor(0xFF616161);
        navProducts.setTextColor(0xFF616161);
        navStockIn.setTextColor(0xFF616161);
        navStockOut.setTextColor(0xFF616161);
        navHistory.setTextColor(0xFF616161);

        if ("HOME".equals(tabKey)) {
            viewDashboard.setVisibility(View.VISIBLE);
            navHome.setTextColor(0xFF1E88E5);
            refreshDashboard();
        } else if ("PRODUCTS".equals(tabKey)) {
            viewProducts.setVisibility(View.VISIBLE);
            navProducts.setTextColor(0xFF1E88E5);
            updateCategorySpinner();
            refreshProductsList();
        } else if ("STOCK_IN".equals(tabKey)) {
            viewStockIn.setVisibility(View.VISIBLE);
            navStockIn.setTextColor(0xFF2E7D32);
            populateStockProductSpinners();
        } else if ("STOCK_OUT".equals(tabKey)) {
            viewStockOut.setVisibility(View.VISIBLE);
            navStockOut.setTextColor(0xFFC62828);
            populateStockProductSpinners();
        } else if ("HISTORY".equals(tabKey)) {
            viewStockHistory.setVisibility(View.VISIBLE);
            navHistory.setTextColor(0xFF1565C0);
            refreshHistoryViews();
        }
    }

    private void setupDashboardControllers() {
        tvDashTotalProducts = (TextView) findViewById(R.id.tv_dash_total_products);
        tvDashTotalQty = (TextView) findViewById(R.id.tv_dash_total_qty);
        tvDashStockInToday = (TextView) findViewById(R.id.tv_dash_stock_in_today);
        tvDashStockOutToday = (TextView) findViewById(R.id.tv_dash_stock_out_today);
        tvDashLowStock = (TextView) findViewById(R.id.tv_dash_low_stock);
        tvDashOutOfStock = (TextView) findViewById(R.id.tv_dash_out_of_stock);
        lvDashRecentBox = (ListView) findViewById(R.id.lv_dash_recent_box);

        boxAdapter = new BoxAdapter(this, legacyBoxList);
        lvDashRecentBox.setAdapter(boxAdapter);

        findViewById(R.id.btn_dash_scan_now).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openScannerOverlay("GENERAL"); }
        });

        findViewById(R.id.btn_dash_upload_image).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openGalleryPicker("GENERAL"); }
        });
    }

    private void refreshDashboard() {
        tvDashTotalProducts.setText(String.valueOf(dbHelper.getTotalProductsCount()));
        tvDashTotalQty.setText(dbHelper.getTotalStockQuantity() + " Pcs");
        tvDashStockInToday.setText("+" + dbHelper.getStockInToday());
        tvDashStockOutToday.setText("-" + dbHelper.getStockOutToday());
        tvDashLowStock.setText(String.valueOf(dbHelper.getLowStockProductsCount()));
        tvDashOutOfStock.setText(String.valueOf(dbHelper.getOutOfStockProductsCount()));

        legacyBoxList.clear();
        legacyBoxList.addAll(dbHelper.getAllInventory());
        boxAdapter.notifyDataSetChanged();
    }

    private void setupProductControllers() {
        etProdSearch = (EditText) findViewById(R.id.et_prod_search);
        spProdCategory = (Spinner) findViewById(R.id.sp_prod_category);
        spProdStatus = (Spinner) findViewById(R.id.sp_prod_status);
        lvProducts = (ListView) findViewById(R.id.lv_products);

        productAdapter = new ProductAdapter(this, productList);
        lvProducts.setAdapter(productAdapter);

        findViewById(R.id.btn_add_new_product).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openProductFormDialog(null, null); }
        });

        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"All Status", "In Stock", "Low Stock", "Out of Stock"});
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spProdStatus.setAdapter(statusAdapter);

        etProdSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { refreshProductsList(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        spinListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isUpdatingSpinner) {
                    refreshProductsList();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };

        spProdCategory.setOnItemSelectedListener(spinListener);
        spProdStatus.setOnItemSelectedListener(spinListener);
    }

    private void updateCategorySpinner() {
        isUpdatingSpinner = true;
        List<String> categories = dbHelper.getUniqueCategories();
        String currentCat = spProdCategory.getSelectedItem() != null ? spProdCategory.getSelectedItem().toString() : "All Categories";

        spProdCategory.setOnItemSelectedListener(null);
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spProdCategory.setAdapter(catAdapter);

        int pos = categories.indexOf(currentCat);
        if (pos >= 0) {
            spProdCategory.setSelection(pos);
        } else {
            spProdCategory.setSelection(0);
        }
        spProdCategory.setOnItemSelectedListener(spinListener);
        isUpdatingSpinner = false;
    }

    private void refreshProductsList() {
        String q = etProdSearch.getText().toString();
        String cat = spProdCategory.getSelectedItem() != null ? spProdCategory.getSelectedItem().toString() : "All Categories";
        String status = spProdStatus.getSelectedItem() != null ? spProdStatus.getSelectedItem().toString() : "All Status";

        productList.clear();
        productList.addAll(dbHelper.getAllActiveProducts(q, cat, status, "Name"));
        productAdapter.notifyDataSetChanged();
    }

    private void setupStockControllers() {
        spStockInProd = (Spinner) findViewById(R.id.sp_stock_in_product);
        spStockOutProd = (Spinner) findViewById(R.id.sp_stock_out_product);

        final EditText etStockInQty = (EditText) findViewById(R.id.et_stock_in_qty);
        final EditText etStockInSupplier = (EditText) findViewById(R.id.et_stock_in_supplier);
        final EditText etStockInNotes = (EditText) findViewById(R.id.et_stock_in_notes);

        final EditText etStockOutQty = (EditText) findViewById(R.id.et_stock_out_qty);
        final EditText etStockOutNotes = (EditText) findViewById(R.id.et_stock_out_notes);

        findViewById(R.id.btn_stock_in_scan).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openScannerOverlay("STOCK_IN"); }
        });
        findViewById(R.id.btn_stock_in_upload).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openGalleryPicker("STOCK_IN"); }
        });

        findViewById(R.id.btn_stock_out_scan).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openScannerOverlay("STOCK_OUT"); }
        });
        findViewById(R.id.btn_stock_out_upload).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openGalleryPicker("STOCK_OUT"); }
        });

        findViewById(R.id.btn_submit_stock_in).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (spStockInProd.getSelectedItemPosition() < 0 || spinnerProdList.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please select a valid product first.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Product p = spinnerProdList.get(spStockInProd.getSelectedItemPosition());
                String qtyStr = etStockInQty.getText().toString().trim();
                if (qtyStr.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter a valid stock quantity.", Toast.LENGTH_SHORT).show();
                    return;
                }
                int qty = Integer.parseInt(qtyStr);
                String notes = etStockInNotes.getText().toString().trim();
                String supplier = etStockInSupplier.getText().toString().trim();
                if (!supplier.isEmpty()) notes = "Supplier: " + supplier + " | " + notes;

                if (dbHelper.recordStockMovement(p.getId(), "IN", qty, notes)) {
                    Toast.makeText(MainActivity.this, "Stock Added Successfully", Toast.LENGTH_SHORT).show();
                    etStockInQty.setText("");
                    etStockInNotes.setText("");
                    etStockInSupplier.setText("");
                    populateStockProductSpinners();
                    refreshDashboard();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to update stock quantity.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.btn_submit_stock_out).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (spStockOutProd.getSelectedItemPosition() < 0 || spinnerProdList.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please select a valid product first.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Product p = spinnerProdList.get(spStockOutProd.getSelectedItemPosition());
                String qtyStr = etStockOutQty.getText().toString().trim();
                if (qtyStr.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter quantity to deduct.", Toast.LENGTH_SHORT).show();
                    return;
                }
                int qty = Integer.parseInt(qtyStr);
                if (qty > p.getStockQuantity()) {
                    Toast.makeText(MainActivity.this, "Insufficient stock! Available: " + p.getStockQuantity(), Toast.LENGTH_LONG).show();
                    return;
                }

                String notes = etStockOutNotes.getText().toString().trim();
                if (dbHelper.recordStockMovement(p.getId(), "OUT", qty, notes)) {
                    Toast.makeText(MainActivity.this, "Stock Removed Successfully", Toast.LENGTH_SHORT).show();
                    etStockOutQty.setText("");
                    etStockOutNotes.setText("");
                    populateStockProductSpinners();
                    refreshDashboard();
                } else {
                    Toast.makeText(MainActivity.this, "Stock Out Failed. Check current stock level.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void populateStockProductSpinners() {
        spinnerProdList = dbHelper.getAllActiveProducts("", "All Categories", "All Status", "Name");
        List<String> names = new ArrayList<>();
        for (Product p : spinnerProdList) {
            names.add(p.getName() + " (Stock: " + p.getStockQuantity() + ")");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spStockInProd.setAdapter(adapter);
        spStockOutProd.setAdapter(adapter);
    }

    private void setupHistoryControllers() {
        rgHistoryTab = (RadioGroup) findViewById(R.id.rg_history_tab);
        rbHistoryAll = (RadioButton) findViewById(R.id.rb_history_all);
        rbHistoryIn = (RadioButton) findViewById(R.id.rb_history_in);
        rbHistoryOut = (RadioButton) findViewById(R.id.rb_history_out);
        rbHistoryScanLog = (RadioButton) findViewById(R.id.rb_history_scan_log);

        containerTransactions = findViewById(R.id.container_transactions);
        containerScanLog = findViewById(R.id.container_scan_log);

        etHistorySearch = (EditText) findViewById(R.id.et_history_search);
        lvTransactions = (ListView) findViewById(R.id.lv_transactions);

        etScanLogSearch = (EditText) findViewById(R.id.et_scan_log_search);
        lvScanLogs = (ListView) findViewById(R.id.lv_scan_logs);
        tvScanLogHeaderCount = (TextView) findViewById(R.id.tv_scan_log_header_count);
        btnClearScanLog = (Button) findViewById(R.id.btn_clear_scan_log);

        transactionAdapter = new TransactionAdapter(this, transactionList);
        lvTransactions.setAdapter(transactionAdapter);

        scanLogAdapter = new ScanLogAdapter(this, scanLogList);
        lvScanLogs.setAdapter(scanLogAdapter);

        rgHistoryTab.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(RadioGroup group, int checkedId) { refreshHistoryViews(); }
        });

        etHistorySearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { refreshHistoryList(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        etScanLogSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { refreshScanLogList(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnClearScanLog.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { confirmClearScanLogs(); }
        });
    }

    private void refreshHistoryViews() {
        if (rbHistoryScanLog.isChecked()) {
            containerTransactions.setVisibility(View.GONE);
            containerScanLog.setVisibility(View.VISIBLE);
            refreshScanLogList();
        } else {
            containerScanLog.setVisibility(View.GONE);
            containerTransactions.setVisibility(View.VISIBLE);
            refreshHistoryList();
        }
    }

    private void refreshHistoryList() {
        String filter = "ALL";
        if (rbHistoryIn.isChecked()) filter = "IN";
        else if (rbHistoryOut.isChecked()) filter = "OUT";

        String search = etHistorySearch.getText().toString();
        transactionList.clear();
        transactionList.addAll(dbHelper.getTransactions(filter, search, ""));
        transactionAdapter.notifyDataSetChanged();
    }

    private void refreshScanLogList() {
        String search = etScanLogSearch.getText().toString();
        scanLogList.clear();
        scanLogList.addAll(dbHelper.getAllScanLogs(search));
        scanLogAdapter.notifyDataSetChanged();
        tvScanLogHeaderCount.setText("Scan Text Log (" + scanLogList.size() + " entries)");
    }

    private void confirmClearScanLogs() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Scan Log")
                .setMessage("Are you sure you want to clear all saved scan log entries?")
                .setPositiveButton("Clear All", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dbHelper.clearAllScanLogs();
                        Toast.makeText(MainActivity.this, "Scan log cleared.", Toast.LENGTH_SHORT).show();
                        refreshScanLogList();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openScannerOverlay(String targetMode) {
        this.scannerTargetMode = targetMode;

        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
            return;
        }

        scannerDialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        scannerDialog.setContentView(R.layout.dialog_scanner);
        scannerDialog.setCancelable(true);

        dialogSurfaceView = (SurfaceView) scannerDialog.findViewById(R.id.dialog_surface_camera);
        final Button btnFlash = (Button) scannerDialog.findViewById(R.id.btn_scanner_flash);
        Button btnClose = (Button) scannerDialog.findViewById(R.id.btn_scanner_close);

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { closeScannerDialog(); }
        });

        btnFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCamera != null) {
                    try {
                        Camera.Parameters p = mCamera.getParameters();
                        if (isFlashOn) {
                            p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                            btnFlash.setText("Flash OFF");
                            isFlashOn = false;
                        } else {
                            p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                            btnFlash.setText("Flash ON");
                            isFlashOn = true;
                        }
                        mCamera.setParameters(p);
                    } catch (Exception ignored) {}
                }
            }
        });

        dialogSurfaceHolder = dialogSurfaceView.getHolder();
        dialogSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                startCameraHardware(holder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                if (mCamera != null) {
                    try {
                        mCamera.stopPreview();
                        mCamera.setPreviewDisplay(holder);
                        mCamera.startPreview();
                    } catch (Exception ignored) {}
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                stopCameraHardware();
            }
        });

        scannerDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override public void onDismiss(DialogInterface dialog) { stopCameraHardware(); }
        });

        scannerDialog.show();
    }

    private void startCameraHardware(SurfaceHolder holder) {
        try {
            mCamera = Camera.open();
            if (mCamera != null) {
                Camera.Parameters params = mCamera.getParameters();

                List<String> focusModes = params.getSupportedFocusModes();
                if (focusModes != null) {
                    if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                    } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                    } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    }
                }

                mCamera.setParameters(params);
                mCamera.setDisplayOrientation(90);
                mCamera.setPreviewDisplay(holder);
                mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(final byte[] data, final Camera camera) {
                        if (isFrameProcessing || data == null) return;
                        isFrameProcessing = true;

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Camera.Parameters p = camera.getParameters();
                                    Camera.Size size = p.getPreviewSize();

                                    Bitmap bmpFrame = convertYuvToBitmap(data, size.width, size.height);
                                    if (bmpFrame != null) {
                                        final ProductScannerResult scanRes = OcrScannerHelper.processBitmapUnified(
                                                MainActivity.this, bmpFrame, multiFormatReader, getUniversalDecodeHints()
                                        );
                                        bmpFrame.recycle();

                                        if (scanRes != null && scanRes.hasAnyData()) {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    closeScannerDialog();
                                                    triggerVibration();
                                                    if (scanRes.getRawDecodedText().isEmpty()) {
                                                        Toast.makeText(MainActivity.this, "No QR Found - Text Extracted via OCR", Toast.LENGTH_SHORT).show();
                                                    } else {
                                                        Toast.makeText(MainActivity.this, "QR Code Scanned Successfully", Toast.LENGTH_SHORT).show();
                                                    }
                                                    handleUnifiedScanResult(scanRes, "Camera");
                                                }
                                            });
                                        }
                                    }
                                } catch (Exception ignored) {
                                } finally {
                                    isFrameProcessing = false;
                                }
                            }
                        }).start();
                    }
                });
                mCamera.startPreview();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open camera hardware", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap convertYuvToBitmap(byte[] data, int width, int height) {
        try {
            int[] pixels = new int[width * height];
            int frameSize = width * height;
            for (int j = 0, yp = 0; j < height; j++) {
                int u = 0, v = 0;
                for (int i = 0; i < width; i++, yp++) {
                    int y = (0xff & ((int) data[yp])) - 16;
                    if (y < 0) y = 0;
                    if ((i & 1) == 0) {
                        v = (0xff & data[frameSize + (j >> 1) * width + i]) - 128;
                        u = (0xff & data[frameSize + (j >> 1) * width + i + 1]) - 128;
                    }
                    int y1192 = 1192 * y;
                    int r = (y1192 + 1634 * v);
                    int g = (y1192 - 833 * v - 400 * u);
                    int b = (y1192 + 2066 * u);

                    if (r < 0) r = 0; else if (r > 262143) r = 262143;
                    if (g < 0) g = 0; else if (g > 262143) g = 262143;
                    if (b < 0) b = 0; else if (b > 262143) b = 262143;

                    pixels[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
                }
            }
            Bitmap bmp = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            return Bitmap.createBitmap(bmp, 0, 0, width, height, matrix, true);
        } catch (Exception e) {
            return null;
        }
    }

    private void stopCameraHardware() {
        if (mCamera != null) {
            try {
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                mCamera.release();
            } catch (Exception ignored) {}
            mCamera = null;
        }
        isFlashOn = false;
    }

    private void closeScannerDialog() {
        stopCameraHardware();
        if (scannerDialog != null && scannerDialog.isShowing()) {
            scannerDialog.dismiss();
        }
    }

    private void openGalleryPicker(String targetMode) {
        this.scannerTargetMode = targetMode;
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) decodeImageFromUri(uri);
        }
    }

    private void decodeImageFromUri(final Uri uri) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream is = null;
                try {
                    is = getContentResolver().openInputStream(uri);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeStream(is, null, options);
                    if (is != null) is.close();

                    int srcWidth = options.outWidth;
                    int srcHeight = options.outHeight;

                    if (srcWidth <= 0 || srcHeight <= 0) {
                        showToastOnUiThread("Invalid or unreadable image file.");
                        return;
                    }

                    int inSampleSize = 1;
                    while ((srcWidth / inSampleSize) > 2000 || (srcHeight / inSampleSize) > 2000) {
                        inSampleSize *= 2;
                    }

                    options.inJustDecodeBounds = false;
                    options.inSampleSize = inSampleSize;
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;

                    is = getContentResolver().openInputStream(uri);
                    Bitmap fullBitmap = BitmapFactory.decodeStream(is, null, options);
                    if (is != null) is.close();

                    if (fullBitmap == null) {
                        showToastOnUiThread("Unable to load image from gallery.");
                        return;
                    }

                    final ProductScannerResult res = OcrScannerHelper.processBitmapUnified(
                            MainActivity.this, fullBitmap, multiFormatReader, getUniversalDecodeHints()
                    );
                    fullBitmap.recycle();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (res != null && res.hasAnyData()) {
                                triggerVibration();
                                if (res.getRawDecodedText().isEmpty()) {
                                    Toast.makeText(MainActivity.this, "No QR Found - Text Extracted via OCR", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(MainActivity.this, "QR Code Scanned Successfully", Toast.LENGTH_SHORT).show();
                                }
                                handleUnifiedScanResult(res, "Gallery");
                            } else {
                                Toast.makeText(MainActivity.this, "No readable text found", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Gallery decode error: " + e.getMessage(), e);
                    showToastOnUiThread("No readable text found");
                } finally {
                    if (is != null) {
                        try { is.close(); } catch (Exception ignored) {}
                    }
                }
            }
        }).start();
    }

    private void showToastOnUiThread(final String msg) {
        runOnUiThread(new Runnable() {
            @Override public void run() { Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show(); }
        });
    }

    private void handleUnifiedScanResult(ProductScannerResult scanResult, String source) {
        if (scanResult == null) return;

        String codeToSearch = !scanResult.getProductCode().isEmpty() ? scanResult.getProductCode() :
                              (!scanResult.getImei1().isEmpty() ? scanResult.getImei1() : scanResult.getBarcode());

        String timeStr = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date());

        String textToLog = scanResult.getRawDecodedText();
        if (textToLog == null || textToLog.trim().isEmpty()) {
            textToLog = scanResult.getRawOcrText();
            if (textToLog != null && !textToLog.trim().isEmpty()) {
                textToLog = "[No QR Found] OCR Text: " + textToLog;
            }
        }
        if (textToLog == null || textToLog.trim().isEmpty()) {
            if (!codeToSearch.isEmpty()) {
                textToLog = "[No QR Found] Code detected: " + codeToSearch;
            } else {
                textToLog = "No readable text found";
            }
        }

        dbHelper.insertScanLog(timeStr, source, codeToSearch, textToLog);
        refreshScanLogList();

        if (!codeToSearch.isEmpty()) {
            Product existing = dbHelper.findProductByCodeOrImei(codeToSearch);
            if (existing != null) {
                if ("STOCK_IN".equals(scannerTargetMode)) {
                    switchTab("STOCK_IN");
                    selectProductInSpinner(spStockInProd, existing.getId());
                } else if ("STOCK_OUT".equals(scannerTargetMode)) {
                    switchTab("STOCK_OUT");
                    selectProductInSpinner(spStockOutProd, existing.getId());
                } else {
                    openProductDetailDialog(existing);
                }
                dbHelper.updateStock(codeToSearch, true);
                refreshDashboard();
                return;
            }
        }

        openProductFormDialog(scanResult, codeToSearch);
    }

    private void selectProductInSpinner(Spinner spinner, long prodId) {
        for (int i = 0; i < spinnerProdList.size(); i++) {
            if (spinnerProdList.get(i).getId() == prodId) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void openProductFormDialog(final Object scanOrProductObj, String prefillCode) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_product_form);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        final Product existingProduct = (scanOrProductObj instanceof Product) ? (Product) scanOrProductObj : null;
        final ProductScannerResult scanRes = (scanOrProductObj instanceof ProductScannerResult) ? (ProductScannerResult) scanOrProductObj : null;

        final boolean isEditMode = (existingProduct != null && existingProduct.getId() > 0);

        TextView tvTitle = (TextView) dialog.findViewById(R.id.tv_form_title);
        final EditText etName = (EditText) dialog.findViewById(R.id.et_f_name);
        final EditText etBrand = (EditText) dialog.findViewById(R.id.et_f_brand);
        final EditText etModel = (EditText) dialog.findViewById(R.id.et_f_model);
        final EditText etCode = (EditText) dialog.findViewById(R.id.et_f_code);
        final EditText etBarcode = (EditText) dialog.findViewById(R.id.et_f_barcode);
        final EditText etImei1 = (EditText) dialog.findViewById(R.id.et_f_imei1);
        final EditText etImei2 = (EditText) dialog.findViewById(R.id.et_f_imei2);
        final EditText etCategory = (EditText) dialog.findViewById(R.id.et_f_category);
        final EditText etColor = (EditText) dialog.findViewById(R.id.et_f_color);
        final EditText etRam = (EditText) dialog.findViewById(R.id.et_f_ram);
        final EditText etStorage = (EditText) dialog.findViewById(R.id.et_f_storage);
        final EditText etBuyPrice = (EditText) dialog.findViewById(R.id.et_f_buy_price);
        final EditText etSellPrice = (EditText) dialog.findViewById(R.id.et_f_sell_price);
        final EditText etStockQty = (EditText) dialog.findViewById(R.id.et_f_stock_qty);
        final EditText etUnit = (EditText) dialog.findViewById(R.id.et_f_unit);
        final EditText etSupplier = (EditText) dialog.findViewById(R.id.et_f_supplier);
        final EditText etLocation = (EditText) dialog.findViewById(R.id.et_f_location);
        final EditText etDesc = (EditText) dialog.findViewById(R.id.et_f_description);

        if (isEditMode) {
            tvTitle.setText("Edit Product #" + existingProduct.getId());
            etName.setText(existingProduct.getName());
            etBrand.setText(existingProduct.getBrand());
            etModel.setText(existingProduct.getModel());
            etCode.setText(existingProduct.getProductCode());
            etBarcode.setText(existingProduct.getBarcode());
            etImei1.setText(existingProduct.getImei1());
            etImei2.setText(existingProduct.getImei2());
            etCategory.setText(existingProduct.getCategory());
            etColor.setText(existingProduct.getColor());
            etRam.setText(existingProduct.getRam());
            etStorage.setText(existingProduct.getStorage());
            etBuyPrice.setText(String.valueOf(existingProduct.getPurchasePrice()));
            etSellPrice.setText(String.valueOf(existingProduct.getSellingPrice()));
            etStockQty.setText(String.valueOf(existingProduct.getStockQuantity()));
            etUnit.setText(existingProduct.getUnit());
            etSupplier.setText(existingProduct.getSupplierName());
            etLocation.setText(existingProduct.getLocation());
            etDesc.setText(existingProduct.getDescription());
        } else {
            tvTitle.setText("Add New Product");

            if (scanRes != null) {
                if (!scanRes.getProductName().isEmpty()) etName.setText(scanRes.getProductName());
                if (!scanRes.getBrand().isEmpty()) etBrand.setText(scanRes.getBrand());
                if (!scanRes.getModel().isEmpty()) etModel.setText(scanRes.getModel());
                if (!scanRes.getProductCode().isEmpty()) etCode.setText(scanRes.getProductCode());
                if (!scanRes.getBarcode().isEmpty()) etBarcode.setText(scanRes.getBarcode());
                if (!scanRes.getImei1().isEmpty()) etImei1.setText(scanRes.getImei1());
                if (!scanRes.getImei2().isEmpty()) etImei2.setText(scanRes.getImei2());
                if (!scanRes.getCategory().isEmpty()) etCategory.setText(scanRes.getCategory());
                if (!scanRes.getColor().isEmpty()) etColor.setText(scanRes.getColor());
                if (!scanRes.getRam().isEmpty()) etRam.setText(scanRes.getRam());
                if (!scanRes.getStorage().isEmpty()) etStorage.setText(scanRes.getStorage());
                if (scanRes.getPurchasePrice() > 0) etBuyPrice.setText(String.valueOf(scanRes.getPurchasePrice()));
                if (scanRes.getSellingPrice() > 0) etSellPrice.setText(String.valueOf(scanRes.getSellingPrice()));
                if (scanRes.getStockQuantity() > 0) etStockQty.setText(String.valueOf(scanRes.getStockQuantity()));
                if (!scanRes.getSupplierName().isEmpty()) etSupplier.setText(scanRes.getSupplierName());
                if (!scanRes.getLocation().isEmpty()) etLocation.setText(scanRes.getLocation());
                if (!scanRes.getDescription().isEmpty()) etDesc.setText(scanRes.getDescription());

                if (etName.getText().toString().trim().isEmpty()) {
                    String defaultName = (scanRes.getBrand() + " " + scanRes.getModel()).trim();
                    if (!defaultName.isEmpty()) etName.setText(defaultName);
                }
                if (etCategory.getText().toString().trim().isEmpty()) {
                    etCategory.setText("Mobile");
                }
                if (etDesc.getText().toString().trim().isEmpty() && !scanRes.getRawOcrText().isEmpty()) {
                    etDesc.setText(scanRes.getRawOcrText());
                }
            }

            if (prefillCode != null && !prefillCode.trim().isEmpty()) {
                if (etCode.getText().toString().trim().isEmpty()) etCode.setText(prefillCode.trim());
                if (etBarcode.getText().toString().trim().isEmpty()) etBarcode.setText(prefillCode.trim());
            }
        }

        dialog.findViewById(R.id.btn_form_cancel).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dialog.dismiss(); }
        });

        dialog.findViewById(R.id.btn_form_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = etName.getText().toString().trim();
                String code = etCode.getText().toString().trim();
                String barcode = etBarcode.getText().toString().trim();

                if (name.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Product name is required.", Toast.LENGTH_SHORT).show();
                    etName.requestFocus();
                    return;
                }

                if (code.isEmpty()) {
                    if (!barcode.isEmpty()) {
                        code = barcode;
                        etCode.setText(code);
                    } else {
                        code = "PRD-" + System.currentTimeMillis();
                        etCode.setText(code);
                    }
                }

                Product p = isEditMode ? existingProduct : new Product();
                p.setName(name);
                p.setProductCode(code);
                p.setBarcode(barcode.isEmpty() ? code : barcode);
                p.setBrand(etBrand.getText().toString().trim());
                p.setModel(etModel.getText().toString().trim());
                p.setImei1(etImei1.getText().toString().trim());
                p.setImei2(etImei2.getText().toString().trim());
                p.setCategory(etCategory.getText().toString().trim());
                p.setColor(etColor.getText().toString().trim());
                p.setRam(etRam.getText().toString().trim());
                p.setStorage(etStorage.getText().toString().trim());
                String unitVal = etUnit.getText().toString().trim();
                p.setUnit(unitVal.isEmpty() ? "Pcs" : unitVal);
                p.setSupplierName(etSupplier.getText().toString().trim());
                p.setLocation(etLocation.getText().toString().trim());
                p.setDescription(etDesc.getText().toString().trim());

                try {
                    String buyStr = etBuyPrice.getText().toString().trim();
                    p.setPurchasePrice(buyStr.isEmpty() ? 0.0 : Double.parseDouble(buyStr));
                } catch (Exception e) { p.setPurchasePrice(0.0); }

                try {
                    String sellStr = etSellPrice.getText().toString().trim();
                    p.setSellingPrice(sellStr.isEmpty() ? 0.0 : Double.parseDouble(sellStr));
                } catch (Exception e) { p.setSellingPrice(0.0); }

                try {
                    String stockStr = etStockQty.getText().toString().trim();
                    p.setStockQuantity(stockStr.isEmpty() ? 0 : Integer.parseInt(stockStr));
                } catch (Exception e) { p.setStockQuantity(0); }

                if (isEditMode) {
                    p.setId(existingProduct.getId());
                    if (dbHelper.updateProduct(p)) {
                        Toast.makeText(MainActivity.this, "Product Updated Successfully", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        updateCategorySpinner();
                        refreshProductsList();
                        refreshDashboard();
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to update product.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Product dup = dbHelper.findProductByCodeOrImei(code);
                    if (dup != null) {
                        Toast.makeText(MainActivity.this, "A product with code '" + code + "' already exists.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    long id = dbHelper.insertProduct(p);
                    if (id > 0) {
                        p.setId(id);
                        Toast.makeText(MainActivity.this, "Product Saved Successfully", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        switchTab("PRODUCTS");
                        refreshDashboard();
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to save product into database.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        dialog.show();
    }

    private void openProductDetailDialog(Product p) {
        if (p == null) return;
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_product_detail);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvName = (TextView) dialog.findViewById(R.id.tv_detail_name);
        TextView tvCode = (TextView) dialog.findViewById(R.id.tv_detail_code);
        TextView tvBody = (TextView) dialog.findViewById(R.id.tv_detail_body);
        Button btnClose = (Button) dialog.findViewById(R.id.btn_detail_close);

        tvName.setText(p.getName());
        tvCode.setText("Code: " + p.getProductCode() + " | Category: " + p.getCategory());

        StringBuilder sb = new StringBuilder();
        if (!p.getBrand().isEmpty()) sb.append("Brand: ").append(p.getBrand()).append("\n");
        if (!p.getModel().isEmpty()) sb.append("Model: ").append(p.getModel()).append("\n");
        if (!p.getBarcode().isEmpty()) sb.append("Barcode: ").append(p.getBarcode()).append("\n");
        if (!p.getImei1().isEmpty()) sb.append("IMEI 1: ").append(p.getImei1()).append("\n");
        if (!p.getImei2().isEmpty()) sb.append("IMEI 2: ").append(p.getImei2()).append("\n");
        if (!p.getSerialNumber().isEmpty()) sb.append("Serial No: ").append(p.getSerialNumber()).append("\n");
        if (!p.getColor().isEmpty()) sb.append("Color: ").append(p.getColor()).append("\n");
        if (!p.getRam().isEmpty() || !p.getStorage().isEmpty()) {
            sb.append("Specs: ").append(p.getRam()).append(" RAM / ").append(p.getStorage()).append(" Storage\n");
        }
        sb.append("Purchase Price: $").append(String.format(Locale.US, "%.2f", p.getPurchasePrice())).append("\n");
        sb.append("Selling Price: $").append(String.format(Locale.US, "%.2f", p.getSellingPrice())).append("\n");
        sb.append("Available Stock: ").append(p.getStockQuantity()).append(" ").append(p.getUnit()).append("\n");
        sb.append("Status: ").append(p.getStockStatus()).append("\n");
        if (!p.getLocation().isEmpty()) sb.append("Location: ").append(p.getLocation()).append("\n");
        if (!p.getSupplierName().isEmpty()) sb.append("Supplier: ").append(p.getSupplierName()).append("\n");
        if (!p.getDescription().isEmpty()) sb.append("Description: ").append(p.getDescription()).append("\n");

        tvBody.setText(sb.toString());

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dialog.dismiss(); }
        });

        dialog.show();
    }

    private void triggerVibration() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) vibrator.vibrate(120);
        } catch (Exception ignored) {}
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
            }
        }
    }

    private class ProductAdapter extends BaseAdapter {
        private final Context ctx;
        private final List<Product> list;

        public ProductAdapter(Context ctx, List<Product> list) {
            this.ctx = ctx;
            this.list = list;
        }

        @Override public int getCount() { return list.size(); }
        @Override public Object getItem(int pos) { return list.get(pos); }
        @Override public long getItemId(int pos) { return list.get(pos).getId(); }

        @Override
        public View getView(final int pos, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(ctx).inflate(R.layout.item_product, parent, false);
            }

            final Product p = list.get(pos);
            TextView tvName = (TextView) convertView.findViewById(R.id.tv_item_prod_name);
            TextView tvSub = (TextView) convertView.findViewById(R.id.tv_item_prod_sub);
            TextView tvPrice = (TextView) convertView.findViewById(R.id.tv_item_prod_price);
            TextView tvStatus = (TextView) convertView.findViewById(R.id.tv_item_prod_status);

            tvName.setText(p.getName());
            tvSub.setText("Code: " + p.getProductCode() + " | Brand: " + (p.getBrand().isEmpty() ? "N/A" : p.getBrand()));
            tvPrice.setText("Sell: $" + String.format(Locale.US, "%.2f", p.getSellingPrice()) + " | Buy: $" + String.format(Locale.US, "%.2f", p.getPurchasePrice()));
            tvStatus.setText(p.getStockStatus() + " (" + p.getStockQuantity() + ")");

            Button btnView = (Button) convertView.findViewById(R.id.btn_item_view);
            Button btnEdit = (Button) convertView.findViewById(R.id.btn_item_edit);
            Button btnDelete = (Button) convertView.findViewById(R.id.btn_item_delete);

            btnView.setFocusable(false);
            btnEdit.setFocusable(false);
            btnDelete.setFocusable(false);

            final long prodId = p.getId();

            btnView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Product freshProd = dbHelper.getProductById(prodId);
                    openProductDetailDialog(freshProd != null ? freshProd : p);
                }
            });

            btnEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Product freshProd = dbHelper.getProductById(prodId);
                    openProductFormDialog(freshProd != null ? freshProd : p, null);
                }
            });

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Delete Product")
                            .setMessage("Are you sure you want to delete '" + p.getName() + "'?")
                            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (dbHelper.softDeleteProduct(prodId)) {
                                        Toast.makeText(MainActivity.this, "Product Deleted Successfully", Toast.LENGTH_SHORT).show();
                                        updateCategorySpinner();
                                        refreshProductsList();
                                        refreshDashboard();
                                    } else {
                                        Toast.makeText(MainActivity.this, "Failed to delete product.", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            });

            return convertView;
        }
    }

    private class TransactionAdapter extends BaseAdapter {
        private final Context ctx;
        private final List<StockTransaction> list;

        public TransactionAdapter(Context ctx, List<StockTransaction> list) {
            this.ctx = ctx;
            this.list = list;
        }

        @Override public int getCount() { return list.size(); }
        @Override public Object getItem(int pos) { return list.get(pos); }
        @Override public long getItemId(int pos) { return list.get(pos).getId(); }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(ctx).inflate(R.layout.item_transaction, parent, false);
            }

            final StockTransaction tx = list.get(pos);
            TextView tvType = (TextView) convertView.findViewById(R.id.tv_tx_type);
            TextView tvName = (TextView) convertView.findViewById(R.id.tv_tx_prod_name);
            TextView tvQty = (TextView) convertView.findViewById(R.id.tv_tx_qty);
            TextView tvDetails = (TextView) convertView.findViewById(R.id.tv_tx_details);
            TextView tvNotes = (TextView) convertView.findViewById(R.id.tv_tx_notes);

            tvName.setText(tx.getProductName());

            if ("IN".equalsIgnoreCase(tx.getType())) {
                tvType.setText("STOCK IN");
                tvType.setTextColor(0xFF2E7D32);
                tvQty.setText("+" + tx.getQuantity() + " Pcs");
                tvQty.setTextColor(0xFF2E7D32);
            } else {
                tvType.setText("STOCK OUT");
                tvType.setTextColor(0xFFC62828);
                tvQty.setText("-" + tx.getQuantity() + " Pcs");
                tvQty.setTextColor(0xFFC62828);
            }

            tvDetails.setText("Prev: " + tx.getPreviousStock() + " | New: " + tx.getUpdatedStock() + " | " + tx.getTransactionDate());
            tvNotes.setText(tx.getNotes().isEmpty() ? "No notes" : tx.getNotes());

            convertView.findViewById(R.id.btn_tx_delete).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(ctx)
                            .setTitle("Delete Transaction")
                            .setMessage("Revert stock movement and delete this record?")
                            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (dbHelper.deleteTransactionAndRecalculate(tx.getId())) {
                                        Toast.makeText(ctx, "Transaction Deleted Successfully", Toast.LENGTH_SHORT).show();
                                        refreshHistoryList();
                                        refreshDashboard();
                                    }
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            });

            return convertView;
        }
    }

    private class ScanLogAdapter extends BaseAdapter {
        private final Context ctx;
        private final List<ScanLogItem> list;

        public ScanLogAdapter(Context ctx, List<ScanLogItem> list) {
            this.ctx = ctx;
            this.list = list;
        }

        @Override public int getCount() { return list.size(); }
        @Override public Object getItem(int pos) { return list.get(pos); }
        @Override public long getItemId(int pos) { return list.get(pos).getId(); }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(ctx).inflate(R.layout.item_scan_log, parent, false);
            }

            final ScanLogItem item = list.get(pos);
            TextView tvSource = (TextView) convertView.findViewById(R.id.tv_scan_source);
            TextView tvTime = (TextView) convertView.findViewById(R.id.tv_scan_time);
            TextView tvBarcode = (TextView) convertView.findViewById(R.id.tv_scan_barcode);
            TextView tvFullText = (TextView) convertView.findViewById(R.id.tv_scan_full_text);
            Button btnCopy = (Button) convertView.findViewById(R.id.btn_copy_scan_text);

            tvSource.setText(item.getSource());
            tvTime.setText(item.getTimestamp());

            if (!item.getBarcode().isEmpty()) {
                tvBarcode.setVisibility(View.VISIBLE);
                tvBarcode.setText("Code / Barcode: " + item.getBarcode());
            } else {
                tvBarcode.setVisibility(View.GONE);
            }

            tvFullText.setText(item.getScannedText());

            btnCopy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        ClipboardManager clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("Scanned Text", item.getScannedText());
                        if (clipboard != null) {
                            clipboard.setPrimaryClip(clip);
                            Toast.makeText(ctx, "Scanned text copied to clipboard", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(ctx, "Failed to copy text", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            return convertView;
        }
    }

    private static class BoxAdapter extends BaseAdapter {
        private final Context context;
        private final List<BoxItem> items;

        public BoxAdapter(Context context, List<BoxItem> items) {
            this.context = context;
            this.items = items;
        }

        @Override public int getCount() { return items.size(); }
        @Override public Object getItem(int position) { return items.get(position); }
        @Override public long getItemId(int position) { return items.get(position).getId(); }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_box_stock, parent, false);
            }

            BoxItem item = items.get(position);
            TextView tvCode = (TextView) convertView.findViewById(R.id.tv_box_code);
            TextView tvUpdated = (TextView) convertView.findViewById(R.id.tv_last_updated);
            TextView tvQty = (TextView) convertView.findViewById(R.id.tv_box_quantity);

            tvCode.setText(item.getQrCode());
            tvUpdated.setText("Updated: " + item.getLastUpdated());
            tvQty.setText(item.getQuantity() + " Pcs");

            return convertView;
        }
    }
}