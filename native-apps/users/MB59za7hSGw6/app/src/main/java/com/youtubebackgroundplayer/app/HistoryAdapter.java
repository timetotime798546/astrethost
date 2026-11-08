package com.youtubebackgroundplayer.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends BaseAdapter {

    public interface OnDeleteClickListener {
        void onDeleteClick(HistoryItem item);
    }

    private Context context;
    private List<HistoryItem> items;
    private OnDeleteClickListener deleteClickListener;
    private SimpleDateFormat dateFormat;

    public HistoryAdapter(Context context, List<HistoryItem> items, OnDeleteClickListener listener) {
        this.context = context;
        this.items = items;
        this.deleteClickListener = listener;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
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
        return items.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);
            holder = new ViewHolder();
            holder.tvTitle = convertView.findViewById(R.id.tv_item_title);
            holder.tvUrl = convertView.findViewById(R.id.tv_item_url);
            holder.tvDate = convertView.findViewById(R.id.tv_item_date);
            holder.btnDelete = convertView.findViewById(R.id.btn_delete_item);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final HistoryItem item = items.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvUrl.setText(item.getUrl());
        holder.tvDate.setText(dateFormat.format(new Date(item.getTimestamp())));

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

    static class ViewHolder {
        TextView tvTitle;
        TextView tvUrl;
        TextView tvDate;
        ImageButton btnDelete;
    }
}