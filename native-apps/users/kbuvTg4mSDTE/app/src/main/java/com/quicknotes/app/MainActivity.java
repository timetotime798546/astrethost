package com.quicknotes.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private NotesStorage storage;
    private List<Note> allNotes = new ArrayList<Note>();
    private List<Note> displayedNotes = new ArrayList<Note>();

    private LinearLayout notesContainer;
    private TextView tvEmptyState;
    private TextView tvStatsSummary;
    private EditText etSearch;
    private Button btnAddNote;

    private Button btnFilterAll;
    private Button btnFilterGeneral;
    private Button btnFilterWork;
    private Button btnFilterPersonal;
    private Button btnFilterIdeas;
    private Button btnFilterUrgent;

    private String activeCategoryFilter = "All";
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        storage = new NotesStorage(this);
        initViews();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshNotes();
    }

    private void initViews() {
        notesContainer = (LinearLayout) findViewById(R.id.notes_list_container);
        tvEmptyState = (TextView) findViewById(R.id.tv_empty_state);
        tvStatsSummary = (TextView) findViewById(R.id.tv_stats_summary);
        etSearch = (EditText) findViewById(R.id.et_search);
        btnAddNote = (Button) findViewById(R.id.btn_add_note);

        btnFilterAll = (Button) findViewById(R.id.btn_filter_all);
        btnFilterGeneral = (Button) findViewById(R.id.btn_filter_general);
        btnFilterWork = (Button) findViewById(R.id.btn_filter_work);
        btnFilterPersonal = (Button) findViewById(R.id.btn_filter_personal);
        btnFilterIdeas = (Button) findViewById(R.id.btn_filter_ideas);
        btnFilterUrgent = (Button) findViewById(R.id.btn_filter_urgent);
    }

    private void setupListeners() {
        btnAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NoteEditorActivity.class);
                startActivity(intent);
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim().toLowerCase(Locale.getDefault());
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        View.OnClickListener filterClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();
                if (id == R.id.btn_filter_all) {
                    activeCategoryFilter = "All";
                } else if (id == R.id.btn_filter_general) {
                    activeCategoryFilter = "General";
                } else if (id == R.id.btn_filter_work) {
                    activeCategoryFilter = "Work";
                } else if (id == R.id.btn_filter_personal) {
                    activeCategoryFilter = "Personal";
                } else if (id == R.id.btn_filter_ideas) {
                    activeCategoryFilter = "Ideas";
                } else if (id == R.id.btn_filter_urgent) {
                    activeCategoryFilter = "Urgent";
                }
                updateFilterButtonsUI();
                applyFilters();
            }
        };

        btnFilterAll.setOnClickListener(filterClickListener);
        btnFilterGeneral.setOnClickListener(filterClickListener);
        btnFilterWork.setOnClickListener(filterClickListener);
        btnFilterPersonal.setOnClickListener(filterClickListener);
        btnFilterIdeas.setOnClickListener(filterClickListener);
        btnFilterUrgent.setOnClickListener(filterClickListener);
    }

    private void updateFilterButtonsUI() {
        btnFilterAll.setBackgroundColor("All".equalsIgnoreCase(activeCategoryFilter) ? 0xFF1E293B : 0xFFE2E8F0);
        btnFilterAll.setTextColor("All".equalsIgnoreCase(activeCategoryFilter) ? 0xFFFFFFFF : 0xFF475569);

        btnFilterGeneral.setBackgroundColor("General".equalsIgnoreCase(activeCategoryFilter) ? 0xFF1E293B : 0xFFE2E8F0);
        btnFilterGeneral.setTextColor("General".equalsIgnoreCase(activeCategoryFilter) ? 0xFFFFFFFF : 0xFF475569);

        btnFilterWork.setBackgroundColor("Work".equalsIgnoreCase(activeCategoryFilter) ? 0xFF1E293B : 0xFFDBEAFE);
        btnFilterWork.setTextColor("Work".equalsIgnoreCase(activeCategoryFilter) ? 0xFFFFFFFF : 0xFF1D4ED8);

        btnFilterPersonal.setBackgroundColor("Personal".equalsIgnoreCase(activeCategoryFilter) ? 0xFF1E293B : 0xFFDCFCE7);
        btnFilterPersonal.setTextColor("Personal".equalsIgnoreCase(activeCategoryFilter) ? 0xFFFFFFFF : 0xFF15803D);

        btnFilterIdeas.setBackgroundColor("Ideas".equalsIgnoreCase(activeCategoryFilter) ? 0xFF1E293B : 0xFFFEF3C7);
        btnFilterIdeas.setTextColor("Ideas".equalsIgnoreCase(activeCategoryFilter) ? 0xFFFFFFFF : 0xFFB45309);

        btnFilterUrgent.setBackgroundColor("Urgent".equalsIgnoreCase(activeCategoryFilter) ? 0xFF1E293B : 0xFFFEE2E2);
        btnFilterUrgent.setTextColor("Urgent".equalsIgnoreCase(activeCategoryFilter) ? 0xFFFFFFFF : 0xFFB91C1C);
    }

    private void refreshNotes() {
        allNotes = storage.loadNotes();
        applyFilters();
    }

    private void applyFilters() {
        displayedNotes.clear();

        List<Note> pinnedMatches = new ArrayList<Note>();
        List<Note> normalMatches = new ArrayList<Note>();

        for (int i = 0; i < allNotes.size(); i++) {
            Note note = allNotes.get(i);
            boolean matchesCategory = "All".equalsIgnoreCase(activeCategoryFilter)
                    || note.getCategory().equalsIgnoreCase(activeCategoryFilter);

            boolean matchesSearch = currentSearchQuery.length() == 0
                    || note.getTitle().toLowerCase(Locale.getDefault()).contains(currentSearchQuery)
                    || note.getContent().toLowerCase(Locale.getDefault()).contains(currentSearchQuery)
                    || note.getCategory().toLowerCase(Locale.getDefault()).contains(currentSearchQuery);

            if (matchesCategory && matchesSearch) {
                if (note.isPinned()) {
                    pinnedMatches.add(note);
                } else {
                    normalMatches.add(note);
                }
            }
        }

        // Pinned notes appear first
        displayedNotes.addAll(pinnedMatches);
        displayedNotes.addAll(normalMatches);

        // Native offline summary calculation
        int totalWords = NoteStatsCalculator.calculateTotalWords(allNotes);
        int pinnedCount = NoteStatsCalculator.calculatePinnedCount(allNotes);
        tvStatsSummary.setText(allNotes.size() + " notes • " + totalWords + " words • " + pinnedCount + " pinned");

        renderNotesView();
    }

    private void renderNotesView() {
        notesContainer.removeAllViews();

        if (displayedNotes.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            notesContainer.addView(tvEmptyState);
            return;
        }

        tvEmptyState.setVisibility(View.GONE);

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault());

        for (int i = 0; i < displayedNotes.size(); i++) {
            final Note note = displayedNotes.get(i);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundColor(0xFFFFFFFF);
            card.setPadding(20, 20, 20, 20);

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 0, 0, 16);
            card.setLayoutParams(cardParams);
            card.setElevation(2f);

            // Card Header: Category & Pin indicator
            LinearLayout topRow = new LinearLayout(this);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvCategory = new TextView(this);
            tvCategory.setText(note.getCategory());
            tvCategory.setTextSize(11f);
            tvCategory.setPadding(12, 4, 12, 4);
            applyCategoryBadgeStyle(tvCategory, note.getCategory());

            topRow.addView(tvCategory);

            if (note.isPinned()) {
                TextView tvPinnedTag = new TextView(this);
                tvPinnedTag.setText("★ PINNED");
                tvPinnedTag.setTextSize(11f);
                tvPinnedTag.setTextColor(0xFFD97706);
                tvPinnedTag.setPadding(14, 0, 0, 0);
                topRow.addView(tvPinnedTag);
            }

            card.addView(topRow);

            // Title
            TextView tvTitle = new TextView(this);
            tvTitle.setText(note.getTitle());
            tvTitle.setTextSize(17f);
            tvTitle.setTextColor(0xFF0F172A);
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            tvTitle.setPadding(0, 12, 0, 6);
            card.addView(tvTitle);

            // Content snippet
            String snippet = note.getContent();
            if (snippet.length() > 140) {
                snippet = snippet.substring(0, 140) + "...";
            }
            TextView tvContent = new TextView(this);
            tvContent.setText(snippet);
            tvContent.setTextSize(14f);
            tvContent.setTextColor(0xFF475569);
            tvContent.setPadding(0, 0, 0, 12);
            card.addView(tvContent);

            // Footer with date and calculated word/read metrics
            LinearLayout footer = new LinearLayout(this);
            footer.setOrientation(LinearLayout.HORIZONTAL);
            footer.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvDate = new TextView(this);
            tvDate.setText(dateFormat.format(new Date(note.getUpdatedAt())));
            tvDate.setTextSize(11f);
            tvDate.setTextColor(0xFF94A3B8);

            LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f
            );
            tvDate.setLayoutParams(dateParams);
            footer.addView(tvDate);

            // Offline Word Count & Reading Time Calculation for each item
            int words = NoteStatsCalculator.calculateWordCount(note.getContent());
            int readMins = NoteStatsCalculator.calculateReadingTimeMinutes(words);
            TextView tvMetrics = new TextView(this);
            tvMetrics.setText(words + " words • " + readMins + "m");
            tvMetrics.setTextSize(11f);
            tvMetrics.setTextColor(0xFF64748B);
            footer.addView(tvMetrics);

            card.addView(footer);

            // Click note card to edit
            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, NoteEditorActivity.class);
                    intent.putExtra("note_id", note.getId());
                    intent.putExtra("title", note.getTitle());
                    intent.putExtra("content", note.getContent());
                    intent.putExtra("category", note.getCategory());
                    intent.putExtra("is_pinned", note.isPinned());
                    intent.putExtra("created_at", note.getCreatedAt());
                    startActivity(intent);
                }
            });

            notesContainer.addView(card);
        }
    }

    private void applyCategoryBadgeStyle(TextView textView, String category) {
        if ("Work".equalsIgnoreCase(category)) {
            textView.setBackgroundColor(0xFFDBEAFE);
            textView.setTextColor(0xFF1D4ED8);
        } else if ("Personal".equalsIgnoreCase(category)) {
            textView.setBackgroundColor(0xFFDCFCE7);
            textView.setTextColor(0xFF15803D);
        } else if ("Ideas".equalsIgnoreCase(category)) {
            textView.setBackgroundColor(0xFFFEF3C7);
            textView.setTextColor(0xFFB45309);
        } else if ("Urgent".equalsIgnoreCase(category)) {
            textView.setBackgroundColor(0xFFFEE2E2);
            textView.setTextColor(0xFFB91C1C);
        } else {
            textView.setBackgroundColor(0xFFE2E8F0);
            textView.setTextColor(0xFF475569);
        }
    }
}