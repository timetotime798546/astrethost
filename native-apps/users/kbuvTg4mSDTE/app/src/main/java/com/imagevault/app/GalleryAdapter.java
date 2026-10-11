package com.imagevault.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class GalleryAdapter extends BaseAdapter {

    public interface OnItemClickListener {
        void onItemClick(String imageId);
    }

    private final Context context;
    private final String token;
    private final List<String> imageIds = new ArrayList<String>();
    private final LruCache<String, Bitmap> memoryCache;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private OnItemClickListener listener;

    public GalleryAdapter(Context context, String token) {
        this.context = context;
        this.token = token;

        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;
        this.memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<String> ids) {
        imageIds.clear();
        if (ids != null) {
            imageIds.addAll(ids);
        }
        notifyDataSetChanged();
    }

    public void addItem(String id) {
        imageIds.add(0, id);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return imageIds.size();
    }

    @Override
    public Object getItem(int position) {
        return imageIds.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private static class ViewHolder {
        ImageView imageView;
        ProgressBar progressBar;
        TextView tvId;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_gallery_image, parent, false);
            holder = new ViewHolder();
            holder.imageView = (ImageView) convertView.findViewById(R.id.iv_thumbnail);
            holder.progressBar = (ProgressBar) convertView.findViewById(R.id.item_progress);
            holder.tvId = (TextView) convertView.findViewById(R.id.tv_image_id);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final String imageId = imageIds.get(position);
        holder.tvId.setText(imageId);
        holder.imageView.setImageBitmap(null);
        holder.imageView.setTag(imageId);

        Bitmap cached = memoryCache.get(imageId);
        if (cached != null) {
            holder.progressBar.setVisibility(View.GONE);
            holder.imageView.setImageBitmap(cached);
        } else {
            holder.progressBar.setVisibility(View.VISIBLE);
            ImageWorkerApi.fetchImage(token, imageId, new ImageWorkerApi.BitmapCallback() {
                @Override
                public void onSuccess(final Bitmap bitmap) {
                    memoryCache.put(imageId, bitmap);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (imageId.equals(holder.imageView.getTag())) {
                                holder.progressBar.setVisibility(View.GONE);
                                holder.imageView.setImageBitmap(bitmap);
                            }
                        }
                    });
                }

                @Override
                public void onError(String message) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (imageId.equals(holder.imageView.getTag())) {
                                holder.progressBar.setVisibility(View.GONE);
                            }
                        }
                    });
                }
            });
        }

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onItemClick(imageId);
                }
            }
        });

        return convertView;
    }
}