package com.gympro.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private BackendApi backendApi;

    // View Containers
    private View viewDashboard, viewMembers, viewPlans, viewAttendance, viewPayments;

    // Nav Item Click Listeners
    private View navDashboard, navMembers, navPlans, navAttendance, navPayments;
    private TextView tvNavDashboard, tvNavMembers, tvNavPlans, tvNavAttendance, tvNavPayments;

    // Dashboard Metric Views
    private TextView tvTotalMembersVal, tvActiveMembersVal, tvRevenueVal, tvPendingVal, tvTodayAttendanceVal, tvExpiringSoonVal;
    private Button btnQuickAddMember, btnQuickAttendance, btnQuickPayment, btnRefresh, btnLogout;

    // ListView Adapters & Data
    private List<MemberModel> memberList = new ArrayList<MemberModel>();
    private List<PlanModel> planList = new ArrayList<PlanModel>();
    private List<AttendanceModel> attendanceList = new ArrayList<AttendanceModel>();
    private List<PaymentModel> paymentList = new ArrayList<PaymentModel>();

    private ListView lvMembers, lvPlans, lvAttendance, lvPayments;
    private MemberAdapter memberAdapter;
    private PlanAdapter planAdapter;
    private AttendanceAdapter attendanceAdapter;
    private PaymentAdapter paymentAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        backendApi = new BackendApi(this);

        if (!backendApi.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initViews();
        setupNavigation();
        setupAdapters();
        setupActionButtons();

        // Initial Data Fetch
        fetchAllData();
    }

    private void initViews() {
        viewDashboard = findViewById(R.id.viewDashboard);
        viewMembers = findViewById(R.id.viewMembers);
        viewPlans = findViewById(R.id.viewPlans);
        viewAttendance = findViewById(R.id.viewAttendance);
        viewPayments = findViewById(R.id.viewPayments);

        navDashboard = findViewById(R.id.navDashboard);
        navMembers = findViewById(R.id.navMembers);
        navPlans = findViewById(R.id.navPlans);
        navAttendance = findViewById(R.id.navAttendance);
        navPayments = findViewById(R.id.navPayments);

        tvNavDashboard = findViewById(R.id.tvNavDashboard);
        tvNavMembers = findViewById(R.id.tvNavMembers);
        tvNavPlans = findViewById(R.id.tvNavPlans);
        tvNavAttendance = findViewById(R.id.tvNavAttendance);
        tvNavPayments = findViewById(R.id.tvNavPayments);

        tvTotalMembersVal = findViewById(R.id.tvTotalMembersVal);
        tvActiveMembersVal = findViewById(R.id.tvActiveMembersVal);
        tvRevenueVal = findViewById(R.id.tvRevenueVal);
        tvPendingVal = findViewById(R.id.tvPendingVal);
        tvTodayAttendanceVal = findViewById(R.id.tvTodayAttendanceVal);
        tvExpiringSoonVal = findViewById(R.id.tvExpiringSoonVal);

        btnQuickAddMember = findViewById(R.id.btnQuickAddMember);
        btnQuickAttendance = findViewById(R.id.btnQuickAttendance);
        btnQuickPayment = findViewById(R.id.btnQuickPayment);

        btnRefresh = findViewById(R.id.btnRefresh);
        btnLogout = findViewById(R.id.btnLogout);

        lvMembers = findViewById(R.id.lvMembers);
        lvPlans = findViewById(R.id.lvPlans);
        lvAttendance = findViewById(R.id.lvAttendance);
        lvPayments = findViewById(R.id.lvPayments);
    }

    private void setupNavigation() {
        navDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(0);
            }
        });
        navMembers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(1);
            }
        });
        navPlans.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
            }
        });
        navAttendance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(3);
            }
        });
        navPayments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(4);
            }
        });
    }

    private void switchTab(int index) {
        viewDashboard.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        viewMembers.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        viewPlans.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        viewAttendance.setVisibility(index == 3 ? View.VISIBLE : View.GONE);
        viewPayments.setVisibility(index == 4 ? View.VISIBLE : View.GONE);

        int primaryColor = getResources().getColor(R.color.emerald_primary);
        int mutedColor = getResources().getColor(R.color.text_secondary);

        tvNavDashboard.setTextColor(index == 0 ? primaryColor : mutedColor);
        tvNavMembers.setTextColor(index == 1 ? primaryColor : mutedColor);
        tvNavPlans.setTextColor(index == 2 ? primaryColor : mutedColor);
        tvNavAttendance.setTextColor(index == 3 ? primaryColor : mutedColor);
        tvNavPayments.setTextColor(index == 4 ? primaryColor : mutedColor);
    }

    private void setupAdapters() {
        memberAdapter = new MemberAdapter(this, memberList);
        lvMembers.setAdapter(memberAdapter);

        planAdapter = new PlanAdapter(this, planList);
        lvPlans.setAdapter(planAdapter);

        attendanceAdapter = new AttendanceAdapter(this, attendanceList);
        lvAttendance.setAdapter(attendanceAdapter);

        paymentAdapter = new PaymentAdapter(this, paymentList);
        lvPayments.setAdapter(paymentAdapter);
    }

    private void setupActionButtons() {
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchAllData();
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backendApi.logout(new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(String responseStr, JSONObject jsonResponse) {
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        finish();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        finish();
                    }
                });
            }
        });

        btnQuickAddMember.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddMemberDialog();
            }
        });

        btnQuickAttendance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMarkAttendanceDialog();
            }
        });

        btnQuickPayment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddPaymentDialog();
            }
        });

        Button btnAddMember = findViewById(R.id.btnAddMember);
        if (btnAddMember != null) {
            btnAddMember.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAddMemberDialog();
                }
            });
        }

        Button btnAddPlan = findViewById(R.id.btnAddPlan);
        if (btnAddPlan != null) {
            btnAddPlan.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAddPlanDialog();
                }
            });
        }

        Button btnMarkAtt = findViewById(R.id.btnMarkAttendance);
        if (btnMarkAtt != null) {
            btnMarkAtt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showMarkAttendanceDialog();
                }
            });
        }

        Button btnAddPay = findViewById(R.id.btnAddPayment);
        if (btnAddPay != null) {
            btnAddPay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAddPaymentDialog();
                }
            });
        }
    }

    private void fetchAllData() {
        fetchMembers();
        fetchPlans();
        fetchAttendance();
        fetchPayments();
    }

    private void fetchMembers() {
        backendApi.readRecords("members", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String responseStr, JSONObject jsonResponse) {
                memberList.clear();
                if (jsonResponse != null) {
                    JSONArray arr = jsonResponse.optJSONArray("records");
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.optJSONObject(i);
                            MemberModel model = MemberModel.fromRecord(obj);
                            if (model != null) memberList.add(model);
                        }
                    }
                }
                memberAdapter.notifyDataSetChanged();
                recalculateDashboardMetrics();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(MainActivity.this, "Error fetching members: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchPlans() {
        backendApi.readRecords("plans", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String responseStr, JSONObject jsonResponse) {
                planList.clear();
                if (jsonResponse != null) {
                    JSONArray arr = jsonResponse.optJSONArray("records");
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.optJSONObject(i);
                            PlanModel model = PlanModel.fromRecord(obj);
                            if (model != null) planList.add(model);
                        }
                    }
                }
                planAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String errorMessage) {
                // Ignore silent load errors
            }
        });
    }

    private void fetchAttendance() {
        backendApi.readRecords("attendance", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String responseStr, JSONObject jsonResponse) {
                attendanceList.clear();
                if (jsonResponse != null) {
                    JSONArray arr = jsonResponse.optJSONArray("records");
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.optJSONObject(i);
                            AttendanceModel model = AttendanceModel.fromRecord(obj);
                            if (model != null) attendanceList.add(model);
                        }
                    }
                }
                attendanceAdapter.notifyDataSetChanged();
                recalculateDashboardMetrics();
            }

            @Override
            public void onError(String errorMessage) {
                // Ignore silent load errors
            }
        });
    }

    private void fetchPayments() {
        backendApi.readRecords("payments", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String responseStr, JSONObject jsonResponse) {
                paymentList.clear();
                if (jsonResponse != null) {
                    JSONArray arr = jsonResponse.optJSONArray("records");
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.optJSONObject(i);
                            PaymentModel model = PaymentModel.fromRecord(obj);
                            if (model != null) paymentList.add(model);
                        }
                    }
                }
                paymentAdapter.notifyDataSetChanged();
                recalculateDashboardMetrics();
            }

            @Override
            public void onError(String errorMessage) {
                // Ignore silent load errors
            }
        });
    }

    // Mandatory Local Calculation Logic
    private void recalculateDashboardMetrics() {
        int totalMembers = memberList.size();
        int activeCount = 0;
        int expiringCount = 0;

        for (int i = 0; i < memberList.size(); i++) {
            MemberModel m = memberList.get(i);
            if (m.getStatus().equalsIgnoreCase("Active")) {
                activeCount++;
            }
            if (AppUtils.isExpiringSoon(m.getEndDate(), 7)) {
                expiringCount++;
            }
        }

        double totalRevenue = 0.0;
        double totalPending = 0.0;
        for (int i = 0; i < paymentList.size(); i++) {
            PaymentModel p = paymentList.get(i);
            totalRevenue += p.getAmount();
            totalPending += p.getPendingAmount();
        }

        String today = AppUtils.getTodayDate();
        int todayPresent = 0;
        for (int i = 0; i < attendanceList.size(); i++) {
            AttendanceModel a = attendanceList.get(i);
            if (a.getDate().equalsIgnoreCase(today)) {
                todayPresent++;
            }
        }

        tvTotalMembersVal.setText(String.valueOf(totalMembers));
        tvActiveMembersVal.setText(String.valueOf(activeCount));
        tvRevenueVal.setText(AppUtils.formatCurrency(totalRevenue));
        tvPendingVal.setText(AppUtils.formatCurrency(totalPending));
        tvTodayAttendanceVal.setText(todayPresent + " Present today");
        tvExpiringSoonVal.setText(expiringCount + " Expiring Soon");
    }

    // Dialog Methods
    private void showAddMemberDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_member, null);
        builder.setView(dialogView);

        final EditText etName = dialogView.findViewById(R.id.dialogMemName);
        final EditText etPhone = dialogView.findViewById(R.id.dialogMemPhone);
        final EditText etEmail = dialogView.findViewById(R.id.dialogMemEmail);
        final EditText etAge = dialogView.findViewById(R.id.dialogMemAge);
        final Spinner spGender = dialogView.findViewById(R.id.dialogMemGender);
        final Spinner spPlan = dialogView.findViewById(R.id.dialogMemPlan);
        Button btnSave = dialogView.findViewById(R.id.dialogBtnSaveMember);

        // Populate Gender Spinner
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, new String[]{"Male", "Female", "Other"});
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spGender.setAdapter(genderAdapter);

        // Populate Plan Spinner
        List<String> planTitles = new ArrayList<String>();
        if (planList.isEmpty()) {
            planTitles.add("Monthly Plan (1 Month)");
            planTitles.add("Quarterly Plan (3 Months)");
            planTitles.add("Annual Plan (12 Months)");
        } else {
            for (int i = 0; i < planList.size(); i++) {
                planTitles.add(planList.get(i).getTitle() + " (" + planList.get(i).getDurationMonths() + "M)");
            }
        }
        ArrayAdapter<String> pAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, planTitles);
        pAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPlan.setAdapter(pAdapter);

        final AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = etName.getText().toString().trim();
                String phone = etPhone.getText().toString().trim();
                String email = etEmail.getText().toString().trim();
                String ageStr = etAge.getText().toString().trim();

                if (name.isEmpty() || phone.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter member name and phone", Toast.LENGTH_SHORT).show();
                    return;
                }

                int age = ageStr.isEmpty() ? 25 : Integer.parseInt(ageStr);
                String gender = spGender.getSelectedItem().toString();
                String planName = spPlan.getSelectedItem().toString();

                int durationMonths = 1;
                if (planName.contains("3M") || planName.toLowerCase().contains("quarterly")) durationMonths = 3;
                else if (planName.contains("12M") || planName.toLowerCase().contains("annual")) durationMonths = 12;

                String startDate = AppUtils.getTodayDate();
                String endDate = AppUtils.calculateEndDate(startDate, durationMonths);

                MemberModel newMember = new MemberModel("", name, phone, email, age, gender, planName, startDate, endDate, "Active");

                backendApi.createRecord("members", newMember.toJsonObject(), new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(String responseStr, JSONObject jsonResponse) {
                        Toast.makeText(MainActivity.this, "Member added successfully!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        fetchMembers();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(MainActivity.this, "Failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        dialog.show();
    }

    private void showAddPlanDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_plan, null);
        builder.setView(dialogView);

        final EditText etTitle = dialogView.findViewById(R.id.dialogPlanTitle);
        final EditText etMonths = dialogView.findViewById(R.id.dialogPlanMonths);
        final EditText etPrice = dialogView.findViewById(R.id.dialogPlanPrice);
        final EditText etDesc = dialogView.findViewById(R.id.dialogPlanDesc);
        Button btnSave = dialogView.findViewById(R.id.dialogBtnSavePlan);

        final AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = etTitle.getText().toString().trim();
                String monthsStr = etMonths.getText().toString().trim();
                String priceStr = etPrice.getText().toString().trim();
                String desc = etDesc.getText().toString().trim();

                if (title.isEmpty() || monthsStr.isEmpty() || priceStr.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Fill in title, duration and price", Toast.LENGTH_SHORT).show();
                    return;
                }

                int months = Integer.parseInt(monthsStr);
                double price = Double.parseDouble(priceStr);

                PlanModel plan = new PlanModel("", title, months, price, desc);

                backendApi.createRecord("plans", plan.toJsonObject(), new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(String responseStr, JSONObject jsonResponse) {
                        Toast.makeText(MainActivity.this, "Plan created!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        fetchPlans();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(MainActivity.this, "Failed to create plan", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        dialog.show();
    }

    private void showMarkAttendanceDialog() {
        if (memberList.isEmpty()) {
            Toast.makeText(this, "Add members first before marking attendance", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_mark_attendance, null);
        builder.setView(dialogView);

        final Spinner spMembers = dialogView.findViewById(R.id.dialogAttMemberSpinner);
        Button btnSave = dialogView.findViewById(R.id.dialogBtnSaveAttendance);

        List<String> memberNames = new ArrayList<String>();
        for (int i = 0; i < memberList.size(); i++) {
            memberNames.add(memberList.get(i).getName());
        }

        ArrayAdapter<String> mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, memberNames);
        mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMembers.setAdapter(mAdapter);

        final AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selectedIdx = spMembers.getSelectedItemPosition();
                if (selectedIdx < 0 || selectedIdx >= memberList.size()) return;

                MemberModel selectedMember = memberList.get(selectedIdx);
                String today = AppUtils.getTodayDate();

                // Prevent duplicates
                for (int i = 0; i < attendanceList.size(); i++) {
                    AttendanceModel att = attendanceList.get(i);
                    if (att.getMemberId().equals(selectedMember.getId()) && att.getDate().equals(today)) {
                        Toast.makeText(MainActivity.this, "Attendance already marked for today!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        return;
                    }
                }

                AttendanceModel attModel = new AttendanceModel("", selectedMember.getId(), selectedMember.getName(), today, "Present");

                backendApi.createRecord("attendance", attModel.toJsonObject(), new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(String responseStr, JSONObject jsonResponse) {
                        Toast.makeText(MainActivity.this, "Attendance marked!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        fetchAttendance();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(MainActivity.this, "Failed to mark attendance", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        dialog.show();
    }

    private void showAddPaymentDialog() {
        if (memberList.isEmpty()) {
            Toast.makeText(this, "Add members first before recording payments", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_payment, null);
        builder.setView(dialogView);

        final Spinner spMembers = dialogView.findViewById(R.id.dialogPayMemberSpinner);
        final EditText etAmount = dialogView.findViewById(R.id.dialogPayAmount);
        final EditText etPending = dialogView.findViewById(R.id.dialogPayPending);
        final Spinner spMethod = dialogView.findViewById(R.id.dialogPayMethodSpinner);
        Button btnSave = dialogView.findViewById(R.id.dialogBtnSavePayment);

        List<String> memberNames = new ArrayList<String>();
        for (int i = 0; i < memberList.size(); i++) {
            memberNames.add(memberList.get(i).getName());
        }

        ArrayAdapter<String> mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, memberNames);
        mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMembers.setAdapter(mAdapter);

        ArrayAdapter<String> methodAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, new String[]{"Cash", "UPI", "Card"});
        methodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMethod.setAdapter(methodAdapter);

        final AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selectedIdx = spMembers.getSelectedItemPosition();
                if (selectedIdx < 0 || selectedIdx >= memberList.size()) return;

                MemberModel selectedMember = memberList.get(selectedIdx);
                String amtStr = etAmount.getText().toString().trim();
                String pendStr = etPending.getText().toString().trim();

                if (amtStr.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Enter payment amount", Toast.LENGTH_SHORT).show();
                    return;
                }

                double amount = Double.parseDouble(amtStr);
                double pending = pendStr.isEmpty() ? 0.0 : Double.parseDouble(pendStr);
                String method = spMethod.getSelectedItem().toString();

                PaymentModel payment = new PaymentModel("", selectedMember.getId(), selectedMember.getName(), amount, pending, method, AppUtils.getTodayDate(), selectedMember.getPlan());

                backendApi.createRecord("payments", payment.toJsonObject(), new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(String responseStr, JSONObject jsonResponse) {
                        Toast.makeText(MainActivity.this, "Payment recorded!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        fetchPayments();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(MainActivity.this, "Failed to record payment", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        dialog.show();
    }

    // Native Adapter Implementations
    private class MemberAdapter extends ArrayAdapter<MemberModel> {
        public MemberAdapter(Context context, List<MemberModel> objects) {
            super(context, 0, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_member, parent, false);
            }
            MemberModel item = getItem(position);
            if (item != null) {
                TextView tvName = convertView.findViewById(R.id.tvItemMemberName);
                TextView tvPhone = convertView.findViewById(R.id.tvItemMemberPhone);
                TextView tvPlan = convertView.findViewById(R.id.tvItemMemberPlan);
                TextView tvExpiry = convertView.findViewById(R.id.tvItemMemberExpiry);
                TextView tvStatus = convertView.findViewById(R.id.tvItemMemberStatus);

                tvName.setText(item.getName());
                tvPhone.setText(item.getPhone());
                tvPlan.setText(item.getPlan());
                tvExpiry.setText("Valid till: " + item.getEndDate());
                tvStatus.setText(item.getStatus());

                if (item.getStatus().equalsIgnoreCase("Expired")) {
                    tvStatus.setBackgroundResource(R.drawable.badge_expired);
                    tvStatus.setTextColor(getResources().getColor(R.color.accent_red));
                } else {
                    tvStatus.setBackgroundResource(R.drawable.badge_active);
                    tvStatus.setTextColor(getResources().getColor(R.color.emerald_primary));
                }
            }
            return convertView;
        }
    }

    private class PlanAdapter extends ArrayAdapter<PlanModel> {
        public PlanAdapter(Context context, List<PlanModel> objects) {
            super(context, 0, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_plan, parent, false);
            }
            PlanModel item = getItem(position);
            if (item != null) {
                TextView tvTitle = convertView.findViewById(R.id.tvPlanTitle);
                TextView tvPrice = convertView.findViewById(R.id.tvPlanPrice);
                TextView tvDuration = convertView.findViewById(R.id.tvPlanDuration);
                TextView tvDesc = convertView.findViewById(R.id.tvPlanDescription);

                tvTitle.setText(item.getTitle());
                tvPrice.setText(AppUtils.formatCurrency(item.getPrice()));
                tvDuration.setText("Duration: " + item.getDurationMonths() + " Months");
                tvDesc.setText(item.getDescription());
            }
            return convertView;
        }
    }

    private class AttendanceAdapter extends ArrayAdapter<AttendanceModel> {
        public AttendanceAdapter(Context context, List<AttendanceModel> objects) {
            super(context, 0, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_attendance, parent, false);
            }
            AttendanceModel item = getItem(position);
            if (item != null) {
                TextView tvName = convertView.findViewById(R.id.tvAttMemberName);
                TextView tvDate = convertView.findViewById(R.id.tvAttDate);
                TextView tvStatus = convertView.findViewById(R.id.tvAttStatus);

                tvName.setText(item.getMemberName());
                tvDate.setText(item.getDate());
                tvStatus.setText(item.getStatus());
            }
            return convertView;
        }
    }

    private class PaymentAdapter extends ArrayAdapter<PaymentModel> {
        public PaymentAdapter(Context context, List<PaymentModel> objects) {
            super(context, 0, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_payment, parent, false);
            }
            PaymentModel item = getItem(position);
            if (item != null) {
                TextView tvName = convertView.findViewById(R.id.tvPayMemberName);
                TextView tvAmount = convertView.findViewById(R.id.tvPayAmount);
                TextView tvMethod = convertView.findViewById(R.id.tvPayMethod);
                TextView tvPending = convertView.findViewById(R.id.tvPayPending);
                TextView tvDate = convertView.findViewById(R.id.tvPayDate);

                tvName.setText(item.getMemberName());
                tvAmount.setText(AppUtils.formatCurrency(item.getAmount()));
                tvMethod.setText("Method: " + item.getPaymentMethod());
                tvPending.setText("Due: " + AppUtils.formatCurrency(item.getPendingAmount()));
                tvDate.setText("Date: " + item.getDate());
            }
            return convertView;
        }
    }
}