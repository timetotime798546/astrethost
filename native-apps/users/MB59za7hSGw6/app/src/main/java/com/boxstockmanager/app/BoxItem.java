package com.boxstockmanager.app;

public class BoxItem {
    private long id;
    private String qrCode;
    private int quantity;
    private String lastUpdated;

    public BoxItem(long id, String qrCode, int quantity, String lastUpdated) {
        this.id = id;
        this.qrCode = qrCode;
        this.quantity = quantity;
        this.lastUpdated = lastUpdated;
    }

    public long getId() {
        return id;
    }

    public String getQrCode() {
        return qrCode;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }
}