package com.smartnotes.app;

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

    public void updateList(List<Note> newList) {
        this.notesList = newList;
        notifyDataSetChanged();
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
            convertView = LayoutInflater.from(context).inflate(R.layout.layout_note_item, parent, false);
        }

        final Note note = notesList.get(position);

        TextView tvTitle = (TextView) convertView.findViewById(R.id.tvNoteTitle);
        TextView tvCategory = (TextView) convertView.findViewById(R.id.tvNoteCategory);
        TextView tvContent = (TextView) convertView.findViewById(R.id.tvNoteContent);
        TextView tvDate = (TextView) convertView.findViewById(R.id.tvNoteDate);
        Button btnEdit = (Button) convertView.findViewById(R.id.btnEditNote);
        Button btnDelete = (Button) convertView.findViewById(R.id.btnDeleteNote);

        tvTitle.setText(note.title);
        tvCategory.setText(note.category);
        
        if ("Work".equals(note.category)) {
            tvCategory.setBackgroundColor(0xFFE3F2FD); 
            tvCategory.setTextColor(0xFF0D47A1);
        } else if ("Personal".equals(note.category)) {
            tvCategory.setBackgroundColor(0xFFE8F5E9); 
            tvCategory.setTextColor(0xFF1B5E20);
        } else if ("Ideas".equals(note.category)) {
            tvCategory.setBackgroundColor(0xFFFFF3E0); 
            tvCategory.setTextColor(0xFFE65100);
        } else if ("Important".equals(note.category)) {
            tvCategory.setBackgroundColor(0xFFFFEBEE); 
            tvCategory.setTextColor(0xFFB71C1C);
        } else {
            tvCategory.setBackgroundColor(0xFFF5F5F5); 
            tvCategory.setTextColor(0xFF424242);
        }

        String previewText = note.content;
        if (previewText != null && previewText.length() > 80) {
            previewText = previewText.substring(0, 77) + "...";
        }
        tvContent.setText(previewText);
        tvDate.setText(note.timestamp);

        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoundManager.playClick();
                if (actionListener != null) {
                    actionListener.onEdit(note);
                }
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoundManager.playClick();
                if (actionListener != null) {
                    actionListener.onDelete(note);
                }
            }
        });

        return convertView;
    }
}