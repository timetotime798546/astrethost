package com.boxstockmanager.app;

public class ScanLogItem {
    private long id;
    private String timestamp;
    private String source;
    private String barcode;
    private String scannedText;

    public ScanLogItem(long id, String timestamp, String source, String barcode, String scannedText) {
        this.id = id;
        this.timestamp = timestamp;
        this.source = source;
        this.barcode = barcode;
        this.scannedText = scannedText;
    }

    public long getId() { return id; }
    public String getTimestamp() { return timestamp != null ? timestamp : ""; }
    public String getSource() { return source != null ? source : "Camera"; }
    public String getBarcode() { return barcode != null ? barcode : ""; }
    public String getScannedText() { return scannedText != null ? scannedText : ""; }
}