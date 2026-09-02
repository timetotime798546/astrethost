package com.notekeep.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.List;

public class NoteListAdapter extends BaseAdapter {
    private Context context;
    private List<Note> noteList;
    private LayoutInflater inflater;

    public NoteListAdapter(Context context, List<Note> noteList) {
        this.context = context;
        this.noteList = noteList;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return noteList.size();
    }

    @Override
    public Object getItem(int position) {
        return noteList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return noteList.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_note, parent, false);
        }

        TextView tvTitle = (TextView) convertView.findViewById(R.id.tv_note_title);
        TextView tvSnippet = (TextView) convertView.findViewById(R.id.tv_note_snippet);
        TextView tvCategory = (TextView) convertView.findViewById(R.id.tv_note_category);
        TextView tvTime = (TextView) convertView.findViewById(R.id.tv_note_time);

        Note note = noteList.get(position);

        tvTitle.setText(note.getTitle());
        tvSnippet.setText(note.getContent());
        tvTime.setText(note.getTimestamp());

        String catName = note.getCategoryName();
        if (catName != null && !catName.isEmpty()) {
            tvCategory.setText(catName);
            tvCategory.setVisibility(View.VISIBLE);
        } else {
            tvCategory.setVisibility(View.GONE);
        }

        return convertView;
    }
}