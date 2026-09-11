package com.howareyou.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private Button btnAmazing;
    private Button btnGood;
    private Button btnTired;
    private Button btnSad;
    private Button btnReset;
    
    private LinearLayout responseCard;
    private TextView responseTitle;
    private TextView responseQuote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind interactive views
        btnAmazing = (Button) findViewById(R.id.btnAmazing);
        btnGood = (Button) findViewById(R.id.btnGood);
        btnTired = (Button) findViewById(R.id.btnTired);
        btnSad = (Button) findViewById(R.id.btnSad);
        btnReset = (Button) findViewById(R.id.btnReset);

        responseCard = (LinearLayout) findViewById(R.id.responseCard);
        responseTitle = (TextView) findViewById(R.id.responseTitle);
        responseQuote = (TextView) findViewById(R.id.responseQuote);

        // Assign interactions with traditional Java 8 compatible anonymous classes
        btnAmazing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showResponse(
                    "That's absolutely incredible! 🚀",
                    "Your energy is wonderful! Keep shining bright and sharing that positive vibe with the world around you today.",
                    "#E8F5E9", 
                    "#2E7D32"  
                );
            }
        });

        btnGood.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showResponse(
                    "Happy to hear! 😊",
                    "A good, steady day is beautiful. Keep focusing on the small happinesses and keep smiling!",
                    "#E3F2FD", 
                    "#1565C0"  
                );
            }
        });

        btnTired.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showResponse(
                    "Take some rest! 🥱",
                    "You've been working hard. Remember it is absolutely okay to pause, take a deep breath, and rest your mind.",
                    "#FFF3E0", 
                    "#EF6C00"  
                );
            }
        });

        btnSad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showResponse(
                    "Sending you warmth ❤️",
                    "It's completely okay to not feel 100% right now. Be gentle with yourself. Clear skies always follow the rain.",
                    "#FFEBEE", 
                    "#C62828"  
                );
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetStatus();
            }
        });
    }

    private void showResponse(String title, String message, String bgColorHex, String textColorHex) {
        responseCard.setVisibility(View.VISIBLE);
        responseCard.setBackgroundColor(Color.parseColor(bgColorHex));
        
        responseTitle.setText(title);
        responseTitle.setTextColor(Color.parseColor(textColorHex));
        
        responseQuote.setText(message);
        
        btnReset.setVisibility(View.VISIBLE);
    }

    private void resetStatus() {
        responseCard.setVisibility(View.GONE);
        btnReset.setVisibility(View.GONE);
    }
}