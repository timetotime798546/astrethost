package com.bhagwatistock.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

public class HistoryAdapter extends ArrayAdapter<ScanModel> {

    public interface OnScanDeleteListener {
        void onDelete(ScanModel model);
    }

    private Context context;
    private List<ScanModel> scans;
    private OnScanDeleteListener deleteListener;

    public HistoryAdapter(Context context, List<ScanModel> scans, OnScanDeleteListener deleteListener) {
        super(context, 0, scans);
        this.context = context;
        this.scans = scans;
        this.deleteListener = deleteListener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);
        }

        final ScanModel scan = getItem(position);
        if (scan == null) {
            return convertView;
        }

        TextView tvSource = convertView.findViewById(R.id.itemSource);
        TextView tvTimestamp = convertView.findViewById(R.id.itemTimestamp);
        TextView tvTextSnippet = convertView.findViewById(R.id.itemTextSnippet);
        Button btnCopy = convertView.findViewById(R.id.btnItemCopy);
        Button btnShare = convertView.findViewById(R.id.btnItemShare);
        Button btnDelete = convertView.findViewById(R.id.btnItemDelete);

        tvSource.setText(scan.getSource());
        tvTimestamp.setText(scan.getTimestamp());
        tvTextSnippet.setText(scan.getText());

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, ResultActivity.class);
                intent.putExtra("scan_id", scan.getId());
                context.startActivity(intent);
            }
        });

        btnCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("OCR Text", scan.getText());
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, scan.getText());
                context.startActivity(Intent.createChooser(shareIntent, "Share OCR Text"));
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (deleteListener != null) {
                    deleteListener.onDelete(scan);
                }
            }
        });

        return convertView;
    }
}