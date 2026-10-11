package com.productimagemanager.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public class ProductAdapter extends BaseAdapter {

    public interface ProductActionCallback {
        void onEdit(Product product);
        void onDelete(Product product);
    }

    private final Context context;
    private final List<Product> productList;
    private final ProductActionCallback callback;
    private final String authToken;
    private final Handler mainHandler;
    private final LruCache<String, Bitmap> memoryCache;

    public ProductAdapter(Context context, List<Product> productList, String authToken, ProductActionCallback callback) {
        this.context = context;
        this.productList = productList;
        this.authToken = authToken;
        this.callback = callback;
        this.mainHandler = new Handler(Looper.getMainLooper());

        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;
        if (cacheSize <= 0) {
            cacheSize = 1024;
        }
        this.memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    @Override
    public int getCount() {
        return productList.size();
    }

    @Override
    public Object getItem(int position) {
        return productList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder {
        ImageView ivProductThumb;
        TextView tvProductName;
        TextView tvProductPrice;
        TextView tvProductDesc;
        TextView tvImageIdTag;
        Button btnEdit;
        Button btnDelete;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
            holder = new ViewHolder();
            holder.ivProductThumb = (ImageView) convertView.findViewById(R.id.ivProductThumb);
            holder.tvProductName = (TextView) convertView.findViewById(R.id.tvProductName);
            holder.tvProductPrice = (TextView) convertView.findViewById(R.id.tvProductPrice);
            holder.tvProductDesc = (TextView) convertView.findViewById(R.id.tvProductDesc);
            holder.tvImageIdTag = (TextView) convertView.findViewById(R.id.tvImageIdTag);
            holder.btnEdit = (Button) convertView.findViewById(R.id.btnEdit);
            holder.btnDelete = (Button) convertView.findViewById(R.id.btnDelete);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final Product product = productList.get(position);

        holder.tvProductName.setText(product.getName());
        holder.tvProductPrice.setText(String.format(Locale.US, "$%.2f", product.getPrice()));
        holder.tvProductDesc.setText(product.getDescription().isEmpty() ? "No description" : product.getDescription());

        final String imageId = product.getImageId();
        if (!imageId.isEmpty()) {
            holder.tvImageIdTag.setText("Image Worker ID: " + imageId);
            holder.ivProductThumb.setTag(imageId);

            Bitmap cached = memoryCache.get(imageId);
            if (cached != null) {
                holder.ivProductThumb.setImageBitmap(cached);
            } else {
                holder.ivProductThumb.setImageResource(android.R.drawable.ic_menu_gallery);
                if (authToken != null && !authToken.isEmpty()) {
                    ImageWorkerApi.fetchImage(imageId, authToken, new ImageWorkerApi.ImageDownloadCallback() {
                        @Override
                        public void onSuccess(final Bitmap bitmap) {
                            memoryCache.put(imageId, bitmap);
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (imageId.equals(holder.ivProductThumb.getTag())) {
                                        holder.ivProductThumb.setImageBitmap(bitmap);
                                    }
                                }
                            });
                        }

                        @Override
                        public void onError(String error) {
                            // Keep placeholder image
                        }
                    });
                }
            }
        } else {
            holder.tvImageIdTag.setText("No Image Attached");
            holder.ivProductThumb.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null) {
                    callback.onEdit(product);
                }
            }
        });

        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null) {
                    callback.onDelete(product);
                }
            }
        });

        return convertView;
    }
}