package com.notescategoriesapp.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NoteAdapter extends ArrayAdapter<Note> {

    private LayoutInflater inflater;

    public NoteAdapter(Context context, List<Note> notes) {
        super(context, 0, notes);
        inflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_note, parent, false);
            holder = new ViewHolder();
            holder.titleTextView = (TextView) convertView.findViewById(R.id.note_title);
            holder.contentTextView = (TextView) convertView.findViewById(R.id.note_content_snippet);
            holder.categoryTextView = (TextView) convertView.findViewById(R.id.note_category);
            holder.timestampTextView = (TextView) convertView.findViewById(R.id.note_timestamp);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Note currentNote = getItem(position);
        if (currentNote != null) {
            holder.titleTextView.setText(currentNote.getTitle());

            // Display a content snippet (e.g., first 100 characters)
            String content = currentNote.getContent();
            if (content.length() > 100) {
                holder.contentTextView.setText(content.substring(0, 100) + "...");
            } else {
                holder.contentTextView.setText(content);
            }

            holder.categoryTextView.setText(currentNote.getCategory());

            // Format timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            holder.timestampTextView.setText(sdf.format(new java.util.Date(currentNote.getTimestamp())));
        }

        return convertView;
    }

    private static class ViewHolder {
        TextView titleTextView;
        TextView contentTextView;
        TextView categoryTextView;
        TextView timestampTextView;
    }
}