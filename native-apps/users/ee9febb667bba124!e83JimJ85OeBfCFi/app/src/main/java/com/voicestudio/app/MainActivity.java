package com.voicestudio.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int REQ_CODE_MIC = 101;

    // Top Navigation Tabs
    private Button btnTabTts;
    private Button btnTabStt;
    private Button btnTabHistory;

    // Tab Panel Layouts
    private View layoutTts;
    private View layoutStt;
    private View layoutHistory;

    // Page 1: Text To Speech Components
    private EditText etTtsInput;
    private Spinner spinnerLanguages;
    private SeekBar seekPitch;
    private SeekBar seekRate;
    private Button btnTtsPlay;
    private Button btnTtsStop;
    private Button btnTtsExport;

    // Page 2: Speech To Text Components
    private TextView tvSttStatus;
    private ImageButton btnSttMic;
    private EditText etSttOutput;
    private Button btnSttCopy;
    private Button btnSttClear;

    // Page 3: Audio History Components
    private ListView listAudioHistory;
    private List<File> audioFileList;
    private HistoryAdapter historyAdapter;

    // Audio Engine & Hardware handlers
    private TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;
    private MediaPlayer mediaPlayer;
    private boolean isListening = false;
    private List<Locale> availableLocales = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getResources().getIdentifier("activity_main", "layout", getPackageName()));

        initViews();
        setupTabs();
        setupTtsEngine();
        setupSttEngine();
        setupAudioHistory();
        setupControlListeners();
    }

    private void initViews() {
        // Tabs
        btnTabTts = (Button) findViewById(getResources().getIdentifier("btn_tab_tts", "id", getPackageName()));
        btnTabStt = (Button) findViewById(getResources().getIdentifier("btn_tab_stt", "id", getPackageName()));
        btnTabHistory = (Button) findViewById(getResources().getIdentifier("btn_tab_history", "id", getPackageName()));

        // Screens
        layoutTts = findViewById(getResources().getIdentifier("layout_tts", "id", getPackageName()));
        layoutStt = findViewById(getResources().getIdentifier("layout_stt", "id", getPackageName()));
        layoutHistory = findViewById(getResources().getIdentifier("layout_history", "id", getPackageName()));

        // TTS Controls
        etTtsInput = (EditText) findViewById(getResources().getIdentifier("et_tts_input", "id", getPackageName()));
        spinnerLanguages = (Spinner) findViewById(getResources().getIdentifier("spinner_languages", "id", getPackageName()));
        seekPitch = (SeekBar) findViewById(getResources().getIdentifier("seek_pitch", "id", getPackageName()));
        seekRate = (SeekBar) findViewById(getResources().getIdentifier("seek_rate", "id", getPackageName()));
        btnTtsPlay = (Button) findViewById(getResources().getIdentifier("btn_tts_play", "id", getPackageName()));
        btnTtsStop = (Button) findViewById(getResources().getIdentifier("btn_tts_stop", "id", getPackageName()));
        btnTtsExport = (Button) findViewById(getResources().getIdentifier("btn_tts_export", "id", getPackageName()));

        // STT Controls
        tvSttStatus = (TextView) findViewById(getResources().getIdentifier("tv_stt_status", "id", getPackageName()));
        btnSttMic = (ImageButton) findViewById(getResources().getIdentifier("btn_stt_mic", "id", getPackageName()));
        etSttOutput = (EditText) findViewById(getResources().getIdentifier("et_stt_output", "id", getPackageName()));
        btnSttCopy = (Button) findViewById(getResources().getIdentifier("btn_stt_copy", "id", getPackageName()));
        btnSttClear = (Button) findViewById(getResources().getIdentifier("btn_stt_clear", "id", getPackageName()));

        // History
        listAudioHistory = (ListView) findViewById(getResources().getIdentifier("list_audio_history", "id", getPackageName()));
    }

    private void setupTabs() {
        btnTabTts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(0);
            }
        });

        btnTabStt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(1);
            }
        });

        btnTabHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
                refreshHistoryList();
            }
        });
    }

    private void switchTab(int tabIndex) {
        btnTabTts.setTextColor(tabIndex == 0 ? 0xFF6200EE : 0xFF757575);
        btnTabTts.setTypeface(null, tabIndex == 0 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        btnTabStt.setTextColor(tabIndex == 1 ? 0xFF6200EE : 0xFF757575);
        btnTabStt.setTypeface(null, tabIndex == 1 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        btnTabHistory.setTextColor(tabIndex == 2 ? 0xFF6200EE : 0xFF757575);
        btnTabHistory.setTypeface(null, tabIndex == 2 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        layoutTts.setVisibility(tabIndex == 0 ? View.VISIBLE : View.GONE);
        layoutStt.setVisibility(tabIndex == 1 ? View.VISIBLE : View.GONE);
        layoutHistory.setVisibility(tabIndex == 2 ? View.VISIBLE : View.GONE);
        
        // Release active player if playing
        stopActivePlayback();
    }

    private void setupTtsEngine() {
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    initAvailableLanguages();
                } else {
                    showToast("Failed to initialize Text to Speech Engine.");
                }
            }
        });

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {}

            @Override
            public void onDone(String utteranceId) {
                if ("EXPORT_ID".equals(utteranceId)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showToast("Speech audio exported successfully!");
                            refreshHistoryList();
                        }
                    });
                }
            }

            @Override
            public void onError(String utteranceId) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showToast("Export failed during processing.");
                    }
                });
            }
        });
    }

    private void initAvailableLanguages() {
        Locale[] systemLocales = Locale.getAvailableLocales();
        availableLocales.clear();

        // Standard list filters for available locale resources
        for (Locale loc : systemLocales) {
            try {
                int res = tts.isLanguageAvailable(loc);
                if (res == TextToSpeech.LANG_AVAILABLE || res == TextToSpeech.LANG_COUNTRY_AVAILABLE || res == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE) {
                    availableLocales.add(loc);
                }
            } catch (Exception ignored) {}
        }

        // Sort languages
        Collections.sort(availableLocales, new Comparator<Locale>() {
            @Override
            public int compare(Locale o1, Locale o2) {
                return o1.getDisplayName().compareTo(o2.getDisplayName());
            }
        });

        // Add fallback default locale if empty
        if (availableLocales.isEmpty()) {
            availableLocales.add(Locale.US);
        }

        List<String> spinnerLabels = new ArrayList<>();
        for (Locale loc : availableLocales) {
            spinnerLabels.add(loc.getDisplayName() + " [" + loc.getLanguage() + "]");
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerLabels);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguages.setAdapter(spinnerAdapter);

        // Choose local locale as default index
        Locale local = Locale.getDefault();
        for (int i = 0; i < availableLocales.size(); i++) {
            if (availableLocales.get(i).getLanguage().equals(local.getLanguage())) {
                spinnerLanguages.setSelection(i);
                break;
            }
        }
    }

    private void setupSttEngine() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            tvSttStatus.setText("Device does not support standard speech translation.");
            btnSttMic.setEnabled(false);
            btnSttMic.setAlpha(0.4f);
            return;
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                tvSttStatus.setText("🎙️ Listening dynamically...");
            }

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                tvSttStatus.setText("Processing transcription...");
            }

            @Override
            public void onError(int error) {
                isListening = false;
                btnSttMic.setBackgroundResource(getResources().getIdentifier("btn_record", "drawable", getPackageName()));
                tvSttStatus.setText("Tap mic to translate voice");
                String message = "Transcription error occurred.";
                if (error == SpeechRecognizer.ERROR_NO_MATCH) {
                    message = "No speech match recognized.";
                } else if (error == SpeechRecognizer.ERROR_AUDIO) {
                    message = "Audio capture error.";
                } else if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    message = "Microphone permissions missing.";
                }
                showToast(message);
            }

            @Override
            public void onResults(Bundle results) {
                isListening = false;
                btnSttMic.setBackgroundResource(getResources().getIdentifier("btn_record", "drawable", getPackageName()));
                tvSttStatus.setText("Tap mic to write more");
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String text = etSttOutput.getText().toString();
                    if (text.trim().isEmpty()) {
                        etSttOutput.setText(matches.get(0));
                    } else {
                        etSttOutput.setText(text + " " + matches.get(0));
                    }
                    etSttOutput.setSelection(etSttOutput.getText().length());
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void setupAudioHistory() {
        audioFileList = new ArrayList<>();
        historyAdapter = new HistoryAdapter();
        listAudioHistory.setAdapter(historyAdapter);
        refreshHistoryList();
    }

    private void refreshHistoryList() {
        audioFileList.clear();
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (storageDir != null && storageDir.exists()) {
            File[] files = storageDir.listFiles();
            if (files != null) {
                // Sort files descending by modification timestamp
                Arrays.sort(files, new Comparator<File>() {
                    @Override
                    public int compare(File f1, File f2) {
                        return Long.compare(f2.lastModified(), f1.lastModified());
                    }
                });
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".wav")) {
                        audioFileList.add(file);
                    }
                }
            }
        }
        historyAdapter.notifyDataSetChanged();
    }

    private void setupControlListeners() {
        // TTS Action Buttons
        btnTtsPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String txt = etTtsInput.getText().toString().trim();
                if (txt.isEmpty()) {
                    showToast("Input text field is empty.");
                    return;
                }
                configureTtsSettings();
                tts.speak(txt, TextToSpeech.QUEUE_FLUSH, null, "PLAY_ID");
            }
        });

        btnTtsStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tts != null) {
                    tts.stop();
                }
            }
        });

        btnTtsExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String txt = etTtsInput.getText().toString().trim();
                if (txt.isEmpty()) {
                    showToast("Please write text to synthesize audio file.");
                    return;
                }
                exportTtsAudio(txt);
            }
        });

        // STT Action Buttons
        btnSttMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleSttMicClick();
            }
        });

        btnSttCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String transcription = etSttOutput.getText().toString().trim();
                if (transcription.isEmpty()) {
                    showToast("No voice transcription transcript to copy!");
                    return;
                }
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Transcribed Audio Text", transcription);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    showToast("Text copied successfully!");
                }
            }
        });

        btnSttClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etSttOutput.setText("");
                showToast("Transcribe editor reset.");
            }
        });
    }

    private void configureTtsSettings() {
        if (spinnerLanguages.getSelectedItemPosition() >= 0) {
            Locale targetLoc = availableLocales.get(spinnerLanguages.getSelectedItemPosition());
            tts.setLanguage(targetLoc);
        }
        float pitch = (float) seekPitch.getProgress() / 10.0f;
        if (pitch < 0.1f) pitch = 0.1f;
        tts.setPitch(pitch);

        float rate = (float) seekRate.getProgress() / 10.0f;
        if (rate < 0.1f) rate = 0.1f;
        tts.setSpeechRate(rate);
    }

    private void exportTtsAudio(String text) {
        configureTtsSettings();
        File directory = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (directory != null) {
            if (!directory.exists()) {
                directory.mkdirs();
            }
            String fileName = "VoiceStudio_" + System.currentTimeMillis() + ".wav";
            File targetFile = new File(directory, fileName);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Bundle params = new Bundle();
                params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "EXPORT_ID");
                int result = tts.synthesizeToFile(text, params, targetFile, "EXPORT_ID");
                if (result != TextToSpeech.SUCCESS) {
                    showToast("Engine could not start export sequence.");
                } else {
                    showToast("Export process initiated...");
                }
            }
        }
    }

    private void handleSttMicClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_CODE_MIC);
                return;
            }
        }
        toggleSttListening();
    }

    private void toggleSttListening() {
        if (speechRecognizer == null) return;
        if (isListening) {
            speechRecognizer.stopListening();
            isListening = false;
            btnSttMic.setBackgroundResource(getResources().getIdentifier("btn_record", "drawable", getPackageName()));
            tvSttStatus.setText("Processing sound...");
        } else {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            if (spinnerLanguages.getSelectedItemPosition() >= 0) {
                Locale currentLoc = availableLocales.get(spinnerLanguages.getSelectedItemPosition());
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLoc.toString());
            } else {
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString());
            }
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            try {
                speechRecognizer.startListening(intent);
                isListening = true;
                btnSttMic.setBackgroundColor(0xFFE91E63);
                tvSttStatus.setText("🎙️ Listening, speak clearly...");
            } catch (Exception e) {
                showToast("Could not start standard recognizer listener.");
            }
        }
    }

    private void stopActivePlayback() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CODE_MIC) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                toggleSttListening();
            } else {
                showToast("Microphone audio recording permission denied.");
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        stopActivePlayback();
        super.onDestroy();
    }

    // Audio History dynamic adapter binding controls
    private class HistoryAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return audioFileList.size();
        }

        @Override
        public Object getItem(int position) {
            return audioFileList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                // Create standard flat layouts dynamically with safe standard APIs
                convertView = LayoutInflater.from(MainActivity.this).inflate(
                        android.R.layout.simple_list_item_1,
                        parent,
                        false
                );
            }
            final File file = audioFileList.get(position);
            TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
            text1.setText(file.getName() + "\n[" + (file.length() / 1024) + " KB]");
            text1.setPadding(20, 20, 20, 20);

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openAudioActionDialog(file);
                }
            });

            return convertView;
        }

        private void openAudioActionDialog(final File file) {
            final String[] choices = {"Play Audio", "Share File", "Rename Archive", "Delete Archive"};
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Studio File Options");
            builder.setItems(choices, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 0) {
                        playAudioArchive(file);
                    } else if (which == 1) {
                        shareAudioArchive(file);
                    } else if (which == 2) {
                        promptRenameDialog(file);
                    } else if (which == 3) {
                        deleteAudioArchive(file);
                    }
                }
            });
            builder.show();
        }

        private void playAudioArchive(File file) {
            stopActivePlayback();
            mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(file.getAbsolutePath());
                mediaPlayer.prepare();
                mediaPlayer.start();
                showToast("Now playing: " + file.getName());
            } catch (Exception e) {
                showToast("Playback failed.");
            }
        }

        private void shareAudioArchive(File file) {
            try {
                Uri uri = Uri.parse("content://" + AudioProvider.AUTHORITY + "/" + file.getName());
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("audio/wav");
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(intent, "Share Voice Clip"));
            } catch (Exception e) {
                showToast("Share failed to instantiate.");
            }
        }

        private void promptRenameDialog(final File file) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Rename Recording");
            final EditText input = new EditText(MainActivity.this);
            input.setText(file.getName().replace(".wav", ""));
            builder.setView(input);
            builder.setPositiveButton("Apply", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        if (!newName.endsWith(".wav")) {
                            newName = newName + ".wav";
                        }
                        File destination = new File(file.getParentFile(), newName);
                        if (file.renameTo(destination)) {
                            showToast("File renamed.");
                            refreshHistoryList();
                        } else {
                            showToast("Could not rename file.");
                        }
                    }
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        }

        private void deleteAudioArchive(File file) {
            if (file.delete()) {
                showToast("File successfully deleted.");
                refreshHistoryList();
            } else {
                showToast("Could not remove file.");
            }
        }
    }
}