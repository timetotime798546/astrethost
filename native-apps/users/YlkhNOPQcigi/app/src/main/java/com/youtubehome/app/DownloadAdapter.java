package com.youtubehome.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class DownloadAdapter extends ArrayAdapter<DownloadItem> {

    public DownloadAdapter(Context context, List<DownloadItem> items) {
        super(context, 0, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DownloadItem item = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_download, parent, false);
        }

        TextView tvFileName = (TextView) convertView.findViewById(R.id.tvFileName);
        TextView tvFileDetails = (TextView) convertView.findViewById(R.id.tvFileDetails);

        if (item != null) {
            tvFileName.setText(item.getFileName());
            tvFileDetails.setText("Date: " + item.getTimestamp() + " | Status: " + item.getStatus());
        }

        return convertView;
    }
}