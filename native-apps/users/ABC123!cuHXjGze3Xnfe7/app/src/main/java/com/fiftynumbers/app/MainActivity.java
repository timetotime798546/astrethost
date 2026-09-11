package com.fiftynumbers.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private TextView detailNumber;
    private TextView detailParity;
    private TextView detailPrime;
    private TextView detailSquare;
    private TextView detailRoman;
    private TextView detailBinary;
    private TextView detailHex;
    private ListView numbersListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind Views
        detailNumber = (TextView) findViewById(R.id.detail_number);
        detailParity = (TextView) findViewById(R.id.detail_parity);
        detailPrime = (TextView) findViewById(R.id.detail_prime);
        detailSquare = (TextView) findViewById(R.id.detail_square);
        detailRoman = (TextView) findViewById(R.id.detail_roman);
        detailBinary = (TextView) findViewById(R.id.detail_binary);
        detailHex = (TextView) findViewById(R.id.detail_hex);
        numbersListView = (ListView) findViewById(R.id.numbers_list_view);

        // Generate numbers (1 to 50)
        final List<Integer> numbers = new ArrayList<Integer>();
        for (int i = 1; i <= 50; i++) {
            numbers.add(i);
        }

        // Set Adapter
        NumbersAdapter adapter = new NumbersAdapter(numbers);
        numbersListView.setAdapter(adapter);

        // Load statistics for the first number initially
        showNumberDetails(1);

        // Grid/Item selection changes details
        numbersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int selectedNumber = numbers.get(position);
                showNumberDetails(selectedNumber);
            }
        });
    }

    private void showNumberDetails(int number) {
        detailNumber.setText(String.valueOf(number));

        // Parity
        boolean isEven = (number % 2 == 0);
        if (isEven) {
            detailParity.setText("Even Number");
            detailParity.setTextColor(0xFF4CAF50); // Green
        } else {
            detailParity.setText("Odd Number");
            detailParity.setTextColor(0xFFFF9800); // Orange
        }

        // Prime Check
        if (isPrime(number)) {
            detailPrime.setText("Prime Number");
            detailPrime.setTextColor(0xFF2196F3); // Blue
        } else {
            detailPrime.setText("Composite / Non-Prime");
            detailPrime.setTextColor(0xFF757575); // Gray
        }

        // Square and Cube Info
        long square = (long) number * number;
        long cube = (long) number * number * number;
        detailSquare.setText("Square: " + square + "  |  Cube: " + cube);

        // Roman numerals conversion
        detailRoman.setText(convertToRoman(number));

        // Binary padding configuration
        String bin = Integer.toBinaryString(number);
        while (bin.length() < 8) {
            bin = "0" + bin;
        }
        detailBinary.setText(bin);

        // Hex representation
        detailHex.setText("0x" + Integer.toHexString(number).toUpperCase());
    }

    private boolean isPrime(int n) {
        if (n <= 1) return false;
        if (n <= 3) return true;
        if (n % 2 == 0 || n % 3 == 0) return false;
        for (int i = 5; i * i <= n; i += 6) {
            if (n % i == 0 || n % (i + 2) == 0) {
                return false;
            }
        }
        return true;
    }

    private String convertToRoman(int number) {
        int[] values = {50, 40, 10, 9, 5, 4, 1};
        String[] symbols = {"L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            while (number >= values[i]) {
                number -= values[i];
                sb.append(symbols[i]);
            }
        }
        return sb.toString();
    }

    private class NumbersAdapter extends BaseAdapter {
        private final List<Integer> mList;

        public NumbersAdapter(List<Integer> list) {
            this.mList = list;
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Object getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.list_item_number, parent, false);
                holder = new ViewHolder();
                holder.numberBadge = (LinearLayout) convertView.findViewById(R.id.number_badge);
                holder.itemNumber = (TextView) convertView.findViewById(R.id.item_number);
                holder.itemLabel = (TextView) convertView.findViewById(R.id.item_label);
                holder.itemDesc = (TextView) convertView.findViewById(R.id.item_desc);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            int value = mList.get(position);
            holder.itemNumber.setText(String.valueOf(value));
            holder.itemLabel.setText("Number " + value);

            boolean even = (value % 2 == 0);
            boolean primeNum = isPrime(value);
            String desc = (even ? "Even" : "Odd") + " • " + (primeNum ? "Prime" : "Non-Prime") + " • Hex: 0x" + Integer.toHexString(value).toUpperCase();
            holder.itemDesc.setText(desc);

            if (even) {
                holder.numberBadge.setBackgroundColor(0xFFE8F5E9); // Soft green
                holder.itemNumber.setTextColor(0xFF2E7D32);
            } else {
                holder.numberBadge.setBackgroundColor(0xFFFFF3E0); // Soft orange
                holder.itemNumber.setTextColor(0xFFE65100);
            }

            return convertView;
        }

        private class ViewHolder {
            LinearLayout numberBadge;
            TextView itemNumber;
            TextView itemLabel;
            TextView itemDesc;
        }
    }
}