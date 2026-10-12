package com.notesapp.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class NoteAdapter extends BaseAdapter {
    private Context context;
    private List<NoteModel> noteList;
    private OnNoteClickListener listener;

    public interface OnNoteClickListener {
        void onNoteClick(NoteModel note);
    }

    public NoteAdapter(Context context, List<NoteModel> noteList, OnNoteClickListener listener) {
        this.context = context;
        this.noteList = noteList;
        this.listener = listener;
    }

    public void updateData(List<NoteModel> newList) {
        this.noteList = newList;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return noteList != null ? noteList.size() : 0;
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
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_note, parent, false);
            holder = new ViewHolder();
            holder.tvCategoryBadge = convertView.findViewById(R.id.tvCategoryBadge);
            holder.tvPinnedIndicator = convertView.findViewById(R.id.tvPinnedIndicator);
            holder.tvNoteWordCount = convertView.findViewById(R.id.tvNoteWordCount);
            holder.tvNoteTitle = convertView.findViewById(R.id.tvNoteTitle);
            holder.tvNoteContent = convertView.findViewById(R.id.tvNoteContent);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final NoteModel note = noteList.get(position);

        holder.tvNoteTitle.setText(note.getTitle().isEmpty() ? "Untitled Note" : note.getTitle());
        holder.tvNoteContent.setText(note.getContent().isEmpty() ? "No additional text" : note.getContent());
        holder.tvNoteWordCount.setText(note.getWordCount() + " words");

        holder.tvCategoryBadge.setText(note.getCategory().toUpperCase());
        
        int badgeColor;
        try {
            badgeColor = Color.parseColor(note.getColor());
        } catch (Exception e) {
            badgeColor = Color.parseColor("#2563EB");
        }

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(8);
        shape.setColor(badgeColor);
        holder.tvCategoryBadge.setBackground(shape);

        if (note.isPinned()) {
            holder.tvPinnedIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.tvPinnedIndicator.setVisibility(View.GONE);
        }

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onNoteClick(note);
                }
            }
        });

        return convertView;
    }

    private static class ViewHolder {
        TextView tvCategoryBadge;
        TextView tvPinnedIndicator;
        TextView tvNoteWordCount;
        TextView tvNoteTitle;
        TextView tvNoteContent;
    }
}