package com.gympro.app;

import org.json.JSONObject;

public class PaymentModel {

    private String id;
    private String memberId;
    private String memberName;
    private double amount;
    private double pendingAmount;
    private String paymentMethod;
    private String date;
    private String planName;

    public PaymentModel(String id, String memberId, String memberName, double amount, double pendingAmount, String paymentMethod, String date, String planName) {
        this.id = id;
        this.memberId = memberId;
        this.memberName = memberName;
        this.amount = amount;
        this.pendingAmount = pendingAmount;
        this.paymentMethod = paymentMethod;
        this.date = date;
        this.planName = planName;
    }

    public static PaymentModel fromRecord(JSONObject recordObj) {
        if (recordObj == null) return null;
        String id = recordObj.optString("id", "");
        JSONObject data = recordObj.optJSONObject("data");
        if (data == null) data = new JSONObject();

        String memberId = data.optString("member_id", "");
        String memberName = data.optString("member_name", "Member");
        double amount = data.optDouble("amount", 0.0);
        double pendingAmount = data.optDouble("pending_amount", 0.0);
        String paymentMethod = data.optString("payment_method", "Cash");
        String date = data.optString("date", AppUtils.getTodayDate());
        String planName = data.optString("plan_name", "General");

        return new PaymentModel(id, memberId, memberName, amount, pendingAmount, paymentMethod, date, planName);
    }

    public JSONObject toJsonObject() {
        JSONObject data = new JSONObject();
        try {
            data.put("member_id", memberId);
            data.put("member_name", memberName);
            data.put("amount", amount);
            data.put("pending_amount", pendingAmount);
            data.put("payment_method", paymentMethod);
            data.put("date", date);
            data.put("plan_name", planName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    public String getId() { return id; }
    public String getMemberId() { return memberId; }
    public String getMemberName() { return memberName; }
    public double getAmount() { return amount; }
    public double getPendingAmount() { return pendingAmount; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getDate() { return date; }
    public String getPlanName() { return planName; }
}