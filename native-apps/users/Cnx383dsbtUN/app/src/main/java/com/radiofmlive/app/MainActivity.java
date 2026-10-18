package com.radiofmlive.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

    private final List<RadioStation> allStations = new ArrayList<RadioStation>();
    private final List<RadioStation> displayedStations = new ArrayList<RadioStation>();

    private int activeTab = TAB_ALL;
    private String currentSearchQuery = "";
    private String selectedGenre = "All Genres";
    private boolean isAutoSearchResults = false;

    // Views
    private TextView txtCurrentStationName;
    private TextView txtCurrentStationInfo;
    private TextView txtPlaybackStatus;
    private TextView txtTimerStatus;
    private ProgressBar progressBuffering;
    private ImageButton btnPlayPause;
    private ImageView btnStop;
    private ImageView btnPreviousStation;
    private ImageView btnNextStation;
    private ImageView btnHeroFav;
    private ImageView btnHeroTimer;
    private ImageView btnSleepTimerTop;
    private ImageView btnMenuOptions;
    private Button btnAutoSearchTop;
    private Button btnTriggerAutoSearch;

    private Button tabAll;
    private Button tabFavorites;
    private Button tabRecent;

    private EditText editSearch;
    private Spinner spinnerGenre;
    private TextView txtListHeader;
    private TextView btnResetPresets;
    private LinearLayout layoutSearchingProgress;
    private TextView txtSearchingMessage;
    private LinearLayout layoutEmptyRetry;
    private TextView txtEmptyView;
    private Button btnRetrySearch;
    private LinearLayout containerStations;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            RadioPlayerService.LocalBinder b = (RadioPlayerService.LocalBinder) binder;
            playerService = b.getService();
            isBound = true;
            playerService.setCallback(MainActivity.this);
            syncPlayerState();
            syncPlaylistToService();
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
        allStations.addAll(StationData.getPresetStations());

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
        btnPreviousStation = (ImageView) findViewById(R.id.btnPreviousStation);
        btnNextStation = (ImageView) findViewById(R.id.btnNextStation);
        btnHeroFav = (ImageView) findViewById(R.id.btnHeroFav);
        btnHeroTimer = (ImageView) findViewById(R.id.btnHeroTimer);
        btnSleepTimerTop = (ImageView) findViewById(R.id.btnSleepTimerTop);
        btnMenuOptions = (ImageView) findViewById(R.id.btnMenuOptions);
        btnAutoSearchTop = (Button) findViewById(R.id.btnAutoSearchTop);
        btnTriggerAutoSearch = (Button) findViewById(R.id.btnTriggerAutoSearch);

        tabAll = (Button) findViewById(R.id.tabAll);
        tabFavorites = (Button) findViewById(R.id.tabFavorites);
        tabRecent = (Button) findViewById(R.id.tabRecent);

        editSearch = (EditText) findViewById(R.id.editSearch);
        spinnerGenre = (Spinner) findViewById(R.id.spinnerGenre);
        txtListHeader = (TextView) findViewById(R.id.txtListHeader);
        btnResetPresets = (TextView) findViewById(R.id.btnResetPresets);
        layoutSearchingProgress = (LinearLayout) findViewById(R.id.layoutSearchingProgress);
        txtSearchingMessage = (TextView) findViewById(R.id.txtSearchingMessage);
        layoutEmptyRetry = (LinearLayout) findViewById(R.id.layoutEmptyRetry);
        txtEmptyView = (TextView) findViewById(R.id.txtEmptyView);
        btnRetrySearch = (Button) findViewById(R.id.btnRetrySearch);
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

        // Next Station button
        btnNextStation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (playerService != null) {
                    syncPlaylistToService();
                    playerService.playNextStation();
                }
            }
        });

        // Previous Station button
        btnPreviousStation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (playerService != null) {
                    syncPlaylistToService();
                    playerService.playPreviousStation();
                }
            }
        });

        btnHeroFav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (playerService != null && playerService.getCurrentStation() != null) {
                    RadioStation st = playerService.getCurrentStation();
                    prefsManager.toggleFavorite(st);
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

        View.OnClickListener autoSearchClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAutoSearchDialog();
            }
        };
        btnAutoSearchTop.setOnClickListener(autoSearchClickListener);
        btnTriggerAutoSearch.setOnClickListener(autoSearchClickListener);

        btnRetrySearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAutoSearchDialog();
            }
        });

        btnResetPresets.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetToPresets();
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

    private void resetToPresets() {
        isAutoSearchResults = false;
        allStations.clear();
        allStations.addAll(StationData.getPresetStations());
        btnResetPresets.setVisibility(View.GONE);
        refreshGenreSpinner();
        applyFiltersAndRender();
        Toast.makeText(this, "Default station presets restored", Toast.LENGTH_SHORT).show();
    }

    private void updateTabStyles() {
        int activeColor = getResources().getColor(R.color.accent_cyan);
        int inactiveColor = getResources().getColor(R.color.text_secondary);

        tabAll.setTextColor(activeTab == TAB_ALL ? activeColor : inactiveColor);
        tabFavorites.setTextColor(activeTab == TAB_FAVORITES ? activeColor : inactiveColor);
        tabRecent.setTextColor(activeTab == TAB_RECENT ? activeColor : inactiveColor);
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    private void showAutoSearchDialog() {
        if (!isOnline()) {
            Toast.makeText(this, "Auto Search requires an active internet connection.", Toast.LENGTH_LONG).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_auto_search, null);
        builder.setView(dialogView);

        final EditText editStationName = (EditText) dialogView.findViewById(R.id.editDialogStationName);
        final EditText editGenre = (EditText) dialogView.findViewById(R.id.editDialogGenre);
        final EditText editCountry = (EditText) dialogView.findViewById(R.id.editDialogCountry);
        final EditText editLanguage = (EditText) dialogView.findViewById(R.id.editDialogLanguage);

        if (!currentSearchQuery.isEmpty()) {
            editStationName.setText(currentSearchQuery);
        }

        builder.setPositiveButton("Start Discovery", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = editStationName.getText().toString().trim();
                String genre = editGenre.getText().toString().trim();
                String country = editCountry.getText().toString().trim();
                String lang = editLanguage.getText().toString().trim();
                executeAutoSearch(name, country, lang, genre);
            }
        });

        builder.setNeutralButton("Quick Scan (Popular)", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                executeAutoSearch("", "", "", "");
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void executeAutoSearch(final String name, final String country, final String language, final String genre) {
        if (!isOnline()) {
            Toast.makeText(this, "Internet connection required for online discovery.", Toast.LENGTH_SHORT).show();
            return;
        }

        layoutSearchingProgress.setVisibility(View.VISIBLE);
        layoutEmptyRetry.setVisibility(View.GONE);
        containerStations.removeAllViews();
        txtListHeader.setText("Searching Radio Directory…");

        RadioBrowserApi.discoverStations(name, country, language, genre, new RadioBrowserApi.DiscoveryCallback() {
            @Override
            public void onSuccess(List<RadioStation> stations) {
                layoutSearchingProgress.setVisibility(View.GONE);
                if (stations == null || stations.isEmpty()) {
                    layoutEmptyRetry.setVisibility(View.VISIBLE);
                    txtEmptyView.setText("No online radio stations matched your query.");
                    btnResetPresets.setVisibility(View.VISIBLE);
                } else {
                    isAutoSearchResults = true;
                    allStations.clear();
                    allStations.addAll(stations);
                    btnResetPresets.setVisibility(View.VISIBLE);
                    activeTab = TAB_ALL;
                    updateTabStyles();
                    refreshGenreSpinner();
                    applyFiltersAndRender();
                    Toast.makeText(MainActivity.this, "Found " + stations.size() + " live broadcasts", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String message) {
                layoutSearchingProgress.setVisibility(View.GONE);
                layoutEmptyRetry.setVisibility(View.VISIBLE);
                txtEmptyView.setText("Discovery error: " + message);
                btnResetPresets.setVisibility(View.VISIBLE);
            }
        });
    }

    private void refreshGenreSpinner() {
        List<String> genres = StationData.getAllGenres(allStations);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, genres);
        spinnerGenre.setAdapter(adapter);
    }

    private void applyFiltersAndRender() {
        displayedStations.clear();

        List<RadioStation> sourceList = new ArrayList<RadioStation>();
        if (activeTab == TAB_FAVORITES) {
            List<RadioStation> favs = prefsManager.getFavoriteStations();
            sourceList.addAll(favs);
        } else if (activeTab == TAB_RECENT) {
            List<String> recents = prefsManager.getRecentStationIds();
            for (int r = 0; r < recents.size(); r++) {
                String id = recents.get(r);
                boolean found = false;
                for (int i = 0; i < allStations.size(); i++) {
                    if (allStations.get(i).getId().equals(id)) {
                        sourceList.add(allStations.get(i));
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    List<RadioStation> favs = prefsManager.getFavoriteStations();
                    for (int f = 0; f < favs.size(); f++) {
                        if (favs.get(f).getId().equals(id)) {
                            sourceList.add(favs.get(f));
                            break;
                        }
                    }
                }
            }
        } else {
            sourceList.addAll(allStations);
        }

        for (int i = 0; i < sourceList.size(); i++) {
            RadioStation st = sourceList.get(i);
            boolean matchesGenre = selectedGenre.equals("All Genres") || st.getGenre().toLowerCase(Locale.getDefault()).contains(selectedGenre.toLowerCase(Locale.getDefault()));
            boolean matchesQuery = currentSearchQuery.isEmpty()
                    || st.getName().toLowerCase(Locale.getDefault()).contains(currentSearchQuery)
                    || st.getGenre().toLowerCase(Locale.getDefault()).contains(currentSearchQuery)
                    || st.getLanguage().toLowerCase(Locale.getDefault()).contains(currentSearchQuery)
                    || st.getCountry().toLowerCase(Locale.getDefault()).contains(currentSearchQuery);

            if (matchesGenre && matchesQuery) {
                displayedStations.add(st);
            }
        }

        syncPlaylistToService();
        renderStationList();
    }

    private void syncPlaylistToService() {
        if (playerService != null) {
            if (!displayedStations.isEmpty()) {
                playerService.setPlaylist(displayedStations);
            } else if (!allStations.isEmpty()) {
                playerService.setPlaylist(allStations);
            }
        }
    }

    private void renderStationList() {
        containerStations.removeAllViews();

        String tabTitle = (activeTab == TAB_ALL)
                ? (isAutoSearchResults ? "Discovered Stations" : "Popular Broadcasts")
                : (activeTab == TAB_FAVORITES ? "Favorite Stations" : "Recently Played");

        txtListHeader.setText(tabTitle + " (" + displayedStations.size() + ")");

        if (displayedStations.isEmpty()) {
            layoutEmptyRetry.setVisibility(View.VISIBLE);
            txtEmptyView.setText("No stations found. Tap 'Auto Search' to discover live global stations.");
            return;
        }

        layoutEmptyRetry.setVisibility(View.GONE);
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
                    prefsManager.toggleFavorite(station);
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
            syncPlaylistToService();
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

    @Override
    public void onPlaylistPositionChanged(int currentIndex, int totalSize) {
        // Notification of playlist bounds
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
        final String[] items = new String[]{"About Radio FM Live", "Auto Search / Discovery Guide", "Privacy Policy", "Exit App"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Radio FM Live Options");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    showAboutDialog();
                } else if (which == 1) {
                    showAutoSearchDialog();
                } else if (which == 2) {
                    showPrivacyDialog();
                } else if (which == 3) {
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
                "• Next & Previous: Effortlessly traverse through stations in your current list with automatic boundary wrap-around.\n" +
                "• Auto Search: Live directory discovery querying authentic public broadcasting feeds across genres, countries, and languages.\n" +
                "• Native Android MediaPlayer: True background streaming with notification controls and MediaSession integration.\n\n" +
                "Notice: Hardware FM chip reception is not accessible to standard third-party apps; all live broadcasts stream over high-efficiency internet data."
        );
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void showPrivacyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Privacy Policy");
        builder.setMessage(
                "Radio FM Live respects your privacy:\n\n" +
                "• No personal data, tracking identifiers, or location records are collected.\n" +
                "• Stream discovery connects solely to legitimate public radio directories.\n" +
                "• Favorites and station history are stored purely on your device via SharedPreferences."
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