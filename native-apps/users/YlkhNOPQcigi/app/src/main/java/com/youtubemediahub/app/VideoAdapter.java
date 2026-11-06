package com.youtubemediahub.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

public class VideoAdapter extends ArrayAdapter<VideoItem> {

    private int resourceLayout;
    private Context context;
    private OnVideoActionListener listener;

    public interface OnVideoActionListener {
        void onPlayClick(VideoItem item);
        void onDownloadClick(VideoItem item);
    }

    public VideoAdapter(Context context, int resource, List<VideoItem> items, OnVideoActionListener listener) {
        super(context, resource, items);
        this.resourceLayout = resource;
        this.context = context;
        this.listener = listener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            v = inflater.inflate(resourceLayout, null);
        }

        final VideoItem item = getItem(position);

        if (item != null) {
            TextView textViewTitle = (TextView) v.findViewById(R.id.textViewTitle);
            TextView textViewChannel = (TextView) v.findViewById(R.id.textViewChannel);
            TextView textViewDuration = (TextView) v.findViewById(R.id.textViewDuration);
            TextView textViewCategory = (TextView) v.findViewById(R.id.textViewCategory);
            Button buttonPlay = (Button) v.findViewById(R.id.buttonPlay);
            Button buttonDownload = (Button) v.findViewById(R.id.buttonDownload);

            if (textViewTitle != null) {
                textViewTitle.setText(item.getTitle());
            }

            if (textViewChannel != null) {
                textViewChannel.setText("Channel: " + item.getChannel());
            }

            if (textViewDuration != null) {
                textViewDuration.setText(item.getDuration());
            }

            if (textViewCategory != null) {
                textViewCategory.setText("[" + item.getCategory() + "]");
            }

            if (buttonPlay != null) {
                buttonPlay.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (listener != null) {
                            listener.onPlayClick(item);
                        }
                    }
                });
            }

            if (buttonDownload != null) {
                buttonDownload.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (listener != null) {
                            listener.onDownloadClick(item);
                        }
                    }
                });
            }
        }

        return v;
    }
}