package com.tukonhaibhai.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Random;

public class MainActivity extends Activity {

    private TextView tvMainText;
    private Button btnFun;
    private Button btnShare;
    private View rootLayout;

    private final String[] alternatives = {
        "tu kon hai bhai?",
        "TU KON HAI BHAI?!",
        "tu kon hai bhai... 🤔",
        "Bhai, tu sach me kon hai?",
        "Who are you, bro? 😎",
        "Arey, tu kon hai bhai! 😂"
    };

    private final int[] colors = {
        0xFFF5F5F5, // Light Grey
        0xFFE3F2FD, // Light Blue
        0xFFE8F5E9, // Light Green
        0xFFFFF3E0, // Light Orange
        0xFFF3E5F5, // Light Purple
        0xFFFFFDE7  // Light Yellow
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rootLayout = findViewById(R.id.rootLayout);
        tvMainText = (TextView) findViewById(R.id.tvMainText);
        btnFun = (Button) findViewById(R.id.btnFun);
        btnShare = (Button) findViewById(R.id.btnShare);

        // Dynamic change on click with smooth animation
        btnFun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Random random = new Random();
                String randomText = alternatives[random.nextInt(alternatives.length)];
                int randomColor = colors[random.nextInt(colors.length)];

                AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
                fadeOut.setDuration(150);
                final String textToSet = randomText;
                final int colorToSet = randomColor;

                fadeOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        tvMainText.setText(textToSet);
                        rootLayout.setBackgroundColor(colorToSet);
                        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
                        fadeIn.setDuration(150);
                        tvMainText.startAnimation(fadeIn);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
                tvMainText.startAnimation(fadeOut);
            }
        });

        // Intent share logic
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phrase = tvMainText.getText().toString();
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, phrase + " - Shared via Tu Kon Hai Bhai App!");
                sendIntent.setType("text/plain");

                Intent shareIntent = Intent.createChooser(sendIntent, "Share with your brother");
                startActivity(shareIntent);
            }
        });

        // Long press response trigger
        tvMainText.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(MainActivity.this, "Haan bhai, main hi hoon!", Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }
}