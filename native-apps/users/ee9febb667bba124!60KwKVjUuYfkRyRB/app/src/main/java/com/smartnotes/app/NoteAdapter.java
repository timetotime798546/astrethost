package com.smartnotes.app;

import android.content.Context;
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
    private SimpleDateFormat dateFormat;

    public NoteAdapter(Context context, List<Note> notes) {
        this.context = context;
        this.notes = notes;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    }

    public void setNotes(List<Note> notes) {
        this.notes = notes;
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
        return notes.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.note_list_item, parent, false);
            holder = new ViewHolder();
            holder.textTitle = (TextView) convertView.findViewById(R.id.text_title);
            holder.textCategory = (TextView) convertView.findViewById(R.id.text_category);
            holder.textDate = (TextView) convertView.findViewById(R.id.text_date);
            holder.textContentSnippet = (TextView) convertView.findViewById(R.id.text_content_snippet);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Note note = notes.get(position);
        holder.textTitle.setText(note.getTitle());
        holder.textCategory.setText(note.getCategory());
        holder.textDate.setText(dateFormat.format(new Date(note.getDate())));
        
        String snippet = note.getContent();
        if (snippet.length() > 80) {
            snippet = snippet.substring(0, 77) + "...";
        }
        holder.textContentSnippet.setText(snippet);

        return convertView;
    } 

    private static class ViewHolder {
        TextView textTitle;
        TextView textCategory;
        TextView textDate;
        TextView textContentSnippet;
    }
}