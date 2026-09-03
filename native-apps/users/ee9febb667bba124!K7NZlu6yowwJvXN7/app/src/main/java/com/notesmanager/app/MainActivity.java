package com.notesmanager.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;
    private ListView listViewNotes;
    private TextView tvEmpty;
    private EditText etSearch;
    private Spinner spinnerFilter;
    private Button btnAddNote;

    // Loader overlay elements
    private RelativeLayout loaderOverlay;
    private TextView tvLoaderStatus;
    private Handler loaderHandler = new Handler();

    private List<Note> currentNotesList = new ArrayList<>();
    private NoteAdapter noteAdapter;

    private final String[] filterCategories = {"All", "Personal", "Work", "Ideas", "Other"};
    private final String[] formCategories = {"Personal", "Work", "Ideas", "Other"};

    @Override
    protected void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        listViewNotes = (ListView) findViewById(R.id.list_view_notes);
        tvEmpty = (TextView) findViewById(R.id.tv_empty);
        etSearch = (EditText) findViewById(R.id.et_search);
        spinnerFilter = (Spinner) findViewById(R.id.spinner_category_filter);
        btnAddNote = (Button) findViewById(R.id.btn_add_note);

        // Explicitly guarantee the Add Button is GONE while loader is active
        if (btnAddNote != null) {
            btnAddNote.setVisibility(View.GONE);
        }

        // Find loader overlay
        loaderOverlay = (RelativeLayout) findViewById(R.id.loader_overlay);
        tvLoaderStatus = (TextView) findViewById(R.id.loader_status);

        setupFilters();
        setupSearch();
        setupListView();

        if (btnAddNote != null) {
            btnAddNote.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playActionSound();
                    showNoteDialog(null);
                }
            });
        }

        loadNotes();
        
        // Initiate custom welcome sound sequence and simulated loading sequences
        startAppLoader();
    }

    private void startAppLoader() {
        // Play dynamic welcome chime sequence in a background thread
        playWelcomeSound();

        // Perform dynamic simulated text loading sequences with step transitions
        loaderHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (tvLoaderStatus != null) {
                    tvLoaderStatus.setText("Establishing secure connection to SQL storage...");
                }
            }
        }, 700);

        loaderHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (tvLoaderStatus != null) {
                    tvLoaderStatus.setText("Synchronizing interactive notes settings...");
                }
            }
        }, 1500);

        loaderHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (tvLoaderStatus != null) {
                    tvLoaderStatus.setText("Workspace successfully loaded!");
                }
                
                // Fade out loader screen cleanly
                if (loaderOverlay != null) {
                    loaderOverlay.animate()
                            .alpha(0f)
                            .setDuration(500)
                            .setListener(new android.animation.AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(android.animation.Animator animation) {
                                    loaderOverlay.setVisibility(View.GONE);
                                    // Make add button visible ONLY after loader is hidden
                                    if (btnAddNote != null) {
                                        btnAddNote.setVisibility(View.VISIBLE);
                                    }
                                }
                            });
                } else {
                    if (btnAddNote != null) {
                        btnAddNote.setVisibility(View.VISIBLE);
                    }
                }
            }
        }, 2200);
    }

    private void setupFilters() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filterCategories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(adapter);

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadNotes();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadNotes();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupListView() {
        noteAdapter = new NoteAdapter(this, currentNotesList);
        listViewNotes.setAdapter(noteAdapter);

        listViewNotes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                playActionSound();
                Note selectedNote = currentNotesList.get(position);
                showNoteDialog(selectedNote);
            }
        });
    }

    private void loadNotes() {
        String searchQuery = etSearch.getText().toString().trim();
        String selectedCategory = spinnerFilter.getSelectedItem().toString();

        currentNotesList.clear();
        currentNotesList.addAll(dbHelper.searchNotes(searchQuery, selectedCategory));
        noteAdapter.notifyDataSetChanged();

        if (currentNotesList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
        } else { 
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void showNoteDialog(final Note noteToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_note, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        TextView tvDialogTitle = (TextView) dialogView.findViewById(R.id.dialog_title);
        final EditText etNoteTitle = (EditText) dialogView.findViewById(R.id.et_note_title);
        final Spinner spinnerNoteCategory = (Spinner) dialogView.findViewById(R.id.spinner_note_category);
        final EditText etNoteContent = (EditText) dialogView.findViewById(R.id.et_note_content);

        Button btnDelete = (Button) dialogView.findViewById(R.id.btn_dialog_delete);
        Button btnCancel = (Button) dialogView.findViewById(R.id.btn_dialog_cancel);
        Button btnSave = (Button) dialogView.findViewById(R.id.btn_dialog_save);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, formCategories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerNoteCategory.setAdapter(categoryAdapter);

        if (noteToEdit != null) {
            tvDialogTitle.setText("Edit Note");
            etNoteTitle.setText(noteToEdit.getTitle());
            etNoteContent.setText(noteToEdit.getContent());
            btnDelete.setVisibility(View.VISIBLE);

            for (int i = 0; i < formCategories.length; i++) {
                if (formCategories[i].equalsIgnoreCase(noteToEdit.getCategory())) {
                    spinnerNoteCategory.setSelection(i);
                    break;
                } 
            }
        } else {
            tvDialogTitle.setText("Add New Note");
            btnDelete.setVisibility(View.GONE);
        }

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playActionSound();
                dialog.dismiss();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = etNoteTitle.getText().toString().trim();
                String content = etNoteContent.getText().toString().trim();
                String category = spinnerNoteCategory.getSelectedItem().toString();

                if (title.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter a title", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (noteToEdit == null) {
                    dbHelper.insertNote(title, content, category);
                    Toast.makeText(MainActivity.this, "Note saved", Toast.LENGTH_SHORT).show();
                } else {
                    dbHelper.updateNote(noteToEdit.getId(), title, content, category);
                    Toast.makeText(MainActivity.this, "Note updated", Toast.LENGTH_SHORT).show();
                }

                playActionSound();
                loadNotes();
                dialog.dismiss();
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playDeleteSound();
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete Note")
                        .setMessage("Are you sure you want to delete this note?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {
                                dbHelper.deleteNote(noteToEdit.getId());
                                Toast.makeText(MainActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                                playDeleteSound();
                                loadNotes();
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {
                                playActionSound();
                            }
                        })
                        .show();
            }
        });

        dialog.show();
    }

    // PROGRAMMATIC AUDIO SYNTHESIZERS USING AUDIOTRACK FOR MAXIMUM RELIABILITY AND NO EXTERNAL DEPENDENCIES
    private void playWelcomeSound() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Ascending chime scale: C5 (523Hz), E5 (659Hz), G5 (784Hz), C6 (1046Hz)
                    int[] chimeNotes = {523, 659, 784, 1046};
                    for (int freq : chimeNotes) {
                        playPcmTone(freq, 150);
                        Thread.sleep(120);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void playActionSound() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                playPcmTone(987, 85); // High clear sound feedback
            }
        }).start();
    }

    private void playDeleteSound() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    playPcmTone(493, 100); 
                    Thread.sleep(80);
                    playPcmTone(329, 160); // Low warning sound sequence
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void playPcmTone(double frequency, int durationMs) {
        try {
            int sampleRate = 8000;
            int numSamples = durationMs * sampleRate / 1000;
            double[] sample = new double[numSamples];
            byte[] generatedSnd = new byte[2 * numSamples];

            for (int i = 0; i < numSamples; ++i) {
                sample[i] = Math.sin(2 * Math.PI * i / (sampleRate / frequency));
            }

            int idx = 0;
            for (double dVal : sample) {
                short val = (short) ((dVal * 32767));
                generatedSnd[idx++] = (byte) (val & 0x00ff);
                generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
            }

            final android.media.AudioTrack audioTrack = new android.media.AudioTrack(
                    android.media.AudioManager.STREAM_MUSIC,
                    sampleRate,
                    android.media.AudioFormat.CHANNEL_OUT_MONO,
                    android.media.AudioFormat.ENCODING_PCM_16BIT,
                    generatedSnd.length,
                    android.media.AudioTrack.MODE_STATIC);

            audioTrack.write(generatedSnd, 0, generatedSnd.length);
            audioTrack.play();
            
            // Release audio track resources after dynamic playback finished
            loaderHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        audioTrack.release();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, durationMs + 200);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class NoteAdapter extends BaseAdapter {
        private Context context;
        private List<Note> notes;

        public NoteAdapter(Context context, List<Note> notes) {
            this.context = context;
            this.notes = notes;
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
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.note_item, parent, false);
            }

            TextView tvTitle = (TextView) convertView.findViewById(R.id.tv_title);
            TextView tvCategory = (TextView) convertView.findViewById(R.id.tv_category);
            TextView tvContent = (TextView) convertView.findViewById(R.id.tv_content);
            TextView tvDate = (TextView) convertView.findViewById(R.id.tv_date);

            Note note = notes.get(position);

            tvTitle.setText(note.getTitle());
            tvContent.setText(note.getContent());
            tvDate.setText(note.getTimestamp());

            String category = note.getCategory();
            tvCategory.setText(category.toUpperCase());

            GradientDrawable background = (GradientDrawable) tvCategory.getBackground();
            if (background != null) {
                int color;
                if ("Personal".equalsIgnoreCase(category)) {
                    color = Color.parseColor("#2196F3");
                } else if ("Work".equalsIgnoreCase(category)) {
                    color = Color.parseColor("#FF9800");
                } else if ("Ideas".equalsIgnoreCase(category)) {
                    color = Color.parseColor("#9C27B0");
                } else {
                    color = Color.parseColor("#009688");
                }
                background.setColor(color);
            }

            return convertView;
        }
    }
}