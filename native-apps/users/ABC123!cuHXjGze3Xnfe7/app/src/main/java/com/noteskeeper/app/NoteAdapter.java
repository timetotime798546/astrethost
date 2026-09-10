package com.noteskeeper.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.List;

public class NoteAdapter extends BaseAdapter {
    private Context context;
    private List<Note> notesList;
    private OnNoteActionListener actionListener;

    public interface OnNoteActionListener {
        void onEdit(Note note);
        void onDelete(Note note);
    }

    public NoteAdapter(Context context, List<Note> notesList, OnNoteActionListener actionListener) {
        this.context = context;
        this.notesList = notesList;
        this.actionListener = actionListener;
    }

    @Override
    public int getCount() {
        return notesList.size();
    }

    @Override
    public Object getItem(int position) {
        return notesList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return notesList.get(position).id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.note_item, parent, false);
        }

        final Note note = notesList.get(position);

        TextView txtTitle = convertView.findViewById(R.id.note_item_title);
        TextView txtSnippet = convertView.findViewById(R.id.note_item_snippet);
        TextView txtCategory = convertView.findViewById(R.id.note_item_category);
        TextView txtDate = convertView.findViewById(R.id.note_item_date);
        TextView btnEdit = convertView.findViewById(R.id.btn_edit_note_action);
        TextView btnDelete = convertView.findViewById(R.id.btn_delete_note_action);

        txtTitle.setText(note.title);
        txtSnippet.setText(note.content);
        txtCategory.setText(note.category);
        
        // Simple display date formatting
        String displayDate = note.createdAt;
        if (displayDate != null && displayDate.length() > 16) {
            displayDate = displayDate.substring(0, 16);
        }
        txtDate.setText(displayDate);

        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (actionListener != null) {
                    actionListener.onEdit(note);
                }
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (actionListener != null) {
                    actionListener.onDelete(note);
                }
            }
        });

        return convertView;
    }
}