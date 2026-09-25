package com.singlejavaapp.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.view.ViewGroup.LayoutParams;
import android.view.Gravity;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // You can also programmatically create UI elements if needed.
        // For example, adding a TextView:
        /*
        TextView textView = new TextView(this);
        textView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        textView.setText("Hello from MainActivity!");
        textView.setGravity(Gravity.CENTER);
        setContentView(textView);
        */
    }
}