package com.bhagwatistock.app;

public class MobileModel {
    private long id;
    private String brand;
    private String model;
    private String modelNumber;
    private String imei1;
    private String imei2;
    private String serialNumber;
    private String ram;
    private String storage;
    private String color;
    private String productCode;
    private double purchasePrice;
    private double sellingPrice;
    private String status; // "AVAILABLE" or "SOLD"
    private String customerName;
    private String customerPhone;
    private String stockInDate;
    private String stockOutDate;
    private String notes;
    private String rawOcrText;

    public MobileModel() {
        this.status = "AVAILABLE";
        this.brand = "";
        this.model = "";
        this.modelNumber = "";
        this.imei1 = "";
        this.imei2 = "";
        this.serialNumber = "";
        this.ram = "";
        this.storage = "";
        this.color = "";
        this.productCode = "";
        this.customerName = "";
        this.customerPhone = "";
        this.stockInDate = "";
        this.stockOutDate = "";
        this.notes = "";
        this.rawOcrText = "";
    }

    public MobileModel(long id, String brand, String model, String modelNumber, String imei1, String imei2,
                       String serialNumber, String ram, String storage, String color, String productCode,
                       double purchasePrice, double sellingPrice, String status, String customerName,
                       String customerPhone, String stockInDate, String stockOutDate, String notes, String rawOcrText) {
        this.id = id;
        this.brand = brand;
        this.model = model;
        this.modelNumber = modelNumber;
        this.imei1 = imei1;
        this.imei2 = imei2;
        this.serialNumber = serialNumber;
        this.ram = ram;
        this.storage = storage;
        this.color = color;
        this.productCode = productCode;
        this.purchasePrice = purchasePrice;
        this.sellingPrice = sellingPrice;
        this.status = status;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.stockInDate = stockInDate;
        this.stockOutDate = stockOutDate;
        this.notes = notes;
        this.rawOcrText = rawOcrText;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getBrand() { return brand != null ? brand : ""; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModel() { return model != null ? model : ""; }
    public void setModel(String model) { this.model = model; }

    public String getModelNumber() { return modelNumber != null ? modelNumber : ""; }
    public void setModelNumber(String modelNumber) { this.modelNumber = modelNumber; }

    public String getImei1() { return imei1 != null ? imei1 : ""; }
    public void setImei1(String imei1) { this.imei1 = imei1; }

    public String getImei2() { return imei2 != null ? imei2 : ""; }
    public void setImei2(String imei2) { this.imei2 = imei2; }

    public String getSerialNumber() { return serialNumber != null ? serialNumber : ""; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getRam() { return ram != null ? ram : ""; }
    public void setRam(String ram) { this.ram = ram; }

    public String getStorage() { return storage != null ? storage : ""; }
    public void setStorage(String storage) { this.storage = storage; }

    public String getColor() { return color != null ? color : ""; }
    public void setColor(String color) { this.color = color; }

    public String getProductCode() { return productCode != null ? productCode : ""; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public double getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(double purchasePrice) { this.purchasePrice = purchasePrice; }

    public double getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(double sellingPrice) { this.sellingPrice = sellingPrice; }

    public String getStatus() { return status != null ? status : "AVAILABLE"; }
    public void setStatus(String status) { this.status = status; }

    public String getCustomerName() { return customerName != null ? customerName : ""; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone != null ? customerPhone : ""; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getStockInDate() { return stockInDate != null ? stockInDate : ""; }
    public void setStockInDate(String stockInDate) { this.stockInDate = stockInDate; }

    public String getStockOutDate() { return stockOutDate != null ? stockOutDate : ""; }
    public void setStockOutDate(String stockOutDate) { this.stockOutDate = stockOutDate; }

    public String getNotes() { return notes != null ? notes : ""; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getRawOcrText() { return rawOcrText != null ? rawOcrText : ""; }
    public void setRawOcrText(String rawOcrText) { this.rawOcrText = rawOcrText; }
}