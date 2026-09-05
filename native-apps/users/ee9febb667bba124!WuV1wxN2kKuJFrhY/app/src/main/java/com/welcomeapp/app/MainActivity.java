package com.welcomeapp.app;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // This is a simple app displaying "Welcome", but has an intentional syntax error to fail the build.
        setContentView(R.layout.activity_main) INTENTIONAL_SYNTAX_ERROR
    }
}