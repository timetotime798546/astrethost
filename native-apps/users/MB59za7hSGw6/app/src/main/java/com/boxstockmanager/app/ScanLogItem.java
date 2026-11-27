package com.boxstockmanager.app;

public class ScanLogItem {
    private String timestamp;
    private String source;
    private String barcodeValue;
    private String fullText;

    public ScanLogItem(String timestamp, String source, String barcodeValue, String fullText) {
        this.timestamp = timestamp != null ? timestamp : "";
        this.source = source != null ? source : "";
        this.barcodeValue = barcodeValue != null ? barcodeValue : "";
        this.fullText = fullText != null ? fullText : "";
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getSource() {
        return source;
    }

    public String getBarcodeValue() {
        return barcodeValue;
    }

    public String getFullText() {
        return fullText;
    }
}