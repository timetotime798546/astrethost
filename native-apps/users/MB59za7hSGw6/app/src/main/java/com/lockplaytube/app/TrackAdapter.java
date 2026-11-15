package com.lockplaytube.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class TrackAdapter extends BaseAdapter {

    public interface OnTrackSelectListener {
        void onTrackSelected(MediaTrack track);
    }

    private Context context;
    private List<MediaTrack> tracks;
    private OnTrackSelectListener listener;
    private MediaTrack currentlyPlayingTrack;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    public TrackAdapter(Context context, List<MediaTrack> tracks, OnTrackSelectListener listener) {
        this.context = context;
        this.tracks = tracks;
        this.listener = listener;
    }

    public void setCurrentlyPlayingTrack(MediaTrack track) {
        this.currentlyPlayingTrack = track;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return tracks.size();
    }

    @Override
    public Object getItem(int position) {
        return tracks.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_track, parent, false);
            holder = new ViewHolder();
            holder.imgThumb = (ImageView) convertView.findViewById(R.id.img_track_thumb);
            holder.txtTitle = (TextView) convertView.findViewById(R.id.txt_track_title);
            holder.txtArtist = (TextView) convertView.findViewById(R.id.txt_track_artist);
            holder.txtDuration = (TextView) convertView.findViewById(R.id.txt_track_duration);
            holder.txtCategory = (TextView) convertView.findViewById(R.id.txt_track_category);
            holder.viewPlayingBadge = convertView.findViewById(R.id.view_playing_badge);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final MediaTrack track = tracks.get(position);
        holder.txtTitle.setText(track.getTitle());
        holder.txtArtist.setText(track.getArtist());
        holder.txtDuration.setText(track.getDurationStr());
        holder.txtCategory.setText(track.getCategory());

        boolean isCurrent = currentlyPlayingTrack != null && currentlyPlayingTrack.getId().equals(track.getId());
        holder.viewPlayingBadge.setVisibility(isCurrent ? View.VISIBLE : View.GONE);

        holder.imgThumb.setImageResource(R.drawable.ic_play);
        if (track.getImageUrl() != null && !track.getImageUrl().isEmpty()) {
            final String imgUrl = track.getImageUrl();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        URL url = new URL(imgUrl);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setConnectTimeout(5000);
                        conn.setReadTimeout(5000);
                        InputStream is = conn.getInputStream();
                        final Bitmap bmp = BitmapFactory.decodeStream(is);
                        is.close();
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (bmp != null) {
                                    holder.imgThumb.setImageBitmap(bmp);
                                }
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onTrackSelected(track);
                }
            }
        });

        return convertView;
    }

    private static class ViewHolder {
        ImageView imgThumb;
        TextView txtTitle;
        TextView txtArtist;
        TextView txtDuration;
        TextView txtCategory;
        View viewPlayingBadge;
    }
}