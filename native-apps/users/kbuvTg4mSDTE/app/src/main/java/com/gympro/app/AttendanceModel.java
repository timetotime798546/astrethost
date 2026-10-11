package com.gympro.app;

import org.json.JSONObject;

public class AttendanceModel {

    private String id;
    private String memberId;
    private String memberName;
    private String date;
    private String status;

    public AttendanceModel(String id, String memberId, String memberName, String date, String status) {
        this.id = id;
        this.memberId = memberId;
        this.memberName = memberName;
        this.date = date;
        this.status = status;
    }

    public static AttendanceModel fromRecord(JSONObject recordObj) {
        if (recordObj == null) return null;
        String id = recordObj.optString("id", "");
        JSONObject data = recordObj.optJSONObject("data");
        if (data == null) data = new JSONObject();

        String memberId = data.optString("member_id", "");
        String memberName = data.optString("member_name", "Member");
        String date = data.optString("date", AppUtils.getTodayDate());
        String status = data.optString("status", "Present");

        return new AttendanceModel(id, memberId, memberName, date, status);
    }

    public JSONObject toJsonObject() {
        JSONObject data = new JSONObject();
        try {
            data.put("member_id", memberId);
            data.put("member_name", memberName);
            data.put("date", date);
            data.put("status", status);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    public String getId() { return id; }
    public String getMemberId() { return memberId; }
    public String getMemberName() { return memberName; }
    public String getDate() { return date; }
    public String getStatus() { return status; }
}