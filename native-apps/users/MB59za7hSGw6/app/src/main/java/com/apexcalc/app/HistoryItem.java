package com.apexcalc.app;

import org.json.JSONException;
import org.json.JSONObject;

public class HistoryItem {
    private String id;
    private String expression;
    private String result;
    private long timestamp;

    public HistoryItem(String id, String expression, String result, long timestamp) {
        this.id = id;
        this.expression = expression;
        this.result = result;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public String getExpression() {
        return expression;
    }

    public String getResult() {
        return result;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public JSONObject toJSONObject() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", id);
            obj.put("expression", expression);
            obj.put("result", result);
            obj.put("timestamp", timestamp);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    public static HistoryItem fromJSONObject(JSONObject obj) {
        if (obj == null) return null;
        String id = obj.optString("id", String.valueOf(System.currentTimeMillis()));
        String expression = obj.optString("expression", "");
        String result = obj.optString("result", "0");
        long timestamp = obj.optLong("timestamp", System.currentTimeMillis());
        return new HistoryItem(id, expression, result, timestamp);
    }
}