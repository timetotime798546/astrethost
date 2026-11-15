package com.lockplaytube.app;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int REQUEST_CODE_HISTORY = 1002;
    private static final int PERMISSION_REQUEST_NOTIFICATIONS = 1003;
    private static final int REQUEST_CODE_MIC = 1004;

    private LinearLayout mainContentContainer;
    private EditText editSearch;
    private ImageButton btnVoiceSearch;
    private ImageButton btnHistory;
    private ImageButton btnExit;

    // Player Card UI
    private LinearLayout cardPlayerView;
    private TextView txtNowPlayingTitle;
    private TextView txtNowPlayingArtist;
    private TextView txtCurrentTime;
    private TextView txtTotalTime;
    private SeekBar seekProgress;
    private ImageButton btnPrevTrack;
    private ImageButton btnPlayPause;
    private ImageButton btnNextTrack;

    // Channel List UI
    private ListView listTracks;
    private TrackAdapter trackAdapter;
    private List<MediaTrack> allTracks = new ArrayList<MediaTrack>();
    private List<MediaTrack> filteredTracks = new ArrayList<MediaTrack>();
    private MediaTrack currentTrack;
    private int currentTrackIndex = 0;

    // Category Buttons
    private Button btnCatAll;
    private Button btnCatLofi;
    private Button btnCatChill;
    private Button btnCatPodcast;
    private Button btnCatSleep;

    // Paywall Overlay Views
    private ScrollView paywallOverlay;
    private TextView txtPaywallTitle;
    private TextView txtPaywallSub;
    private TextView txtDeviceId;
    private Button btnCopyDeviceId;
    private ImageView imgQrCode;
    private ProgressBar qrProgress;
    private TextView txtUpiId;
    private Button btnCopyUpi;
    private Button btnWhatsappProof;
    private Button btnCheckStatus;
    private ProgressBar checkStatusProgress;

    private HistoryDbHelper dbHelper;
    private TrialPaywallManager trialPaywallManager;
    private Intent serviceIntent;
    private BackgroundAudioService audioService;
    private boolean isServiceBound = false;
    private boolean isExiting = false;
    private SpeechRecognizer speechRecognizer;

    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private Runnable updateProgressRunnable = new Runnable() {
        @Override
        public void run() {
            if (audioService != null && audioService.isPlaying()) {
                int cur = audioService.getCurrentPosition();
                int dur = audioService.getDuration();
                if (dur > 0) {
                    seekProgress.setMax(dur);
                    seekProgress.setProgress(cur);
                    txtCurrentTime.setText(formatTime(cur));
                    txtTotalTime.setText(formatTime(dur));
                }
            }
            if (!isExiting) {
                mainHandler.postDelayed(this, 1000);
            }
        }
    };

    private Runnable trialMonitorRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isExiting) {
                if (trialPaywallManager != null && !trialPaywallManager.isAccessGranted(MainActivity.this)) {
                    stopPlaybackAndLock();
                } else {
                    mainHandler.postDelayed(this, 3000);
                }
            }
        }
    };

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BackgroundAudioService.LocalBinder binder = (BackgroundAudioService.LocalBinder) service;
            audioService = binder.getService();
            isServiceBound = true;

            if (!trialPaywallManager.isAccessGranted(MainActivity.this)) {
                stopPlaybackAndLock();
                return;
            }

            audioService.setMediaStateChangeListener(new BackgroundAudioService.MediaStateChangeListener() {
                @Override
                public void onPlayStateChanged(final boolean playing) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            btnPlayPause.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
                        }
                    });
                }

                @Override
                public void onTrackCompleted() {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            playNextTrack();
                        }
                    });
                }

                @Override
                public void onErrorOccurred(final String message) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            audioService = null;
            isServiceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new HistoryDbHelper(this);
        trialPaywallManager = new TrialPaywallManager();

        initViews();
        setupMediaData();
        setupListeners();
        setupPaywallListeners();
        checkNotificationPermission();

        if (trialPaywallManager.isAccessGranted(this)) {
            startMediaService();
        }

        verifyAccessAndLoadApp();
    }

    private void initViews() {
        mainContentContainer = (LinearLayout) findViewById(R.id.main_content_container);
        editSearch = (EditText) findViewById(R.id.edit_search);
        btnVoiceSearch = (ImageButton) findViewById(R.id.btn_voice_search);
        btnHistory = (ImageButton) findViewById(R.id.btn_history);
        btnExit = (ImageButton) findViewById(R.id.btn_exit);

        cardPlayerView = (LinearLayout) findViewById(R.id.card_player_view);
        txtNowPlayingTitle = (TextView) findViewById(R.id.txt_now_playing_title);
        txtNowPlayingArtist = (TextView) findViewById(R.id.txt_now_playing_artist);
        txtCurrentTime = (TextView) findViewById(R.id.txt_current_time);
        txtTotalTime = (TextView) findViewById(R.id.txt_total_time);
        seekProgress = (SeekBar) findViewById(R.id.seek_progress);
        btnPrevTrack = (ImageButton) findViewById(R.id.btn_prev_track);
        btnPlayPause = (ImageButton) findViewById(R.id.btn_play_pause);
        btnNextTrack = (ImageButton) findViewById(R.id.btn_next_track);

        listTracks = (ListView) findViewById(R.id.list_tracks);

        btnCatAll = (Button) findViewById(R.id.btn_cat_all);
        btnCatLofi = (Button) findViewById(R.id.btn_cat_lofi);
        btnCatChill = (Button) findViewById(R.id.btn_cat_chill);
        btnCatPodcast = (Button) findViewById(R.id.btn_cat_podcast);
        btnCatSleep = (Button) findViewById(R.id.btn_cat_sleep);

        // Paywall Overlay Views
        paywallOverlay = (ScrollView) findViewById(R.id.paywall_overlay);
        txtPaywallTitle = (TextView) findViewById(R.id.txt_paywall_title);
        txtPaywallSub = (TextView) findViewById(R.id.txt_paywall_sub);
        txtDeviceId = (TextView) findViewById(R.id.txt_device_id);
        btnCopyDeviceId = (Button) findViewById(R.id.btn_copy_device_id);
        imgQrCode = (ImageView) findViewById(R.id.img_qr_code);
        qrProgress = (ProgressBar) findViewById(R.id.qr_progress);
        txtUpiId = (TextView) findViewById(R.id.txt_upi_id);
        btnCopyUpi = (Button) findViewById(R.id.btn_copy_upi);
        btnWhatsappProof = (Button) findViewById(R.id.btn_whatsapp_proof);
        btnCheckStatus = (Button) findViewById(R.id.btn_check_status);
        checkStatusProgress = (ProgressBar) findViewById(R.id.check_status_progress);
    }

    private void setupMediaData() {
        allTracks.add(new MediaTrack("1", "Lo-Fi Midnight Chill Study Beats", "Lo-Fi Beats Station", "Lo-Fi", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3", "06:12", "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=200"));
        allTracks.add(new MediaTrack("2", "Acoustic Chillout Instrumental Vibes", "Acoustic Collective", "Chill", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3", "07:05", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=200"));
        allTracks.add(new MediaTrack("3", "Deep Focus Instrumental Study Session", "Focus Mind Audio", "Chill", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3", "05:44", "https://images.unsplash.com/photo-1485579149621-3123dd979885?w=200"));
        allTracks.add(new MediaTrack("4", "Sleep Ambient Rain & Synth Pads", "Sleep Soundscapes", "Sleep", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3", "05:02", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=200"));
        allTracks.add(new MediaTrack("5", "Tech Talks & Innovation Podcast", "LockPlay Network", "Podcast", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3", "07:53", "https://images.unsplash.com/photo-1478737270239-2f02b77fc618?w=200"));
        allTracks.add(new MediaTrack("6", "Piano Melodies for Meditation & Relaxation", "Peaceful Piano", "Sleep", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3", "06:21", "https://images.unsplash.com/photo-1520523839897-bd0b52f945a0?w=200"));

        filteredTracks.addAll(allTracks);

        trackAdapter = new TrackAdapter(this, filteredTracks, new TrackAdapter.OnTrackSelectListener() {
            @Override
            public void onTrackSelected(MediaTrack track) {
                selectAndPlayTrack(track);
            }
        });
        listTracks.setAdapter(trackAdapter);
    }

    private void selectAndPlayTrack(MediaTrack track) {
        if (!trialPaywallManager.isAccessGranted(this)) {
            stopPlaybackAndLock();
            return;
        }

        this.currentTrack = track;
        for (int i = 0; i < filteredTracks.size(); i++) {
            if (filteredTracks.get(i).getId().equals(track.getId())) {
                currentTrackIndex = i;
                break;
            }
        }

        trackAdapter.setCurrentlyPlayingTrack(track);

        txtNowPlayingTitle.setText(track.getTitle());
        txtNowPlayingArtist.setText(track.getArtist());
        cardPlayerView.setVisibility(View.VISIBLE);

        dbHelper.addHistory(track.getTitle(), track.getStreamUrl());

        if (audioService != null) {
            audioService.playStream(track.getStreamUrl(), track.getTitle(), track.getArtist());
        }

        mainHandler.removeCallbacks(updateProgressRunnable);
        mainHandler.post(updateProgressRunnable);
    }

    private void playNextTrack() {
        if (filteredTracks.isEmpty()) return;
        currentTrackIndex = (currentTrackIndex + 1) % filteredTracks.size();
        selectAndPlayTrack(filteredTracks.get(currentTrackIndex));
    }

    private void playPreviousTrack() {
        if (filteredTracks.isEmpty()) return;
        currentTrackIndex = (currentTrackIndex - 1 + filteredTracks.size()) % filteredTracks.size();
        selectAndPlayTrack(filteredTracks.get(currentTrackIndex));
    }

    private void filterTracks(String category, String query) {
        filteredTracks.clear();
        for (MediaTrack t : allTracks) {
            boolean matchCat = (category == null || category.equalsIgnoreCase("ALL") || t.getCategory().equalsIgnoreCase(category));
            boolean matchQuery = (query == null || query.trim().isEmpty() ||
                    t.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                    t.getArtist().toLowerCase().contains(query.toLowerCase()));
            if (matchCat && matchQuery) {
                filteredTracks.add(t);
            }
        }
        trackAdapter.notifyDataSetChanged();
    }

    private void setupListeners() {
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTracks(null, s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnVoiceSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startNativeSpeechRecognition();
            }
        });

        btnCatAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterTracks("ALL", editSearch.getText().toString());
            }
        });

        btnCatLofi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterTracks("Lo-Fi", editSearch.getText().toString());
            }
        });

        btnCatChill.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterTracks("Chill", editSearch.getText().toString());
            }
        });

        btnCatPodcast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterTracks("Podcast", editSearch.getText().toString());
            }
        });

        btnCatSleep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterTracks("Sleep", editSearch.getText().toString());
            }
        });

        btnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (audioService != null) {
                    if (audioService.isPlaying()) {
                        audioService.pauseAudio();
                    } else {
                        audioService.resumeAudio();
                    }
                }
            }
        });

        btnPrevTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPreviousTrack();
            }
        });

        btnNextTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNextTrack();
            }
        });

        seekProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && audioService != null) {
                    audioService.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                startActivityForResult(intent, REQUEST_CODE_HISTORY);
            }
        });

        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopAppAndAudio();
            }
        });
    }

    private void startMediaService() {
        if (!isServiceBound && trialPaywallManager.isAccessGranted(this)) {
            serviceIntent = new Intent(this, BackgroundAudioService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void verifyAccessAndLoadApp() {
        trialPaywallManager.checkAccessStatus(this, new TrialPaywallManager.StatusCallback() {
            @Override
            public void onStatusResult(int status, String message) {
                if (status == TrialPaywallManager.STATUS_EXPIRED || !trialPaywallManager.isAccessGranted(MainActivity.this)) {
                    stopPlaybackAndLock();
                } else {
                    hidePaywallLockOverlay();
                    startMediaService();
                    mainHandler.removeCallbacks(trialMonitorRunnable);
                    mainHandler.postDelayed(trialMonitorRunnable, 3000);
                }
            }
        });
    }

    private void stopPlaybackAndLock() {
        mainHandler.removeCallbacks(trialMonitorRunnable);
        mainHandler.removeCallbacks(updateProgressRunnable);

        if (audioService != null) {
            audioService.stopForegroundAndAudio();
            audioService = null;
        }

        if (isServiceBound) {
            try {
                unbindService(serviceConnection);
            } catch (Exception e) {
                e.printStackTrace();
            }
            isServiceBound = false;
        }

        if (serviceIntent != null) {
            try {
                stopService(serviceIntent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        showPaywallLockOverlay();
    }

    private void showPaywallLockOverlay() {
        mainContentContainer.setVisibility(View.GONE);
        paywallOverlay.setVisibility(View.VISIBLE);

        String deviceId = trialPaywallManager.getDeviceId(this);
        String upiId = trialPaywallManager.getUpiId(this);
        String amount = trialPaywallManager.getPremiumAmount(this);
        String message = trialPaywallManager.getWorkerMessage(this);

        if (message != null && !message.trim().isEmpty()) {
            txtPaywallTitle.setText("🔒 " + message);
        } else {
            txtPaywallTitle.setText("🔒 Premium Plan Required");
        }

        txtDeviceId.setText(deviceId);
        if (amount != null && !amount.trim().isEmpty()) {
            txtPaywallSub.setText("Upgrade to Lifetime Premium access for " + amount);
        } else {
            txtPaywallSub.setText("Upgrade to Lifetime Premium access to continue listening");
        }

        if (upiId != null && !upiId.trim().isEmpty()) {
            txtUpiId.setText("UPI ID: " + upiId);
        } else {
            txtUpiId.setText("UPI ID: --");
        }

        loadDynamicQrCode(upiId, amount);
    }

    private void hidePaywallLockOverlay() {
        paywallOverlay.setVisibility(View.GONE);
        mainContentContainer.setVisibility(View.VISIBLE);
    }

    private void loadDynamicQrCode(String upiId, String amount) {
        if (upiId == null || upiId.trim().isEmpty()) {
            imgQrCode.setImageBitmap(null);
            qrProgress.setVisibility(View.GONE);
            return;
        }

        String data = "upi://pay?pa=" + upiId + "&pn=LockPlayTube";
        if (amount != null && !amount.trim().isEmpty()) {
            String cleanAmount = amount.replaceAll("[^0-9.]", "");
            if (!cleanAmount.isEmpty()) {
                data += "&am=" + cleanAmount;
            }
        }

        final String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=" + Uri.encode(data);

        qrProgress.setVisibility(View.VISIBLE);
        imgQrCode.setImageBitmap(null);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(qrUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);
                    conn.connect();

                    InputStream is = conn.getInputStream();
                    final Bitmap bitmap = BitmapFactory.decodeStream(is);
                    is.close();

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            qrProgress.setVisibility(View.GONE);
                            if (bitmap != null) {
                                imgQrCode.setImageBitmap(bitmap);
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            qrProgress.setVisibility(View.GONE);
                        }
                    });
                }
            }
        }).start();
    }

    private void setupPaywallListeners() {
        btnCopyDeviceId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String deviceId = txtDeviceId.getText().toString();
                copyToClipboard("Device ID", deviceId);
                Toast.makeText(MainActivity.this, "Device ID Copied!", Toast.LENGTH_SHORT).show();
            }
        });

        btnCopyUpi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String upiId = trialPaywallManager.getUpiId(MainActivity.this);
                if (upiId != null && !upiId.trim().isEmpty()) {
                    copyToClipboard("UPI ID", upiId);
                    Toast.makeText(MainActivity.this, "UPI ID Copied!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "No UPI ID available.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnWhatsappProof.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String whatsappNum = trialPaywallManager.getWhatsappNumber(MainActivity.this);
                String deviceId = trialPaywallManager.getDeviceId(MainActivity.this);
                String amount = trialPaywallManager.getPremiumAmount(MainActivity.this);

                if (whatsappNum == null || whatsappNum.trim().isEmpty()) {
                    Toast.makeText(MainActivity.this, "Support WhatsApp number not configured.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String amountStr = (amount != null && !amount.trim().isEmpty()) ? amount : "Premium Plan";
                String msg = "Hi, I have paid " + amountStr + " for LockPlayTube. My Device ID is: " + deviceId;
                String waUrl = "https://wa.me/" + whatsappNum + "?text=" + Uri.encode(msg);

                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(waUrl));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "WhatsApp is not installed on this device.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnCheckStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkStatusProgress.setVisibility(View.VISIBLE);
                btnCheckStatus.setEnabled(false);

                trialPaywallManager.recheckAccessStatus(MainActivity.this, new TrialPaywallManager.StatusCallback() {
                    @Override
                    public void onStatusResult(int status, String message) {
                        checkStatusProgress.setVisibility(View.GONE);
                        btnCheckStatus.setEnabled(true);

                        if (status == TrialPaywallManager.STATUS_GRANTED && trialPaywallManager.isAccessGranted(MainActivity.this)) {
                            Toast.makeText(MainActivity.this, "Lifetime Premium Activated!", Toast.LENGTH_LONG).show();
                            hidePaywallLockOverlay();
                            startMediaService();
                            mainHandler.postDelayed(trialMonitorRunnable, 3000);
                        } else {
                            Toast.makeText(MainActivity.this, "Payment verification pending. Please send proof via WhatsApp.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }

    private void startNativeSpeechRecognition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_MIC);
                return;
            }
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Voice search is not available on this device", Toast.LENGTH_SHORT).show();
            return;
        }

        if (speechRecognizer != null) {
            try {
                speechRecognizer.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak channel name or audio title...");

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Toast.makeText(MainActivity.this, "Listening...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {
                Toast.makeText(MainActivity.this, "Voice recognition error", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResults(Bundle results) {
                if (results != null) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String query = matches.get(0);
                        editSearch.setText(query);
                        filterTracks(null, query);
                    }
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        speechRecognizer.startListening(intent);
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, PERMISSION_REQUEST_NOTIFICATIONS);
            }
        }
    }

    private void copyToClipboard(String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText(label, text);
            clipboard.setPrimaryClip(clip);
        }
    }

    private String formatTime(int ms) {
        int seconds = (ms / 1000) % 60;
        int minutes = (ms / (1000 * 60)) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private void stopAppAndAudio() {
        isExiting = true;
        mainHandler.removeCallbacks(trialMonitorRunnable);
        mainHandler.removeCallbacks(updateProgressRunnable);

        if (speechRecognizer != null) {
            try {
                speechRecognizer.destroy();
                speechRecognizer = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (audioService != null) {
            audioService.stopForegroundAndAudio();
            audioService = null;
        }

        if (isServiceBound) {
            try {
                unbindService(serviceConnection);
            } catch (Exception e) {
                e.printStackTrace();
            }
            isServiceBound = false;
        }

        if (serviceIntent != null) {
            try {
                stopService(serviceIntent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask();
        } else {
            finish();
        }
        System.exit(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!trialPaywallManager.isAccessGranted(this)) {
            stopPlaybackAndLock();
        } else {
            mainHandler.removeCallbacks(trialMonitorRunnable);
            mainHandler.postDelayed(trialMonitorRunnable, 1000);
        }
    }

    @Override
    public void onBackPressed() {
        if (paywallOverlay != null && paywallOverlay.getVisibility() == View.VISIBLE) {
            Toast.makeText(this, "Please upgrade to premium access to continue.", Toast.LENGTH_SHORT).show();
            return;
        }
        moveTaskToBack(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_HISTORY && resultCode == RESULT_OK && data != null) {
            String selectedUrl = data.getStringExtra("selected_url");
            if (selectedUrl != null && !selectedUrl.trim().isEmpty()) {
                for (MediaTrack t : allTracks) {
                    if (t.getStreamUrl().equalsIgnoreCase(selectedUrl)) {
                        selectAndPlayTrack(t);
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacks(trialMonitorRunnable);
        mainHandler.removeCallbacks(updateProgressRunnable);

        if (speechRecognizer != null) {
            try {
                speechRecognizer.destroy();
                speechRecognizer = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (isServiceBound) {
            try {
                unbindService(serviceConnection);
            } catch (Exception e) {
                e.printStackTrace();
            }
            isServiceBound = false;
        }
        super.onDestroy();
    }
}