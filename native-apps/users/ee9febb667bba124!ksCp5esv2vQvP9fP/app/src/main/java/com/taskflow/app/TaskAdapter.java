package com.taskflow.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends BaseAdapter {

    private Context ctx;
    private List<DatabaseHelper.TaskItem> rawList;
    private List<DatabaseHelper.TaskItem> filteredList;
    private DatabaseHelper dbHelper;
    private int currentUserId;
    private Runnable refreshCall;

    public TaskAdapter(Context ctx, List<DatabaseHelper.TaskItem> items, int currentUserId, Runnable refreshCall) {
        this.ctx = ctx;
        this.rawList = items;
        this.filteredList = new ArrayList<>(items);
        this.dbHelper = new DatabaseHelper(ctx);
        this.currentUserId = currentUserId;
        this.refreshCall = refreshCall;
    }

    @Override
    public int getCount() {
        return filteredList.size();
    }

    @Override
    public Object getItem(int pos) {
        return filteredList.get(pos);
    }

    @Override
    public long getItemId(int pos) {
        return filteredList.get(pos).id;
    }

    @Override
    public View getView(final int pos, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(ctx).inflate(R.layout.list_item_task, parent, false);
        }

        final DatabaseHelper.TaskItem item = filteredList.get(pos);

        TextView tvTitle = (TextView) convertView.findViewById(R.id.itemTaskTitle);
        TextView tvDesc = (TextView) convertView.findViewById(R.id.itemTaskDesc);
        TextView tvCategory = (TextView) convertView.findViewById(R.id.itemTaskCategory);
        TextView tvDueDate = (TextView) convertView.findViewById(R.id.itemTaskDueDate);
        CheckBox cbComplete = (CheckBox) convertView.findViewById(R.id.itemTaskCheckbox);
        TextView tvStar = (TextView) convertView.findViewById(R.id.itemTaskStar);

        tvTitle.setText(item.title);
        tvDesc.setText(item.description);
        tvCategory.setText(item.category);
        tvDueDate.setText("Due: " + item.dueDate);

        // Check visual line strikes
        cbComplete.setChecked(item.isCompleted);
        if (item.isCompleted) {
            tvTitle.setPaintFlags(tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tvTitle.setTextColor(Color.GRAY);
        } else {
            tvTitle.setPaintFlags(tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            tvTitle.setTextColor(Color.BLACK);
        }

        tvStar.setText(item.isFavorite ? "★" : "☆");

        // Quick actions trigger inside ListView adapter
        cbComplete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                item.isCompleted = !item.isCompleted;
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues val = new ContentValues();
                val.put(DatabaseHelper.ITEM_COL_COMPLETED, item.isCompleted ? 1 : 0);
                db.update(DatabaseHelper.TABLE_ITEMS, val, DatabaseHelper.ITEM_COL_ID + "=?", new String[]{String.valueOf(item.id)});

                dbHelper.logActivity(currentUserId, "Toggled status", "Completed marked " + item.isCompleted + " on item: " + item.title);
                if (refreshCall != null) refreshCall.run();
            }
        });

        tvStar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                item.isFavorite = !item.isFavorite;
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues val = new ContentValues();
                val.put(DatabaseHelper.ITEM_COL_FAVORITE, item.isFavorite ? 1 : 0);
                db.update(DatabaseHelper.TABLE_ITEMS, val, DatabaseHelper.ITEM_COL_ID + "=?", new String[]{String.valueOf(item.id)});

                dbHelper.logActivity(currentUserId, "Toggled Star", "Favorite star toggle: " + item.title);
                if (refreshCall != null) refreshCall.run();
            }
        });

        return convertView;
    }

    public void performSearch(String query) {
        filteredList.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(rawList);
        } else {
            String lower = query.toLowerCase().trim();
            for (DatabaseHelper.TaskItem t : rawList) {
                if (t.title.toLowerCase().contains(lower) || t.description.toLowerCase().contains(lower) || t.category.toLowerCase().contains(lower)) {
                    filteredList.add(t);
                }
            }
        }
        notifyDataSetChanged();
    }
}