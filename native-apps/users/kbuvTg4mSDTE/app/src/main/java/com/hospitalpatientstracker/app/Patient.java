package com.hospitalpatientstracker.app;

import org.json.JSONObject;

public class Patient {
    public String id;
    public String name;
    public int age;
    public String gender;
    public String admissionDate;
    public String illness;
    public String roomNumber;
    public String status; // "Admitted" or "Discharged"

    public Patient() {
    }

    public static Patient fromJson(JSONObject obj) {
        try {
            Patient p = new Patient();
            p.id = obj.optString("id", "");
            
            JSONObject dataObj = obj.optJSONObject("data");
            if (dataObj != null) {
                p.name = dataObj.optString("name", "Unknown Patient");
                p.age = dataObj.optInt("age", 0);
                p.gender = dataObj.optString("gender", "Unspecified");
                p.admissionDate = dataObj.optString("admission_date", "");
                p.illness = dataObj.optString("illness", "");
                p.roomNumber = dataObj.optString("room_number", "");
                p.status = dataObj.optString("status", "Admitted");
            }
            return p;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public JSONObject toDataJson() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("name", name);
            obj.put("age", age);
            obj.put("gender", gender);
            obj.put("admission_date", admissionDate);
            obj.put("illness", illness);
            obj.put("room_number", roomNumber);
            obj.put("status", status);
            return obj;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}