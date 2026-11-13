package com.bgtubeplayer.app;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.Date;
import java.util.List;

public class HistoryAdapter extends BaseAdapter {

    public interface OnDeleteClickListener {
        void onDeleteClick(HistoryItem item);
    }

    private Context context;
    private List<HistoryItem> historyList;
    private OnDeleteClickListener deleteClickListener;

    public HistoryAdapter(Context context, List<HistoryItem> historyList, OnDeleteClickListener listener) {
        this.context = context;
        this.historyList = historyList;
        this.deleteClickListener = listener;
    }

    @Override
    public int getCount() {
        return historyList.size();
    }

    @Override
    public Object getItem(int position) {
        return historyList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return historyList.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);
            holder = new ViewHolder();
            holder.txtTitle = (TextView) convertView.findViewById(R.id.txt_item_title);
            holder.txtUrl = (TextView) convertView.findViewById(R.id.txt_item_url);
            holder.txtDate = (TextView) convertView.findViewById(R.id.txt_item_date);
            holder.btnDelete = (ImageButton) convertView.findViewById(R.id.btn_delete_item);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final HistoryItem item = historyList.get(position);
        holder.txtTitle.setText(item.getTitle());
        holder.txtUrl.setText(item.getUrl());

        String formattedDate = DateFormat.format("MMM dd, yyyy - hh:mm a", new Date(item.getTimestamp())).toString();
        holder.txtDate.setText(formattedDate);

        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (deleteClickListener != null) {
                    deleteClickListener.onDeleteClick(item);
                }
            }
        });

        return convertView;
    }

    private static class ViewHolder {
        TextView txtTitle;
        TextView txtUrl;
        TextView txtDate;
        ImageButton btnDelete;
    }
}