package com.offlinenotes.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class NoteAdapter extends BaseAdapter {
    private Context context;
    private List<Note> notes;
    private LayoutInflater inflater;

    public NoteAdapter(Context context, List<Note> notes) {
        this.context = context;
        this.notes = notes;
        this.inflater = LayoutInflater.from(context);
    }

    public void updateList(List<Note> newNotes) {
        this.notes = newNotes;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return notes != null ? notes.size() : 0;
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
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_note, parent, false);
            holder = new ViewHolder();
            holder.cardContainer = (LinearLayout) convertView.findViewById(R.id.note_card_container);
            holder.tvTitle = (TextView) convertView.findViewById(R.id.item_title);
            holder.tvExcerpt = (TextView) convertView.findViewById(R.id.item_excerpt);
            holder.tvCategory = (TextView) convertView.findViewById(R.id.item_category);
            holder.tvWords = (TextView) convertView.findViewById(R.id.item_words);
            holder.tvDate = (TextView) convertView.findViewById(R.id.item_date);
            holder.tvPin = (TextView) convertView.findViewById(R.id.item_pin);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Note note = notes.get(position);

        String title = note.getTitle().trim();
        if (title.isEmpty()) {
            holder.tvTitle.setText("Untitled Note");
        } else {
            holder.tvTitle.setText(title);
        }

        String content = note.getContent().trim();
        if (content.isEmpty()) {
            holder.tvExcerpt.setText("(No additional text)");
        } else {
            holder.tvExcerpt.setText(content);
        }

        holder.tvCategory.setText(note.getCategory());
        holder.tvDate.setText(note.getFormattedDate());

        int wordCount = note.calculateWordCount();
        holder.tvWords.setText(wordCount + (wordCount == 1 ? " word" : " words"));

        if (note.isPinned()) {
            holder.tvPin.setVisibility(View.VISIBLE);
        } else {
            holder.tvPin.setVisibility(View.GONE);
        }

        // Apply custom color to note card
        try {
            int parsedColor = Color.parseColor(note.getColorHex());
            GradientDrawable shape = new GradientDrawable();
            shape.setColor(parsedColor);
            shape.setCornerRadius(24f);
            shape.setStroke(2, Color.parseColor("#E2E8F0"));
            holder.cardContainer.setBackground(shape);
        } catch (Exception e) {
            holder.cardContainer.setBackgroundResource(R.drawable.card_bg);
        }

        return convertView;
    }

    private static class ViewHolder {
        LinearLayout cardContainer;
        TextView tvTitle;
        TextView tvExcerpt;
        TextView tvCategory;
        TextView tvWords;
        TextView tvDate;
        TextView tvPin;
    }
}