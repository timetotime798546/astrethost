package com.gympro.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MembersActivity extends Activity {

    private EditText etMemberName;
    private EditText etMemberPhone;
    private EditText etMemberPlan;
    private EditText etMemberFee;
    private Button btnAddMember;
    private ProgressBar pbMembers;
    private ListView lvMembers;

    private BackendApi backendApi;
    private List<String> memberListItems;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_members);

        backendApi = new BackendApi(this);

        etMemberName = (EditText) findViewById(R.id.etMemberName);
        etMemberPhone = (EditText) findViewById(R.id.etMemberPhone);
        etMemberPlan = (EditText) findViewById(R.id.etMemberPlan);
        etMemberFee = (EditText) findViewById(R.id.etMemberFee);
        btnAddMember = (Button) findViewById(R.id.btnAddMember);
        pbMembers = (ProgressBar) findViewById(R.id.pbMembers);
        lvMembers = (ListView) findViewById(R.id.lvMembers);

        memberListItems = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, memberListItems);
        lvMembers.setAdapter(adapter);

        btnAddMember.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addMember();
            }
        });

        loadMembers();
    }

    private void loadMembers() {
        pbMembers.setVisibility(View.VISIBLE);
        backendApi.getCollection("members", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                pbMembers.setVisibility(View.GONE);
                memberListItems.clear();
                JSONArray records = response.optJSONArray("records");
                if (records != null) {
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject rec = records.optJSONObject(i);
                        if (rec != null) {
                            JSONObject data = rec.optJSONObject("data");
                            if (data != null) {
                                String name = data.optString("name", "Unknown");
                                String phone = data.optString("phone", "-");
                                String plan = data.optString("plan", "Standard");
                                double fee = data.optDouble("fee", 0.0);
                                memberListItems.add(name + " (" + plan + " - $" + fee + ")\nPhone: " + phone);
                            }
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String errorMessage) {
                pbMembers.setVisibility(View.GONE);
                Toast.makeText(MembersActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addMember() {
        String name = etMemberName.getText().toString().trim();
        String phone = etMemberPhone.getText().toString().trim();
        String plan = etMemberPlan.getText().toString().trim();
        String feeStr = etMemberFee.getText().toString().trim();

        if (name.isEmpty() || feeStr.isEmpty()) {
            Toast.makeText(this, "Name and Fee are required", Toast.LENGTH_SHORT).show();
            return;
        }

        double fee = 0.0;
        try {
            fee = Double.parseDouble(feeStr);
        } catch (Exception e) {
            fee = 0.0;
        }

        pbMembers.setVisibility(View.VISIBLE);
        btnAddMember.setEnabled(false);

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        try {
            JSONObject data = new JSONObject();
            data.put("name", name);
            data.put("phone", phone);
            data.put("email", "");
            data.put("joining_date", today);
            data.put("plan", plan.isEmpty() ? "Monthly" : plan);
            data.put("fee", fee);
            data.put("balance", 0.0);
            data.put("status", "Active");
            data.put("expiry_date", today);
            data.put("notes", "New member");

            backendApi.createRecord("members", data, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    pbMembers.setVisibility(View.GONE);
                    btnAddMember.setEnabled(true);
                    etMemberName.setText("");
                    etMemberPhone.setText("");
                    etMemberPlan.setText("");
                    etMemberFee.setText("");
                    Toast.makeText(MembersActivity.this, "Member saved successfully", Toast.LENGTH_SHORT).show();
                    loadMembers();
                }

                @Override
                public void onError(String errorMessage) {
                    pbMembers.setVisibility(View.GONE);
                    btnAddMember.setEnabled(true);
                    Toast.makeText(MembersActivity.this, "Failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            pbMembers.setVisibility(View.GONE);
            btnAddMember.setEnabled(true);
        }
    }
}