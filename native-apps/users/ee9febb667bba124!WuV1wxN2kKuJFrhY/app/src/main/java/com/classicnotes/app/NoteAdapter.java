package com.classicnotes.app;

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
    private LayoutInflater inflater;

    public NoteAdapter(Context context, List<Note> notes) {
        this.context = context;
        this.notes = notes;
        this.inflater = LayoutInflater.from(context);
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
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.note_list_item, parent, false);
            holder = new ViewHolder();
            holder.title = (TextView) convertView.findViewById(R.id.note_title);
            holder.snippet = (TextView) convertView.findViewById(R.id.note_snippet);
            holder.category = (TextView) convertView.findViewById(R.id.note_category_badge);
            holder.date = (TextView) convertView.findViewById(R.id.note_date);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Note note = notes.get(position);
        holder.title.setText(note.getTitle());
        
        String content = note.getContent();
        if (content.length() > 80) {
            holder.snippet.setText(content.substring(0, 77) + "...");
        } else {
            holder.snippet.setText(content);
        }
        
        holder.category.setText(note.getCategoryName());
        holder.date.setText(note.getDate());

        return convertView;
    }

    private static class ViewHolder {
        TextView title;
        TextView snippet;
        TextView category;
        TextView date;
    }
}