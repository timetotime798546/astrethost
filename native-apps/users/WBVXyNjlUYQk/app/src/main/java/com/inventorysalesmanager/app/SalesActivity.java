package com.inventorysalesmanager.app;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import java.util.Locale;

public class SalesActivity extends Activity {

    private Spinner spProducts;
    private EditText etSaleQty;
    private TextView tvTotalCalculated;
    private ListView lvTodaySales;
    private TextView tvSalesEmpty;

    private DatabaseHelper dbHelper;
    private List<Product> availableProducts;
    private List<Sale> todaySales;
    private SalesHistoryAdapter historyAdapter;

    private Product selectedProduct = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales);

        dbHelper = new DatabaseHelper(this);

        spProducts = (Spinner) findViewById(R.id.sp_products);
        etSaleQty = (EditText) findViewById(R.id.et_sale_qty);
        tvTotalCalculated = (TextView) findViewById(R.id.tv_total_calculated);
        lvTodaySales = (ListView) findViewById(R.id.lv_today_sales);
        tvSalesEmpty = (TextView) findViewById(R.id.tv_sales_empty);
        Button btnCompleteSale = (Button) findViewById(R.id.btn_complete_sale);

        loadProductsSpinner();
        loadTodaySalesLedger();

        spProducts.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < availableProducts.size()) {
                    selectedProduct = availableProducts.get(position);
                    recalculateTotalPrice();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedProduct = null;
            }
        });

        etSaleQty.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                recalculateTotalPrice();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnCompleteSale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processSale();
            }
        });
    }

    private void loadProductsSpinner() {
        availableProducts = dbHelper.getAllProducts(null);
        ArrayAdapter<Product> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, availableProducts);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spProducts.setAdapter(adapter);

        if (availableProducts.isEmpty()) {
            selectedProduct = null;
        } else {
            selectedProduct = availableProducts.get(0);
        }
        recalculateTotalPrice();
    }

    private void loadTodaySalesLedger() {
        todaySales = dbHelper.getTodaySales();
        if (todaySales.isEmpty()) {
            tvSalesEmpty.setVisibility(View.VISIBLE);
            lvTodaySales.setVisibility(View.GONE);
        } else {
            tvSalesEmpty.setVisibility(View.GONE);
            lvTodaySales.setVisibility(View.VISIBLE);
        }
        historyAdapter = new SalesHistoryAdapter(this, todaySales);
        lvTodaySales.setAdapter(historyAdapter);
    }

    private void recalculateTotalPrice() {
        if (selectedProduct == null) {
            tvTotalCalculated.setText("$0.00");
            return;
        }

        String qtyStr = etSaleQty.getText().toString().trim();
        if (qtyStr.isEmpty()) {
            tvTotalCalculated.setText("$0.00");
            return;
        }

        try {
            int qty = Integer.parseInt(qtyStr);
            double total = qty * selectedProduct.getSellingPrice();
            tvTotalCalculated.setText(String.format(Locale.getDefault(), "$%.2f", total));
        } catch (NumberFormatException e) {
            tvTotalCalculated.setText("$0.00");
        }
    }

    private void processSale() {
        if (selectedProduct == null) {
            Toast.makeText(this, "Please create and select a product first.", Toast.LENGTH_SHORT).show();
            return;
        }

        String qtyStr = etSaleQty.getText().toString().trim();
        if (qtyStr.isEmpty()) {
            etSaleQty.setError("Quantity required.");
            return;
        }

        int qty = 0;
        try {
            qty = Integer.parseInt(qtyStr);
            if (qty <= 0) {
                etSaleQty.setError("Quantity must be greater than 0.");
                return;
            }
        } catch (NumberFormatException e) {
            etSaleQty.setError("Invalid number format.");
            return;
        }

        if (qty > selectedProduct.getStockQty()) {
            etSaleQty.setError("Out of Stock! Only " + selectedProduct.getStockQty() + " units available.");
            return;
        }

        double totalPrice = qty * selectedProduct.getSellingPrice();

        boolean outcome = dbHelper.recordSale(selectedProduct.getId(), qty, totalPrice);
        if (outcome) {
            Toast.makeText(this, "Sale recorded successfully!", Toast.LENGTH_SHORT).show();
            etSaleQty.setText("1");
            loadProductsSpinner(); // reload stock levels
            loadTodaySalesLedger(); // refresh ledger list view
        } else {
            Toast.makeText(this, "Error processing sale. Try again.", Toast.LENGTH_LONG).show();
        }
    }

    // Custom List Adapter for Sales history representation
    private static class SalesHistoryAdapter extends BaseAdapter {
        private final Context context;
        private final List<Sale> list;

        public SalesHistoryAdapter(Context context, List<Sale> list) {
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
            return list.get(position).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.sale_list_item, parent, false);
            }

            Sale sale = list.get(position);

            TextView tvName = (TextView) convertView.findViewById(R.id.sale_item_name);
            TextView tvPrice = (TextView) convertView.findViewById(R.id.sale_item_total_price);
            TextView tvQty = (TextView) convertView.findViewById(R.id.sale_item_quantity);
            TextView tvTime = (TextView) convertView.findViewById(R.id.sale_item_time);

            tvName.setText(sale.getProductName());
            tvPrice.setText(String.format(Locale.getDefault(), "$%.2f", sale.getTotalPrice()));
            tvQty.setText("Qty: " + sale.getQuantity() + " units");

            // Format date for simpler representation
            String fullDate = sale.getSaleDate();
            if (fullDate != null && fullDate.length() > 11) {
                tvTime.setText(fullDate.substring(11)); // only HH:mm:ss
            } else {
                tvTime.setText(fullDate);
            }

            return convertView;
        }
    }
}