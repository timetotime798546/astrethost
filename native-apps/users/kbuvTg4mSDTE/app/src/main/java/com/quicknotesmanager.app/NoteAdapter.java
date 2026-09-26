package com.quicknotesmanager.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.List;

public class NoteAdapter extends BaseAdapter {
    private Context context;
    private List<Note> notes;

    public NoteAdapter(Context context, List<Note> notes) {
        this.context = context;
        this.notes = notes;
    }

    public void updateData(List<Note> newNotes) {
        this.notes = newNotes;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return notes.size();
    }

    @Override
    public Object getItem(int position) {
        return notes.get(position);
    }

    @Override
    public long getItemId(int position) {
        return notes.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.note_item, parent, false);
        }

        Note note = notes.get(position);

        TextView title = (TextView) convertView.findViewById(R.id.note_title);
        TextView preview = (TextView) convertView.findViewById(R.id.note_preview);
        TextView category = (TextView) convertView.findViewById(R.id.note_category);
        TextView date = (TextView) convertView.findViewById(R.id.note_date);

        title.setText(note.getTitle());
        
        String contentText = note.getContent();
        if (contentText.length() > 60) {
            contentText = contentText.substring(0, 57) + "...";
        }
        preview.setText(contentText);
        
        category.setText(note.getCategoryName());
        date.setText(note.getCreatedAt());

        return convertView;
    }
}