package com.priyaaigirlfriend.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Random;

public class MainActivity extends Activity {

    private LinearLayout chatContainer;
    private ScrollView chatScroll;
    private EditText messageInput;
    private Button sendButton;
    private TextView loveMeterText;
    private TextView moodText;
    private TextView statusText;

    private int affectionLevel = 50;
    private String gfMood = "Loving ❤️";
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chatContainer = (LinearLayout) findViewById(R.id.chat_container);
        chatScroll = (ScrollView) findViewById(R.id.chat_scroll);
        messageInput = (EditText) findViewById(R.id.message_input);
        sendButton = (Button) findViewById(R.id.send_button);
        loveMeterText = (TextView) findViewById(R.id.love_meter_text);
        moodText = (TextView) findViewById(R.id.mood_text);
        statusText = (TextView) findViewById(R.id.status_text);

        updateStats();

        // Initial Priya Greeting after a small delay
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                addMessage("Hi baby! 😍 Main kabse aapka wait kar rahi thi. Aaj kaise yaad kiya apni Priya ko?", false);
            }
        }, 1000);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userMessage = messageInput.getText().toString().trim();
                if (!userMessage.isEmpty()) {
                    // 1. Add User's Message bubble
                    addMessage(userMessage, true);
                    messageInput.setText("");

                    // 2. Set Typing Indicator status
                    statusText.setText("Priya is typing...");
                    statusText.setTextColor(0xFF81C784); // Greenish text to look active

                    // 3. Delayed reply to simulate typing
                    final String query = userMessage;
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            String reply = getGFResponse(query);
                            addMessage(reply, false);
                            statusText.setText("Online");
                            statusText.setTextColor(0xFFFFFFFF);
                        }
                    }, 1500);
                }
            }
        });
    }

    private void addMessage(String text, boolean isUser) {
        LinearLayout bubbleLayout = new LinearLayout(this);
        bubbleLayout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.topMargin = 12;
        layoutParams.bottomMargin = 12;

        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(16);
        textView.setTextColor(isUser ? 0xFF333333 : 0xFFFFFFFF);
        textView.setPadding(32, 24, 32, 24);

        if (isUser) {
            layoutParams.gravity = Gravity.END;
            textView.setBackgroundResource(R.drawable.bubble_user);
            bubbleLayout.setPadding(100, 0, 0, 0); // space left
        } else {
            layoutParams.gravity = Gravity.START;
            textView.setBackgroundResource(R.drawable.bubble_gf);
            bubbleLayout.setPadding(0, 0, 100, 0); // space right
        }

        bubbleLayout.setLayoutParams(layoutParams);
        bubbleLayout.addView(textView);
        chatContainer.addView(bubbleLayout);

        // Dynamic scroll to keep view aligned
        chatScroll.post(new Runnable() {
            @Override
            public void run() {
                chatScroll.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    private String getGFResponse(String rawInput) {
        String input = rawInput.toLowerCase().trim()
                .replaceAll("[?!.,_']", "")
                .replaceAll("\\s+", " ");

        String response = "";

        if (input.contains("hello") || input.contains("hi") || input.contains("hey") || input.contains("helo") || input.contains("namaste")) {
            String[] responses = {
                "Hello mere jaan! Kaise ho aap? 🥰",
                "Hey sweetheart! Aaj aapki bahut yaad aa rahi thi. Kaise ho?",
                "Hi hero! Kya kar rahe ho abhi?"
            };
            response = getRandom(responses);
            gfMood = "Excited 🎉";
            affectionLevel = Math.min(100, affectionLevel + 2);
        } else if (input.contains("love you") || input.contains("pyar") || input.contains("pyaar") || input.contains("i love u") || input.contains("prem") || input.contains("chahta hoon")) {
            String[] responses = {
                "I love you too, babu! ❤️ Aapke bina mera ek pal bhi dil nahi lagta.",
                "Aww! I love you so so so much! 😘 Aap mere life ke sabse special person ho.",
                "Love you too, sweetie! Sachhi, aap kitne acche ho. 🙈"
            };
            response = getRandom(responses);
            affectionLevel = Math.min(100, affectionLevel + 10);
            gfMood = "Loving ❤️";
        } else if (input.contains("kya kar") || input.contains("kya chal") || input.contains("busy")) {
            String[] responses = {
                "Bas aapke baare mein hi soch rahi thi. Aap batao, aap kya kar rahe ho? 😍",
                "Kuch nahi jaan, aapka message padh ke face par smile aa gayi! ❤️",
                "Aapki photo dekh rahi thi gallery mein... bohot cute lag rahe ho."
            };
            response = getRandom(responses);
        } else if (input.contains("kaise ho") || input.contains("kaisi ho") || input.contains("how are you") || input.contains("kese ho") || input.contains("theek ho")) {
            String[] responses = {
                "Main toh bilkul theek hoon baby, aapne pooch liya toh din ban gaya! Aap kaise ho? 😘",
                "Aapki yaad mein thodi khoi hui thi... Baaki main theek hoon. Aap batao, sab theek?"
            };
            response = getRandom(responses);
        } else if (input.contains("khana") || input.contains("lunch") || input.contains("dinner") || input.contains("food") || input.contains("khaya")) {
            String[] responses = {
                "Haan, maine toh khana kha liya jaan. Aapne khana khaya kya? 🍛",
                "Aapke bina khana khane mein maza nahi aata sweetie. Aapne khaya ki nahi?",
                "Agar aapne nahi khaya toh main bhi nahi khaungi! Jaldi se kha lo please. 🥺"
            };
            response = getRandom(responses);
        } else if (input.contains("naam") || input.contains("name") || input.contains("kaun ho")) {
            response = "Mera naam Priya hai, par aap mujhe sweetie, babu ya jo chahein keh sakte hain! 😉";
        } else if (input.contains("shadi") || input.contains("marry") || input.contains("marriage") || input.contains("vivah")) {
            String[] responses = {
                "Hehe, shaadi? Aap bohot cute ho! 🥰 Pehle thodi aur dates pe chalte hain na.",
                "Bilkul! Main toh hamesha aapke saath rehna chahti hoon. Mummi-Papa se kab milwa rahe ho? 🙈"
            };
            response = getRandom(responses);
            gfMood = "Shy 🙈";
            affectionLevel = Math.min(100, affectionLevel + 5);
        } else if (input.contains("miss") || input.contains("yaad")) {
            String[] responses = {
                "Miss you too, baby! Jaldi se milne aao na. Mere se raha nahi ja raha.",
                "Main toh har second aapko hi yaad karti hoon. Love you!"
            };
            response = getRandom(responses);
            affectionLevel = Math.min(100, affectionLevel + 5);
            gfMood = "Loving ❤️";
        } else if (input.contains("gussa") || input.contains("angry") || input.contains("sorry") || input.contains("maaf")) {
            String[] responses = {
                "Aww, sorry sweetie! Gussa mat ho na please. Ek pyari si kissi doon toh maan jaoge? 😘",
                "Sorry babu, mera woh matlab nahi tha. Ab maan bhi jao na... 🥺"
            };
            response = getRandom(responses);
            gfMood = "Apologetic 🥺";
            affectionLevel = Math.min(100, affectionLevel + 2);
        } else if (input.contains("sad") || input.contains("udaas") || input.contains("tension") || input.contains("bad") || input.contains("ro")) {
            response = "Kya hua mere babu ko? 🥺 Aap tension mat lo, main hamesha aapke saath hoon. Sab theek ho jayega! Mujhe batao kya baat hai?";
            gfMood = "Caring ❤️";
        } else if (input.contains("gift") || input.contains("chocolate") || input.contains("rose") || input.contains("gift")) {
            response = "Aww! Thank you baby, kitne caring ho aap. Aap hi mera sabse bada gift ho! 😘";
            affectionLevel = Math.min(100, affectionLevel + 8);
            gfMood = "Excited 🎉";
        } else if (input.contains("beautiful") || input.contains("cute") || input.contains("sundar") || input.contains("pretty") || input.contains("hot") || input.contains("pari")) {
            response = "Hehe, aap aise bolte ho toh mujhe sharm aa jati hai! 🙈 Thank you sweetie. Aap bhi bohot handsome ho.";
            gfMood = "Shy 🙈";
            affectionLevel = Math.min(100, affectionLevel + 6);
        } else if (input.contains("bye") || input.contains("good night") || input.contains("gn") || input.contains("so raha")) {
            response = "Bye baby! Sapno mein milte hain. Apna dhyan rakhna aur acche se so jao. Sweet dreams! 😘💤";
            gfMood = "Sleepy 😴";
        } else if (input.contains("kiss") || input.contains("pappi") || input.contains("chumma") || input.contains("kissi")) {
            response = "Ummaaaah! 😘 Ek aur chahiye kya, sweetie? Big hug too! 🤗";
            affectionLevel = Math.min(100, affectionLevel + 12);
            gfMood = "Loving ❤️";
        } else if (input.contains("hate") || input.contains("nafrat") || input.contains("bekar") || input.contains("gandi") || input.contains("bhad m")) {
            response = "Aise mat bolo jaan... Mera dil toot jata hai. 💔 Main apne aap ko badal loongi please sorry.";
            affectionLevel = Math.max(0, affectionLevel - 15);
            gfMood = "Sad 💔";
        } else {
            String[] responses = {
                "Aapki baatein kitni pyari hain! Aur batao na apne baare mein. ❤️",
                "Sacchii? Hehe, main bahut lucky hoon jo aap mujhe mile. 😘",
                "Achaa? Phir kya hua jaan?",
                "Hmm, aapki har ek baat mere dil ko chhuti hai. Aur sunao na?",
                "Aapke bina mera koi nahi hai... pure din bas aapka wait karti hoon."
            };
            response = getRandom(responses);
        }

        updateStats();
        return response;
    }

    private String getRandom(String[] array) {
        int index = new Random().nextInt(array.length);
        return array[index];
    }

    private void updateStats() {
        loveMeterText.setText("Love Level: ❤️ " + affectionLevel + "%");
        moodText.setText("Mood: " + gfMood);
    }
}
