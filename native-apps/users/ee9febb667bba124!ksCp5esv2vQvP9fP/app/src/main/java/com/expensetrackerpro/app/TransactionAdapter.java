package com.expensetrackerpro.app;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.List;

public class TransactionAdapter extends BaseAdapter {
    private Context context;
    private List<Transaction> list;

    public TransactionAdapter(Context context, List<Transaction> list) {
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
            convertView = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        }

        Transaction tx = list.get(position);

        View tagIndicator = convertView.findViewById(R.id.tag_indicator);
        TextView txtCategory = convertView.findViewById(R.id.txt_item_category);
        TextView txtNote = convertView.findViewById(R.id.txt_item_note);
        TextView txtAmount = convertView.findViewById(R.id.txt_item_amount);
        TextView txtDate = convertView.findViewById(R.id.txt_item_date);

        txtCategory.setText(tx.getCategory());
        if (tx.getNote() == null || tx.getNote().trim().isEmpty()) {
            txtNote.setVisibility(View.GONE);
        } else {
            txtNote.setVisibility(View.VISIBLE);
            txtNote.setText(tx.getNote());
        }
        txtDate.setText(tx.getDate());

        if ("INCOME".equals(tx.getType())) {
            tagIndicator.setBackgroundColor(Color.parseColor("#2E7D32"));
            txtAmount.setTextColor(Color.parseColor("#2E7D32"));
            txtAmount.setText(String.format("+$%.2f", tx.getAmount()));
        } else {
            tagIndicator.setBackgroundColor(Color.parseColor("#C62828"));
            txtAmount.setTextColor(Color.parseColor("#C62828"));
            txtAmount.setText(String.format("-$%.2f", tx.getAmount()));
        }

        return convertView;
    }
}