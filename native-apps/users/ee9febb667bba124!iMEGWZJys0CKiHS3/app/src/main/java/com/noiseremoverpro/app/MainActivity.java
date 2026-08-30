package com.noiseremoverpro.app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.audiofx.NoiseSuppressor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final int REQUEST_CODE_PERMISSIONS = 1001;
    private static final int REQUEST_CODE_FILE_SELECT = 2002;
    
    // Audio Recording Parameters
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private int minBufferSize;
    
    private AudioRecord audioRecord;
    private NoiseSuppressor hardwareNoiseSuppressor;
    private boolean isRecording = false;
    
    // UI Components
    private CheckBox cbHardwareSuppressor;
    private Button btnRecord, btnStopRecord, btnSelectFile, btnCleanNow, btnPlayOriginal, btnPlayCleaned;
    private SeekBar sbNoiseGate, sbHpf, sbBoost;
    private TextView tvRecordStatus, tvGateLabel, tvHpfLabel, tvBoostLabel, tvSelectedFileInfo, tvProcessingStatus;
    private ProgressBar progressBar;
    
    // Audio Files state
    private File rawRecordedFile;
    private File wavRecordedFile;
    private File selectedInputWavFile;
    private File outputCleanedFile;
    
    private MediaPlayer mediaPlayer;
    private final ExecutorService backgroundThreadExecutor = Executors.newFixedThreadPool(4);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // DSP Configuration Parameters updated dynamically by SeekBars
    private int dspGateThreshold = 400;
    private float dspHighPassFactor = 0.2f;
    private float dspGainBoostMultiplier = 1.5f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Assign UI component references
        cbHardwareSuppressor = findViewById(R.id.cb_hardware_suppressor);
        btnRecord = findViewById(R.id.btn_record);
        btnStopRecord = findViewById(R.id.btn_stop_record);
        btnSelectFile = findViewById(R.id.btn_select_file);
        btnCleanNow = findViewById(R.id.btn_clean_now);
        btnPlayOriginal = findViewById(R.id.btn_play_original);
        btnPlayCleaned = findViewById(R.id.btn_play_cleaned);
        
        sbNoiseGate = findViewById(R.id.sb_noise_gate);
        sbHpf = findViewById(R.id.sb_hpf);
        sbBoost = findViewById(R.id.sb_boost);
        
        tvRecordStatus = findViewById(R.id.tv_record_status);
        tvGateLabel = findViewById(R.id.tv_gate_label);
        tvHpfLabel = findViewById(R.id.tv_hpf_label);
        tvBoostLabel = findViewById(R.id.tv_boost_label);
        tvSelectedFileInfo = findViewById(R.id.tv_selected_file_info);
        tvProcessingStatus = findViewById(R.id.tv_processing_status);
        progressBar = findViewById(R.id.progress_bar);

        minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

        // Default working directories setup
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (storageDir == null) {
            storageDir = getFilesDir();
        }
        rawRecordedFile = new File(storageDir, "raw_record.pcm");
        wavRecordedFile = new File(storageDir, "recorded_original.wav");
        outputCleanedFile = new File(storageDir, "cleaned_output.wav");

        // Check essential hardware permission profile
        checkAppPermissions();
        setupInteractiveListeners();
    }

    private void checkAppPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, REQUEST_CODE_PERMISSIONS);
            }
        }
    }

    private void setupInteractiveListeners() {
        
        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecordingFlow();
            }
        });

        btnStopRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecordingFlow();
            }
        });

        btnSelectFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerFilePicker();
            }
        });

        btnCleanNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processDস্পFiltersOnAudio();
            }
        });

        btnPlayOriginal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playLocalFile(selectedInputWavFile != null ? selectedInputWavFile : wavRecordedFile);
            }
        });

        btnPlayCleaned.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playLocalFile(outputCleanedFile);
            }
        });

        // Seekbar listeners mapping to core DSP variables
        sbNoiseGate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                dspGateThreshold = progress;
                tvGateLabel.setText("Noise Gate Threshold: " + progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        sbHpf.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                dspHighPassFactor = progress / 100.0f;
                if (progress == 0) {
                    tvHpfLabel.setText("Hum cut (High-Pass): Off");
                } else {
                    tvHpfLabel.setText("Hum cut (High-Pass): Level " + progress + "%");
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        sbBoost.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                dspGainBoostMultiplier = 0.5f + (progress / 10.0f);
                tvBoostLabel.setText("Speech Dialog Booster: " + String.format("%.1f", dspGainBoostMultiplier) + "x");
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void startRecordingFlow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && 
                checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Mic permission needed!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    minBufferSize
            );

            // Apply built-in android hardware noise canceller if checked
            if (cbHardwareSuppressor.isChecked() && NoiseSuppressor.isAvailable()) {
                hardwareNoiseSuppressor = NoiseSuppressor.create(audioRecord.getAudioSessionId());
                if (hardwareNoiseSuppressor != null) {
                    hardwareNoiseSuppressor.setEnabled(true);
                    tvRecordStatus.setText("Recording with Hardware Noise Suppressor...");
                }
            } else {
                tvRecordStatus.setText("Recording raw mic audio...");
            }

            audioRecord.startRecording();
            isRecording = true;
            btnRecord.setEnabled(false);
            btnStopRecord.setEnabled(true);

            backgroundThreadExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    writeRawAudioToDisk();
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void writeRawAudioToDisk() {
        byte[] data = new byte[minBufferSize];
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(rawRecordedFile);
            while (isRecording) {
                int read = audioRecord.read(data, 0, minBufferSize);
                if (AudioRecord.ERROR_INVALID_OPERATION != read && AudioRecord.ERROR_BAD_VALUE != read) {
                    os.write(data, 0, read);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                try { os.close(); } catch (IOException e) { e.printStackTrace(); }
            }
        }
    }

    private void stopRecordingFlow() {
        if (!isRecording) return;
        
        isRecording = false;
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        if (hardwareNoiseSuppressor != null) {
            hardwareNoiseSuppressor.release();
            hardwareNoiseSuppressor = null;
        }

        btnRecord.setEnabled(true);
        btnStopRecord.setEnabled(false);
        tvRecordStatus.setText("Converting raw sample capture to WAV...");

        backgroundThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                convertPcmToWav(rawRecordedFile, wavRecordedFile);
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        tvRecordStatus.setText("Recorded WAV saved: " + wavRecordedFile.length() / 1024 + " KB");
                        btnPlayOriginal.setEnabled(true);
                        Toast.makeText(MainActivity.this, "Recording finalized successfully!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void triggerFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/wav");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Select WAV File"), REQUEST_CODE_FILE_SELECT);
        } catch (Exception e) {
            Toast.makeText(this, "Please install a file explorer!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_FILE_SELECT && resultCode == RESULT_OK && data != null) {
            Uri selectedFileUri = data.getData();
            if (selectedFileUri != null) {
                importSelectedWavFile(selectedFileUri);
            }
        }
    }

    private void importSelectedWavFile(final Uri uri) {
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(true);
        tvSelectedFileInfo.setText("Importing selected WAV file...");
        
        backgroundThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                File storageDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
                if (storageDir == null) storageDir = getFilesDir();
                selectedInputWavFile = new File(storageDir, "imported_input.wav");

                InputStream in = null;
                OutputStream out = null;
                try {
                    in = getContentResolver().openInputStream(uri);
                    out = new FileOutputStream(selectedInputWavFile);
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                            tvSelectedFileInfo.setText("Custom File: " + selectedInputWavFile.getName() + " (" + (selectedInputWavFile.length()/1024) + " KB)");
                            btnPlayOriginal.setEnabled(true);
                        }
                    });
                } catch (IOException e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                            tvSelectedFileInfo.setText("Failed to import file.");
                            Toast.makeText(MainActivity.this, "Error importing WAV file", Toast.LENGTH_LONG).show();
                        }
                    });
                } finally {
                    try {
                        if (in != null) in.close();
                        if (out != null) out.close();
                    } catch (IOException ignored) {}
                }
            }
        });
    }

    private void processDস্পFiltersOnAudio() {
        final File targetInputFile = (selectedInputWavFile != null) ? selectedInputWavFile : wavRecordedFile;
        
        if (!targetInputFile.exists()) {
            Toast.makeText(this, "Please record first or import an audio file!", Toast.LENGTH_LONG).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(false);
        progressBar.setProgress(0);
        tvProcessingStatus.setText("Applying advanced audio filters and noise suppression algorithms...");
        btnCleanNow.setEnabled(false);

        backgroundThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                boolean success = performDSPMathAlgorithms(targetInputFile, outputCleanedFile);
                
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        btnCleanNow.setEnabled(true);
                        if (success) {
                            tvProcessingStatus.setText("Successfully cleaned! Background noise eliminated.");
                            btnPlayCleaned.setEnabled(true);
                            Toast.makeText(MainActivity.this, "Noise suppression applied successfully!", Toast.LENGTH_LONG).show();
                        } else {
                            tvProcessingStatus.setText("Process aborted or finished with errors.");
                            Toast.makeText(MainActivity.this, "An error occurred during audio filter pass.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }

    /**
     * Mathematical Digital Signal Processing routine executing 16-bit PCM algorithms
     * 1. High Pass Hum attenuation filter (using single-pole direct design)
     * 2. Gate amplitude-level expander to eliminate low background floor noise
     * 3. Dialog frequency amplification and peak clipping safeguard
     */
    private boolean performDSPMathAlgorithms(File input, File output) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(input);
            fos = new FileOutputStream(output);

            // Safely write standard 44-byte WAV header first
            byte[] wavHeader = new byte[44];
            int headerBytes = fis.read(wavHeader, 0, 44);
            if (headerBytes < 44) return false;
            fos.write(wavHeader);

            byte[] buffer = new byte[4096];
            int bytesRead;
            int totalProcessed = 0;
            final long fileLength = input.length();

            // Filter variables for the high-pass hum cut filter
            float previousRawSample = 0;
            float previousFilteredSample = 0;

            while ((bytesRead = fis.read(buffer)) != -1) {
                // Process PCM 16-bit (2 bytes per sample)
                for (int i = 0; i < bytesRead; i += 2) {
                    if (i + 1 >= bytesRead) break;
                    
                    // Construct short sample from 2 bytes (Little-Endian configuration)
                    short sample = (short) ((buffer[i] & 0xFF) | (buffer[i + 1] << 8));
                    float floatSample = (float) sample;

                    // 1. High-Pass Filter (removes low-frequency rumbles & electrical mains hums)
                    if (dspHighPassFactor > 0.05f) {
                        float rawSampleVal = floatSample;
                        // Highpass IIR Difference Equation
                        floatSample = dspHighPassFactor * (previousFilteredSample + rawSampleVal - previousRawSample);
                        previousRawSample = rawSampleVal;
                        previousFilteredSample = floatSample;
                    }

                    // 2. Active Noise Gate (attenuates absolute values below custom floor threshold)
                    float absValue = Math.abs(floatSample);
                    if (absValue < dspGateThreshold) {
                        // Soft dynamic gating multiplier to smooth sound cut-offs instead of hard mutes
                        float attenuationRatio = absValue / (float) dspGateThreshold;
                        floatSample = floatSample * attenuationRatio * 0.1f;
                    }

                    // 3. Dialog Boost (increases dynamic range scaling for voice audio profiles)
                    floatSample *= dspGainBoostMultiplier;

                    // Guard against loud peak clipping distortion
                    if (floatSample > 32767.0f) floatSample = 32767.0f;
                    if (floatSample < -32768.0f) floatSample = -32768.0f;

                    short finalSample = (short) floatSample;
                    buffer[i] = (byte) (finalSample & 0xFF);
                    buffer[i + 1] = (byte) ((finalSample >> 8) & 0xFF);
                }

                fos.write(buffer, 0, bytesRead);
                totalProcessed += bytesRead;
                
                // Dynamically update conversion progress bar metric
                if (fileLength > 0) {
                    final int progress = (int) ((totalProcessed * 100) / fileLength);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setProgress(progress);
                        }
                    });
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (fis != null) fis.close();
                if (fos != null) fos.close();
            } catch (IOException ignored) {}
        }
    }

    private void convertPcmToWav(File pcmFile, File wavFile) {
        long totalAudioLen = pcmFile.length();
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = SAMPLE_RATE;
        int channels = 1;
        long byteRate = 16 * SAMPLE_RATE * channels / 8;

        byte[] header = new byte[44];
        header[0] = 'R';  // RIFF
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';  // WAVE
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt '
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // Sub-chunk size
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;   // audio format = 1 (PCM)
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (channels * 16 / 8);  // block align
        header[33] = 0;
        header[34] = 16;  // bits per sample
        header[35] = 0;
        header[36] = 'd'; // data chunk identifier
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(pcmFile);
            fos = new FileOutputStream(wavFile);
            fos.write(header);
            byte[] buffer = new byte[4096];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null) fis.close();
                if (fos != null) fos.close();
            } catch (IOException ignored) {}
        }
    }

    private void playLocalFile(File file) {
        if (!file.exists()) {
            Toast.makeText(this, "Audio target file not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(file.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            Toast.makeText(this, "Playing: " + file.getName(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Failed to play file structure.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        backgroundThreadExecutor.shutdown();
    }
}