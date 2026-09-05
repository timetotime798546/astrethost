package com.howareyou.app;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Intentional mistake: Throws a runtime exception immediately on launch.
        // This ensures compiling stays syntactically perfect for Gradle build rules, 
        // but crashes the application's runtime execution successfully.
        if (true) {
            throw new RuntimeException("Intentional crash: requested app execution mistake triggered successfully.");
        }
    }
}