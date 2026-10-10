package com.ludoclassic.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class WelcomeActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        Button btnMode2 = (Button) findViewById(R.id.btn_mode_2);
        Button btnMode3 = (Button) findViewById(R.id.btn_mode_3);
        Button btnMode4 = (Button) findViewById(R.id.btn_mode_4);

        btnMode2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGame(2);
            }
        });

        btnMode3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGame(3);
            }
        });

        btnMode4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGame(4);
            }
        });
    }

    private void startGame(int playerCount) {
        Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
        intent.putExtra("player_count", playerCount);
        startActivity(intent);
    }
}