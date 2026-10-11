package com.productimagemanager.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements ProductAdapter.ProductActionCallback {

    private static final int REQ_AUTH = 1001;
    private static final int REQ_PRODUCT = 1002;

    private TextView tvUserStatus;
    private Button btnAuthAction;
    private Button btnAddProduct;
    private Button btnRefresh;
    private TextView tvCatalogSummary;
    private ListView lvProducts;
    private TextView tvEmptyState;
    private ProgressBar mainProgressBar;

    private ProductDbHelper dbHelper;
    private BackendApi backendApi;
    private List<Product> productList;
    private ProductAdapter adapter;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new ProductDbHelper(this);
        backendApi = new BackendApi(this);
        mainHandler = new Handler(Looper.getMainLooper());
        productList = new ArrayList<Product>();

        tvUserStatus = (TextView) findViewById(R.id.tvUserStatus);
        btnAuthAction = (Button) findViewById(R.id.btnAuthAction);
        btnAddProduct = (Button) findViewById(R.id.btnAddProduct);
        btnRefresh = (Button) findViewById(R.id.btnRefresh);
        tvCatalogSummary = (TextView) findViewById(R.id.tvCatalogSummary);
        lvProducts = (ListView) findViewById(R.id.lvProducts);
        tvEmptyState = (TextView) findViewById(R.id.tvEmptyState);
        mainProgressBar = (ProgressBar) findViewById(R.id.mainProgressBar);

        btnAuthAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (backendApi.isLoggedIn()) {
                    performLogout();
                } else {
                    startActivityForResult(new Intent(MainActivity.this, LoginActivity.class), REQ_AUTH);
                }
            }
        });

        btnAddProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!backendApi.isLoggedIn()) {
                    Toast.makeText(MainActivity.this, "Please sign in to upload images to Image Worker", Toast.LENGTH_SHORT).show();
                    startActivityForResult(new Intent(MainActivity.this, LoginActivity.class), REQ_AUTH);
                    return;
                }
                Intent intent = new Intent(MainActivity.this, ProductEditActivity.class);
                startActivityForResult(intent, REQ_PRODUCT);
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadLocalProducts();
                Toast.makeText(MainActivity.this, "Catalog reloaded", Toast.LENGTH_SHORT).show();
            }
        });

        updateAuthStatusUi();
        loadLocalProducts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAuthStatusUi();
        loadLocalProducts();
    }

    private void updateAuthStatusUi() {
        if (backendApi.isLoggedIn()) {
            tvUserStatus.setText("Logged in: " + backendApi.getEmail());
            btnAuthAction.setText("Sign Out");
        } else {
            tvUserStatus.setText("Not signed in (Local Offline mode)");
            btnAuthAction.setText("Sign In");
        }
    }

    private void performLogout() {
        mainProgressBar.setVisibility(View.VISIBLE);
        backendApi.logout(new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mainProgressBar.setVisibility(View.GONE);
                        updateAuthStatusUi();
                        loadLocalProducts();
                        Toast.makeText(MainActivity.this, "Signed out successfully", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(final String error) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mainProgressBar.setVisibility(View.GONE);
                        updateAuthStatusUi();
                        loadLocalProducts();
                    }
                });
            }
        });
    }

    private void loadLocalProducts() {
        productList.clear();
        productList.addAll(dbHelper.getAllProducts());

        // Calculate inventory summary deterministically locally
        double totalValue = 0.0;
        for (int i = 0; i < productList.size(); i++) {
            totalValue += productList.get(i).getPrice();
        }

        tvCatalogSummary.setText(String.format(Locale.US, "Products: %d | Total Catalog Value: $%.2f", productList.size(), totalValue));

        if (productList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            lvProducts.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            lvProducts.setVisibility(View.VISIBLE);
        }

        adapter = new ProductAdapter(this, productList, backendApi.getToken(), this);
        lvProducts.setAdapter(adapter);
    }

    @Override
    public void onEdit(Product product) {
        Intent intent = new Intent(this, ProductEditActivity.class);
        intent.putExtra(ProductEditActivity.EXTRA_PRODUCT_ID, product.getId());
        startActivityForResult(intent, REQ_PRODUCT);
    }

    @Override
    public void onDelete(final Product product) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete '" + product.getName() + "' and its associated image from the Image Worker?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        executeDeleteProductProcess(product);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeDeleteProductProcess(final Product product) {
        mainProgressBar.setVisibility(View.VISIBLE);
        final String imageId = product.getImageId();

        if (imageId != null && !imageId.trim().isEmpty() && backendApi.isLoggedIn()) {
            // Delete image on Image Worker first
            ImageWorkerApi.deleteImage(imageId, backendApi.getToken(), new ImageWorkerApi.DeleteCallback() {
                @Override
                public void onSuccess() {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            dbHelper.deleteProduct(product.getId());
                            mainProgressBar.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this, "Product and private image deleted", Toast.LENGTH_SHORT).show();
                            loadLocalProducts();
                        }
                    });
                }

                @Override
                public void onError(final String error) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mainProgressBar.setVisibility(View.GONE);
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Image Worker Deletion Error")
                                    .setMessage("Failed to delete remote image: " + error + ". Do you still want to delete the local product record?")
                                    .setPositiveButton("Delete Local Record", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dbHelper.deleteProduct(product.getId());
                                            loadLocalProducts();
                                        }
                                    })
                                    .setNegativeButton("Keep Record", null)
                                    .show();
                        }
                    });
                }
            });
        } else {
            // No image on worker or not logged in, delete local record
            dbHelper.deleteProduct(product.getId());
            mainProgressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Local product record deleted", Toast.LENGTH_SHORT).show();
            loadLocalProducts();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_AUTH || requestCode == REQ_PRODUCT) {
            updateAuthStatusUi();
            loadLocalProducts();
        }
    }
}