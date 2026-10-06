package com.notesphere.app;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.note_list_item, parent, false);
        }

        Note note = notes.get(position);

        TextView titleText = (TextView) convertView.findViewById(R.id.text_note_title);
        TextView categoryText = (TextView) convertView.findViewById(R.id.text_note_category);
        TextView previewText = (TextView) convertView.findViewById(R.id.text_note_preview);
        TextView dateText = (TextView) convertView.findViewById(R.id.text_note_date);

        titleText.setText(note.getTitle());
        categoryText.setText(note.getCategory());
        
        String content = note.getContent();
        if (content.length() > 60) {
            previewText.setText(content.substring(0, 57) + "...");
        } else {
            previewText.setText(content);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        long tsValue = System.currentTimeMillis();
        try {
            tsValue = Long.parseLong(note.getTimestamp());
        } catch (Exception ignored) {}
        dateText.setText(sdf.format(new Date(tsValue)));

        int bgAccent;
        int textAccent;
        String cat = note.getCategory().toLowerCase();

        if (cat.equals("work")) {
            bgAccent = 0xFFE3F2FD; // light blue
            textAccent = 0xFF1E88E5;
        } else if (cat.equals("personal")) {
            bgAccent = 0xFFF1F8E9; // light green
            textAccent = 0xFF7CB342;
        } else if (cat.equals("ideas")) {
            bgAccent = 0xFFFFFDE7; // light yellow
            textAccent = 0xFFFBC02D;
        } else if (cat.equals("todo")) {
            bgAccent = 0xFFFFEBEE; // light red
            textAccent = 0xFFE53935;
        } else {
            bgAccent = 0xFFF5F5F5; // light grey
            textAccent = 0xFF757575;
        }

        categoryText.setTextColor(textAccent);
        GradientDrawable gd = (GradientDrawable) categoryText.getBackground().mutate();
        gd.setColor(bgAccent);

        return convertView;
    }
}