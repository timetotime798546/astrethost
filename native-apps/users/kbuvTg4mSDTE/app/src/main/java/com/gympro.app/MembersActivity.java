package com.gympro.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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

public class MembersActivity extends Activity {
    private EditText etName, etPhone, etPlan, etFee;
    private Button btnAdd;
    private ListView lvMembers;
    private ProgressBar pb;
    private BackendApi api;
    private List<JSONObject> memberList = new ArrayList<>();
    private ArrayAdapter<JSONObject> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_members);

        api = new BackendApi(this);

        etName = findViewById(R.id.etMemberName);
        etPhone = findViewById(R.id.etMemberPhone);
        etPlan = findViewById(R.id.etMemberPlan);
        etFee = findViewById(R.id.etMemberFee);
        btnAdd = findViewById(R.id.btnAddMember);
        lvMembers = findViewById(R.id.lvMembers);
        pb = findViewById(R.id.pbMembers);

        adapter = new ArrayAdapter<JSONObject>(this, android.R.layout.simple_list_item_2, android.R.id.text1, memberList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);

                JSONObject item = getItem(position);
                if (item != null) {
                    JSONObject data = item.optJSONObject("data");
                    if (data != null) {
                        text1.setText(data.optString("name", "Unknown Member"));
                        text1.setTextColor(0xFFFFFFFF);
                        String sub = "Phone: " + data.optString("phone") + " | Plan: " + data.optString("plan") + " ($" + data.optDouble("fee", 0) + ")";
                        text2.setText(sub);
                        text2.setTextColor(0xFFA4ADA6);
                    }
                }
                return view;
            }
        };

        lvMembers.setAdapter(adapter);

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addMember();
            }
        });

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
                Toast.makeText(MembersActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addMember() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String plan = etPlan.getText().toString().trim();
        String feeStr = etFee.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Name and Phone are required", Toast.LENGTH_SHORT).show();
            return;
        }

        double fee = 0;
        try {
            fee = Double.parseDouble(feeStr);
        } catch (Exception ignored) {}

        String joiningDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        try {
            JSONObject data = new JSONObject();
            data.put("name", name);
            data.put("phone", phone);
            data.put("plan", plan.isEmpty() ? "Standard" : plan);
            data.put("fee", fee);
            data.put("joining_date", joiningDate);
            data.put("status", "Active");

            pb.setVisibility(View.VISIBLE);
            api.createRecord("members", data, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    pb.setVisibility(View.GONE);
                    etName.setText("");
                    etPhone.setText("");
                    etPlan.setText("");
                    etFee.setText("");
                    Toast.makeText(MembersActivity.this, "Member saved successfully", Toast.LENGTH_SHORT).show();
                    loadMembers();
                }

                @Override
                public void onError(String errorMessage) {
                    pb.setVisibility(View.GONE);
                    Toast.makeText(MembersActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Error building request", Toast.LENGTH_SHORT).show();
        }
    }
}