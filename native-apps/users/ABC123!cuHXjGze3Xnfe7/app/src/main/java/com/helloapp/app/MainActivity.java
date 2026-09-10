package com.helloapp.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final TextView helloText = (TextView) findViewById(R.id.helloText);
        Button actionButton = (Button) findViewById(R.id.actionButton);

        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                helloText.setText("Hello, World! Welcome to our AI generated App!");
                Toast.makeText(MainActivity.this, "Hello greeted!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}