package com.expensetracker.app;

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
        void onDelete(TransactionModel transaction);
    }

    private final Context context;
    private final List<TransactionModel> items;
    private final LayoutInflater inflater;
    private final OnDeleteClickListener deleteClickListener;

    public TransactionAdapter(Context context, List<TransactionModel> items, OnDeleteClickListener deleteClickListener) {
        this.context = context;
        this.items = items;
        this.deleteClickListener = deleteClickListener;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return items != null ? items.size() : 0;
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
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_transaction, parent, false);
            holder = new ViewHolder();
            holder.tvBadge = (TextView) convertView.findViewById(R.id.tvItemCategoryBadge);
            holder.tvTitle = (TextView) convertView.findViewById(R.id.tvItemTitle);
            holder.tvSubtitle = (TextView) convertView.findViewById(R.id.tvItemSubtitle);
            holder.tvAmount = (TextView) convertView.findViewById(R.id.tvItemAmount);
            holder.btnDelete = (TextView) convertView.findViewById(R.id.btnItemDelete);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final TransactionModel item = items.get(position);

        holder.tvTitle.setText(item.getTitle());
        holder.tvSubtitle.setText(item.getCategory() + " • " + item.getDate());

        if (item.isIncome()) {
            holder.tvBadge.setText("💰");
            holder.tvAmount.setText(String.format(Locale.US, "+$%.2f", item.getAmount()));
            holder.tvAmount.setTextColor(Color.parseColor("#10B981"));
        } else {
            holder.tvBadge.setText(getCategoryEmoji(item.getCategory()));
            holder.tvAmount.setText(String.format(Locale.US, "-$%.2f", item.getAmount()));
            holder.tvAmount.setTextColor(Color.parseColor("#EF4444"));
        }

        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (deleteClickListener != null) {
                    deleteClickListener.onDelete(item);
                }
            }
        });

        return convertView;
    }

    private String getCategoryEmoji(String category) {
        if (category == null) return "💳";
        if (category.contains("Food")) return "🍔";
        if (category.contains("Transport")) return "🚗";
        if (category.contains("Housing")) return "🏠";
        if (category.contains("Shopping")) return "🛍️";
        if (category.contains("Entertainment")) return "🎬";
        if (category.contains("Healthcare")) return "💊";
        if (category.contains("Utilities")) return "⚡";
        return "💳";
    }

    private static class ViewHolder {
        TextView tvBadge;
        TextView tvTitle;
        TextView tvSubtitle;
        TextView tvAmount;
        TextView btnDelete;
    }
}