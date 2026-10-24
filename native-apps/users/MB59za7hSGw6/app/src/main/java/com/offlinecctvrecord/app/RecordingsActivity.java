package com.offlinecctvrecord.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecordingsActivity extends Activity {

    private ListView lvRecordings;
    private TextView tvTotalSummary;
    private TextView tvFreeDiskSummary;
    private TextView tvEmptyState;
    private Button btnClearAll;

    private List<RecordingModel> recordingsList = new ArrayList<RecordingModel>();
    private RecordingsAdapter adapter;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recordings);

        lvRecordings = (ListView) findViewById(R.id.lv_recordings);
        tvTotalSummary = (TextView) findViewById(R.id.tv_total_summary);
        tvFreeDiskSummary = (TextView) findViewById(R.id.tv_free_disk_summary);
        tvEmptyState = (TextView) findViewById(R.id.tv_empty_state);
        btnClearAll = (Button) findViewById(R.id.btn_clear_all);

        adapter = new RecordingsAdapter(this, recordingsList);
        lvRecordings.setAdapter(adapter);

        lvRecordings.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                RecordingModel model = recordingsList.get(position);
                Intent intent = new Intent(RecordingsActivity.this, VideoPlayerActivity.class);
                intent.putExtra("video_path", model.getFile().getAbsolutePath());
                intent.putExtra("video_title", model.getName());
                startActivity(intent);
            }
        });

        btnClearAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recordingsList.isEmpty()) {
                    Toast.makeText(RecordingsActivity.this, "Archive is already empty.", Toast.LENGTH_SHORT).show();
                    return;
                }
                confirmPurgeAll();
            }
        });

        loadRecordings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecordings();
    }

    private void loadRecordings() {
        recordingsList.clear();
        List<RecordingModel> items = StorageHelper.getSavedRecordings(this);
        recordingsList.addAll(items);
        adapter.notifyDataSetChanged();

        if (recordingsList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            lvRecordings.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            lvRecordings.setVisibility(View.VISIBLE);
        }

        long totalBytes = StorageHelper.getTotalCctvBytes(this);
        long freeBytes = StorageHelper.getAvailableInternalDiskSpace(this);

        tvTotalSummary.setText("Archive: " + recordingsList.size() + " clips (" + StorageHelper.formatBytes(totalBytes) + ")");
        tvFreeDiskSummary.setText("Free Disk: " + StorageHelper.formatBytes(freeBytes));
    }

    private void confirmPurgeAll() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Purge CCTV Archive");
        builder.setMessage("Are you sure you want to permanently delete ALL offline CCTV footage? This cannot be undone.");
        builder.setPositiveButton("Purge All", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                File dir = StorageHelper.getCctvDirectory(RecordingsActivity.this);
                File[] files = dir.listFiles();
                if (files != null) {
                    for (int i = 0; i < files.length; i++) {
                        files[i].delete();
                    }
                }
                Toast.makeText(RecordingsActivity.this, "All CCTV clips removed.", Toast.LENGTH_SHORT).show();
                loadRecordings();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private class RecordingsAdapter extends BaseAdapter {
        private Context mContext;
        private List<RecordingModel> mList;
        private LayoutInflater inflater;

        public RecordingsAdapter(Context context, List<RecordingModel> list) {
            this.mContext = context;
            this.mList = list;
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Object getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_recording, parent, false);
                holder = new ViewHolder();
                holder.tvName = (TextView) convertView.findViewById(R.id.tv_item_name);
                holder.tvMeta = (TextView) convertView.findViewById(R.id.tv_item_meta);
                holder.btnDelete = (Button) convertView.findViewById(R.id.btn_item_delete);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final RecordingModel item = mList.get(position);
            holder.tvName.setText(item.getName());
            String dateStr = dateFormat.format(new Date(item.getLastModified()));
            holder.tvMeta.setText(dateStr + " • " + item.getFormattedSize());

            holder.btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setTitle("Delete Clip");
                    builder.setMessage("Delete " + item.getName() + " permanently?");
                    builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (item.getFile().exists()) {
                                item.getFile().delete();
                            }
                            loadRecordings();
                        }
                    });
                    builder.setNegativeButton("Cancel", null);
                    builder.show();
                }
            });

            return convertView;
        }

        private class ViewHolder {
            TextView tvName;
            TextView tvMeta;
            Button btnDelete;
        }
    }
}