package com.beautifulnotes.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private TextView tvStatTotalNotes;
    private TextView tvStatTotalWords;
    private EditText etSearch;
    private ListView lvNotes;
    private View llEmptyState;
    private Button btnCreateNote;
    private Button btnLogout;
    private ProgressBar pbMain;

    private BackendApi backendApi;
    private List<Note> originalNotesList = new ArrayList<>();
    private List<Note> filteredNotesList = new ArrayList<>();
    private NotesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatTotalNotes = (TextView) findViewById(R.id.tv_stat_total_notes);
        tvStatTotalWords = (TextView) findViewById(R.id.tv_stat_total_words);
        etSearch = (EditText) findViewById(R.id.et_search);
        lvNotes = (ListView) findViewById(R.id.lv_notes);
        llEmptyState = findViewById(R.id.ll_empty_state);
        btnCreateNote = (Button) findViewById(R.id.btn_create_note);
        btnLogout = (Button) findViewById(R.id.btn_logout);
        pbMain = (ProgressBar) findViewById(R.id.pb_main);

        backendApi = new BackendApi(this);

        String token = getIntent().getStringExtra("auth_token");
        if (token == null) {
            SharedPreferences prefs = getSharedPreferences("beautiful_notes_prefs", MODE_PRIVATE);
            token = prefs.getString("auth_token", "");
        }
        backendApi.setToken(token);

        adapter = new NotesAdapter(this, filteredNotesList);
        lvNotes.setAdapter(adapter);

        lvNotes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Note note = filteredNotesList.get(position);
                openNoteDetail(note);
            }
        });

        btnCreateNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openNoteDetail(null);
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerLogout();
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                runQueryFilter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncNotesFromServer();
    }

    private void syncNotesFromServer() {
        pbMain.setVisibility(View.VISIBLE);
        backendApi.getNotes(new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(final JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pbMain.setVisibility(View.GONE);
                        try {
                            JSONArray recordsArray = response.getJSONArray("records");
                            originalNotesList.clear();
                            for (int i = 0; i < recordsArray.length(); i++) {
                                JSONObject recObj = recordsArray.getJSONObject(i);
                                Note note = Note.fromJsonRecord(recObj);
                                if (note != null) {
                                    originalNotesList.add(note);
                                }
                            }
                            runQueryFilter(etSearch.getText().toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, "Response schema extraction failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onError(final String errorMessage) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pbMain.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, "Sync Error: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    // MANDATORY OFFLINE CALCULATION: Sum total word lists and stats
    private void calculateAndDisplayStats() {
        int totalNotes = originalNotesList.size();
        int totalWords = 0;

        for (int i = 0; i < originalNotesList.size(); i++) {
            totalWords += originalNotesList.get(i).getWordCount();
        }

        tvStatTotalNotes.setText(String.valueOf(totalNotes));
        tvStatTotalWords.setText(String.valueOf(totalWords));
    }

    private void runQueryFilter(String query) {
        filteredNotesList.clear();
        String lowQuery = query.toLowerCase().trim();

        if (lowQuery.isEmpty()) {
            filteredNotesList.addAll(originalNotesList);
        } else {
            for (int i = 0; i < originalNotesList.size(); i++) {
                Note item = originalNotesList.get(i);
                if (item.getTitle().toLowerCase().contains(lowQuery) || 
                    item.getContent().toLowerCase().contains(lowQuery)) {
                    filteredNotesList.add(item);
                }
            }
        }

        adapter.notifyDataSetChanged();

        if (filteredNotesList.isEmpty()) {
            llEmptyState.setVisibility(View.VISIBLE);
        } else {
            llEmptyState.setVisibility(View.GONE);
        }

        // Always compute dynamic analytics from memory locally
        calculateAndDisplayStats();
    }

    private void openNoteDetail(Note note) {
        Intent intent = new Intent(MainActivity.this, NoteDetailActivity.class);
        intent.putExtra("auth_token", backendApi.getToken());
        if (note != null) {
            intent.putExtra("note_id", note.getId());
            intent.putExtra("note_title", note.getTitle());
            intent.putExtra("note_content", note.getContent());
            intent.putExtra("note_color", note.getColor());
            intent.putExtra("note_time", note.getUpdatedAt());
        }
        startActivity(intent);
    }

    private void triggerLogout() {
        pbMain.setVisibility(View.VISIBLE);
        backendApi.logout(new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                cleanLocalSessionAndExit();
            }

            @Override
            public void onError(String errorMessage) {
                cleanLocalSessionAndExit();
            }
        });
    }

    private void cleanLocalSessionAndExit() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pbMain.setVisibility(View.GONE);
                SharedPreferences.Editor editor = getSharedPreferences("beautiful_notes_prefs", MODE_PRIVATE).edit();
                editor.remove("auth_token");
                editor.apply();

                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    // Beautiful Custom Adapter implementing Rounded Gradient Dynamic Colors
    private static class NotesAdapter extends BaseAdapter {
        private final Context context;
        private final List<Note> notes;
        private final LayoutInflater inflater;

        public NotesAdapter(Context context, List<Note> notes) {
            this.context = context;
            this.notes = notes;
            this.inflater = LayoutInflater.from(context);
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
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_note, parent, false);
                holder = new ViewHolder();
                holder.tvTitle = (TextView) convertView.findViewById(R.id.tv_item_title);
                holder.tvWordBadge = (TextView) convertView.findViewById(R.id.tv_item_word_badge);
                holder.tvExcerpt = (TextView) convertView.findViewById(R.id.tv_item_excerpt);
                holder.tvTime = (TextView) convertView.findViewById(R.id.tv_item_time);
                holder.viewAccent = convertView.findViewById(R.id.view_item_accent_color);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            Note note = notes.get(position);
            holder.tvTitle.setText(note.getTitle().isEmpty() ? "(Untitled Thought)" : note.getTitle());
            holder.tvExcerpt.setText(note.getContent().isEmpty() ? "Empty description." : note.getContent());
            holder.tvWordBadge.setText(note.getWordCount() + " words");
            holder.tvTime.setText(note.getUpdatedAt());

            // Build beautifully rounded background tint dynamically for each note card
            int noteBgColor = Color.parseColor("#FFFFFF");
            try {
                noteBgColor = Color.parseColor(note.getColor());
            } catch (Exception e) {
                // Keep default fallback color
            }

            GradientDrawable dynamicBg = new GradientDrawable();
            dynamicBg.setShape(GradientDrawable.RECTANGLE);
            dynamicBg.setCornerRadius(14 * context.getResources().getDisplayMetrics().density);
            dynamicBg.setColor(noteBgColor);
            dynamicBg.setStroke((int) (1 * context.getResources().getDisplayMetrics().density), Color.parseColor("#EAEAEA"));
            convertView.setBackground(dynamicBg);

            // Dynamic color indicator marker
            GradientDrawable accentBg = new GradientDrawable();
            accentBg.setShape(GradientDrawable.OVAL);
            accentBg.setColor(noteBgColor);
            accentBg.setStroke((int) (1.5f * context.getResources().getDisplayMetrics().density), Color.parseColor("#CCCCCC"));
            holder.viewAccent.setBackground(accentBg);

            return convertView;
        }

        private static class ViewHolder {
            TextView tvTitle;
            TextView tvWordBadge;
            TextView tvExcerpt;
            TextView tvTime;
            View viewAccent;
        }
    }
}