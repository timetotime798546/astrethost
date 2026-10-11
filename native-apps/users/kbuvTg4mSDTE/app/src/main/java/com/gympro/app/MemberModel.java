package com.gympro.app;

import org.json.JSONObject;

public class MemberModel {

    private String id;
    private String name;
    private String phone;
    private String email;
    private int age;
    private String gender;
    private String plan;
    private String startDate;
    private String endDate;
    private String status;

    public MemberModel(String id, String name, String phone, String email, int age, String gender, String plan, String startDate, String endDate, String status) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.age = age;
        this.gender = gender;
        this.plan = plan;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public static MemberModel fromRecord(JSONObject recordObj) {
        if (recordObj == null) return null;
        String id = recordObj.optString("id", "");
        JSONObject data = recordObj.optJSONObject("data");
        if (data == null) data = new JSONObject();

        String name = data.optString("name", "Member");
        String phone = data.optString("phone", "");
        String email = data.optString("email", "");
        int age = data.optInt("age", 25);
        String gender = data.optString("gender", "Male");
        String plan = data.optString("plan", "Monthly");
        String startDate = data.optString("start_date", AppUtils.getTodayDate());
        String endDate = data.optString("end_date", AppUtils.getTodayDate());
        String status = data.optString("status", "Active");

        if (AppUtils.isExpired(endDate)) {
            status = "Expired";
        }

        return new MemberModel(id, name, phone, email, age, gender, plan, startDate, endDate, status);
    }

    public JSONObject toJsonObject() {
        JSONObject data = new JSONObject();
        try {
            data.put("name", name);
            data.put("phone", phone);
            data.put("email", email);
            data.put("age", age);
            data.put("gender", gender);
            data.put("plan", plan);
            data.put("start_date", startDate);
            data.put("end_date", endDate);
            data.put("status", status);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public int getAge() { return age; }
    public String getGender() { return gender; }
    public String getPlan() { return plan; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public String getStatus() { return status; }
}