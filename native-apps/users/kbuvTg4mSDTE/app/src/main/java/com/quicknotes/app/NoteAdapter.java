package com.quicknotes.app;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class NoteAdapter extends BaseAdapter {

    private Context context;
    private List<Note> notes;
    private OnNoteActionListener listener;

    public interface OnNoteActionListener {
        void onEdit(Note note);
        void onDelete(Note note);
    }

    public NoteAdapter(Context context, List<Note> notes, OnNoteActionListener listener) {
        this.context = context;
        this.notes = notes;
        this.listener = listener;
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
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.note_item, parent, false);
        }

        final Note note = notes.get(position);

        TextView categoryText = convertView.findViewById(R.id.note_category);
        TextView titleText = convertView.findViewById(R.id.note_title);
        TextView contentText = convertView.findViewById(R.id.note_content);
        TextView timestampText = convertView.findViewById(R.id.note_timestamp);
        Button btnEdit = convertView.findViewById(R.id.btn_edit);
        Button btnDelete = convertView.findViewById(R.id.btn_delete);

        categoryText.setText(note.getCategory());
        titleText.setText(note.getTitle());
        contentText.setText(note.getContent());

        // Format date/time
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(note.getTimestamp());
        String date = DateFormat.format("MMM dd, yyyy hh:mm a", cal).toString();
        timestampText.setText(date);

        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onEdit(note);
                }
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onDelete(note);
                }
            }
        });

        return convertView;
    }
}