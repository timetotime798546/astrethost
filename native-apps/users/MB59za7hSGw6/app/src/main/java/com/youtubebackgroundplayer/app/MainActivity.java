package com.youtubebackgroundplayer.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements BackgroundAudioService.OnAudioStateChangeListener {

    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 1001;

    private List<AudioTrack> fullCatalog;
    private List<AudioTrack> displayedTracks;
    private AudioTrackAdapter trackAdapter;
    private int currentTrackIndex = -1;

    private BackgroundAudioService audioService;
    private boolean isBound = false;

    private Handler uiHandler = new Handler();
    private Runnable progressUpdater;

    // UI elements
    private EditText etSearch;
    private Button chipAll, chipTrending, chipLofi, chipPodcasts, chipFavorites;
    private ListView lvTracks;

    // Mini Player
    private LinearLayout miniPlayerBar;
    private TextView tvMiniIcon, tvMiniTitle, tvMiniArtist;
    private TextView btnMiniPlayPause, btnMiniNext;

    // Full Player Overlay
    private RelativeLayout fullPlayerOverlay;
    private TextView btnCloseFullPlayer, btnFullFav;
    private TextView tvFullLargeIcon, tvFullTitle, tvFullArtist;
    private SeekBar seekbarAudio, seekbarVolume;
    private TextView tvCurrentTime, tvTotalTime;
    private TextView btnFullShuffle, btnFullPrev, btnFullPlayPause, btnFullNext, btnFullRepeat;

    // Header & Features
    private TextView btnSleepTimer;
    private AudioManager audioManager;
    private Handler sleepTimerHandler = new Handler();
    private Runnable sleepTimerRunnable;

    private String activeCategory = "All";
    private boolean isShuffle = false;
    private boolean isRepeat = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BackgroundAudioService.LocalBinder binder = (BackgroundAudioService.LocalBinder) service;
            audioService = binder.getService();
            audioService.setOnAudioStateChangeListener(MainActivity.this);
            isBound = true;
            updateUIFromService();
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

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        fullCatalog = AudioTrack.getSampleCatalog();
        displayedTracks = new ArrayList<AudioTrack>(fullCatalog);

        initViews();
        setupListView();
        setupSearchAndFilters();
        setupPlayerControls();
        setupVolumeControl();
        checkNotificationPermission();
        startAndBindService();

        initProgressRunnable();
    }

    private void initViews() {
        etSearch = findViewById(R.id.et_search);
        chipAll = findViewById(R.id.chip_all);
        chipTrending = findViewById(R.id.chip_trending);
        chipLofi = findViewById(R.id.chip_lofi);
        chipPodcasts = findViewById(R.id.chip_podcasts);
        chipFavorites = findViewById(R.id.chip_favorites);
        lvTracks = findViewById(R.id.lv_audio_tracks);

        miniPlayerBar = findViewById(R.id.mini_player_bar);
        tvMiniIcon = findViewById(R.id.tv_mini_icon);
        tvMiniTitle = findViewById(R.id.tv_mini_title);
        tvMiniArtist = findViewById(R.id.tv_mini_artist);
        btnMiniPlayPause = findViewById(R.id.btn_mini_play_pause);
        btnMiniNext = findViewById(R.id.btn_mini_next);

        fullPlayerOverlay = findViewById(R.id.full_player_overlay);
        btnCloseFullPlayer = findViewById(R.id.btn_close_full_player);
        btnFullFav = findViewById(R.id.btn_full_fav);
        tvFullLargeIcon = findViewById(R.id.tv_full_large_icon);
        tvFullTitle = findViewById(R.id.tv_full_title);
        tvFullArtist = findViewById(R.id.tv_full_artist);
        seekbarAudio = findViewById(R.id.seekbar_audio);
        seekbarVolume = findViewById(R.id.seekbar_volume);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalTime = findViewById(R.id.tv_total_time);

        btnFullShuffle = findViewById(R.id.btn_full_shuffle);
        btnFullPrev = findViewById(R.id.btn_full_prev);
        btnFullPlayPause = findViewById(R.id.btn_full_play_pause);
        btnFullNext = findViewById(R.id.btn_full_next);
        btnFullRepeat = findViewById(R.id.btn_full_repeat);
        btnSleepTimer = findViewById(R.id.btn_sleep_timer);
    }

    private void setupListView() {
        trackAdapter = new AudioTrackAdapter();
        lvTracks.setAdapter(trackAdapter);

        lvTracks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                currentTrackIndex = position;
                playCurrentTrack();
            }
        });
    }

    private void setupSearchAndFilters() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTracks();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        View.OnClickListener chipListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetChipBackgrounds();
                Button b = (Button) v;
                b.setBackgroundResource(R.color.yt_red);

                if (v.getId() == R.id.chip_all) activeCategory = "All";
                else if (v.getId() == R.id.chip_trending) activeCategory = "Trending";
                else if (v.getId() == R.id.chip_lofi) activeCategory = "Lo-Fi";
                else if (v.getId() == R.id.chip_podcasts) activeCategory = "Podcasts";
                else if (v.getId() == R.id.chip_favorites) activeCategory = "Favorites";

                filterTracks();
            }
        };

        chipAll.setOnClickListener(chipListener);
        chipTrending.setOnClickListener(chipListener);
        chipLofi.setOnClickListener(chipListener);
        chipPodcasts.setOnClickListener(chipListener);
        chipFavorites.setOnClickListener(chipListener);
    }

    private void resetChipBackgrounds() {
        chipAll.setBackgroundResource(R.color.chip_bg);
        chipTrending.setBackgroundResource(R.color.chip_bg);
        chipLofi.setBackgroundResource(R.color.chip_bg);
        chipPodcasts.setBackgroundResource(R.color.chip_bg);
        chipFavorites.setBackgroundResource(R.color.chip_bg);
    }

    private void filterTracks() {
        String query = etSearch.getText().toString().trim().toLowerCase();
        displayedTracks.clear();

        for (AudioTrack track : fullCatalog) {
            boolean matchesCategory = activeCategory.equals("All") ||
                    (activeCategory.equals("Favorites") && track.isFavorite()) ||
                    track.getCategory().equalsIgnoreCase(activeCategory);

            boolean matchesQuery = query.isEmpty() ||
                    track.getTitle().toLowerCase().contains(query) ||
                    track.getArtist().toLowerCase().contains(query);

            if (matchesCategory && matchesQuery) {
                displayedTracks.add(track);
            }
        }
        trackAdapter.notifyDataSetChanged();
    }

    private void setupPlayerControls() {
        miniPlayerBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentTrackIndex >= 0 && currentTrackIndex < displayedTracks.size()) {
                    fullPlayerOverlay.setVisibility(View.VISIBLE);
                }
            }
        });

        btnCloseFullPlayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fullPlayerOverlay.setVisibility(View.GONE);
            }
        });

        View.OnClickListener togglePlayPauseListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBound && audioService != null) {
                    if (currentTrackIndex == -1 && !displayedTracks.isEmpty()) {
                        currentTrackIndex = 0;
                        playCurrentTrack();
                    } else {
                        audioService.togglePlayPause();
                    }
                }
            }
        };

        btnMiniPlayPause.setOnClickListener(togglePlayPauseListener);
        btnFullPlayPause.setOnClickListener(togglePlayPauseListener);

        View.OnClickListener nextListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNextTrack();
            }
        };
        btnMiniNext.setOnClickListener(nextListener);
        btnFullNext.setOnClickListener(nextListener);

        btnFullPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPreviousTrack();
            }
        });

        btnFullShuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isShuffle = !isShuffle;
                btnFullShuffle.setTextColor(getResources().getColor(isShuffle ? R.color.yt_red : R.color.text_gray));
                Toast.makeText(MainActivity.this, isShuffle ? "Shuffle ON" : "Shuffle OFF", Toast.LENGTH_SHORT).show();
            }
        });

        btnFullRepeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isRepeat = !isRepeat;
                btnFullRepeat.setTextColor(getResources().getColor(isRepeat ? R.color.yt_red : R.color.text_gray));
                Toast.makeText(MainActivity.this, isRepeat ? "Repeat Track ON" : "Repeat Track OFF", Toast.LENGTH_SHORT).show();
            }
        });

        btnFullFav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentTrackIndex >= 0 && currentTrackIndex < displayedTracks.size()) {
                    AudioTrack track = displayedTracks.get(currentTrackIndex);
                    track.setFavorite(!track.isFavorite());
                    btnFullFav.setText(track.isFavorite() ? "❤️" : "♡");
                    trackAdapter.notifyDataSetChanged();
                }
            }
        });

        seekbarAudio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && isBound && audioService != null) {
                    int duration = audioService.getDuration();
                    if (duration > 0) {
                        int seekToMs = (int) (((float) progress / 100) * duration);
                        audioService.seekTo(seekToMs);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnSleepTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSleepTimerDialog();
            }
        });
    }

    private void setupVolumeControl() {
        if (audioManager != null) {
            int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            seekbarVolume.setMax(maxVol);
            seekbarVolume.setProgress(curVol);

            seekbarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && audioManager != null) {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }
    }

    private void playCurrentTrack() {
        if (currentTrackIndex >= 0 && currentTrackIndex < displayedTracks.size()) {
            AudioTrack track = displayedTracks.get(currentTrackIndex);
            updateTrackInfoUI(track);

            if (isBound && audioService != null) {
                audioService.playTrack(track.getStreamUrl(), track.getTitle(), track.getArtist());
            }
            uiHandler.post(progressUpdater);
        }
    }

    private void playNextTrack() {
        if (displayedTracks.isEmpty()) return;

        if (isShuffle) {
            currentTrackIndex = (int) (Math.random() * displayedTracks.size());
        } else {
            currentTrackIndex = (currentTrackIndex + 1) % displayedTracks.size();
        }
        playCurrentTrack();
    }

    private void playPreviousTrack() {
        if (displayedTracks.isEmpty()) return;

        if (currentTrackIndex > 0) {
            currentTrackIndex--;
        } else {
            currentTrackIndex = displayedTracks.size() - 1;
        }
        playCurrentTrack();
    }

    private void updateTrackInfoUI(AudioTrack track) {
        tvMiniTitle.setText(track.getTitle());
        tvMiniArtist.setText(track.getArtist());
        tvMiniIcon.setText(track.getIcon());

        tvFullTitle.setText(track.getTitle());
        tvFullArtist.setText(track.getArtist());
        tvFullLargeIcon.setText(track.getIcon());
        btnFullFav.setText(track.isFavorite() ? "❤️" : "♡");
        tvTotalTime.setText(track.getFormattedDuration());
    }

    private void initProgressRunnable() {
        progressUpdater = new Runnable() {
            @Override
            public void run() {
                if (isBound && audioService != null && audioService.isPlaying()) {
                    int currentMs = audioService.getCurrentPosition();
                    int totalMs = audioService.getDuration();

                    if (totalMs > 0) {
                        int progressPercent = (int) (((float) currentMs / totalMs) * 100);
                        seekbarAudio.setProgress(progressPercent);
                        tvCurrentTime.setText(formatTime(currentMs / 1000));
                        tvTotalTime.setText(formatTime(totalMs / 1000));
                    }
                    uiHandler.postDelayed(this, 1000);
                }
            }
        };
    }

    private String formatTime(int totalSecs) {
        int m = totalSecs / 60;
        int s = totalSecs % 60;
        return String.format("%d:%02d", m, s);
    }

    @Override
    public void onTrackPrepared(int durationMs) {
        tvTotalTime.setText(formatTime(durationMs / 1000));
        uiHandler.post(progressUpdater);
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        btnMiniPlayPause.setText(isPlaying ? "⏸" : "▶");
        btnFullPlayPause.setText(isPlaying ? "⏸" : "▶");
        if (isPlaying) {
            uiHandler.post(progressUpdater);
        }
    }

    @Override
    public void onTrackCompleted() {
        if (isRepeat) {
            playCurrentTrack();
        } else {
            playNextTrack();
        }
    }

    @Override
    public void onError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void updateUIFromService() {
        if (isBound && audioService != null && audioService.isPlaying()) {
            onPlaybackStateChanged(true);
        }
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

    private void showSleepTimerDialog() {
        final String[] options = {"5 Minutes", "15 Minutes", "30 Minutes", "60 Minutes", "Turn Off Timer"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Sleep Timer");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (sleepTimerRunnable != null) {
                    sleepTimerHandler.removeCallbacks(sleepTimerRunnable);
                }

                long delayMs = 0;
                if (which == 0) delayMs = 5 * 60 * 1000;
                else if (which == 1) delayMs = 15 * 60 * 1000;
                else if (which == 2) delayMs = 30 * 60 * 1000;
                else if (which == 3) delayMs = 60 * 60 * 1000;

                if (delayMs > 0) {
                    sleepTimerRunnable = new Runnable() {
                        @Override
                        public void run() {
                            if (isBound && audioService != null) {
                                audioService.pauseAudio();
                                Toast.makeText(MainActivity.this, "Sleep Timer: Audio Paused", Toast.LENGTH_LONG).show();
                            }
                        }
                    };
                    sleepTimerHandler.postDelayed(sleepTimerRunnable, delayMs);
                    Toast.makeText(MainActivity.this, "Timer set for " + options[which], Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Sleep Timer Cancelled", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.show();
    }

    @Override
    public void onBackPressed() {
        if (fullPlayerOverlay.getVisibility() == View.VISIBLE) {
            fullPlayerOverlay.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        uiHandler.removeCallbacks(progressUpdater);
        super.onDestroy();
    }

    // Standard Custom Adapter for ListView
    private class AudioTrackAdapter extends BaseAdapter {

        @Override
        public int getCount() { return displayedTracks.size(); }

        @Override
        public Object getItem(int position) { return displayedTracks.get(position); }

        @Override
        public long getItemId(int position) { return position; }

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
            final TextView btnFav = convertView.findViewById(R.id.btn_track_fav);

            tvIcon.setText(track.getIcon());
            tvTitle.setText(track.getTitle());
            tvSubtitle.setText(track.getArtist() + " • " + track.getCategory());
            tvDuration.setText(track.getFormattedDuration());
            btnFav.setText(track.isFavorite() ? "❤️" : "♡");

            btnFav.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    track.setFavorite(!track.isFavorite());
                    btnFav.setText(track.isFavorite() ? "❤️" : "♡");
                }
            });

            return convertView;
        }
    }
}