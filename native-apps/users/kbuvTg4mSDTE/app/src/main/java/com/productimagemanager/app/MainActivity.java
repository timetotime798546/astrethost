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

import org.json.JSONArray;
import org.json.JSONObject;

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

    private BackendApi backendApi;
    private List<Product> productList;
    private ProductAdapter adapter;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                    Toast.makeText(MainActivity.this, "Please sign in to manage cloud products", Toast.LENGTH_SHORT).show();
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
                fetchCloudProducts();
            }
        });

        updateAuthStatusUi();
        fetchCloudProducts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAuthStatusUi();
        if (backendApi.isLoggedIn()) {
            fetchCloudProducts();
        }
    }

    private void updateAuthStatusUi() {
        if (backendApi.isLoggedIn()) {
            tvUserStatus.setText("Online: " + backendApi.getEmail());
            btnAuthAction.setText("Sign Out");
            btnAddProduct.setEnabled(true);
        } else {
            tvUserStatus.setText("Sign-in required (Online Only Persistence)");
            btnAuthAction.setText("Sign In");
            productList.clear();
            renderProductList();
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
                        productList.clear();
                        renderProductList();
                        updateAuthStatusUi();
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
                        productList.clear();
                        renderProductList();
                        updateAuthStatusUi();
                    }
                });
            }
        });
    }

    private void fetchCloudProducts() {
        if (!backendApi.isLoggedIn()) {
            productList.clear();
            renderProductList();
            return;
        }

        mainProgressBar.setVisibility(View.VISIBLE);
        backendApi.fetchRecords("products", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(final String response) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mainProgressBar.setVisibility(View.GONE);
                        parseAndDisplayRecords(response);
                    }
                });
            }

            @Override
            public void onError(final String error) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mainProgressBar.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, "Error fetching catalog: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void parseAndDisplayRecords(String responseJson) {
        productList.clear();
        try {
            JSONObject root = new JSONObject(responseJson);
            if (root.optBoolean("success", false) && root.has("records")) {
                JSONArray records = root.getJSONArray("records");
                for (int i = 0; i < records.length(); i++) {
                    JSONObject recObj = records.getJSONObject(i);
                    String recId = recObj.optString("id", "");
                    String createdAt = recObj.optString("created_at", "");
                    JSONObject dataObj = recObj.optJSONObject("data");

                    if (dataObj != null) {
                        String name = dataObj.optString("name", "");
                        double price = dataObj.optDouble("price", 0.0);
                        String description = dataObj.optString("description", "");
                        String imageId = dataObj.optString("image_id", "");

                        Product product = new Product(recId, name, price, description, imageId, createdAt);
                        productList.add(product);
                    }
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to parse records: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        renderProductList();
    }

    private void renderProductList() {
        // MANDATORY OFFLINE CALCULATION: Deterministically compute catalog totals locally in Java
        double totalValue = 0.0;
        for (int i = 0; i < productList.size(); i++) {
            totalValue += productList.get(i).getPrice();
        }

        tvCatalogSummary.setText(String.format(Locale.US, "Cloud Products: %d | Total Catalog Value: $%.2f", productList.size(), totalValue));

        if (productList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            if (!backendApi.isLoggedIn()) {
                tvEmptyState.setText("Please sign in to access your cloud products.\n(Offline local storage is disabled)");
            } else {
                tvEmptyState.setText("No products found in cloud database.\nTap '+ Add Product' to create one.");
            }
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
        intent.putExtra(ProductEditActivity.EXTRA_PRODUCT_NAME, product.getName());
        intent.putExtra(ProductEditActivity.EXTRA_PRODUCT_PRICE, product.getPrice());
        intent.putExtra(ProductEditActivity.EXTRA_PRODUCT_DESC, product.getDescription());
        intent.putExtra(ProductEditActivity.EXTRA_PRODUCT_IMAGE_ID, product.getImageId());
        startActivityForResult(intent, REQ_PRODUCT);
    }

    @Override
    public void onDelete(final Product product) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Product from Cloud")
                .setMessage("Are you sure you want to permanently delete '" + product.getName() + "' and its image from the cloud?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        executeCloudDelete(product);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeCloudDelete(final Product product) {
        mainProgressBar.setVisibility(View.VISIBLE);
        final String imageId = product.getImageId();

        // 1. Delete associated image on Image Worker if present
        if (imageId != null && !imageId.trim().isEmpty() && backendApi.isLoggedIn()) {
            ImageWorkerApi.deleteImage(imageId, backendApi.getToken(), new ImageWorkerApi.DeleteCallback() {
                @Override
                public void onSuccess() {
                    deleteRecordFromCloudBackend(product.getId());
                }

                @Override
                public void onError(String error) {
                    // Proceed to delete record even if image deletion encounters issue
                    deleteRecordFromCloudBackend(product.getId());
                }
            });
        } else {
            deleteRecordFromCloudBackend(product.getId());
        }
    }

    private void deleteRecordFromCloudBackend(final String recordId) {
        backendApi.deleteRecord(recordId, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mainProgressBar.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, "Product deleted from cloud", Toast.LENGTH_SHORT).show();
                        fetchCloudProducts();
                    }
                });
            }

            @Override
            public void onError(final String error) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mainProgressBar.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, "Failed to delete from cloud: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_AUTH || requestCode == REQ_PRODUCT) {
            updateAuthStatusUi();
            if (backendApi.isLoggedIn()) {
                fetchCloudProducts();
            }
        }
    }
}