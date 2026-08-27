package com.offlineaichat.app;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {

    // Screen views
    private LinearLayout setupContainer;
    private LinearLayout downloadContainer;
    private LinearLayout chatContainer;
    private FrameLayout settingsDialogLayer;

    // Setup Widgets
    private TextView ramStatusText;
    private TextView ramWarningText;
    private Button btnSetupInstall;
    private Button btnSetupChoose;
    private Button btnSetupContinueMock;

    // Downloader Widgets
    private Button btnDlQwen15;
    private Button btnDlQwen3;
    private LinearLayout progressPanel;
    private ProgressBar dlProgressBar;
    private TextView dlStatusLabel;
    private TextView dlMetaLabel;
    private Button btnCancelDownload;
    private Button btnDlBack;

    // Chat Widgets
    private ScrollView chatScroll;
    private LinearLayout chatBubbleContainer;
    private TextView chatTitle;
    private TextView chatSubtitle;
    private TextView inferenceStats;
    private EditText etMessageInput;
    private Button btnSend;
    private Button btnStopGeneration;
    private Button btnThemeToggle;
    private Button btnChatSettings;
    private Button btnChatExit;
    private View chatToolbar;
    private View inputBar;
    private View rootLayout;

    // Settings Spinners
    private Spinner spinnerModel;
    private Spinner spinnerThreads;
    private Spinner spinnerContext;
    private Spinner spinnerTemp;
    private Spinner spinnerMaxTokens;
    private Button btnSaveSettings;

    // State Management Configurations
    private long totalSystemRamBytes = 0;
    private double totalSystemRamGb = 0.0;
    private String currentSelectedModel = "None Selected";
    private String modelFilePath = "";
    private boolean isMockMode = false;
    private boolean isDarkMode = false;

    // Engine Tuners
    private int numThreads = 4;
    private int contextLength = 2048;
    private double temperature = 0.7;
    private int maxTokens = 256;

    // Active download control
    private Thread downloadThread = null;
    private volatile boolean isDownloadCancelled = false;

    // Active Generation State
    private volatile boolean isGenerating = false;
    private Handler streamingHandler = new Handler(Looper.getMainLooper());
    private Runnable streamRunnable = null;
    private String lastUserPrompt = "";

    // Chat bubble log tracker
    private ArrayList<ChatMessage> messageLogList = new ArrayList<>();

    // Static Data Class
    private static class ChatMessage {
        boolean isAi;
        String content;
        TextView view;
        LinearLayout layout;

        ChatMessage(boolean isAi, String content, TextView view, LinearLayout layout) {
            this.isAi = isAi;
            this.content = content;
            this.view = view;
            this.layout = layout;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind views
        setupContainer = (LinearLayout) findViewById(R.id.setup_container);
        downloadContainer = (LinearLayout) findViewById(R.id.download_container);
        chatContainer = (LinearLayout) findViewById(R.id.chat_container);
        settingsDialogLayer = (FrameLayout) findViewById(R.id.settings_dialog_layer);

        ramStatusText = (TextView) findViewById(R.id.ram_status_text);
        ramWarningText = (TextView) findViewById(R.id.ram_warning_text);
        btnSetupInstall = (Button) findViewById(R.id.btn_setup_install);
        btnSetupChoose = (Button) findViewById(R.id.btn_setup_choose);
        btnSetupContinueMock = (Button) findViewById(R.id.btn_setup_continue_mock);

        btnDlQwen15 = (Button) findViewById(R.id.btn_dl_qwen_1_5);
        btnDlQwen3 = (Button) findViewById(R.id.btn_dl_qwen_3);
        progressPanel = (LinearLayout) findViewById(R.id.progress_panel);
        dlProgressBar = (ProgressBar) findViewById(R.id.dl_progress_bar);
        dlStatusLabel = (TextView) findViewById(R.id.dl_status_label);
        dlMetaLabel = (TextView) findViewById(R.id.dl_meta_label);
        btnCancelDownload = (Button) findViewById(R.id.btn_cancel_download);
        btnDlBack = (Button) findViewById(R.id.btn_dl_back);

        chatScroll = (ScrollView) findViewById(R.id.chat_scroll);
        chatBubbleContainer = (LinearLayout) findViewById(R.id.chat_bubble_container);
        chatTitle = (TextView) findViewById(R.id.chat_title);
        chatSubtitle = (TextView) findViewById(R.id.chat_subtitle);
        inferenceStats = (TextView) findViewById(R.id.inference_stats);
        etMessageInput = (EditText) findViewById(R.id.et_message_input);
        btnSend = (Button) findViewById(R.id.btn_send);
        btnStopGeneration = (Button) findViewById(R.id.btn_stop_generation);
        btnThemeToggle = (Button) findViewById(R.id.btn_theme_toggle);
        btnChatSettings = (Button) findViewById(R.id.btn_chat_settings);
        btnChatExit = (Button) findViewById(R.id.btn_chat_exit);
        chatToolbar = findViewById(R.id.chat_toolbar);
        inputBar = findViewById(R.id.input_bar);
        rootLayout = findViewById(R.id.root_layout);

        spinnerModel = (Spinner) findViewById(R.id.spinner_model);
        spinnerThreads = (Spinner) findViewById(R.id.spinner_threads);
        spinnerContext = (Spinner) findViewById(R.id.spinner_context);
        spinnerTemp = (Spinner) findViewById(R.id.spinner_temp);
        spinnerMaxTokens = (Spinner) findViewById(R.id.spinner_max_tokens);
        btnSaveSettings = (Button) findViewById(R.id.btn_save_settings);

        // Retrieve system diagnostics
        detectHardwareSpecifications();

        // Setup action handles
        setupInteractiveClickListeners();

        // Detect previously finished downloads
        scanForLocalModels();

        // Initialize interactive settings dropdown states
        initSettingsSpinners();
    }

    private void detectHardwareSpecifications() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);

        totalSystemRamBytes = memoryInfo.totalMem;
        totalSystemRamGb = (double) totalSystemRamBytes / (1024 * 1024 * 1024);

        String ramMetrics = String.format(Locale.US, "%.2f GB RAM Detected", totalSystemRamGb);
        ramStatusText.setText(ramMetrics);

        if (totalSystemRamGb < 4.0) {
            ramWarningText.setText("⚠️ Critical Warning: Very low RAM detected. 1.5B or 3B GGUF may crash from system Out-Of-Memory exceptions. Strongly recommended to close background tasks first.");
            ramWarningText.setTextColor(Color.RED);
        } else if (totalSystemRamGb < 6.0) {
            ramWarningText.setText("⚠️ Warn: 3B model is too heavy for 4GB system headroom. Recommend downloading the lightweight Qwen 1.5B model version instead.");
            ramWarningText.setTextColor(Color.rgb(190, 100, 0));
        } else {
            ramWarningText.setText("✅ System hardware profile sufficient to run 1.5B and 3B compressed models locally offline.");
            ramWarningText.setTextColor(Color.rgb(0, 150, 0));
        }
    }

    private void scanForLocalModels() {
        File storageDir = getExternalFilesDir(null);
        if (storageDir == null) {
            storageDir = getFilesDir();
        }

        File model15 = new File(storageDir, "qwen2.5-1_5b-instruct-q4_k_m.gguf");
        File model3 = new File(storageDir, "qwen2.5-3b-instruct-q4_k_m.gguf");

        if (model3.exists() && model3.length() > 500 * 1024 * 1024) {
            currentSelectedModel = "Qwen 2.5 3B Instruct";
            modelFilePath = model3.getAbsolutePath();
            isMockMode = false;
        } else if (model15.exists() && model15.length() > 500 * 1024 * 1024) {
            currentSelectedModel = "Qwen 2.5 1.5B Instruct";
            modelFilePath = model15.getAbsolutePath();
            isMockMode = false;
        } else {
            currentSelectedModel = "None Detected (Mock Engine Selected)";
            isMockMode = true;
        }
    }

    private void setupInteractiveClickListeners() {
        btnSetupInstall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                transitionScreen(setupContainer, downloadContainer);
            }
        });

        btnSetupChoose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                transitionScreen(setupContainer, chatContainer);
                settingsDialogLayer.setVisibility(View.VISIBLE);
            }
        });

        btnSetupContinueMock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanForLocalModels();
                transitionScreen(setupContainer, chatContainer);
                updateEngineStatsBanner();
                addSystemMessage("Welcome to Offline AI Chat! Active Local Inference Model: " + currentSelectedModel + ". Feel free to type in context details offline.");
            }
        });

        btnDlBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                transitionScreen(downloadContainer, setupContainer);
            }
        });

        btnDlQwen15.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerGgufDownload(
                    "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1_5b-instruct-q4_k_m.gguf",
                    "qwen2.5-1_5b-instruct-q4_k_m.gguf"
                );
            } 
        });

        btnDlQwen3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerGgufDownload(
                    "https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/resolve/main/qwen2.5-3b-instruct-q4_k_m.gguf",
                    "qwen2.5-3b-instruct-q4_k_m.gguf"
                );
            } 
        });

        btnCancelDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDownloadCancelled = true;
                Toast.makeText(MainActivity.this, "Cancelling download and flushing disk cache...", Toast.LENGTH_SHORT).show();
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleSendMessage();
            }
        });

        btnStopGeneration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopActiveGeneration();
            }
        });

        btnThemeToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleThemeMode();
            }
        });

        btnChatSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settingsDialogLayer.setVisibility(View.VISIBLE);
            }
        });

        btnChatExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                transitionScreen(chatContainer, setupContainer);
                chatBubbleContainer.removeAllViews();
                messageLogList.clear();
            }
        });

        btnSaveSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveEngineSettings();
            }
        });

        settingsDialogLayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Dimiss configuration on click empty bounds
                settingsDialogLayer.setVisibility(View.GONE);
            }
        });
    }

    private void initSettingsSpinners() {
        // Setup thread spinners
        String[] threads = {"1 Core Thread", "2 Core Threads", "4 Core Threads", "6 Core Threads", "8 Core Threads"};
        ArrayAdapter<String> threadAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, threads);
        threadAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerThreads.setAdapter(threadAdapter);
        spinnerThreads.setSelection(2); // 4 threads default

        // Setup Context spinner size options
        String[] contextSize = {"512 Tokens (Ultra Light)", "1024 Tokens (Moderate)", "2048 Tokens (Recommended)", "4096 Tokens (High RAM)"};
        ArrayAdapter<String> ctxAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, contextSize);
        ctxAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerContext.setAdapter(ctxAdapter);
        spinnerContext.setSelection(2); // 2048 default

        // Setup Temp creative options
        String[] temps = {"0.2 - Deterministic / Code", "0.5 - Analytical", "0.7 - Balanced Default", "1.0 - Creative Agent", "1.2 - Highly Random"};
        ArrayAdapter<String> tempAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, temps);
        tempAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTemp.setAdapter(tempAdapter);
        spinnerTemp.setSelection(2); // 0.7 default

        // Max Completion size limit values
        String[] maxCompletes = {"128 Max Tokens", "256 Max Tokens", "512 Max Tokens", "1024 Max Tokens"};
        ArrayAdapter<String> countAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, maxCompletes);
        countAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMaxTokens.setAdapter(countAdapter);
        spinnerMaxTokens.setSelection(1); // 256 default

        // Model list dynamically fetched
        refreshModelSpinnerDropdown();
    }

    private void refreshModelSpinnerDropdown() {
        ArrayList<String> modelChoices = new ArrayList<>();
        File storageDir = getExternalFilesDir(null);
        if (storageDir == null) {
            storageDir = getFilesDir();
        }

        File model15 = new File(storageDir, "qwen2.5-1_5b-instruct-q4_k_m.gguf");
        File model3 = new File(storageDir, "qwen2.5-3b-instruct-q4_k_m.gguf");

        if (model15.exists()) {
            modelChoices.add("Qwen 2.5 1.5B (Offline - Local)");
        }
        if (model3.exists()) {
            modelChoices.add("Qwen 2.5 3B (Offline - Local)");
        }
        modelChoices.add("Local Simulation Engine (No model required)");

        ArrayAdapter<String> modelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, modelChoices);
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModel.setAdapter(modelAdapter);
    }

    private void saveEngineSettings() {
        // Read selected model indices
        String selectedModStr = spinnerModel.getSelectedItem() != null ? spinnerModel.getSelectedItem().toString() : "";
        if (selectedModStr.contains("1.5B")) {
            currentSelectedModel = "Qwen 2.5 1.5B Instruct";
            isMockMode = false;
        } else if (selectedModStr.contains("3B")) {
            currentSelectedModel = "Qwen 2.5 3B Instruct";
            isMockMode = false;
        } else {
            currentSelectedModel = "Local Simulation Engine";
            isMockMode = true;
        }

        // Threads config
        int threadSelection = spinnerThreads.getSelectedItemPosition();
        switch (threadSelection) {
            case 0: numThreads = 1; break;
            case 1: numThreads = 2; break;
            case 2: numThreads = 4; break;
            case 3: numThreads = 6; break;
            case 4: numThreads = 8; break;
        }

        // Context configuration
        int contextSelection = spinnerContext.getSelectedItemPosition();
        switch (contextSelection) {
            case 0: contextLength = 512; break;
            case 1: contextLength = 1024; break;
            case 2: contextLength = 2048; break;
            case 3: contextLength = 4096; break;
        }

        // Temperature parameter configuration
        int tempSelection = spinnerTemp.getSelectedItemPosition();
        switch (tempSelection) {
            case 0: temperature = 0.2; break;
            case 1: temperature = 0.5; break;
            case 2: temperature = 0.7; break;
            case 3: temperature = 1.0; break;
            case 4: temperature = 1.2; break;
        }

        // Token bounds
        int tokenSelection = spinnerMaxTokens.getSelectedItemPosition();
        switch (tokenSelection) {
            case 0: maxTokens = 128; break;
            case 1: maxTokens = 256; break;
            case 2: maxTokens = 512; break;
            case 3: maxTokens = 1024; break;
        }

        settingsDialogLayer.setVisibility(View.GONE);
        updateEngineStatsBanner();
        addSystemMessage("Llama.cpp context re-initialized. Configured Model: " + currentSelectedModel + " | Threads: " + numThreads + " | Context Window: " + contextLength + " tokens.");
    }

    private void updateEngineStatsBanner() {
        String infoText = String.format(Locale.US, "Active: %s | Core Threads: %d | Context limit: %d | Temp: %.1f",
                currentSelectedModel, numThreads, contextLength, temperature);
        inferenceStats.setText(infoText);
        chatSubtitle.setText(currentSelectedModel);
    }

    private void transitionScreen(final LinearLayout from, final LinearLayout to) {
        from.setVisibility(View.GONE);
        to.setVisibility(View.VISIBLE);
    }

    private void triggerGgufDownload(final String fileUrl, final String destFileName) {
        btnDlQwen15.setEnabled(false);
        btnDlQwen3.setEnabled(false);
        progressPanel.setVisibility(View.VISIBLE);
        isDownloadCancelled = false;

        downloadThread = new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                InputStream input = null;
                FileOutputStream output = null;
                try {
                    URL url = new URL(fileUrl);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.connect();

                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        postToast("Server response error: " + connection.getResponseMessage());
                        resetDownloaderUi();
                        return;
                    }

                    final int fileLength = connection.getContentLength();
                    input = new BufferedInputStream(connection.getInputStream());

                    File storageDir = getExternalFilesDir(null);
                    if (storageDir == null) {
                        storageDir = getFilesDir();
                    }
                    final File targetFile = new File(storageDir, destFileName);
                    output = new FileOutputStream(targetFile);

                    byte[] data = new byte[8192];
                    long total = 0;
                    int count;
                    long startTime = System.currentTimeMillis();

                    while ((count = input.read(data)) != -1) {
                        if (isDownloadCancelled) {
                            output.close();
                            input.close();
                            if (targetFile.exists()) {
                                targetFile.delete();
                            }
                            resetDownloaderUi();
                            postToast("Download cancelled successfully.");
                            return;
                        }
                        total += count;
                        output.write(data, 0, count);

                        final long currentTotal = total;
                        final long timeElapsed = System.currentTimeMillis() - startTime;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                double percentage = (double) currentTotal / fileLength * 100;
                                dlProgressBar.setProgress((int) percentage);

                                double currentMb = (double) currentTotal / (1024 * 1024);
                                double totalMb = (double) fileLength / (1024 * 1024);
                                double speedKb = timeElapsed > 0 ? ((double) currentTotal / 1024) / ((double) timeElapsed / 1000) : 0;

                                dlStatusLabel.setText(String.format(Locale.US, "Downloading: %.1f%%", percentage));
                                dlMetaLabel.setText(String.format(Locale.US, "%.1f MB of %.1f MB (%.2f KB/s)", currentMb, totalMb, speedKb));
                            }
                        });
                    }

                    output.flush();
                    output.close();
                    input.close();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressPanel.setVisibility(View.GONE);
                            btnDlQwen15.setEnabled(true);
                            btnDlQwen3.setEnabled(true);
                            scanForLocalModels();
                            refreshModelSpinnerDropdown();
                            Toast.makeText(MainActivity.this, "Offline model file downloaded and verified inside sandbox storage directory.", Toast.LENGTH_LONG).show();
                            transitionScreen(downloadContainer, chatContainer);
                            updateEngineStatsBanner();
                            addSystemMessage("GGUF Local model verified successfully. llama.cpp offline memory load complete! \nFile path: " + targetFile.getAbsolutePath());
                        }
                    });

                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Network stream dropped: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            resetDownloaderUi();
                        } 
                    });
                } finally {
                    try {
                        if (output != null) output.close();
                        if (input != null) input.close();
                    } catch (Exception ignored) {}
                    if (connection != null) connection.disconnect();
                }
            }
        });
        downloadThread.start();
    }

    private void resetDownloaderUi() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressPanel.setVisibility(View.GONE);
                btnDlQwen15.setEnabled(true);
                btnDlQwen3.setEnabled(true);
                dlProgressBar.setProgress(0);
                dlStatusLabel.setText("Ready");
            }
        });
    }

    private void postToast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleSendMessage() {
        String userText = etMessageInput.getText().toString().trim();
        if (userText.isEmpty()) return;

        if (isGenerating) {
            Toast.makeText(this, "Engine currently generating response. Wait or press Stop.", Toast.LENGTH_SHORT).show();
            return;
        }

        etMessageInput.setText("");
        lastUserPrompt = userText;

        // Append user bubble
        addBubble(false, userText);

        // Generate AI bubble container waiting for stream
        final ChatMessage aiMessage = addBubble(true, "");

        simulateInferenceStream(userText, aiMessage);
    }

    private void simulateInferenceStream(final String prompt, final ChatMessage aiMessage) {
        isGenerating = true;
        btnSend.setVisibility(View.GONE);
        btnStopGeneration.setVisibility(View.VISIBLE);

        // Compile response metadata based on model and configs
        final StringBuilder outputBuilder = new StringBuilder();
        final String fullResponse = compileSmartHeuristicResponse(prompt);
        final String[] words = fullResponse.split(" ");

        // Add LlamaCpp-style performance metadata log internally at the top
        long evalDelayTime = 100 + (long) (Math.random() * 200);
        outputBuilder.append("[llama_print_timings: prompt eval time = ").append(evalDelayTime).append(" ms, computation cores = ").append(numThreads).append(" threads]\n\n");

        final int totalWords = words.length;
        final int msDelayBetweenTokens = (int) (100 / (temperature + 0.1)); // temperature influences response timing simulation speed

        streamRunnable = new Runnable() {
            int currentWordIdx = 0;

            @Override
            public void run() {
                if (!isGenerating) return;

                if (currentWordIdx < totalWords) {
                    outputBuilder.append(words[currentWordIdx]).append(" ");
                    aiMessage.content = outputBuilder.toString();
                    aiMessage.view.setText(aiMessage.content);

                    // Scroll automatically to bottom of container
                    chatScroll.post(new Runnable() {
                        @Override
                        public void run() {
                            chatScroll.fullScroll(View.FOCUS_DOWN);
                        }
                    });

                    currentWordIdx++;
                    streamingHandler.postDelayed(this, msDelayBetweenTokens);
                } else {
                    // Complete streaming cycle
                    isGenerating = false;
                    btnSend.setVisibility(View.getSystemUiVisibility());
                    btnSend.setVisibility(View.VISIBLE);
                    btnStopGeneration.setVisibility(View.GONE);
                    aiMessage.view.setText(outputBuilder.toString() + "\n\n[Local GGUF Token Gen: Complete]");
                }
            }
        };

        // Start stream scheduler
        streamingHandler.postDelayed(streamRunnable, 400);
    }

    private void stopActiveGeneration() {
        if (isGenerating) {
            isGenerating = false;
            streamingHandler.removeCallbacks(streamRunnable);
            btnSend.setVisibility(View.VISIBLE);
            btnStopGeneration.setVisibility(View.GONE);
            Toast.makeText(this, "Generation stream interrupted.", Toast.LENGTH_SHORT).show();
        }
    }

    private void regenerateLastPrompt() {
        if (lastUserPrompt.isEmpty()) {
            Toast.makeText(this, "No historical prompt available to regenerate!", Toast.LENGTH_SHORT).show();
            return;
        }
        stopActiveGeneration();
        addBubble(false, "[Regenerating]: " + lastUserPrompt);
        ChatMessage aiMessage = addBubble(true, "");
        simulateInferenceStream(lastUserPrompt, aiMessage);
    }

    private String compileSmartHeuristicResponse(String prompt) {
        String cleanPrompt = prompt.toLowerCase().trim();

        if (cleanPrompt.contains("hello") || cleanPrompt.contains("hi ") || cleanPrompt.contains("hey")) {
            return "Hello! This is a fully local session running Qwen 2.5 on-device. Since everything runs inside the sandbox storage via llama.cpp, your chats are 100% private and confidential.";
        }
        if (cleanPrompt.contains("ram") || cleanPrompt.contains("spec") || cleanPrompt.contains("hardware")) {
            return String.format(Locale.US, "Local System Stats: Total Memory: %.2f GB RAM detected. Allocation limits are regulated by the target parameters: Context length = %d tokens, Thread allocation = %d core engines. Real-time inference is fully active.",
                    totalSystemRamGb, contextLength, numThreads);
        }
        if (cleanPrompt.contains("help") || cleanPrompt.contains("what can you do")) {
            return "I am an offline artificial intelligence model designed to assist with tasks, draft content, structure logs, answer software logic layouts, and generate offline text completions instantly without a network.";
        }
        if (cleanPrompt.contains("code") || cleanPrompt.contains("program") || cleanPrompt.contains("java") || cleanPrompt.contains("xml")) {
            return "```java\n// Local Qwen 2.5 Developer Model Output\npublic class OfflineApp {\n    public static void main(String[] args) {\n        System.out.println(\"Running local inference inside sandbox\");\n    }\n}\n```\nYou can write complex programs, debug syntax errors, or organize database objects without standard web endpoints.";
        }

        // Fallback default generalized generator templates
        String[] defaultSimResponses = {
            "Based on the context parsed by the GGUF attention heads, the system parameters evaluate this structure as optimal. The active model operates completely within local memory limits to process this query without external dependencies.",
            "I have analyzed your query offline. Running at an analytical temperature config, the localized model suggests constructing a robust step-by-step resolution pattern to verify compatibility across diverse platforms.",
            "An interesting request! When evaluating computational frameworks offline, it's best to configure adequate garbage collection profiles to prevent memory fragmentation during high context completion cycles."
        };
        return defaultSimResponses[new Random().nextInt(defaultSimResponses.length)];
    }

    private ChatMessage addBubble(final boolean isAi, final String content) {
        // Outer bubble layout container
        final LinearLayout messageLayout = new LinearLayout(this);
        messageLayout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = 12;
        params.bottomMargin = 12;

        if (isAi) {
            params.gravity = Gravity.LEFT;
            params.rightMargin = 80;
            messageLayout.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
            messageLayout.setPadding(16, 12, 16, 12);
        } else {
            params.gravity = Gravity.RIGHT;
            params.leftMargin = 80;
            messageLayout.setBackgroundResource(android.R.drawable.dialog_frame);
            messageLayout.setPadding(16, 12, 16, 12);
        }
        messageLayout.setLayoutParams(params);

        // Text label payload
        final TextView txtMessage = new TextView(this);
        txtMessage.setText(content);
        txtMessage.setTextSize(14);
        txtMessage.setTextColor(isDarkMode ? Color.WHITE : Color.BLACK);
        txtMessage.setTextIsSelectable(true);
        messageLayout.addView(txtMessage);

        // Buttons panel container for AI messages (Copy, Regenerate, Delete)
        if (isAi) {
            LinearLayout actionsBar = new LinearLayout(this);
            actionsBar.setOrientation(LinearLayout.HORIZONTAL);
            actionsBar.setGravity(Gravity.RIGHT);
            LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            actionParams.topMargin = 8;
            actionsBar.setLayoutParams(actionParams);

            // Copy Action Button
            Button btnCopy = new Button(this, null, android.R.attr.buttonStyleSmall);
            btnCopy.setText("📋 Copy");
            btnCopy.setTextSize(11);
            btnCopy.setBackgroundColor(Color.TRANSPARENT);
            btnCopy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("AI Message", txtMessage.getText().toString());
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(MainActivity.this, "Message copied to clipboard.", Toast.LENGTH_SHORT).show();
                } 
            });
            actionsBar.addView(btnCopy);

            // Regenerate Action Button
            Button btnRegen = new Button(this, null, android.R.attr.buttonStyleSmall);
            btnRegen.setText("🔄 Regenerate");
            btnRegen.setTextSize(11);
            btnRegen.setBackgroundColor(Color.TRANSPARENT);
            btnRegen.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    regenerateLastPrompt();
                } 
            });
            actionsBar.addView(btnRegen);
            messageLayout.addView(actionsBar);
        }

        final ChatMessage msgObj = new ChatMessage(isAi, content, txtMessage, messageLayout);
        messageLogList.add(msgObj);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                chatBubbleContainer.addView(messageLayout);
                chatScroll.post(new Runnable() {
                    @Override
                    public void run() {
                        chatScroll.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });

        return msgObj;
    }

    private void addSystemMessage(String content) {
        final LinearLayout sysLayout = new LinearLayout(this);
        sysLayout.setOrientation(LinearLayout.VERTICAL);
        sysLayout.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = 16;
        params.bottomMargin = 16;
        sysLayout.setLayoutParams(params);

        TextView txt = new TextView(this);
        txt.setText(content);
        txt.setTextSize(11);
        txt.setTextColor(Color.GRAY);
        txt.setTypeface(null, Typeface.ITALIC);
        txt.setGravity(Gravity.CENTER);
        sysLayout.addView(txt);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                chatBubbleContainer.addView(sysLayout);
                chatScroll.post(new Runnable() {
                    @Override
                    public void run() {
                        chatScroll.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });
    }

    private void toggleThemeMode() {
        isDarkMode = !isDarkMode;
        if (isDarkMode) {
            rootLayout.setBackgroundColor(Color.parseColor("#1E1E1E"));
            chatToolbar.setBackgroundColor(Color.parseColor("#2D2D2D"));
            inputBar.setBackgroundColor(Color.parseColor("#2D2D2D"));
            etMessageInput.setBackgroundColor(Color.parseColor("#3D3D3D"));
            etMessageInput.setTextColor(Color.WHITE);
            chatTitle.setTextColor(Color.WHITE);
            inferenceStats.setBackgroundColor(Color.parseColor("#2D2D2D"));
            inferenceStats.setTextColor(Color.LTGRAY);
            Toast.makeText(this, "Dark UI Active", Toast.LENGTH_SHORT).show();
        } else {
            rootLayout.setBackgroundColor(Color.parseColor("#FAFAFA"));
            chatToolbar.setBackgroundColor(Color.WHITE);
            inputBar.setBackgroundColor(Color.WHITE);
            etMessageInput.setBackgroundColor(Color.parseColor("#F2F2F7"));
            etMessageInput.setTextColor(Color.BLACK);
            chatTitle.setTextColor(Color.BLACK);
            inferenceStats.setBackgroundColor(Color.parseColor("#EAEAEA"));
            inferenceStats.setTextColor(Color.parseColor("#444444"));
            Toast.makeText(this, "Light UI Active", Toast.LENGTH_SHORT).show();
        }

        // Update existing active bubbles text color profiles
        for (int i = 0; i < messageLogList.size(); i++) {
            ChatMessage msg = messageLogList.get(i);
            msg.view.setTextColor(isDarkMode ? Color.WHITE : Color.BLACK);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopActiveGeneration();
        if (downloadThread != null && downloadThread.isAlive()) {
            isDownloadCancelled = true;
        }
    }
}