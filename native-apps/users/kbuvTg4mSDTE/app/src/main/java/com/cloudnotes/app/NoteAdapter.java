package com.cloudnotes.app;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class NoteAdapter extends BaseAdapter {
    private final Context context;
    private final List<Note> originalList;
    private final List<Note> filteredList;
    private final OnNoteDeleteListener deleteListener;

    public interface OnNoteDeleteListener {
        void onDelete(Note note);
    }

    public NoteAdapter(Context context, List<Note> list, OnNoteDeleteListener deleteListener) {
        this.context = context;
        this.originalList = list;
        this.filteredList = new ArrayList<>(list);
        this.deleteListener = deleteListener;
    }

    @Override
    public int getCount() {
        return filteredList.size();
    }

    @Override
    public Note getItem(int position) {
        return filteredList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.note_item, parent, false);
        }

        final Note note = getItem(position);

        TextView tvTitle = (TextView) convertView.findViewById(R.id.itemTitle);
        TextView tvCategoryChip = (TextView) convertView.findViewById(R.id.itemCategoryChip);
        TextView tvContent = (TextView) convertView.findViewById(R.id.itemContent);
        TextView tvWordCount = (TextView) convertView.findViewById(R.id.itemWordCount);
        TextView tvDate = (TextView) convertView.findViewById(R.id.itemDate);
        Button btnDelete = (Button) convertView.findViewById(R.id.btnItemDelete);

        tvTitle.setText(note.getTitle());
        tvContent.setText(note.getContent());
        tvDate.setText(note.getDate());

        // Local dynamic word count calculation (Mandatory local calculation requirement)
        int words = note.getWordCount();
        tvWordCount.setText(words + (words == 1 ? " word" : " words"));

        // Category style color selection
        String category = note.getCategory().toUpperCase();
        tvCategoryChip.setText(category);
        if (category.equals("WORK")) {
            tvCategoryChip.setBackgroundColor(Color.parseColor("#1E88E5"));
        } else if (category.equals("PERSONAL")) {
            tvCategoryChip.setBackgroundColor(Color.parseColor("#43A047"));
        } else if (category.equals("IDEAS")) {
            tvCategoryChip.setBackgroundColor(Color.parseColor("#8E24AA"));
        } else {
            tvCategoryChip.setBackgroundColor(Color.parseColor("#FFB300"));
        }

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (deleteListener != null) {
                    deleteListener.onDelete(note);
                }
            }
        });

        return convertView;
    }

    public void filter(String category, String query) {
        filteredList.clear();
        String lowerQuery = query.toLowerCase().trim();

        for (Note note : originalList) {
            boolean matchesCategory = category.equalsIgnoreCase("ALL") || note.getCategory().equalsIgnoreCase(category);
            boolean matchesQuery = lowerQuery.isEmpty() ||
                    note.getTitle().toLowerCase().contains(lowerQuery) ||
                    note.getContent().toLowerCase().contains(lowerQuery);

            if (matchesCategory && matchesQuery) {
                filteredList.add(note);
            }
        }
        notifyDataSetChanged();
    }
}