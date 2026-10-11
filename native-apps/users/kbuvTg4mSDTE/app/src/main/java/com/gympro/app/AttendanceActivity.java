package com.gympro.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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

    private TextView tvAttendanceDate;
    private ProgressBar pbAttendance;
    private ListView lvAttendance;

    private BackendApi backendApi;
    private List<String> memberNames;
    private List<String> memberIds;
    private List<String> displayList;
    private ArrayAdapter<String> adapter;
    private String todayDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        backendApi = new BackendApi(this);

        tvAttendanceDate = (TextView) findViewById(R.id.tvAttendanceDate);
        pbAttendance = (ProgressBar) findViewById(R.id.pbAttendance);
        lvAttendance = (ListView) findViewById(R.id.lvAttendance);

        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        tvAttendanceDate.setText("Date: " + todayDate + " (Tap member to mark Present)");

        memberNames = new ArrayList<>();
        memberIds = new ArrayList<>();
        displayList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayList);
        lvAttendance.setAdapter(adapter);

        lvAttendance.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position < memberNames.size()) {
                    markAttendance(memberIds.get(position), memberNames.get(position));
                }
            }
        });

        loadMembersAndAttendance();
    }

    private void loadMembersAndAttendance() {
        pbAttendance.setVisibility(View.VISIBLE);
        backendApi.getCollection("members", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                pbAttendance.setVisibility(View.GONE);
                memberNames.clear();
                memberIds.clear();
                displayList.clear();

                JSONArray records = response.optJSONArray("records");
                if (records != null) {
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject rec = records.optJSONObject(i);
                        if (rec != null) {
                            String id = rec.optString("id", "");
                            JSONObject data = rec.optJSONObject("data");
                            if (data != null) {
                                String name = data.optString("name", "Unknown");
                                memberIds.add(id);
                                memberNames.add(name);
                                displayList.add("Member: " + name + " [ Tap to check in ]");
                            }
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String errorMessage) {
                pbAttendance.setVisibility(View.GONE);
                Toast.makeText(AttendanceActivity.this, "Error loading members", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void markAttendance(String memberId, String memberName) {
        pbAttendance.setVisibility(View.VISIBLE);
        try {
            JSONObject data = new JSONObject();
            data.put("member_id", memberId);
            data.put("member_name", memberName);
            data.put("date", todayDate);
            data.put("status", "Present");

            backendApi.createRecord("attendance", data, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    pbAttendance.setVisibility(View.GONE);
                    Toast.makeText(AttendanceActivity.this, "Attendance marked Present!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String errorMessage) {
                    pbAttendance.setVisibility(View.GONE);
                    Toast.makeText(AttendanceActivity.this, "Failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            pbAttendance.setVisibility(View.GONE);
        }
    }
}