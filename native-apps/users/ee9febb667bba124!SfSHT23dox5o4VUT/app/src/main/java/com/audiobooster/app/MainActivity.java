package com.audiobooster.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int REQUEST_PERMISSION_CODE = 1001;

    // Audio Config for CD Quality WAV format
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private Thread recordingThread = null;
    private String tempRawPath = null;
    private File outputFolder = null;

    // Selected items info
    private File selectedFile = null;
    private int boostMultiplier = 15; // default 15x
    private MediaPlayer mediaPlayer;

    // UI Components
    private Button btnRecordStart;
    private Button btnRecordStop;
    private TextView txtRecordStatus;
    private TextView txtSelectedFile;
    private TextView txtAmplifyLevel;
    private SeekBar seekAmplify;
    private Button btnAmplifyProcess;
    private ListView listAudioFiles;
    private Button btnPlaySelected;
    private Button btnStopSelected;
    private Button btnDeleteSelected;

    private ArrayList<File> audioFileList = new ArrayList<>();
    private ArrayAdapter<String> listAdapter;
    private ArrayList<String> audioFileNamesList = new ArrayList<>();

    private Handler uiHandler;
    private long recordStartTime = 0;
    private Runnable timerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getResources().getIdentifier("activity_main", "layout", getPackageName()));

        uiHandler = new Handler(Looper.getMainLooper());

        // Setup Directory
        outputFolder = new File(getExternalFilesDir(null), "AudioBooster");
        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }
        tempRawPath = new File(getExternalFilesDir(null), "temp.raw").getAbsolutePath();

        // Initialize UI controls
        btnRecordStart = (Button) findViewById(getResources().getIdentifier("btn_record_start", "id", getPackageName()));
        btnRecordStop = (Button) findViewById(getResources().getIdentifier("btn_record_stop", "id", getPackageName()));
        txtRecordStatus = (TextView) findViewById(getResources().getIdentifier("txt_record_status", "id", getPackageName()));
        txtSelectedFile = (TextView) findViewById(getResources().getIdentifier("txt_selected_file", "id", getPackageName()));
        txtAmplifyLevel = (TextView) findViewById(getResources().getIdentifier("txt_amplify_level", "id", getPackageName()));
        seekAmplify = (SeekBar) findViewById(getResources().getIdentifier("seek_amplify", "id", getPackageName()));
        btnAmplifyProcess = (Button) findViewById(getResources().getIdentifier("btn_amplify_process", "id", getPackageName()));
        listAudioFiles = (ListView) findViewById(getResources().getIdentifier("list_audio_files", "id", getPackageName()));
        btnPlaySelected = (Button) findViewById(getResources().getIdentifier("btn_play_selected", "id", getPackageName()));
        btnStopSelected = (Button) findViewById(getResources().getIdentifier("btn_stop_selected", "id", getPackageName()));
        btnDeleteSelected = (Button) findViewById(getResources().getIdentifier("btn_delete_selected", "id", getPackageName()));

        // Default List Setup
        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice, audioFileNamesList);
        listAudioFiles.setAdapter(listAdapter);
        listAudioFiles.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        // Click Listeners
        btnRecordStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissionAndStartRecording();
            }
        });

        btnRecordStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
            }
        });

        seekAmplify.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                boostMultiplier = progress + 1;
                txtAmplifyLevel.setText("Amplification Level: " + boostMultiplier + "x");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnAmplifyProcess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                amplifyAndSaveSelectedFile();
            }
        });

        listAudioFiles.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedFile = audioFileList.get(position);
                txtSelectedFile.setText("Selected File: " + selectedFile.getName());
                btnAmplifyProcess.setEnabled(true);
                btnPlaySelected.setEnabled(true);
                btnDeleteSelected.setEnabled(true);
            }
        });

        btnPlaySelected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSelectedFile();
            }
        });

        btnStopSelected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopPlayback();
            }
        });

        btnDeleteSelected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteSelectedFile();
            }
        });

        // Setup timer updates
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRecording) {
                    long durationSec = (System.currentTimeMillis() - recordStartTime) / 1000;
                    txtRecordStatus.setText(String.format(Locale.getDefault(), "Status: Recording... (%02d:%02d)", durationSec / 60, durationSec % 60));
                    uiHandler.postDelayed(this, 1000);
                }
            }
        };

        // Refresh Directory
        refreshFilesList();
    }

    private void checkPermissionAndStartRecording() {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, REQUEST_PERMISSION_CODE);
        } else {
            startRecording();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording();
            } else {
                Toast.makeText(this, "Permission denied. Cannot record audio.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startRecording() {
        int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            minBufferSize = 4096;
        }

        try {
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, minBufferSize);
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Toast.makeText(this, "Unable to initialize Audio Recorder", Toast.LENGTH_SHORT).show();
                return;
            }

            audioRecord.startRecording();
            isRecording = true;
            recordStartTime = System.currentTimeMillis();

            // Start stream saver thread
            recordingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    writeRawAudioFile();
                }
            }, "Recording Thread");
            recordingThread.start();

            btnRecordStart.setEnabled(false);
            btnRecordStop.setEnabled(true);
            uiHandler.post(timerRunnable);
        } catch (SecurityException e) {
            Toast.makeText(this, "Permission Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Initialization error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void writeRawAudioFile() {
        byte[] data = new byte[2048];
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(tempRawPath);
            while (isRecording) {
                int readBytes = audioRecord.read(data, 0, data.length);
                if (AudioRecord.ERROR_INVALID_OPERATION != readBytes && AudioRecord.ERROR_BAD_VALUE != readBytes && readBytes > 0) {
                    os.write(data, 0, readBytes);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void stopRecording() {
        if (audioRecord == null) return;
        isRecording = false;
        try {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            if (recordingThread != null) {
                recordingThread.join();
                recordingThread = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        btnRecordStart.setEnabled(true);
        btnRecordStop.setEnabled(false);
        txtRecordStatus.setText("Status: Converting Raw to WAV format...");

        // Convert Raw captured PCM file into complete WAV
        new Thread(new Runnable() {
            @Override
            public void run() {
                String dateStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                final File destWavFile = new File(outputFolder, "Record_" + dateStamp + ".wav");

                long rawAudioLength = new File(tempRawPath).length();
                long totalDataLength = rawAudioLength + 36;
                long byteRate = SAMPLE_RATE * 2; // sample_rate * mono_16bits_factor_2

                try {
                    FileInputStream rawIn = new FileInputStream(tempRawPath);
                    FileOutputStream wavOut = new FileOutputStream(destWavFile);

                    writeWavHeader(wavOut, rawAudioLength, totalDataLength, SAMPLE_RATE, 1, byteRate);

                    byte[] buffer = new byte[4096];
                    int readBytes;
                    while ((readBytes = rawIn.read(buffer)) != -1) {
                        wavOut.write(buffer, 0, readBytes);
                    }

                    rawIn.close();
                    wavOut.close();

                    // Clean temporary raw storage
                    new File(tempRawPath).delete();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtRecordStatus.setText("Status: File recorded completely");
                            refreshFilesList();
                            Toast.makeText(MainActivity.this, "Saved to " + destWavFile.getName(), Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtRecordStatus.setText("Status: Error saving file");
                        }
                    });
                }
            }
        }).start();
    }

    private void writeWavHeader(FileOutputStream out, long rawAudioLength, long totalDataLength, long longSampleRate, int channels, long byteRate) throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLength & 0xff);
        header[5] = (byte) ((totalDataLength >> 8) & 0xff);
        header[6] = (byte) ((totalDataLength >> 16) & 0xff);
        header[7] = (byte) ((totalDataLength >> 24) & 0xff);
        header[8] = 'W'; // WAVE
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // subchunk size: 16
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // sample format = PCM
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
        header[32] = (byte) (channels * 2); // Block Align
        header[33] = 0;
        header[34] = 16; // Bits per sample
        header[35] = 0;
        header[36] = 'd'; // data chunk identifier
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (rawAudioLength & 0xff);
        header[41] = (byte) ((rawAudioLength >> 8) & 0xff);
        header[42] = (byte) ((rawAudioLength >> 16) & 0xff);
        header[43] = (byte) ((rawAudioLength >> 24) & 0xff);
        out.write(header, 0, 44);
    }

    private void amplifyAndSaveSelectedFile() {
        if (selectedFile == null) {
            Toast.makeText(this, "Please select a source file from standard list.", Toast.LENGTH_SHORT).show();
            return;
        }

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Processing Audio Booster, Please wait...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.show();

        final float scaleFactor = boostMultiplier;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    FileInputStream fis = new FileInputStream(selectedFile);
                    byte[] headerBytes = new byte[44];
                    int headerBytesRead = fis.read(headerBytes);
                    if (headerBytesRead < 44) {
                        throw new IOException("Invalid File structure");
                    }

                    // Generate dynamic name
                    String name = selectedFile.getName().replace(".wav", "");
                    final File outBoostedFile = new File(outputFolder, name + "_" + boostMultiplier + "x_boost.wav");
                    FileOutputStream fos = new FileOutputStream(outBoostedFile);

                    // Copy the header completely first
                    fos.write(headerBytes);

                    long totalPayloadBytes = selectedFile.length() - 44;
                    byte[] samplePair = new byte[2];
                    long bytesReadCounter = 0;

                    // Process 16-bit little-endian samples individually and clamp strictly to prevent cracking/distortion
                    while (fis.read(samplePair) == 2) {
                        bytesReadCounter += 2;
                        int rawSample = (short) (((samplePair[1] & 0xFF) << 8) | (samplePair[0] & 0xFF));
                        
                        float boostedVal = rawSample * scaleFactor;

                        // Standard soft clipping normalization limiters prevent high frequency screeching noise
                        if (boostedVal > 32767.0f) {
                            boostedVal = 32767.0f;
                        } else if (boostedVal < -32768.0f) {
                            boostedVal = -32768.0f;
                        }

                        short outputSample = (short) boostedVal;
                        samplePair[0] = (byte) (outputSample & 0xFF);
                        samplePair[1] = (byte) ((outputSample >> 8) & 0xFF);
                        fos.write(samplePair);

                        if (bytesReadCounter % 4096 == 0) {
                            final int progressPercent = (int) ((bytesReadCounter * 100) / totalPayloadBytes);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.setProgress(progressPercent);
                                }
                            });
                        }
                    }

                    fis.close();
                    fos.close();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            refreshFilesList();
                            Toast.makeText(MainActivity.this, "Boost Completed! saved as " + outBoostedFile.getName(), Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Amplify process failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void playSelectedFile() {
        if (selectedFile == null) return;
        stopPlayback();
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(selectedFile.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            btnPlaySelected.setEnabled(false);
            btnStopSelected.setEnabled(true);
            
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    stopPlayback();
                }
            });
        } catch (IOException e) {
            Toast.makeText(this, "Error during audio playback: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopPlayback() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaPlayer = null;
        }
        btnPlaySelected.setEnabled(selectedFile != null);
        btnStopSelected.setEnabled(false);
    }

    private void deleteSelectedFile() {
        if (selectedFile == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Delete Audio File")
                .setMessage("Are you sure you want to permanently delete " + selectedFile.getName() + "?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        stopPlayback();
                        if (selectedFile.delete()) {
                            Toast.makeText(MainActivity.this, "File deleted", Toast.LENGTH_SHORT).show();
                            selectedFile = null;
                            txtSelectedFile.setText("Selected File: None (Tap on standard recording below)");
                            btnAmplifyProcess.setEnabled(false);
                            btnPlaySelected.setEnabled(false);
                            btnDeleteSelected.setEnabled(false);
                            refreshFilesList();
                        } else {
                            Toast.makeText(MainActivity.this, "Cannot delete requested file", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void refreshFilesList() {
        audioFileList.clear();
        audioFileNamesList.clear();

        File[] files = outputFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".wav")) {
                    audioFileList.add(file);
                    long sizeKb = file.length() / 1024;
                    audioFileNamesList.add(file.getName() + " (" + sizeKb + " KB)");
                }
            }
        }
        listAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRecording = false;
        if (audioRecord != null) {
            try {
                audioRecord.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        stopPlayback();
    }
}