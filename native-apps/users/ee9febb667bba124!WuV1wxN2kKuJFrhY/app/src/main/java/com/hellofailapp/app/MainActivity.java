package com.hellofailapp.app;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // INTENTIONAL SYNTAX ERROR: The user explicitly requested to leave a syntax error so the build fails.
        SYNTAX_ERROR_TRIGGER_FORCED_FAILURE!!!
    }
}