package com.cloudnotespro.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class NoteAdapter extends BaseAdapter {

    private final Context context;
    private final List<NoteModel> noteList;
    private final LayoutInflater inflater;

    public NoteAdapter(Context context, List<NoteModel> noteList) {
        this.context = context;
        this.noteList = noteList;
        this.inflater = LayoutInflater.from(context);
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
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_note, parent, false);
        }

        NoteModel note = noteList.get(position);

        TextView tvTitle = convertView.findViewById(R.id.tvNoteTitle);
        TextView tvCategory = convertView.findViewById(R.id.tvNoteCategory);
        TextView tvContent = convertView.findViewById(R.id.tvNoteContent);

        tvTitle.setText(note.getTitle());
        tvCategory.setText(note.getCategory() != null && !note.getCategory().isEmpty() ? note.getCategory() : "General");
        tvContent.setText(note.getContent());

        return convertView;
    }
}