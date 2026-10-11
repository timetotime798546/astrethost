package com.gympro.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttendanceActivity extends Activity {
    private TextView tvDate;
    private ListView lvAttendance;
    private ProgressBar pb;
    private BackendApi api;
    private List<JSONObject> memberList = new ArrayList<>();
    private ArrayAdapter<JSONObject> adapter;
    private String todayDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        api = new BackendApi(this);
        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        tvDate = findViewById(R.id.tvAttendanceDate);
        lvAttendance = findViewById(R.id.lvAttendance);
        pb = findViewById(R.id.pbAttendance);

        tvDate.setText("Today: " + todayDate);

        adapter = new ArrayAdapter<JSONObject>(this, R.layout.activity_attendance, android.R.id.text1, memberList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                LinearLayout layout = new LinearLayout(getContext());
                layout.setOrientation(LinearLayout.HORIZONTAL);
                layout.setPadding(12, 16, 12, 16);

                final JSONObject item = getItem(position);
                if (item != null) {
                    final JSONObject data = item.optJSONObject("data");
                    final String memberId = item.optString("id");
                    final String memberName = data != null ? data.optString("name", "Member") : "Member";

                    TextView nameTv = new TextView(getContext());
                    nameTv.setText(memberName);
                    nameTv.setTextColor(0xFFFFFFFF);
                    nameTv.setTextSize(16);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
                    nameTv.setLayoutParams(lp);

                    Button markBtn = new Button(getContext());
                    markBtn.setText("MARK PRESENT");
                    markBtn.setTextSize(12);
                    markBtn.setBackgroundResource(R.drawable.bg_button_accent);
                    markBtn.setTextColor(0xFF101211);

                    markBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            markAttendance(memberId, memberName);
                        }
                    });

                    layout.addView(nameTv);
                    layout.addView(markBtn);
                }
                return layout;
            }
        };

        lvAttendance.setAdapter(adapter);
        loadMembers();
    }

    private void loadMembers() {
        pb.setVisibility(View.VISIBLE);
        api.getCollection("members", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                pb.setVisibility(View.GONE);
                memberList.clear();
                JSONArray records = response.optJSONArray("records");
                if (records != null) {
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject r = records.optJSONObject(i);
                        if (r != null) {
                            memberList.add(r);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String errorMessage) {
                pb.setVisibility(View.GONE);
                Toast.makeText(AttendanceActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void markAttendance(String memberId, String memberName) {
        try {
            JSONObject data = new JSONObject();
            data.put("member_id", memberId);
            data.put("member_name", memberName);
            data.put("date", todayDate);
            data.put("status", "Present");

            pb.setVisibility(View.VISIBLE);
            api.createRecord("attendance", data, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    pb.setVisibility(View.GONE);
                    Toast.makeText(AttendanceActivity.this, "Attendance marked present!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String errorMessage) {
                    pb.setVisibility(View.GONE);
                    Toast.makeText(AttendanceActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Error building request", Toast.LENGTH_SHORT).show();
        }
    }
}