package com.modernexpensetracker.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public class CategoryReportAdapter extends BaseAdapter {

    private final Context context;
    private final List<CategorySummary> summaries;

    public CategoryReportAdapter(Context context, List<CategorySummary> summaries) {
        this.context = context;
        this.summaries = summaries;
    }

    @Override
    public int getCount() {
        return summaries.size();
    }

    @Override
    public Object getItem(int position) {
        return summaries.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_category_report, parent, false);
        }

        CategorySummary summary = summaries.get(position);

        TextView tvIcon = (TextView) convertView.findViewById(R.id.tv_cat_icon);
        TextView tvName = (TextView) convertView.findViewById(R.id.tv_cat_name);
        TextView tvAmount = (TextView) convertView.findViewById(R.id.tv_cat_amount);
        ProgressBar pbPercent = (ProgressBar) convertView.findViewById(R.id.pb_cat_percent);
        TextView tvPercentText = (TextView) convertView.findViewById(R.id.tv_cat_percent_text);

        tvIcon.setText(CalculationHelper.getCategoryIcon(summary.getCategory()));
        tvName.setText(summary.getCategory());
        tvAmount.setText(String.format(Locale.US, "$%.2f", summary.getTotalAmount()));
        pbPercent.setProgress((int) Math.round(summary.getPercentage()));
        tvPercentText.setText(String.format(Locale.US, "%.1f%%", summary.getPercentage()));

        return convertView;
    }
}