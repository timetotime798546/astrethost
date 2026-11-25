package com.kkgstore.store;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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

public class PurchaseListActivity extends Activity {

    private DatabaseHelper dbHelper;
    private ListView lvPurchases;
    private List<PurchaseModel> purchaseList = new ArrayList<>();
    private PurchaseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DemoLockManager.checkAndEnforceLock(this)) return;

        setContentView(R.layout.activity_purchase_list);
        dbHelper = new DatabaseHelper(this);

        lvPurchases = (ListView) findViewById(R.id.lv_purchases);
        Button btnAdd = (Button) findViewById(R.id.btn_add_purchase);

        adapter = new PurchaseAdapter(this, purchaseList);
        lvPurchases.setAdapter(adapter);

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(PurchaseListActivity.this, AddPurchaseActivity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DemoLockManager.checkAndEnforceLock(this)) return;
        loadPurchases();
    }

    private void loadPurchases() {
        purchaseList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sql = "SELECT p.id, p.supplier, pr.name, p.quantity, pr.unit, p.amount, p.purchase_date "
                + "FROM purchases p LEFT JOIN products pr ON p.product_id = pr.id ORDER BY p.id DESC";

        Cursor c = db.rawQuery(sql, null);
        while (c.moveToNext()) {
            purchaseList.add(new PurchaseModel(
                    c.getLong(0),
                    c.getString(1),
                    c.getString(2) == null ? "Item" : c.getString(2),
                    c.getDouble(3),
                    c.getString(4) == null ? "units" : c.getString(4),
                    c.getDouble(5),
                    c.getString(6)
            ));
        }
        c.close();
        adapter.notifyDataSetChanged();
    }

    private static class PurchaseModel {
        long id; String supplier, productName, unit, date; double quantity, amount;
        PurchaseModel(long id, String supplier, String productName, double quantity, String unit, double amount, String date) {
            this.id = id; this.supplier = supplier; this.productName = productName;
            this.quantity = quantity; this.unit = unit; this.amount = amount; this.date = date;
        }
    }

    private class PurchaseAdapter extends BaseAdapter {
        private Context context; private List<PurchaseModel> items;
        PurchaseAdapter(Context context, List<PurchaseModel> items) { this.context = context; this.items = items; }
        @Override public int getCount() { return items.size(); }
        @Override public Object getItem(int position) { return items.get(position); }
        @Override public long getItemId(int position) { return items.get(position).id; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_purchase, parent, false);
            }
            PurchaseModel m = items.get(position);
            TextView tvProduct = (TextView) convertView.findViewById(R.id.tv_purchase_product);
            TextView tvSupplier = (TextView) convertView.findViewById(R.id.tv_purchase_supplier);
            TextView tvDate = (TextView) convertView.findViewById(R.id.tv_purchase_date);
            TextView tvQty = (TextView) convertView.findViewById(R.id.tv_purchase_qty);
            TextView tvAmount = (TextView) convertView.findViewById(R.id.tv_purchase_amount);

            tvProduct.setText(m.productName);
            tvSupplier.setText("Supplier: " + (m.supplier.isEmpty() ? "General Dealer" : m.supplier));
            tvDate.setText(m.date);
            tvQty.setText(String.format("+ %.2f %s", m.quantity, m.unit));
            tvAmount.setText(String.format("₹ %.2f", m.amount));
            return convertView;
        }
    }
}