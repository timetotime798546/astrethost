package com.expensetrackerpro.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.List;

public class CategoryReportAdapter extends BaseAdapter {
    private Context context;
    private List<CategoryReport> list;

    public CategoryReportAdapter(Context context, List<CategoryReport> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_report, parent, false);
        }

        CategoryReport report = list.get(position);

        TextView txtCatName = convertView.findViewById(R.id.txt_report_cat_name);
        TextView txtCatAmount = convertView.findViewById(R.id.txt_report_cat_amount);
        ProgressBar progressBar = convertView.findViewById(R.id.progress_report_bar);
        TextView txtPercentage = convertView.findViewById(R.id.txt_report_cat_percentage);

        txtCatName.setText(report.getCategory());
        txtCatAmount.setText(String.format("$%.2f", report.getAmount()));
        progressBar.setProgress(report.getPercentage());
        txtPercentage.setText(report.getPercentage() + "%");

        return convertView;
    }
}