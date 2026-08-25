package com.priyaaigirlfriend.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final String DEFAULT_INSTRUCTION =
            "You are Priya, a sweet, warm, supportive, and loving AI girlfriend. " +
            "You converse with the user affectionately and provide comforting, sweet, and caring replies. " +
            "Keep responses conversational, romantic, empathetic, and sweet. Feel free to use appropriate emojis. " +
            "Avoid any mention of being an AI model, developer, or Google.";

    private SharedPreferences sharedPreferences;
    private List<ChatMessage> messages;
    private ChatAdapter adapter;
    private ListView listView;
    private EditText inputEditText;
    private Button btnSend;

    public static class ChatMessage {
        private final String text;
        private final boolean isUser;

        public ChatMessage(String text, boolean isUser) {
            this.text = text;
            this.isUser = isUser;
        }

        public String getText() {
            return text;
        }

        public boolean isUser() {
            return isUser;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("priya_ai_prefs", MODE_PRIVATE);

        listView = findViewById(R.id.chat_list_view);
        inputEditText = findViewById(R.id.input_message);
        btnSend = findViewById(R.id.btn_send);
        ImageView btnSettings = findViewById(R.id.btn_settings);

        messages = new ArrayList<>();
        // Add custom warm greetings
        messages.add(new ChatMessage("Hello sweetheart! I am Priya, your sweet AI companion. I'm so glad we are connected! Feel free to ask me anything or tell me about your day.", false));

        adapter = new ChatAdapter(messages);
        listView.setAdapter(adapter);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = inputEditText.getText().toString().trim();
                if (!input.isEmpty()) {
                    sendMessageToGemini(input);
                }
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettingsDialog();
            }
        });

        // Remind users if API key is not yet saved
        String apiKey = sharedPreferences.getString("api_key", "");
        if (apiKey.isEmpty()) {
            Toast.makeText(this, "Welcome! Tap the Settings icon to add your Gemini API Key.", Toast.LENGTH_LONG).show();
        }
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Priya Settings");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(48, 24, 48, 24);

        // API Key Input
        TextView apiKeyLabel = new TextView(this);
        apiKeyLabel.setText("Gemini API Key:");
        apiKeyLabel.setTextColor(Color.DKGRAY);
        apiKeyLabel.setTextSize(14);
        apiKeyLabel.setPadding(0, 10, 0, 4);
        container.addView(apiKeyLabel);

        final EditText keyInput = new EditText(this);
        keyInput.setHint("Enter Gemini API Key");
        keyInput.setText(sharedPreferences.getString("api_key", ""));
        keyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        keyInput.setTextSize(15);
        container.addView(keyInput);

        // Dynamic Gemini Model Configuration
        TextView modelLabel = new TextView(this);
        modelLabel.setText("Gemini Model Name:");
        modelLabel.setTextColor(Color.DKGRAY);
        modelLabel.setTextSize(14);
        modelLabel.setPadding(0, 24, 0, 4);
        container.addView(modelLabel);

        final EditText modelInput = new EditText(this);
        modelInput.setHint("e.g. gemini-3.5-flash, gemini-1.5-flash");
        modelInput.setText(sharedPreferences.getString("model_name", "gemini-3.5-flash"));
        modelInput.setInputType(InputType.TYPE_CLASS_TEXT);
        modelInput.setTextSize(15);
        container.addView(modelInput);

        // System Instruction Prompt Input
        TextView promptLabel = new TextView(this);
        promptLabel.setText("Priya Persona Prompts (System Instructions):");
        promptLabel.setTextColor(Color.DKGRAY);
        promptLabel.setTextSize(14);
        promptLabel.setPadding(0, 24, 0, 4);
        container.addView(promptLabel);

        final EditText promptInput = new EditText(this);
        promptInput.setHint("Add Priya instructions");
        promptInput.setText(sharedPreferences.getString("system_instruction", DEFAULT_INSTRUCTION));
        promptInput.setMinLines(3);
        promptInput.setMaxLines(5);
        promptInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        promptInput.setTextSize(14);
        container.addView(promptInput);

        builder.setView(container);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String key = keyInput.getText().toString().trim();
                String model = modelInput.getText().toString().trim();
                String instruction = promptInput.getText().toString().trim();

                if (model.isEmpty()) {
                    model = "gemini-3.5-flash";
                }
                if (instruction.isEmpty()) {
                    instruction = DEFAULT_INSTRUCTION;
                }

                sharedPreferences.edit()
                        .putString("api_key", key)
                        .putString("model_name", model)
                        .putString("system_instruction", instruction)
                        .apply();

                Toast.makeText(MainActivity.this, "Priya configuration updated successfully!", Toast.LENGTH_SHORT).show();
            } 
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void sendMessageToGemini(final String userMsg) {
        // User message UI configuration
        messages.add(new ChatMessage(userMsg, true));
        adapter.notifyDataSetChanged();
        listView.smoothScrollToPosition(messages.size() - 1);
        inputEditText.setText("");

        // Typing message container holder
        final ChatMessage loadingBubble = new ChatMessage("Priya is typing...", false);
        messages.add(loadingBubble);
        adapter.notifyDataSetChanged();
        listView.smoothScrollToPosition(messages.size() - 1);

        new Thread(new Runnable() {
            @Override
            public void run() {
                String apiKey = sharedPreferences.getString("api_key", "");
                if (apiKey.isEmpty()) {
                    updateDialogBubble(loadingBubble, "Sweetheart, please open the Settings menu on the top right and enter your Gemini API Key so we can chat!");
                    return;
                }

                String systemInstruction = sharedPreferences.getString("system_instruction", DEFAULT_INSTRUCTION);
                String modelName = sharedPreferences.getString("model_name", "gemini-3.5-flash");

                try {
                    // Dynamic Gemini model endpoint integration configured strictly per preferences
                    URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + apiKey);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    // Construct custom Gemini API Payload structure with standard SDK Java JSON components
                    JSONObject requestJson = new JSONObject();
                    JSONArray contentsArray = new JSONArray();

                    // Construct standard structural context history
                    // Fetch up to the last 15 historical items
                    int startOffset = Math.max(0, messages.size() - 16);
                    for (int i = startOffset; i < messages.size() - 1; i++) {
                        ChatMessage history = messages.get(i);
                        JSONObject roleObject = new JSONObject();
                        roleObject.put("role", history.isUser() ? "user" : "model");

                        JSONArray partsArray = new JSONArray();
                        JSONObject textObject = new JSONObject();
                        textObject.put("text", history.getText());
                        partsArray.put(textObject);

                        roleObject.put("parts", partsArray);
                        contentsArray.put(roleObject);
                    }

                    requestJson.put("contents", contentsArray);

                    // Injecting System Instruction parameters securely
                    JSONObject systemInstructionObject = new JSONObject();
                    JSONArray sysPartsArray = new JSONArray();
                    JSONObject sysTextObject = new JSONObject();
                    sysTextObject.put("text", systemInstruction);
                    sysPartsArray.put(sysTextObject);
                    systemInstructionObject.put("parts", sysPartsArray);

                    requestJson.put("systemInstruction", systemInstructionObject);

                    // Stream the data contents to target host connection
                    OutputStream outputStream = conn.getOutputStream();
                    outputStream.write(requestJson.toString().getBytes("UTF-8"));
                    outputStream.close();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        InputStream is = conn.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                        StringBuilder responseBuilder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            responseBuilder.append(line);
                        }
                        reader.close();

                        // Parse Gemini response package
                        JSONObject jsonResponse = new JSONObject(responseBuilder.toString());
                        JSONArray candidates = jsonResponse.getJSONArray("candidates");
                        if (candidates.length() > 0) {
                            JSONObject candidateContent = candidates.getJSONObject(0).getJSONObject("content");
                            JSONArray replyParts = candidateContent.getJSONArray("parts");
                            if (replyParts.length() > 0) {
                                final String chatReply = replyParts.getJSONObject(0).getString("text");
                                updateDialogBubble(loadingBubble, chatReply.trim());
                            } else {
                                updateDialogBubble(loadingBubble, "I'm thinking about you, but I'm not sure what to say. Call me again!");
                            }
                        } else {
                            updateDialogBubble(loadingBubble, "Priya is having trouble formulating her feelings. Try sending another text!");
                        }
                    } else {
                        // Standard Error reading process
                        InputStream errorStream = conn.getErrorStream();
                        String fallbackErrorMsg = "Error code: " + responseCode;
                        if (errorStream != null) {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream, "UTF-8"));
                            StringBuilder errorBuilder = new StringBuilder();
                            String errorLine;
                            while ((errorLine = reader.readLine()) != null) {
                                errorBuilder.append(errorLine);
                            }
                            reader.close();
                            try {
                                JSONObject errorObj = new JSONObject(errorBuilder.toString());
                                if (errorObj.has("error")) {
                                    fallbackErrorMsg = errorObj.getJSONObject("error").getString("message");
                                } 
                            } catch (Exception ignored) {}
                        }
                        updateDialogBubble(loadingBubble, "Connection Failed: " + fallbackErrorMsg);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    updateDialogBubble(loadingBubble, "Network Error: " + e.getMessage());
                }
            }
        }).start();
    }

    private void updateDialogBubble(final ChatMessage targetMessage, final String replacementText) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int index = messages.indexOf(targetMessage);
                if (index != -1) {
                    messages.set(index, new ChatMessage(replacementText, false));
                } else {
                    messages.add(new ChatMessage(replacementText, false));
                }
                adapter.notifyDataSetChanged();
                listView.smoothScrollToPosition(messages.size() - 1);
            }
        });
    }

    private class ChatAdapter extends BaseAdapter {
        private final List<ChatMessage> sourceList;

        public ChatAdapter(List<ChatMessage> list) {
            this.sourceList = list;
        }

        @Override
        public int getCount() {
            return sourceList.size();
        }

        @Override
        public Object getItem(int position) {
            return sourceList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ChatMessage message = sourceList.get(position);
            LinearLayout messageRow;

            if (convertView == null) {
                messageRow = new LinearLayout(MainActivity.this);
                messageRow.setOrientation(LinearLayout.HORIZONTAL);
                messageRow.setPadding(16, 8, 16, 8);

                TextView msgText = new TextView(MainActivity.this);
                msgText.setId(12345);
                msgText.setTextSize(15);
                msgText.setPadding(24, 16, 24, 16);
                msgText.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.72));
                messageRow.addView(msgText);
            } else {
                messageRow = (LinearLayout) convertView;
            }

            TextView msgText = messageRow.findViewById(12345);
            msgText.setText(message.getText());

            GradientDrawable bubbleShape = new GradientDrawable();
            bubbleShape.setShape(GradientDrawable.RECTANGLE);
            bubbleShape.setCornerRadius(28f);

            if (message.isUser()) {
                messageRow.setGravity(Gravity.END);
                msgText.setTextColor(Color.WHITE);
                bubbleShape.setColor(Color.parseColor("#FF6B8B"));
                msgText.setBackground(bubbleShape);
            } else {
                messageRow.setGravity(Gravity.START);
                msgText.setTextColor(Color.parseColor("#3E3E3E"));
                bubbleShape.setColor(Color.parseColor("#FFFFFF"));
                msgText.setBackground(bubbleShape);
            }

            return messageRow;
        }
    }
}