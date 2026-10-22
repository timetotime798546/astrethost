package com.luminacalc.app;

import org.json.JSONException;
import org.json.JSONObject;

public class CalculationItem {
    private String expression;
    private String result;
    private long timestamp;

    public CalculationItem(String expression, String result, long timestamp) {
        this.expression = expression;
        this.result = result;
        this.timestamp = timestamp;
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
            obj.put("expression", expression);
            obj.put("result", result);
            obj.put("timestamp", timestamp);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    public static CalculationItem fromJSONObject(JSONObject obj) {
        if (obj == null) return null;
        String expr = obj.optString("expression", "");
        String res = obj.optString("result", "");
        long time = obj.optLong("timestamp", System.currentTimeMillis());
        return new CalculationItem(expr, res, time);
    }
}