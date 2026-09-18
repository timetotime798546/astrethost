package com.harmonyplayer.app;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {

    private MusicService musicService;
    private boolean musicBound = false;

    // Layout Sections
    private LinearLayout containerLibrary;
    private LinearLayout containerPlayer;
    private LinearLayout containerEffects;

    private TextView txtNavLibrary;
    private TextView txtNavPlayer;
    private TextView txtNavEffects;

    private ListView songListView;
    private EditText searchBar;
    private Button btnAllSongs;
    private Button btnFavSongs;

    private VisualizerView playerVisualizer;
    private TextView playerSongTitle;
    private TextView playerSongArtist;
    private TextView playerTimeCurrent;
    private TextView playerTimeTotal;
    private SeekBar playerSeekBar;
    private Button btnPlay;
    private Button btnPrev;
    private Button btnNext;
    private Button btnToggleFav;

    private TextView lblSleepTimer;
    private Button btnTimer5;
    private Button btnTimer15;
    private Button btnTimer30;
    private Button btnTimerOff;

    // EQ Sliders
    private SeekBar eqBassBar;
    private SeekBar eqTrebleBar;
    private SeekBar eqVocalsBar;
    private SeekBar eqReverbBar;
    private TextView lblEqBass;
    private TextView lblEqTreble;
    private TextView lblEqVocals;
    private TextView lblEqReverb;

    private Button btnRegenerateSynths;

    // State Variables
    private List<Song> allSongs = new ArrayList<>();
    private List<Song> filteredSongs = new ArrayList<>();
    private SongAdapter songAdapter;
    private boolean showFavoritesOnly = false;
    private Set<String> favoriteSongPaths = new HashSet<>();
    private SharedPreferences sharedPrefs;

    private static final int PERMISSION_REQUEST_CODE = 1002;
    private Handler progressHandler;
    private static final int UPDATE_PROGRESS_MSG = 101;

    // Sleep Timer countdown
    private Handler sleepTimerHandler = new Handler();
    private int sleepTimeRemainingSeconds = 0;
    private Runnable sleepTimerRunnable;

    private MusicReceiver musicReceiver;

    private ServiceConnection playConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            musicService.setPlaylist(allSongs);
            musicBound = true;
            updateUIForCurrentPlaying();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPrefs = getSharedPreferences("harmony_prefs", MODE_PRIVATE);
        loadFavorites();

        initViews();
        setupNavigation();
        setupProgressHandler();
        generateDemoSynthTracks();
        checkPermissionsAndScan();
        setupSleepTimer();
        setupEqualizer();

        // Bind Service
        Intent playIntent = new Intent(this, MusicService.class);
        startService(playIntent);
        bindService(playIntent, playConnection, Context.BIND_AUTO_CREATE);

        // Register broadcast receiver safely for Android 14+ (API 34)
        musicReceiver = new MusicReceiver();
        IntentFilter filter = new IntentFilter(MusicService.ACTION_STATUS_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(musicReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(musicReceiver, filter);
        }
    }

    private void initViews() {
        // Layout Sections
        containerLibrary = (LinearLayout) findViewById(R.id.container_library);
        containerPlayer = (LinearLayout) findViewById(R.id.container_player);
        containerEffects = (LinearLayout) findViewById(R.id.container_effects);

        // Navigation Text
        txtNavLibrary = (TextView) findViewById(R.id.txt_nav_library);
        txtNavPlayer = (TextView) findViewById(R.id.txt_nav_player);
        txtNavEffects = (TextView) findViewById(R.id.txt_nav_effects);

        // Library Section
        songListView = (ListView) findViewById(R.id.song_listview);
        searchBar = (EditText) findViewById(R.id.search_bar);
        btnAllSongs = (Button) findViewById(R.id.btn_all_songs);
        btnFavSongs = (Button) findViewById(R.id.btn_fav_songs);

        // Player Section
        playerVisualizer = (VisualizerView) findViewById(R.id.player_visualizer);
        playerSongTitle = (TextView) findViewById(R.id.player_song_title);
        playerSongArtist = (TextView) findViewById(R.id.player_song_artist);
        playerTimeCurrent = (TextView) findViewById(R.id.player_time_current);
        playerTimeTotal = (TextView) findViewById(R.id.player_time_total);
        playerSeekBar = (SeekBar) findViewById(R.id.player_seekbar);
        btnPlay = (Button) findViewById(R.id.btn_play);
        btnPrev = (Button) findViewById(R.id.btn_prev);
        btnNext = (Button) findViewById(R.id.btn_next);
        btnToggleFav = (Button) findViewById(R.id.btn_toggle_fav);

        // Sleep Timer
        lblSleepTimer = (TextView) findViewById(R.id.lbl_sleep_timer);
        btnTimer5 = (Button) findViewById(R.id.btn_timer_5);
        btnTimer15 = (Button) findViewById(R.id.btn_timer_15);
        btnTimer30 = (Button) findViewById(R.id.btn_timer_30);
        btnTimerOff = (Button) findViewById(R.id.btn_timer_off);

        // EQ
        eqBassBar = (SeekBar) findViewById(R.id.eq_bass_bar);
        eqTrebleBar = (SeekBar) findViewById(R.id.eq_treble_bar);
        eqVocalsBar = (SeekBar) findViewById(R.id.eq_vocals_bar);
        eqReverbBar = (SeekBar) findViewById(R.id.eq_reverb_bar);
        lblEqBass = (TextView) findViewById(R.id.lbl_eq_bass);
        lblEqTreble = (TextView) findViewById(R.id.lbl_eq_treble);
        lblEqVocals = (TextView) findViewById(R.id.lbl_eq_vocals);
        lblEqReverb = (TextView) findViewById(R.id.lbl_eq_reverb);

        btnRegenerateSynths = (Button) findViewById(R.id.btn_regenerate_synths);

        // Init adapter
        songAdapter = new SongAdapter();
        songListView.setAdapter(songAdapter);

        // Listeners
        songListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Song song = filteredSongs.get(position);
                int playlistIndex = allSongs.indexOf(song);
                if (musicBound && playlistIndex != -1) {
                    musicService.playSong(playlistIndex);
                    showSection(2); // Auto navigate to Player screen
                }
            }
        });

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnAllSongs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFavoritesOnly = false;
                btnAllSongs.setTextColor(0xFFFFFFFF);
                btnFavSongs.setTextColor(0xFF9CA3AF);
                applyFilter(searchBar.getText().toString());
            }
        });

        btnFavSongs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFavoritesOnly = true;
                btnFavSongs.setTextColor(0xFFFFFFFF);
                btnAllSongs.setTextColor(0xFF9CA3AF);
                applyFilter(searchBar.getText().toString());
            }
        });

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (musicBound) {
                    musicService.playPause();
                }
            }
        });

        btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (musicBound) musicService.prev();
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (musicBound) musicService.next();
            }
        });

        btnToggleFav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleCurrentSongFavorite();
            }
        });

        playerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && musicBound) {
                    musicService.seekTo(progress);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnRegenerateSynths.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                regenerateSynthsAction();
            }
        });
    }

    private void setupNavigation() {
        findViewById(R.id.nav_library).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSection(1);
            }
        });
        findViewById(R.id.nav_player).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSection(2);
            }
        });
        findViewById(R.id.nav_effects).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSection(3);
            }
        });
    }

    private void showSection(int index) {
        containerLibrary.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        containerPlayer.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        containerEffects.setVisibility(index == 3 ? View.VISIBLE : View.GONE);

        txtNavLibrary.setTextColor(index == 1 ? 0xFF3B82F6 : 0xFF9CA3AF);
        txtNavPlayer.setTextColor(index == 2 ? 0xFF3B82F6 : 0xFF9CA3AF);
        txtNavEffects.setTextColor(index == 3 ? 0xFF3B82F6 : 0xFF9CA3AF);
    }

    private void setupProgressHandler() {
        progressHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (msg.what == UPDATE_PROGRESS_MSG) {
                    if (musicBound && musicService.isPlaying()) {
                        int pos = musicService.getPosition();
                        int dur = musicService.getDuration();

                        playerSeekBar.setMax(dur);
                        playerSeekBar.setProgress(pos);

                        playerTimeCurrent.setText(formatTime(pos));
                        playerTimeTotal.setText(formatTime(dur));
                    }
                    progressHandler.sendEmptyMessageDelayed(UPDATE_PROGRESS_MSG, 500);
                }
                return true;
            }
        });
        progressHandler.sendEmptyMessage(UPDATE_PROGRESS_MSG);
    }

    private void generateDemoSynthTracks() {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Synthesizing dynamic Premium Loop Tracks...");
        dialog.setCancelable(false);
        dialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                File cacheDir = getCacheDir();
                final List<Song> demoTracks = new ArrayList<>();

                File t1 = WavGenerator.generateTrack(cacheDir, 1);
                demoTracks.add(new Song(9001, "Neon Horizon (Synthwave Loop)", "Harmony Synthesizer", t1.getAbsolutePath(), 15000, true));

                File t2 = WavGenerator.generateTrack(cacheDir, 2);
                demoTracks.add(new Song(9002, "Retro Lullaby (Chiptune Loop)", "Harmony Synthesizer", t2.getAbsolutePath(), 15000, true));

                File t3 = WavGenerator.generateTrack(cacheDir, 3);
                demoTracks.add(new Song(9003, "Cosmic Whispers (Ambient Loop)", "Harmony Synthesizer", t3.getAbsolutePath(), 15000, true));

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing() || isDestroyed()) return;
                        dialog.dismiss();
                        allSongs.addAll(demoTracks);
                        applyFilter("");
                        if (musicBound) {
                            musicService.setPlaylist(allSongs);
                        }
                    }
                });
            }
        }).start();
    }

    private void regenerateSynthsAction() {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Regenerating sound modules...");
        dialog.setCancelable(false);
        dialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                File cacheDir = getCacheDir();
                for (int i = 1; i <= 3; i++) {
                    File f = new File(cacheDir, "track_" + i + ".wav");
                    if (f.exists()) f.delete();
                }

                WavGenerator.generateTrack(cacheDir, 1);
                WavGenerator.generateTrack(cacheDir, 2);
                WavGenerator.generateTrack(cacheDir, 3);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing() || isDestroyed()) return;
                        dialog.dismiss();
                        Toast.makeText(MainActivity.this, "Synth patches successfully re-routed and tuned!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    private void checkPermissionsAndScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_AUDIO}, PERMISSION_REQUEST_CODE);
            } else {
                scanLocalSongs();
            }
        } else {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            } else {
                scanLocalSongs();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanLocalSongs();
            } else {
                Toast.makeText(this, "Storage scan disabled. Only demo synthesizers will play.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void scanLocalSongs() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<Song> localSongs = new ArrayList<>();
                android.content.ContentResolver resolver = getContentResolver();
                android.net.Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                String selection = android.provider.MediaStore.Audio.Media.IS_MUSIC + "!= 0";
                String sortOrder = android.provider.MediaStore.Audio.Media.TITLE + " ASC";

                android.database.Cursor cursor = null;
                try {
                    cursor = resolver.query(uri, null, selection, null, sortOrder);
                    if (cursor != null && cursor.moveToFirst()) {
                        int titleCol = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE);
                        int artistCol = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.ARTIST);
                        int pathCol = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.DATA);
                        int durationCol = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.DURATION);
                        int idCol = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media._ID);

                        do {
                            long id = idCol >= 0 ? cursor.getLong(idCol) : 0;
                            String title = titleCol >= 0 ? cursor.getString(titleCol) : "Unknown Title";
                            String artist = artistCol >= 0 ? cursor.getString(artistCol) : "Unknown Artist";
                            String path = pathCol >= 0 ? cursor.getString(pathCol) : "";
                            long duration = durationCol >= 0 ? cursor.getLong(durationCol) : 0;

                            if (path != null && !path.isEmpty()) {
                                localSongs.add(new Song(id, title, artist, path, duration, false));
                            }
                        } while (cursor.moveToNext());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (cursor != null) cursor.close();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing() || isDestroyed()) return;
                        if (!localSongs.isEmpty()) {
                            allSongs.addAll(localSongs);
                            applyFilter("");
                            if (musicService != null) {
                                musicService.setPlaylist(allSongs);
                            }
                        }
                    }
                });
            }
        }).start();
    }

    private void applyFilter(String query) {
        filteredSongs.clear();
        String q = query.toLowerCase(Locale.getDefault());
        for (int i = 0; i < allSongs.size(); i++) {
            Song song = allSongs.get(i);
            boolean matchesSearch = song.getTitle().toLowerCase(Locale.getDefault()).contains(q)
                    || song.getArtist().toLowerCase(Locale.getDefault()).contains(q);
            boolean matchesFav = !showFavoritesOnly || favoriteSongPaths.contains(song.getPath());

            if (matchesSearch && matchesFav) {
                filteredSongs.add(song);
            }
        }
        songAdapter.notifyDataSetChanged();
    }

    private void updateUIForCurrentPlaying() {
        if (!musicBound) return;
        int index = musicService.getCurrentSongIndex();
        if (index >= 0 && index < allSongs.size()) {
            Song current = allSongs.get(index);
            playerSongTitle.setText(current.getTitle());
            playerSongArtist.setText(current.getArtist());
            boolean isFav = favoriteSongPaths.contains(current.getPath());
            btnToggleFav.setText(isFav ? "❤ Favorited" : "❤ Add to Favorites");

            boolean playing = musicService.isPlaying();
            btnPlay.setText(playing ? "⏸" : "▶");
            playerVisualizer.setPlaying(playing);
        }
    }

    private void toggleCurrentSongFavorite() {
        if (!musicBound) return;
        int index = musicService.getCurrentSongIndex();
        if (index >= 0 && index < allSongs.size()) {
            Song current = allSongs.get(index);
            String path = current.getPath();
            if (favoriteSongPaths.contains(path)) {
                favoriteSongPaths.remove(path);
                btnToggleFav.setText("❤ Add to Favorites");
                Toast.makeText(this, "Removed from Favorites", Toast.LENGTH_SHORT).show();
            } else {
                favoriteSongPaths.add(path);
                btnToggleFav.setText("❤ Favorited");
                Toast.makeText(this, "Added to Favorites", Toast.LENGTH_SHORT).show();
            }
            saveFavorites();
            if (showFavoritesOnly) {
                applyFilter(searchBar.getText().toString());
            }
        }
    }

    private void loadFavorites() {
        Set<String> favs = sharedPrefs.getStringSet("favorites", null);
        if (favs != null) {
            favoriteSongPaths.addAll(favs);
        }
    }

    private void saveFavorites() {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putStringSet("favorites", favoriteSongPaths);
        editor.apply();
    }

    private void setupSleepTimer() {
        sleepTimerRunnable = new Runnable() {
            @Override
            public void run() {
                if (sleepTimeRemainingSeconds > 0) {
                    sleepTimeRemainingSeconds--;
                    int mins = sleepTimeRemainingSeconds / 60;
                    int secs = sleepTimeRemainingSeconds % 60;
                    lblSleepTimer.setText(String.format(Locale.getDefault(), "💤 Sleep Timer: %02d:%02d", mins, secs));
                    sleepTimerHandler.postDelayed(this, 1000);
                } else {
                    lblSleepTimer.setText("💤 Sleep Timer: Off");
                    if (musicBound && musicService.isPlaying()) {
                        musicService.playPause();
                        Toast.makeText(MainActivity.this, "Sleep timer expired. Playback paused.", Toast.LENGTH_LONG).show();
                    }
                }
            }
        };

        btnTimer5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSleepTimer(5);
            }
        });
        btnTimer15.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSleepTimer(15);
            }
        });
        btnTimer30.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSleepTimer(30);
            }
        });
        btnTimerOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelSleepTimer();
            }
        });
    }

    private void startSleepTimer(int minutes) {
        sleepTimerHandler.removeCallbacks(sleepTimerRunnable);
        sleepTimeRemainingSeconds = minutes * 60;
        sleepTimerHandler.post(sleepTimerRunnable);
        Toast.makeText(this, "Sleep timer set for " + minutes + " minutes.", Toast.LENGTH_SHORT).show();
    }

    private void cancelSleepTimer() {
        sleepTimerHandler.removeCallbacks(sleepTimerRunnable);
        sleepTimeRemainingSeconds = 0;
        lblSleepTimer.setText("💤 Sleep Timer: Off");
        Toast.makeText(this, "Sleep timer cancelled.", Toast.LENGTH_SHORT).show();
    }

    private void setupEqualizer() {
        SeekBar.OnSeekBarChangeListener eqChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateEQText();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        eqBassBar.setOnSeekBarChangeListener(eqChangeListener);
        eqTrebleBar.setOnSeekBarChangeListener(eqChangeListener);
        eqVocalsBar.setOnSeekBarChangeListener(eqChangeListener);
        eqReverbBar.setOnSeekBarChangeListener(eqChangeListener);

        // Equalizer Preset Configurations
        findViewById(R.id.btn_preset_flat).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                eqBassBar.setProgress(15);
                eqTrebleBar.setProgress(15);
                eqVocalsBar.setProgress(15);
                eqReverbBar.setProgress(0);
                Toast.makeText(MainActivity.this, "Preset: Flat Loaded", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_preset_bass).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                eqBassBar.setProgress(26);
                eqTrebleBar.setProgress(12);
                eqVocalsBar.setProgress(14);
                eqReverbBar.setProgress(3);
                Toast.makeText(MainActivity.this, "Preset: Bass Booster Loaded", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_preset_vocal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                eqBassBar.setProgress(11);
                eqTrebleBar.setProgress(22);
                eqVocalsBar.setProgress(28);
                eqReverbBar.setProgress(2);
                Toast.makeText(MainActivity.this, "Preset: Vocal Enhancer Loaded", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_preset_electronic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                eqBassBar.setProgress(24);
                eqTrebleBar.setProgress(25);
                eqVocalsBar.setProgress(12);
                eqReverbBar.setProgress(6);
                Toast.makeText(MainActivity.this, "Preset: Electro Phase Loaded", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateEQText() {
        int bassDB = eqBassBar.getProgress() - 15;
        int trebleDB = eqTrebleBar.getProgress() - 15;
        int vocalDB = eqVocalsBar.getProgress() - 15;
        int reverb = eqReverbBar.getProgress();

        lblEqBass.setText("Sub Bass Boost: " + (bassDB >= 0 ? "+" : "") + bassDB + "dB");
        lblEqTreble.setText("High Treble Clarity: " + (trebleDB >= 0 ? "+" : "") + trebleDB + "dB");
        lblEqVocals.setText("Vocal Mid Presence: " + (vocalDB >= 0 ? "+" : "") + vocalDB + "dB");
        lblEqReverb.setText("Stage Reverb Depth: " + (reverb == 0 ? "Off" : reverb + "/10"));
    }

    private String formatTime(int ms) {
        int totalSecs = ms / 1000;
        int minutes = totalSecs / 60;
        int seconds = totalSecs % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    private class SongAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return filteredSongs.size();
        }

        @Override
        public Object getItem(int position) {
            return filteredSongs.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(android.R.layout.simple_list_item_2, parent, false);
            }
            TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
            TextView text2 = (TextView) convertView.findViewById(android.R.id.text2);

            Song song = filteredSongs.get(position);
            text1.setText(song.getTitle());
            text1.setTextColor(0xFFFFFFFF);
            text1.setTextSize(16);

            String durationStr = formatTime((int) song.getDuration());
            String subtitle = song.getArtist() + " • " + durationStr + (song.isDemo() ? " [SYNTH]" : "");
            text2.setText(subtitle);
            text2.setTextColor(0xFF9CA3AF);
            text2.setTextSize(12);

            return convertView;
        }
    }

    private class MusicReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (MusicService.ACTION_STATUS_CHANGED.equals(intent.getAction())) {
                updateUIForCurrentPlaying();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (musicBound) {
            try {
                unbindService(playConnection);
            } catch (Exception ignored) {}
            musicBound = false;
        }
        if (musicReceiver != null) {
            try {
                unregisterReceiver(musicReceiver);
            } catch (Exception ignored) {}
        }
        progressHandler.removeMessages(UPDATE_PROGRESS_MSG);
        sleepTimerHandler.removeCallbacks(sleepTimerRunnable);
        super.onDestroy();
    }
}