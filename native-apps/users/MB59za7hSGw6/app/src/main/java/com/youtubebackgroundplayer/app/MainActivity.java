package com.youtubebackgroundplayer.app;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity implements BackgroundAudioService.OnTrackChangeListener {

    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 1001;
    private static final String PREFS_NAME = "YTBgPlayerPrefs";
    private static final String KEY_FAVORITES = "favorite_track_ids";

    private EditText etSearch;
    private TextView chipAll, chipTrending, chipLofi, chipPodcasts, chipFavs;
    private ListView lvTracks;
    private TextView tvActiveIcon, tvActiveTitle, tvActiveArtist, btnActiveFav;
    private TextView tvTimeCurrent, tvTimeTotal, btnPrev, btnPlayPause, btnNext;
    private SeekBar sbProgress;

    private List<AudioTrack> fullCatalog = new ArrayList<AudioTrack>();
    private List<AudioTrack> displayedTracks = new ArrayList<AudioTrack>();
    private TrackAdapter trackAdapter;
    private String currentCategoryFilter = "All";

    private BackgroundAudioService audioService;
    private boolean isBound = false;
    private int currentTrackIndex = -1;

    private Handler updateHandler = new Handler();
    private Runnable updateProgressRunnable = new Runnable() {
        @Override
        public void run() {
            if (isBound && audioService != null && audioService.isPlaying()) {
                int curr = audioService.getCurrentPosition();
                int total = audioService.getDuration();
                sbProgress.setMax(total > 0 ? total : 100);
                sbProgress.setProgress(curr);
                tvTimeCurrent.setText(formatMs(curr));
                tvTimeTotal.setText(formatMs(total));
                updateHandler.postDelayed(this, 500);
            }
        }
    };

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BackgroundAudioService.LocalBinder binder = (BackgroundAudioService.LocalBinder) service;
            audioService = binder.getService();
            isBound = true;
            audioService.setOnTrackChangeListener(MainActivity.this);
            syncPlayerState();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            audioService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        loadCatalogData();
        setupFiltersAndSearch();
        setupSeekBar();
        setupTransportControls();

        checkNotificationPermission();
        startAndBindService();
    }

    private void initViews() {
        etSearch = findViewById(R.id.et_search);
        chipAll = findViewById(R.id.chip_all);
        chipTrending = findViewById(R.id.chip_trending);
        chipLofi = findViewById(R.id.chip_lofi);
        chipPodcasts = findViewById(R.id.chip_podcasts);
        chipFavs = findViewById(R.id.chip_favs);

        lvTracks = findViewById(R.id.lv_tracks);

        tvActiveIcon = findViewById(R.id.tv_active_icon);
        tvActiveTitle = findViewById(R.id.tv_active_title);
        tvActiveArtist = findViewById(R.id.tv_active_artist);
        btnActiveFav = findViewById(R.id.btn_active_fav);

        tvTimeCurrent = findViewById(R.id.tv_time_current);
        tvTimeTotal = findViewById(R.id.tv_time_total);
        sbProgress = findViewById(R.id.sb_progress);

        btnPrev = findViewById(R.id.btn_prev);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnNext = findViewById(R.id.btn_next);

        trackAdapter = new TrackAdapter();
        lvTracks.setAdapter(trackAdapter);
    }

    private void loadCatalogData() {
        fullCatalog = AudioTrack.getSampleCatalog();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> favIds = prefs.getStringSet(KEY_FAVORITES, new HashSet<String>());

        for (int i = 0; i < fullCatalog.size(); i++) {
            AudioTrack track = fullCatalog.get(i);
            if (favIds.contains(track.getId())) {
                track.setFavorite(true);
            }
        }
        filterCatalog();
    }

    private void setupFiltersAndSearch() {
        chipAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCategoryFilter("All");
            }
        });

        chipTrending.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCategoryFilter("Trending");
            }
        });

        chipLofi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCategoryFilter("Lo-Fi");
            }
        });

        chipPodcasts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCategoryFilter("Podcasts");
            }
        });

        chipFavs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCategoryFilter("Favorites");
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCatalog();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setCategoryFilter(String category) {
        currentCategoryFilter = category;
        chipAll.setBackgroundResource("All".equals(category) ? R.color.chip_selected : R.color.chip_unselected);
        chipTrending.setBackgroundResource("Trending".equals(category) ? R.color.chip_selected : R.color.chip_unselected);
        chipLofi.setBackgroundResource("Lo-Fi".equals(category) ? R.color.chip_selected : R.color.chip_unselected);
        chipPodcasts.setBackgroundResource("Podcasts".equals(category) ? R.color.chip_selected : R.color.chip_unselected);
        chipFavs.setBackgroundResource("Favorites".equals(category) ? R.color.chip_selected : R.color.chip_unselected);

        filterCatalog();
    }

    private void filterCatalog() {
        displayedTracks.clear();
        String query = etSearch.getText().toString().trim().toLowerCase();

        for (int i = 0; i < fullCatalog.size(); i++) {
            AudioTrack track = fullCatalog.get(i);
            boolean matchesCategory = "All".equals(currentCategoryFilter)
                    || ("Favorites".equals(currentCategoryFilter) && track.isFavorite())
                    || track.getCategory().equalsIgnoreCase(currentCategoryFilter);

            boolean matchesQuery = query.isEmpty()
                    || track.getTitle().toLowerCase().contains(query)
                    || track.getArtist().toLowerCase().contains(query);

            if (matchesCategory && matchesQuery) {
                displayedTracks.add(track);
            }
        }
        trackAdapter.notifyDataSetChanged();
    }

    private void setupSeekBar() {
        sbProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && isBound && audioService != null) {
                    audioService.seekTo(progress);
                    tvTimeCurrent.setText(formatMs(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupTransportControls() {
        btnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isBound || audioService == null) return;

                if (audioService.isPlaying()) {
                    audioService.pauseAudio();
                    btnPlayPause.setText("▶");
                } else {
                    if (audioService.getCurrentTrack() != null) {
                        audioService.playAudio();
                        btnPlayPause.setText("⏸");
                        updateProgressLoop();
                    } else if (!displayedTracks.isEmpty()) {
                        playTrackAtIndex(0);
                    }
                }
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (displayedTracks.isEmpty()) return;
                currentTrackIndex = (currentTrackIndex + 1) % displayedTracks.size();
                playTrackAtIndex(currentTrackIndex);
            }
        });

        btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (displayedTracks.isEmpty()) return;
                currentTrackIndex--;
                if (currentTrackIndex < 0) {
                    currentTrackIndex = displayedTracks.size() - 1;
                }
                playTrackAtIndex(currentTrackIndex);
            }
        });

        btnActiveFav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentTrackIndex >= 0 && currentTrackIndex < displayedTracks.size()) {
                    AudioTrack track = displayedTracks.get(currentTrackIndex);
                    toggleFavorite(track);
                    btnActiveFav.setText(track.isFavorite() ? "♥" : "♡");
                    trackAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    private void playTrackAtIndex(int index) {
        if (index < 0 || index >= displayedTracks.size()) return;
        currentTrackIndex = index;
        AudioTrack track = displayedTracks.get(index);

        tvActiveIcon.setText(track.getIcon());
        tvActiveTitle.setText(track.getTitle());
        tvActiveArtist.setText(track.getArtist());
        btnActiveFav.setText(track.isFavorite() ? "♥" : "♡");
        btnPlayPause.setText("⏸");

        if (isBound && audioService != null) {
            audioService.playTrack(track);
            updateProgressLoop();
        }
    }

    private void toggleFavorite(AudioTrack track) {
        track.setFavorite(!track.isFavorite());
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> favIds = new HashSet<String>(prefs.getStringSet(KEY_FAVORITES, new HashSet<String>()));

        if (track.isFavorite()) {
            favIds.add(track.getId());
            Toast.makeText(this, "Added to Favorites", Toast.LENGTH_SHORT).show();
        } else {
            favIds.remove(track.getId());
            Toast.makeText(this, "Removed from Favorites", Toast.LENGTH_SHORT).show();
        }
        prefs.edit().putStringSet(KEY_FAVORITES, favIds).apply();
    }

    private void syncPlayerState() {
        if (isBound && audioService != null) {
            AudioTrack current = audioService.getCurrentTrack();
            if (current != null) {
                tvActiveIcon.setText(current.getIcon());
                tvActiveTitle.setText(current.getTitle());
                tvActiveArtist.setText(current.getArtist());
                btnActiveFav.setText(current.isFavorite() ? "♥" : "♡");
                btnPlayPause.setText(audioService.isPlaying() ? "⏸" : "▶");
                if (audioService.isPlaying()) {
                    updateProgressLoop();
                }
            }
        }
    }

    private void updateProgressLoop() {
        updateHandler.removeCallbacks(updateProgressRunnable);
        updateHandler.post(updateProgressRunnable);
    }

    @Override
    public void onTrackStateChanged(boolean isPlaying, int currentPosition, int duration) {
        btnPlayPause.setText(isPlaying ? "⏸" : "▶");
        if (isPlaying) {
            updateProgressLoop();
        } else {
            updateHandler.removeCallbacks(updateProgressRunnable);
        }
    }

    private String formatMs(int ms) {
        int totalSeconds = ms / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private void startAndBindService() {
        Intent serviceIntent = new Intent(this, BackgroundAudioService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_POST_NOTIFICATIONS);
            }
        }
    }

    @Override
    protected void onDestroy() {
        updateHandler.removeCallbacks(updateProgressRunnable);
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        super.onDestroy();
    }

    private class TrackAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return displayedTracks.size();
        }

        @Override
        public Object getItem(int position) {
            return displayedTracks.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_audio_track, parent, false);
            }

            final AudioTrack track = displayedTracks.get(position);

            TextView tvIcon = convertView.findViewById(R.id.tv_track_icon);
            TextView tvTitle = convertView.findViewById(R.id.tv_track_title);
            TextView tvSubtitle = convertView.findViewById(R.id.tv_track_subtitle);
            TextView tvDuration = convertView.findViewById(R.id.tv_track_duration);
            TextView btnFav = convertView.findViewById(R.id.btn_track_fav);

            tvIcon.setText(track.getIcon());
            tvTitle.setText(track.getTitle());
            tvSubtitle.setText(track.getArtist() + " • " + track.getCategory());
            tvDuration.setText(track.getFormattedDuration());
            btnFav.setText(track.isFavorite() ? "♥" : "♡");

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playTrackAtIndex(position);
                }
            });

            btnFav.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleFavorite(track);
                    notifyDataSetChanged();
                }
            });

            return convertView;
        }
    }
}