package com.quicknotes.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import java.util.List;

public class NoteAdapter extends ArrayAdapter<Note> {
    private Context context;
    private List<Note> notes;
    private NotesDatabaseHelper dbHelper;
    private NoteDeleteListener deleteListener;

    public interface NoteDeleteListener {
        void onNoteDeleted();
    }

    public NoteAdapter(Context context, List<Note> notes, NotesDatabaseHelper dbHelper, NoteDeleteListener deleteListener) {
        super(context, 0, notes);
        this.context = context;
        this.notes = notes;
        this.dbHelper = dbHelper;
        this.deleteListener = deleteListener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Note note = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.note_item, parent, false);
        }

        TextView textView = (TextView) convertView.findViewById(R.id.textViewNoteText);
        Button btnDelete = (Button) convertView.findViewById(R.id.buttonDeleteNote);

        if (note != null) {
            textView.setText(note.getText());
            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dbHelper.deleteNote(note.getId());
                    if (deleteListener != null) {
                        deleteListener.onNoteDeleted();
                    }
                }
            });
        }

        return convertView;
    }
}