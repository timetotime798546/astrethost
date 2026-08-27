package com.localqwenai.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class MainActivity extends Activity {

    private SharedPreferences prefs;
    private ScrollView chatScrollView;
    private LinearLayout chatContainer;
    private EditText inputMessage;
    private Button btnSend;
    private ImageButton btnSettings;

    private Handler pollingHandler;
    private Runnable pollingRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("LocalQwenPrefs", MODE_PRIVATE);
        pollingHandler = new Handler(Looper.getMainLooper());

        chatScrollView = findViewById(R.id.chat_scroll_view);
        chatContainer = findViewById(R.id.chat_container);
        inputMessage = findViewById(R.id.input_message);
        btnSend = findViewById(R.id.btn_send);
        btnSettings = findViewById(R.id.btn_settings);

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettingsDialog();
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        // Add Default Greeting Message on Start
        addMessage("Hello! I am your Local Qwen AI Assistant. Configure my server URL in settings (Default: http://127.0.0.1:3333). How can I assist you today?", false);
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_settings, null);
        builder.setView(dialogView);

        final EditText editServerUrl = dialogView.findViewById(R.id.edit_server_url);
        final EditText editSystemInstruction = dialogView.findViewById(R.id.edit_system_instruction);

        editServerUrl.setText(prefs.getString("server_url", "http://127.0.0.1:3333"));
        editSystemInstruction.setText(prefs.getString("system_instruction", "You are a helpful local assistant."));

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String serverUrl = editServerUrl.getText().toString().trim();
                String systemInstruction = editSystemInstruction.getText().toString().trim();

                if (serverUrl.isEmpty()) {
                    serverUrl = "http://127.0.0.1:3333";
                }
                if (serverUrl.endsWith("/")) {
                    serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
                }

                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("server_url", serverUrl);
                editor.putString("system_instruction", systemInstruction);
                editor.apply();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private TextView addMessage(String text, boolean isUser) {
        LinearLayout bubbleLayout = new LinearLayout(this);
        bubbleLayout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        containerParams.setMargins(0, 8, 0, 8);
        bubbleLayout.setLayoutParams(containerParams);

        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(15);
        textView.setPadding(20, 14, 20, 14);

        LinearLayout.LayoutParams bubbleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        if (isUser) {
            bubbleLayout.setGravity(Gravity.RIGHT);
            textView.setBackgroundResource(R.drawable.user_bubble);
            textView.setTextColor(Color.WHITE);
            bubbleParams.gravity = Gravity.RIGHT;
            bubbleParams.leftMargin = 80;
        } else {
            bubbleLayout.setGravity(Gravity.LEFT);
            textView.setBackgroundResource(R.drawable.bot_bubble);
            textView.setTextColor(Color.BLACK);
            bubbleParams.gravity = Gravity.LEFT;
            bubbleParams.rightMargin = 80;
        }

        textView.setLayoutParams(bubbleParams);
        bubbleLayout.addView(textView);
        chatContainer.addView(bubbleLayout);

        // Auto Scroll to absolute bottom
        chatScrollView.post(new Runnable() {
            @Override
            public void run() {
                chatScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });

        return textView;
    }

    private void sendMessage() {
        final String userMsg = inputMessage.getText().toString().trim();
        if (userMsg.isEmpty()) return;

        // Clear input field
        inputMessage.setText("");

        // Show User Message
        addMessage(userMsg, true);

        // Add Temporary Bot Message View placeholder
        final TextView botMessageView = addMessage("Connecting with local server...", false);

        // Block controls
        inputMessage.setEnabled(false);
        btnSend.setEnabled(false);

        final String serverUrl = prefs.getString("server_url", "http://127.0.0.1:3333");
        String systemInstruction = prefs.getString("system_instruction", "You are a helpful local assistant.");
        final String combinedPrompt = systemInstruction + "\n\nUser: " + userMsg;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(serverUrl + "/chat");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; utf-8");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(6000);
                    conn.setReadTimeout(6000);

                    JSONObject jsonParam = new JSONObject();
                    jsonParam.put("prompt", combinedPrompt);

                    byte[] input = jsonParam.toString().getBytes("utf-8");
                    OutputStream os = conn.getOutputStream();
                    os.write(input, 0, input.length);
                    os.close();

                    int code = conn.getResponseCode();
                    if (code == 200 || code == 201) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        br.close();

                        JSONObject jsonResponse = new JSONObject(response.toString());
                        final String jobId = jsonResponse.getString("job_id");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                botMessageView.setText("Server received job. Processing...");
                                startPolling(serverUrl, jobId, botMessageView);
                            } 
                        });
                    } else {
                        throw new Exception("HTTP " + code);
                    }
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            botMessageView.setText("Error: Failed to reach local flask server.\nMake sure your server is running at " + serverUrl + " and accessible from Android emulator/device.");
                            enableInputs();
                        }
                    });
                }
            }
        }).start();
    }

    private void startPolling(final String baseUrl, final String jobId, final TextView botTextView) {
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            URL url = new URL(baseUrl + "/job/" + jobId);
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.setRequestMethod("GET");
                            conn.setConnectTimeout(5000);
                            conn.setReadTimeout(5000);

                            int code = conn.getResponseCode();
                            if (code == 200) {
                                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                                StringBuilder response = new StringBuilder();
                                String responseLine;
                                while ((responseLine = br.readLine()) != null) {
                                    response.append(responseLine.trim());
                                }
                                br.close();

                                JSONObject jsonResponse = new JSONObject(response.toString());
                                final String status = jsonResponse.optString("status", "");
                                // Replaced reading "result" and "error" with "response"
                                final String apiResponse = jsonResponse.optString("response", "");

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if ("completed".equalsIgnoreCase(status) || "success".equalsIgnoreCase(status)) {
                                            botTextView.setText(apiResponse.isEmpty() ? "Processing completed empty-handed." : apiResponse);
                                            enableInputs();
                                        } else if ("failed".equalsIgnoreCase(status) || "error".equalsIgnoreCase(status)) {
                                            botTextView.setText("Error: " + (apiResponse.isEmpty() ? "Processing job failed on server" : apiResponse));
                                            enableInputs();
                                        } else {
                                            botTextView.setText("Qwen AI is thinking... (Status: " + status + ")");
                                            pollingHandler.postDelayed(pollingRunnable, 2000);
                                        }
                                    }
                                });
                            } else {
                                throw new Exception("HTTP Status code " + code);
                            }
                        } catch (final Exception e) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    botTextView.setText("Connection error during generation polling: " + e.getMessage());
                                    enableInputs();
                                }
                            });
                        }
                    }
                }).start();
            }
        };

        pollingHandler.postDelayed(pollingRunnable, 2000);
    }

    private void enableInputs() {
        inputMessage.setEnabled(true);
        btnSend.setEnabled(true);
    }

    @Override
    protected void onDestroy() {
        if (pollingHandler != null && pollingRunnable != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
        }
        super.onDestroy();
    }
}