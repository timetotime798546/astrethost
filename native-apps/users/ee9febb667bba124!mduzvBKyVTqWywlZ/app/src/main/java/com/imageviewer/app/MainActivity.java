package com.imageviewer.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Put application into strict full screen mode
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        ImageView imageView = (ImageView) findViewById(R.id.img_display);
        TextView txtStatus = (TextView) findViewById(R.id.txt_status);

        // Safely check and dynamically load the target resource
        int mainResId = getResources().getIdentifier("asset_6a9f20fb943f9", "drawable", getPackageName());
        
        if (mainResId != 0) {
            try {
                imageView.setImageResource(mainResId);
                if (txtStatus != null) {
                    txtStatus.setText("Successfully Loaded asset_6a9f20fb943f9");
                }
            } catch (Exception e) {
                if (txtStatus != null) {
                    txtStatus.setText("Error loading showcase asset");
                }
            }
        } else {
            // Safe fallback scan to preserve display if build order fluctuates
            String[] fallbackDrawables = {
                "asset_6a9f18319c82d",
                "asset_6a9f1839db93c",
                "asset_6a9efd3f547c3"
            };
            boolean fallbackFound = false;
            for (int i = 0; i < fallbackDrawables.length; i++) {
                int altId = getResources().getIdentifier(fallbackDrawables[i], "drawable", getPackageName());
                if (altId != 0) {
                    try {
                        imageView.setImageResource(altId);
                        if (txtStatus != null) {
                            txtStatus.setText("Fallback: " + fallbackDrawables[i]);
                        }
                        fallbackFound = true;
                        break;
                    } catch (Exception ignored) {}
                }
            }
            if (!fallbackFound && txtStatus != null) {
                txtStatus.setText("Awaiting primary image asset deployment");
            }
        }

        Toast.makeText(this, "Showcase active", Toast.LENGTH_SHORT).show();
    }
}