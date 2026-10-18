package com.radiofmlive.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements RadioPlayerService.ServiceCallback {

    private static final int TAB_ALL = 0;
    private static final int TAB_FAVORITES = 1;
    private static final int TAB_RECENT = 2;

    private PreferencesManager prefsManager;
    private RadioPlayerService playerService;
    private boolean isBound = false;

    private List<RadioStation> allStations;
    private final List<RadioStation> displayedStations = new ArrayList<RadioStation>();

    private int activeTab = TAB_ALL;
    private String currentSearchQuery = "";
    private String selectedGenre = "All Genres";

    // Views
    private TextView txtCurrentStationName;
    private TextView txtCurrentStationInfo;
    private TextView txtPlaybackStatus;
    private TextView txtTimerStatus;
    private ProgressBar progressBuffering;
    private ImageButton btnPlayPause;
    private ImageView btnStop;
    private ImageView btnHeroFav;
    private ImageView btnHeroTimer;
    private ImageView btnSleepTimerTop;
    private ImageView btnMenuOptions;

    private Button tabAll;
    private Button tabFavorites;
    private Button tabRecent;

    private EditText editSearch;
    private Spinner spinnerGenre;
    private TextView txtListHeader;
    private TextView txtEmptyView;
    private LinearLayout containerStations;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            RadioPlayerService.LocalBinder b = (RadioPlayerService.LocalBinder) binder;
            playerService = b.getService();
            isBound = true;
            playerService.setCallback(MainActivity.this);
            syncPlayerState();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            playerService = null;
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefsManager = new PreferencesManager(this);
        allStations = StationData.getPresetStations();

        initViews();
        setupFilters();
        setupEvents();

        startAndBindService();
        applyFiltersAndRender();
    }

    private void initViews() {
        txtCurrentStationName = (TextView) findViewById(R.id.txtCurrentStationName);
        txtCurrentStationInfo = (TextView) findViewById(R.id.txtCurrentStationInfo);
        txtPlaybackStatus = (TextView) findViewById(R.id.txtPlaybackStatus);
        txtTimerStatus = (TextView) findViewById(R.id.txtTimerStatus);
        progressBuffering = (ProgressBar) findViewById(R.id.progressBuffering);
        btnPlayPause = (ImageButton) findViewById(R.id.btnPlayPause);
        btnStop = (ImageView) findViewById(R.id.btnStop);
        btnHeroFav = (ImageView) findViewById(R.id.btnHeroFav);
        btnHeroTimer = (ImageView) findViewById(R.id.btnHeroTimer);
        btnSleepTimerTop = (ImageView) findViewById(R.id.btnSleepTimerTop);
        btnMenuOptions = (ImageView) findViewById(R.id.btnMenuOptions);

        tabAll = (Button) findViewById(R.id.tabAll);
        tabFavorites = (Button) findViewById(R.id.tabFavorites);
        tabRecent = (Button) findViewById(R.id.tabRecent);

        editSearch = (EditText) findViewById(R.id.editSearch);
        spinnerGenre = (Spinner) findViewById(R.id.spinnerGenre);
        txtListHeader = (TextView) findViewById(R.id.txtListHeader);
        txtEmptyView = (TextView) findViewById(R.id.txtEmptyView);
        containerStations = (LinearLayout) findViewById(R.id.containerStations);
    }

    private void setupFilters() {
        List<String> genres = StationData.getAllGenres(allStations);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, genres);
        spinnerGenre.setAdapter(adapter);

        spinnerGenre.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedGenre = (String) parent.getItemAtPosition(position);
                applyFiltersAndRender();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim().toLowerCase(Locale.getDefault());
                applyFiltersAndRender();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupEvents() {
        btnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (playerService == null) return;
                int state = playerService.getCurrentState();
                if (state == RadioPlayerService.STATE_PLAYING) {
                    playerService.pause();
                } else if (state == RadioPlayerService.STATE_PAUSED) {
                    playerService.play();
                } else {
                    RadioStation current = playerService.getCurrentStation();
                    if (current != null) {
                        playerService.playStation(current);
                    } else if (!displayedStations.isEmpty()) {
                        playStation(displayedStations.get(0));
                    } else if (!allStations.isEmpty()) {
                        playStation(allStations.get(0));
                    }
                }
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (playerService != null) {
                    playerService.stop();
                }
            }
        });

        btnHeroFav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (playerService != null && playerService.getCurrentStation() != null) {
                    RadioStation st = playerService.getCurrentStation();
                    prefsManager.toggleFavorite(st.getId());
                    updateHeroFavoriteIcon(st);
                    applyFiltersAndRender();
                } else {
                    Toast.makeText(MainActivity.this, "No station is active", Toast.LENGTH_SHORT).show();
                }
            }
        });

        View.OnClickListener timerClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSleepTimerDialog();
            }
        };
        btnHeroTimer.setOnClickListener(timerClickListener);
        btnSleepTimerTop.setOnClickListener(timerClickListener);

        btnMenuOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAppMenuDialog();
            }
        });

        tabAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activeTab = TAB_ALL;
                updateTabStyles();
                applyFiltersAndRender();
            }
        });

        tabFavorites.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activeTab = TAB_FAVORITES;
                updateTabStyles();
                applyFiltersAndRender();
            }
        });

        tabRecent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activeTab = TAB_RECENT;
                updateTabStyles();
                applyFiltersAndRender();
            }
        });
    }

    private void updateTabStyles() {
        int activeColor = getResources().getColor(R.color.accent_cyan);
        int inactiveColor = getResources().getColor(R.color.text_secondary);

        tabAll.setTextColor(activeTab == TAB_ALL ? activeColor : inactiveColor);
        tabFavorites.setTextColor(activeTab == TAB_FAVORITES ? activeColor : inactiveColor);
        tabRecent.setTextColor(activeTab == TAB_RECENT ? activeColor : inactiveColor);
    }

    private void applyFiltersAndRender() {
        displayedStations.clear();

        List<RadioStation> sourceList = new ArrayList<RadioStation>();
        if (activeTab == TAB_FAVORITES) {
            for (int i = 0; i < allStations.size(); i++) {
                RadioStation st = allStations.get(i);
                if (prefsManager.isFavorite(st.getId())) {
                    sourceList.add(st);
                }
            }
        } else if (activeTab == TAB_RECENT) {
            List<String> recents = prefsManager.getRecentStationIds();
            for (int r = 0; r < recents.size(); r++) {
                String id = recents.get(r);
                for (int i = 0; i < allStations.size(); i++) {
                    if (allStations.get(i).getId().equals(id)) {
                        sourceList.add(allStations.get(i));
                        break;
                    }
                }
            }
        } else {
            sourceList.addAll(allStations);
        }

        // Apply search query and genre filter
        for (int i = 0; i < sourceList.size(); i++) {
            RadioStation st = sourceList.get(i);
            boolean matchesGenre = selectedGenre.equals("All Genres") || st.getGenre().equalsIgnoreCase(selectedGenre);
            boolean matchesQuery = currentSearchQuery.isEmpty()
                    || st.getName().toLowerCase(Locale.getDefault()).contains(currentSearchQuery)
                    || st.getGenre().toLowerCase(Locale.getDefault()).contains(currentSearchQuery)
                    || st.getLanguage().toLowerCase(Locale.getDefault()).contains(currentSearchQuery)
                    || st.getCountry().toLowerCase(Locale.getDefault()).contains(currentSearchQuery);

            if (matchesGenre && matchesQuery) {
                displayedStations.add(st);
            }
        }

        renderStationList();
    }

    private void renderStationList() {
        containerStations.removeAllViews();

        String tabTitle = (activeTab == TAB_ALL) ? "All Stations" : (activeTab == TAB_FAVORITES ? "Favorite Stations" : "Recently Played");
        txtListHeader.setText(tabTitle + " (" + displayedStations.size() + ")");

        if (displayedStations.isEmpty()) {
            txtEmptyView.setVisibility(View.VISIBLE);
            return;
        }

        txtEmptyView.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < displayedStations.size(); i++) {
            final RadioStation station = displayedStations.get(i);
            View row = inflater.inflate(R.layout.item_station, containerStations, false);

            TextView txtFreq = (TextView) row.findViewById(R.id.txtItemFreq);
            TextView txtName = (TextView) row.findViewById(R.id.txtItemName);
            TextView txtGenre = (TextView) row.findViewById(R.id.txtItemGenre);
            TextView txtCountry = (TextView) row.findViewById(R.id.txtItemCountry);
            final ImageView btnFav = (ImageView) row.findViewById(R.id.btnItemFav);

            txtFreq.setText(station.getFrequency());
            txtName.setText(station.getName());
            txtGenre.setText(station.getGenre() + " • " + station.getLanguage());
            txtCountry.setText(station.getCountry());

            updateItemFavIcon(btnFav, station.getId());

            btnFav.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    prefsManager.toggleFavorite(station.getId());
                    updateItemFavIcon(btnFav, station.getId());
                    if (playerService != null && playerService.getCurrentStation() != null
                            && playerService.getCurrentStation().getId().equals(station.getId())) {
                        updateHeroFavoriteIcon(station);
                    }
                    if (activeTab == TAB_FAVORITES) {
                        applyFiltersAndRender();
                    }
                }
            });

            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playStation(station);
                }
            });

            containerStations.addView(row);
        }
    }

    private void updateItemFavIcon(ImageView view, String id) {
        if (prefsManager.isFavorite(id)) {
            view.setImageResource(R.drawable.ic_favorite_on);
        } else {
            view.setImageResource(R.drawable.ic_favorite_off);
        }
    }

    private void updateHeroFavoriteIcon(RadioStation station) {
        if (station != null && prefsManager.isFavorite(station.getId())) {
            btnHeroFav.setImageResource(R.drawable.ic_favorite_on);
        } else {
            btnHeroFav.setImageResource(R.drawable.ic_favorite_off);
        }
    }

    private void playStation(RadioStation station) {
        if (playerService != null) {
            prefsManager.addRecent(station.getId());
            playerService.playStation(station);
        }
    }

    private void startAndBindService() {
        Intent intent = new Intent(this, RadioPlayerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void syncPlayerState() {
        if (playerService == null) return;
        RadioStation current = playerService.getCurrentStation();
        if (current != null) {
            onStationChanged(current);
        }
        onPlaybackStateChanged(playerService.getCurrentState(), "");
        onSleepTimerTick(playerService.getSleepTimeRemaining());
    }

    @Override
    public void onPlaybackStateChanged(final int state, final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (state) {
                    case RadioPlayerService.STATE_BUFFERING:
                        progressBuffering.setVisibility(View.VISIBLE);
                        txtPlaybackStatus.setText(message.isEmpty() ? getString(R.string.status_buffering) : message);
                        txtPlaybackStatus.setTextColor(getResources().getColor(R.color.accent_yellow));
                        btnPlayPause.setImageResource(R.drawable.ic_pause);
                        break;
                    case RadioPlayerService.STATE_PLAYING:
                        progressBuffering.setVisibility(View.GONE);
                        txtPlaybackStatus.setText(getString(R.string.status_playing));
                        txtPlaybackStatus.setTextColor(getResources().getColor(R.color.success_color));
                        btnPlayPause.setImageResource(R.drawable.ic_pause);
                        break;
                    case RadioPlayerService.STATE_PAUSED:
                        progressBuffering.setVisibility(View.GONE);
                        txtPlaybackStatus.setText("Stream Paused");
                        txtPlaybackStatus.setTextColor(getResources().getColor(R.color.text_secondary));
                        btnPlayPause.setImageResource(R.drawable.ic_play);
                        break;
                    case RadioPlayerService.STATE_ERROR:
                        progressBuffering.setVisibility(View.GONE);
                        txtPlaybackStatus.setText(message.isEmpty() ? getString(R.string.status_error) : message);
                        txtPlaybackStatus.setTextColor(getResources().getColor(R.color.error_color));
                        btnPlayPause.setImageResource(R.drawable.ic_play);
                        break;
                    case RadioPlayerService.STATE_STOPPED:
                    default:
                        progressBuffering.setVisibility(View.GONE);
                        txtPlaybackStatus.setText(getString(R.string.status_stopped));
                        txtPlaybackStatus.setTextColor(getResources().getColor(R.color.text_secondary));
                        btnPlayPause.setImageResource(R.drawable.ic_play);
                        break;
                }
            }
        });
    }

    @Override
    public void onStationChanged(final RadioStation station) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (station != null) {
                    txtCurrentStationName.setText(station.getName());
                    txtCurrentStationInfo.setText(station.getGenre() + " • " + station.getFrequency());
                    updateHeroFavoriteIcon(station);
                }
            }
        });
    }

    @Override
    public void onSleepTimerTick(final long millisUntilFinished) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (millisUntilFinished <= 0) {
                    txtTimerStatus.setText("");
                } else {
                    long minutes = millisUntilFinished / 1000 / 60;
                    long seconds = (millisUntilFinished / 1000) % 60;
                    txtTimerStatus.setText(String.format(Locale.US, "Timer: %02d:%02d", minutes, seconds));
                }
            }
        });
    }

    private void showSleepTimerDialog() {
        final String[] options = new String[]{"Off", "15 minutes", "30 minutes", "45 minutes", "60 minutes"};
        final int[] values = new int[]{0, 15, 30, 45, 60};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.sleep_timer);
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (playerService != null) {
                    int mins = values[which];
                    playerService.setSleepTimer(mins);
                    if (mins > 0) {
                        Toast.makeText(MainActivity.this, "Sleep timer set for " + mins + " minutes", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Sleep timer disabled", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        builder.show();
    }

    private void showAppMenuDialog() {
        final String[] items = new String[]{"About Radio FM Live", "Privacy Policy", "Exit App"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Radio FM Live Options");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    showAboutDialog();
                } else if (which == 1) {
                    showPrivacyDialog();
                } else if (which == 2) {
                    if (playerService != null) {
                        playerService.stop();
                    }
                    finish();
                }
            }
        });
        builder.show();
    }

    private void showAboutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("About Radio FM Live");
        builder.setMessage(
                "Radio FM Live delivers high-fidelity internet radio streaming directly to your Android device.\n\n" +
                "Note: Modern Android hardware does not grant direct control over local FM chips to third-party apps; this application operates as a high-efficiency internet streaming tuner connecting to verified global broadcasts.\n\n" +
                "Version: 1.0\n" +
                "Architecture: Native Android Java & MediaSession"
        );
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void showPrivacyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Privacy Policy");
        builder.setMessage(
                "Radio FM Live respects your privacy:\n\n" +
                "• No personal data, tracking identifiers, or contact details are collected.\n" +
                "• Audio focus, network state, and foreground playback permissions are used strictly to deliver uninterrupted live radio playback.\n" +
                "• Favorites and station history are stored locally on your device via SharedPreferences."
        );
        builder.setPositiveButton("Close", null);
        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            if (playerService != null) {
                playerService.setCallback(null);
            }
            unbindService(serviceConnection);
            isBound = false;
        }
    }
}