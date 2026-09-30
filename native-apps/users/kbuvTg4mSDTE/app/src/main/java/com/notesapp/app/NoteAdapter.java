package com.notesapp.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class NoteAdapter extends ArrayAdapter<Note> {

    public NoteAdapter(Context context, List<Note> notes) {
        super(context, 0, notes);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Note note = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_note, parent, false);
        }

        // Lookup view for data population
        TextView tvTitle = (TextView) convertView.findViewById(R.id.noteTitleTextView);
        TextView tvCategory = (TextView) convertView.findViewById(R.id.noteCategoryTextView);
        TextView tvContent = (TextView) convertView.findViewById(R.id.noteContentTextView);

        // Populate the data into the template view using the data object
        if (note != null) {
            tvTitle.setText(note.getTitle());
            tvCategory.setText(note.getCategory());
            tvContent.setText(note.getContent());
        }

        // Return the completed view to render on screen
        return convertView;
    }
}