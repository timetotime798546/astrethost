package com.boxstockmanager.app;

public class ProductScannerResult {
    private String productName = "";
    private String productCode = "";
    private String barcode = "";
    private String brand = "";
    private String model = "";
    private String imei1 = "";
    private String imei2 = "";
    private String serialNumber = "";
    private String category = "";
    private String color = "";
    private String ram = "";
    private String storage = "";
    private double purchasePrice = 0.0;
    private double sellingPrice = 0.0;
    private int stockQuantity = 0;
    private String supplierName = "";
    private String location = "";
    private String description = "";
    private String rawOcrText = "";
    private String rawDecodedText = "";

    public ProductScannerResult() {}

    public String getProductName() { return productName != null ? productName : ""; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductCode() { return productCode != null ? productCode : ""; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getBarcode() { return barcode != null ? barcode : ""; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getBrand() { return brand != null ? brand : ""; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModel() { return model != null ? model : ""; }
    public void setModel(String model) { this.model = model; }

    public String getImei1() { return imei1 != null ? imei1 : ""; }
    public void setImei1(String imei1) { this.imei1 = imei1; }

    public String getImei2() { return imei2 != null ? imei2 : ""; }
    public void setImei2(String imei2) { this.imei2 = imei2; }

    public String getSerialNumber() { return serialNumber != null ? serialNumber : ""; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getCategory() { return category != null ? category : ""; }
    public void setCategory(String category) { this.category = category; }

    public String getColor() { return color != null ? color : ""; }
    public void setColor(String color) { this.color = color; }

    public String getRam() { return ram != null ? ram : ""; }
    public void setRam(String ram) { this.ram = ram; }

    public String getStorage() { return storage != null ? storage : ""; }
    public void setStorage(String storage) { this.storage = storage; }

    public double getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(double purchasePrice) { this.purchasePrice = purchasePrice; }

    public double getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(double sellingPrice) { this.sellingPrice = sellingPrice; }

    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }

    public String getSupplierName() { return supplierName != null ? supplierName : ""; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getLocation() { return location != null ? location : ""; }
    public void setLocation(String location) { this.location = location; }

    public String getDescription() { return description != null ? description : ""; }
    public void setDescription(String description) { this.description = description; }

    public String getRawOcrText() { return rawOcrText != null ? rawOcrText : ""; }
    public void setRawOcrText(String rawOcrText) { this.rawOcrText = rawOcrText; }

    public String getRawDecodedText() { return rawDecodedText != null ? rawDecodedText : ""; }
    public void setRawDecodedText(String rawDecodedText) { this.rawDecodedText = rawDecodedText; }

    public boolean hasAnyData() {
        return !productName.isEmpty() || !productCode.isEmpty() || !barcode.isEmpty() ||
               !brand.isEmpty() || !model.isEmpty() || !imei1.isEmpty() || !imei2.isEmpty() ||
               !serialNumber.isEmpty() || !category.isEmpty() || !color.isEmpty() ||
               !ram.isEmpty() || !storage.isEmpty() || purchasePrice > 0 || sellingPrice > 0 ||
               stockQuantity > 0 || !supplierName.isEmpty() || !location.isEmpty() ||
               !description.isEmpty() || !rawOcrText.isEmpty() || !rawDecodedText.isEmpty();
    }
}