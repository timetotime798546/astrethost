package com.quicknotes.app;

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
    private List<Note> noteList;
    private OnNoteActionListener listener;

    public interface OnNoteActionListener {
        void onEdit(Note note);
        void onDelete(Note note);
        void onShare(Note note);
    }

    public NoteAdapter(Context context, List<Note> noteList, OnNoteActionListener listener) {
        this.context = context;
        this.noteList = noteList;
        this.listener = listener;
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
            convertView = LayoutInflater.from(context).inflate(R.layout.note_list_item, parent, false);
        }

        final Note note = noteList.get(position);

        TextView titleView = (TextView) convertView.findViewById(R.id.itemTitle);
        TextView contentView = (TextView) convertView.findViewById(R.id.itemContent);
        TextView tagView = (TextView) convertView.findViewById(R.id.itemTag);
        TextView dateView = (TextView) convertView.findViewById(R.id.itemDate);
        Button btnEdit = (Button) convertView.findViewById(R.id.itemBtnEdit);
        Button btnDelete = (Button) convertView.findViewById(R.id.itemBtnDelete);
        Button btnShare = (Button) convertView.findViewById(R.id.itemBtnShare);

        titleView.setText(note.getTitle());
        contentView.setText(note.getContent());
        dateView.setText(note.getTimestamp());

        String category = note.getCategory();
        tagView.setText(category);

        // Styling based on category tags
        if ("Work".equalsIgnoreCase(category)) {
            tagView.setBackgroundResource(R.drawable.tag_work_bg);
            tagView.setTextColor(context.getResources().getColor(R.color.tag_work));
        } else if ("Personal".equalsIgnoreCase(category)) {
            tagView.setBackgroundResource(R.drawable.tag_personal_bg);
            tagView.setTextColor(context.getResources().getColor(R.color.tag_personal));
        } else {
            tagView.setBackgroundResource(R.drawable.tag_idea_bg);
            tagView.setTextColor(context.getResources().getColor(R.color.tag_idea));
        }

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

        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onShare(note);
                }
            }
        });

        return convertView;
    }
}