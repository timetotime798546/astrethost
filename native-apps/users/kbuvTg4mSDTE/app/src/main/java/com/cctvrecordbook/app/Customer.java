package com.cctvrecordbook.app;

import org.json.JSONObject;
import java.io.Serializable;

public class Customer implements Serializable {
    public String id;
    public String name;
    public String phone;
    public String address;
    public String date;
    public int cameraCount;
    public double totalAmount;
    public double amountReceived;
    public double balance;
    public String details;

    public Customer() {}

    public Customer(JSONObject recordObj) {
        try {
            this.id = recordObj.optString("id");
            JSONObject data = recordObj.optJSONObject("data");
            if (data != null) {
                this.name = data.optString("customer_name", "");
                this.phone = data.optString("phone", "");
                this.address = data.optString("address", "");
                this.date = data.optString("installation_date", "");
                this.cameraCount = data.optInt("camera_count", 0);
                this.totalAmount = data.optDouble("total_amount", 0.0);
                this.amountReceived = data.optDouble("amount_received", 0.0);
                this.balance = data.optDouble("balance", 0.0);
                this.details = data.optString("camera_details", "");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JSONObject toJsonObject() {
        JSONObject data = new JSONObject();
        try {
            data.put("customer_name", name);
            data.put("phone", phone);
            data.put("address", address);
            data.put("installation_date", date);
            data.put("camera_count", cameraCount);
            data.put("total_amount", totalAmount);
            data.put("amount_received", amountReceived);
            data.put("balance", balance);
            data.put("camera_details", details);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }
}