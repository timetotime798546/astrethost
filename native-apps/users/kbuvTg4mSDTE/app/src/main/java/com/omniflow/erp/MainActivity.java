package com.omniflow.erp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final BackendApi api = new BackendApi(this);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (api.isLoggedIn()) {
                    startActivity(new Intent(MainActivity.this, ERPActivity.class));
                } else {
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                }
                finish();
            }
        }, 1500);
    }
}