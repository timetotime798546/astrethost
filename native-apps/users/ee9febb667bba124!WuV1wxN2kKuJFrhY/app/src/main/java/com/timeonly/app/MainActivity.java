package com.timeonly.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView timeTextView;
    private Handler handler = new Handler();
    private Runnable timeUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timeTextView = (TextView) findViewById(R.id.timeTextView);

        timeUpdater = new Runnable() {
            @Override
            public void run() {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                String currentTime = sdf.format(new Date());
                timeTextView.setText(currentTime);
                handler.postDelayed(this, 1000);
            }
        };

        // Start updating time
        handler.post(timeUpdater);

        // Deliberate System Error requested by user.
        // This throws a java.lang.Error (System Error) so that the application crashes immediately when executed,
        // while allowing the code to compile flawlessly during the Gradle assembleDebug build phase.
        if (true) {
            throw new java.lang.Error("Deliberate System Error requested by user");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && timeUpdater != null) {
            handler.removeCallbacks(timeUpdater);
        }
    }
}