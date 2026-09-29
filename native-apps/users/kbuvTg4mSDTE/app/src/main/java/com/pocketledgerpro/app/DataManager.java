package com.pocketledgerpro.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.List;

public class DataManager {
    private static final String PREF_NAME = "pocketledger_prefs";
    private static final String KEY_TRANSACTIONS = "transactions";
    private SharedPreferences prefs;

    public DataManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveTransactions(List<Transaction> transactions) {
        JSONArray array = new JSONArray();
        for (Transaction t : transactions) {
            array.put(t.toJsonObject());
        }
        prefs.edit().putString(KEY_TRANSACTIONS, array.toString()).apply();
    }

    public List<Transaction> getTransactions() {
        List<Transaction> list = new ArrayList<>();
        String json = prefs.getString(KEY_TRANSACTIONS, "[]");
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                Transaction t = Transaction.fromJsonObject(array.getJSONObject(i));
                if (t != null) list.add(t);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void clearData() {
        prefs.edit().remove(KEY_TRANSACTIONS).apply();
    }
}