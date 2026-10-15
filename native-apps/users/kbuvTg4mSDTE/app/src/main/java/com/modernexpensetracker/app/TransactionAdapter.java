package com.modernexpensetracker.app;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends BaseAdapter {

    public interface OnDeleteClickListener {
        void onDeleteClick(TransactionModel transaction);
    }

    private final Context context;
    private final List<TransactionModel> items;
    private final OnDeleteClickListener deleteListener;

    public TransactionAdapter(Context context, List<TransactionModel> items, OnDeleteClickListener deleteListener) {
        this.context = context;
        this.items = items;
        this.deleteListener = deleteListener;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        }

        final TransactionModel transaction = items.get(position);

        TextView tvIcon = (TextView) convertView.findViewById(R.id.tv_item_icon);
        TextView tvTitle = (TextView) convertView.findViewById(R.id.tv_item_title);
        TextView tvMeta = (TextView) convertView.findViewById(R.id.tv_item_meta);
        TextView tvAmount = (TextView) convertView.findViewById(R.id.tv_item_amount);
        TextView btnDelete = (TextView) convertView.findViewById(R.id.btn_delete_item);

        tvTitle.setText(transaction.getTitle());
        String metaText = transaction.getCategory() + " • " + transaction.getDate();
        if (!transaction.getNotes().isEmpty()) {
            metaText += " • " + transaction.getNotes();
        }
        tvMeta.setText(metaText);

        tvIcon.setText(CalculationHelper.getCategoryIcon(transaction.getCategory()));

        if (transaction.isExpense()) {
            tvAmount.setText(String.format(Locale.US, "-$%.2f", transaction.getAmount()));
            tvAmount.setTextColor(Color.parseColor("#EF4444"));
            tvIcon.setBackgroundResource(R.drawable.bg_expense_badge);
        } else {
            tvAmount.setText(String.format(Locale.US, "+$%.2f", transaction.getAmount()));
            tvAmount.setTextColor(Color.parseColor("#10B981"));
            tvIcon.setBackgroundResource(R.drawable.bg_income_badge);
        }

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (deleteListener != null) {
                    deleteListener.onDeleteClick(transaction);
                }
            }
        });

        return convertView;
    }
}