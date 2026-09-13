package com.notesmaster.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import java.util.List;

public class NoteAdapter extends BaseAdapter {
    private Context context;
    private List<Note> notes;
    private OnNoteDeleteListener deleteListener;

    public interface OnNoteDeleteListener {
        void onDelete(Note note);
    }

    public NoteAdapter(Context context, List<Note> notes, OnNoteDeleteListener deleteListener) {
        this.context = context;
        this.notes = notes;
        this.deleteListener = deleteListener;
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

        final Note note = notes.get(position);

        TextView titleView = (TextView) convertView.findViewById(R.id.note_title);
        TextView categoryView = (TextView) convertView.findViewById(R.id.note_category);
        TextView contentView = (TextView) convertView.findViewById(R.id.note_content);
        TextView timestampView = (TextView) convertView.findViewById(R.id.note_timestamp);
        Button deleteBtn = (Button) convertView.findViewById(R.id.delete_btn);

        titleView.setText(note.getTitle());
        categoryView.setText(note.getCategory());
        contentView.setText(note.getContent());
        timestampView.setText(note.getTimestamp());

        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (deleteListener != null) {
                    deleteListener.onDelete(note);
                }
            }
        });

        return convertView;
    }
}