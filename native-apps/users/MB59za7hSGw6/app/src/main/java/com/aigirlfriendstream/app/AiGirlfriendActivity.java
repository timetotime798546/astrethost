package com.aigirlfriendstream.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AiGirlfriendActivity extends Activity {

    private ListView lvChatMessages;
    private EditText etMessageInput;
    private HinglishAiEngine aiEngine;
    private BackendApi backendApi;
    private ChatAdapter chatAdapter;
    private final List<ChatMessage> messageList = new ArrayList<>();
    private final Handler handler = new Handler();

    private static class ChatMessage {
        String sender;
        String message;
        boolean isUser;

        ChatMessage(String sender, String message, boolean isUser) {
            this.sender = sender;
            this.message = message;
            this.isUser = isUser;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_girlfriend);

        lvChatMessages = (ListView) findViewById(R.id.lvChatMessages);
        etMessageInput = (EditText) findViewById(R.id.etMessageInput);
        Button btnSend = (Button) findViewById(R.id.btnSend);

        aiEngine = new HinglishAiEngine();
        backendApi = new BackendApi(this);

        chatAdapter = new ChatAdapter();
        lvChatMessages.setAdapter(chatAdapter);

        // Initial Greeting
        addMessage("Riya", aiEngine.getRandomGreeting(), false);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
    }

    private void sendMessage() {
        final String text = etMessageInput.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }

        addMessage("You", text, true);
        etMessageInput.setText("");

        saveMessageToBackend("You", text);

        // Simulate typing delay for realistic AI girlfriend response
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String aiReply = aiEngine.generateResponse(text);
                addMessage("Riya ❤️", aiReply, false);
                saveMessageToBackend("Riya", aiReply);
            }
        }, 1200);
    }

    private void addMessage(String sender, String text, boolean isUser) {
        messageList.add(new ChatMessage(sender, text, isUser));
        chatAdapter.notifyDataSetChanged();
        lvChatMessages.setSelection(chatAdapter.getCount() - 1);
    }

    private void saveMessageToBackend(String sender, String message) {
        try {
            JSONObject data = new JSONObject();
            data.put("sender", sender);
            data.put("message", message);
            data.put("timestamp", String.valueOf(System.currentTimeMillis()));
            data.put("mode", "Hinglish_AI");

            backendApi.createRecord("chat_messages", data, new BackendApi.Callback() {
                @Override
                public void onSuccess(String response) {
                    // Sync success
                }

                @Override
                public void onError(String error) {
                    // Non-blocking background log
                }
            });
        } catch (Exception e) {
            // Ignore format exceptions
        }
    }

    private class ChatAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return messageList.size();
        }

        @Override
        public Object getItem(int position) {
            return messageList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(AiGirlfriendActivity.this)
                        .inflate(R.layout.item_chat_message, parent, false);
            }

            ChatMessage msg = messageList.get(position);
            LinearLayout bubbleContainer = (LinearLayout) convertView.findViewById(R.id.bubbleContainer);
            TextView tvSender = (TextView) convertView.findViewById(R.id.tvSender);
            TextView tvMessageText = (TextView) convertView.findViewById(R.id.tvMessageText);

            tvSender.setText(msg.sender);
            tvMessageText.setText(msg.message);

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) bubbleContainer.getLayoutParams();

            if (msg.isUser) {
                params.gravity = android.view.Gravity.END;
                bubbleContainer.setBackgroundColor(0xFF3F51B5);
                tvSender.setTextColor(0xFF80D8FF);
            } else {
                params.gravity = android.view.Gravity.START;
                bubbleContainer.setBackgroundColor(0xFF2C1E2A);
                tvSender.setTextColor(0xFFFF80AB);
            }
            bubbleContainer.setLayoutParams(params);

            return convertView;
        }
    }
}