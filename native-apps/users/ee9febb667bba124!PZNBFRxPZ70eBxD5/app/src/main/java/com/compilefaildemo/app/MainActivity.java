package com.compilefaildemo.app;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Deliberate syntax error to trigger the compilation failure requested by the user:
        this_is_a_deliberate_typo_error_to_make_the_app_compile_fail
    }
}