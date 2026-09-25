package com.simplejavaapp.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.view.ViewGroup.LayoutParams;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView helloTextView = (TextView) findViewById(R.id.helloTextView);
        helloTextView.setText("Hello from your single Java file!");
    }
}