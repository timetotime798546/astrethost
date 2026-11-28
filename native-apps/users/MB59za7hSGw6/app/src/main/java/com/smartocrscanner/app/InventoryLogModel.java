package com.smartocrscanner.app;

public class InventoryLogModel {
    private long id;
    private long mobileId;
    private String action; // "STOCK_IN", "STOCK_OUT", "EDIT", "DELETE"
    private String details;
    private String timestamp;

    public InventoryLogModel(long id, long mobileId, String action, String details, String timestamp) {
        this.id = id;
        this.mobileId = mobileId;
        this.action = action;
        this.details = details;
        this.timestamp = timestamp;
    }

    public long getId() { return id; }
    public long getMobileId() { return mobileId; }
    public String getAction() { return action != null ? action : ""; }
    public String getDetails() { return details != null ? details : ""; }
    public String getTimestamp() { return timestamp != null ? timestamp : ""; }
}