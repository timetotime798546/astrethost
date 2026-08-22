package com.multiutilitysuite.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;

public class SensorExplorerActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout parent = new LinearLayout(this);
        parent.setOrientation(LinearLayout.VERTICAL);
        parent.setBackgroundColor(Color.parseColor("#F5F5F5"));

        // Custom Header Bar
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setBackgroundColor(Color.parseColor("#3F51B5"));
        header.setPadding(32, 24, 32, 24);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button backButton = new Button(this);
        backButton.setText("< Back");
        backButton.setTextColor(Color.WHITE);
        backButton.setBackgroundColor(Color.TRANSPARENT);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        header.addView(backButton);

        TextView headerTitle = new TextView(this);
        headerTitle.setText("Sensor Explorer");
        headerTitle.setTextColor(Color.WHITE);
        headerTitle.setTextSize(20);
        headerTitle.setPadding(32, 0, 0, 0);
        header.addView(headerTitle);
        parent.addView(header);

        // Scrollable Sensor List Container
        ScrollView scrollView = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        scrollView.setLayoutParams(scrollParams);

        LinearLayout contentList = new LinearLayout(this);
        contentList.setOrientation(LinearLayout.VERTICAL);
        contentList.setPadding(32, 32, 32, 32);

        SensorManager sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sm != null) {
            List<Sensor> list = sm.getSensorList(Sensor.TYPE_ALL);
            for (int i = 0; i < list.size(); i++) {
                Sensor s = list.get(i);
                
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.VERTICAL);
                row.setBackgroundColor(Color.WHITE);
                row.setPadding(24, 24, 24, 24);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.setMargins(0, 8, 0, 8);
                row.setLayoutParams(rowParams);

                TextView sName = new TextView(this);
                sName.setText((i + 1) + ". " + s.getName());
                sName.setTextSize(16);
                sName.setTextColor(Color.parseColor("#3F51B5"));
                sName.setGravity(Gravity.START);
                row.addView(sName);

                TextView sMeta = new TextView(this);
                sMeta.setText("Vendor: " + s.getVendor() + "\nType ID: " + s.getType());
                sMeta.setTextSize(12);
                sMeta.setPadding(0, 8, 0, 0);
                sMeta.setTextColor(Color.parseColor("#757575"));
                row.addView(sMeta);

                contentList.addView(row);
            }
        } else {
            TextView error = new TextView(this);
            error.setText("Sensor Services are unreachable on this build platform.");
            error.setTextSize(16);
            contentList.addView(error);
        }

        scrollView.addView(contentList);
        parent.addView(scrollView);
        setContentView(parent);
    }
}