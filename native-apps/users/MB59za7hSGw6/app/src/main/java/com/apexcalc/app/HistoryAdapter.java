package com.apexcalc.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends BaseAdapter {

    public interface OnHistorySelectedListener {
        void onSelectHistory(HistoryItem item);
    }

    private Context context;
    private List<HistoryItem> items;
    private LayoutInflater inflater;
    private SimpleDateFormat timeFormat;
    private OnHistorySelectedListener listener;

    public HistoryAdapter(Context context, List<HistoryItem> items, OnHistorySelectedListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
        this.inflater = LayoutInflater.from(context);
        this.timeFormat = new SimpleDateFormat("MMM d • h:mm a", Locale.getDefault());
    }

    public void updateList(List<HistoryItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items != null ? items.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return items != null ? items.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_history, parent, false);
            holder = new ViewHolder();
            holder.tvExpression = (TextView) convertView.findViewById(R.id.tv_item_expression);
            holder.tvResult = (TextView) convertView.findViewById(R.id.tv_item_result);
            holder.tvTime = (TextView) convertView.findViewById(R.id.tv_item_time);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final HistoryItem item = items.get(position);
        holder.tvExpression.setText(item.getExpression());
        holder.tvResult.setText("= " + item.getResult());
        holder.tvTime.setText(timeFormat.format(new Date(item.getTimestamp())));

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onSelectHistory(item);
                }
            }
        });

        return convertView;
    }

    static class ViewHolder {
        TextView tvExpression;
        TextView tvResult;
        TextView tvTime;
    }
}