package com.alfariskirana.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PurchaseListActivity extends Activity {

    private DatabaseHelper dbHelper;
    private ListView lvPurchases;
    private List<Purchase> purchaseList = new ArrayList<Purchase>();
    private PurchaseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase_list);

        dbHelper = new DatabaseHelper(this);
        lvPurchases = (ListView) findViewById(R.id.lv_purchases);
        Button btnAdd = (Button) findViewById(R.id.btn_add_purchase);

        adapter = new PurchaseAdapter();
        lvPurchases.setAdapter(adapter);

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PurchaseListActivity.this, AddPurchaseActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        purchaseList = dbHelper.getAllPurchases();
        adapter.notifyDataSetChanged();
    }

    private class PurchaseAdapter extends BaseAdapter {
        @Override
        public int getCount() { return purchaseList.size(); }

        @Override
        public Object getItem(int position) { return purchaseList.get(position); }

        @Override
        public long getItemId(int position) { return purchaseList.get(position).getId(); }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(PurchaseListActivity.this).inflate(R.layout.item_purchase, parent, false);
            }

            Purchase p = purchaseList.get(position);
            TextView tvProd = (TextView) convertView.findViewById(R.id.tv_purchase_product);
            TextView tvSupplier = (TextView) convertView.findViewById(R.id.tv_purchase_supplier);
            TextView tvDate = (TextView) convertView.findViewById(R.id.tv_purchase_date);
            TextView tvQty = (TextView) convertView.findViewById(R.id.tv_purchase_qty);
            TextView tvAmt = (TextView) convertView.findViewById(R.id.tv_purchase_amount);

            tvProd.setText(p.getProductName());
            tvSupplier.setText("Supplier: " + (p.getSupplierName().isEmpty() ? "Direct Mandi" : p.getSupplierName()));
            tvDate.setText(p.getPurchaseDate());
            tvQty.setText(String.format(Locale.getDefault(), "+ %.2f in stock", p.getQuantity()));
            tvAmt.setText(String.format(Locale.getDefault(), "₹ %.2f", p.getPurchaseAmount()));

            return convertView;
        }
    }
}