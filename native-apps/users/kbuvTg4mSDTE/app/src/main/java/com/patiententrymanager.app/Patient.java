package com.patiententrymanager.app;

import org.json.JSONObject;
import java.io.Serializable;

public class Patient implements Serializable {

    private String id;
    private String name;
    private int age;
    private String gender;
    private String phone;
    private String visitDate;
    private String notes;
    private String imageId;

    public Patient() {
    }

    public Patient(String id, String name, int age, String gender, String phone, String visitDate, String notes, String imageId) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.phone = phone;
        this.visitDate = visitDate;
        this.notes = notes;
        this.imageId = imageId;
    }

    public static Patient fromJsonRecord(JSONObject recordObj) {
        try {
            Patient patient = new Patient();
            patient.setId(recordObj.optString("id", ""));

            JSONObject dataObj = recordObj.optJSONObject("data");
            if (dataObj == null) {
                dataObj = recordObj;
            }

            patient.setName(dataObj.optString("name", "Unknown"));
            patient.setAge(dataObj.optInt("age", 0));
            patient.setGender(dataObj.optString("gender", "Unspecified"));
            patient.setPhone(dataObj.optString("phone", "N/A"));
            patient.setVisitDate(dataObj.optString("visit_date", "N/A"));
            patient.setNotes(dataObj.optString("notes", ""));
            patient.setImageId(dataObj.optString("image_id", ""));

            return patient;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public JSONObject toDataJson() {
        JSONObject data = new JSONObject();
        try {
            data.put("name", name);
            data.put("age", age);
            data.put("gender", gender);
            data.put("phone", phone);
            data.put("visit_date", visitDate);
            data.put("notes", notes);
            data.put("image_id", imageId != null ? imageId : "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(String visitDate) {
        this.visitDate = visitDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }
}