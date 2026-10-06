package com.strictstudymode.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class BlockOverlayActivity extends Activity {

    private TextView txtBlockedAppName;
    private Button btnReturnToStudy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_block_overlay);

        txtBlockedAppName = (TextView) findViewById(R.id.txtBlockedAppName);
        btnReturnToStudy = (Button) findViewById(R.id.btnReturnToStudy);

        String blockedPackage = getIntent().getStringExtra("blocked_package");
        if (blockedPackage != null && !blockedPackage.isEmpty()) {
            PackageManager pm = getPackageManager();
            try {
                String appLabel = pm.getApplicationLabel(pm.getApplicationInfo(blockedPackage, 0)).toString();
                txtBlockedAppName.setText("“" + appLabel + "” is Restricted");
            } catch (Exception e) {
                txtBlockedAppName.setText("Distracting Application Intercepted");
            }
        } else {
            txtBlockedAppName.setText("Restricted App Detected");
        }

        btnReturnToStudy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                minimizeAndGoHome();
            }
        });
    }

    private void minimizeAndGoHome() {
        // Direct target to main focus panel, and also send User back to launcher home so they are cleanly ejected
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Intentionally left blank to enforce strict screen lock
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        // Triggered when home button is pressed, which is fine since they are leaving the entertainment app
    }
}