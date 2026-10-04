package com.shopinventorysync.app;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import java.util.ArrayList;

public class SalesHistoryActivity extends Activity {

    private ListView listSalesHistory;
    private TextView txtLedgerTotal;

    private BackendApi api;
    private ArrayList<Sale> salesRecords = new ArrayList<>();
    private LedgerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales_history);

        api = new BackendApi(this);

        listSalesHistory = (ListView) findViewById(R.id.listSalesHistory);
        txtLedgerTotal = (TextView) findViewById(R.id.txtLedgerTotal);

        adapter = new LedgerAdapter(this, salesRecords);
        listSalesHistory.setAdapter(adapter);

        loadSalesHistory();
    }

    private void loadSalesHistory() {
        // Read cached transactions immediately
        try {
            JSONArray cachedArr = OfflineCache.getCachedSales(this);
            processSales(cachedArr);
        } catch (Exception e) {
            e.printStackTrace();
        }

        api.getRecords("sales", new BackendApi.ApiCallback<JSONArray>() {
            @Override
            public void onSuccess(JSONArray result) {
                OfflineCache.cacheSales(SalesHistoryActivity.this, result);
                processSales(result);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(SalesHistoryActivity.this, "Offline mode. Displaying cached ledger.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processSales(JSONArray arr) {
        salesRecords.clear();
        double runningRevenue = 0.0;

        for (int i = 0; i < arr.length(); i++) {
            try {
                Sale sale = new Sale(arr.getJSONObject(i));
                salesRecords.add(sale);

                // Dynamic sum calculation of all ledger records
                runningRevenue += sale.totalRevenue;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        txtLedgerTotal.setText(String.format("$%.2f", runningRevenue));
        adapter.notifyDataSetChanged();
    }

    // Ledger custom adapter representation
    private static class LedgerAdapter extends BaseAdapter {
        private final Context ctx;
        private final ArrayList<Sale> list;

        public LedgerAdapter(Context ctx, ArrayList<Sale> list) {
            this.ctx = ctx;
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
                convertView = LayoutInflater.from(ctx).inflate(android.R.layout.simple_list_item_2, parent, false);
            }

            Sale sale = list.get(position);

            TextView txt1 = (TextView) convertView.findViewById(android.R.id.text1);
            TextView txt2 = (TextView) convertView.findViewById(android.R.id.text2);

            txt1.setText(sale.date + " - Revenue: " + String.format("$%.2f", sale.totalRevenue));
            txt2.setText(sale.itemsSummary);

            return convertView;
        }
    }
}