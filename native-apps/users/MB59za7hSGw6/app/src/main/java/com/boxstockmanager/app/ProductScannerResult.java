package com.boxstockmanager.app;

public class ProductScannerResult {
    private String productCode = "";
    private String barcode = "";
    private String brand = "";
    private String model = "";
    private String imei1 = "";
    private String imei2 = "";
    private String serialNumber = "";
    private String color = "";
    private String ram = "";
    private String storage = "";
    private String rawOcrText = "";

    public ProductScannerResult() {}

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

    public String getColor() { return color != null ? color : ""; }
    public void setColor(String color) { this.color = color; }

    public String getRam() { return ram != null ? ram : ""; }
    public void setRam(String ram) { this.ram = ram; }

    public String getStorage() { return storage != null ? storage : ""; }
    public void setStorage(String storage) { this.storage = storage; }

    public String getRawOcrText() { return rawOcrText != null ? rawOcrText : ""; }
    public void setRawOcrText(String rawOcrText) { this.rawOcrText = rawOcrText; }

    public boolean hasAnyData() {
        return !productCode.isEmpty() || !barcode.isEmpty() || !brand.isEmpty() ||
               !model.isEmpty() || !imei1.isEmpty() || !imei2.isEmpty() ||
               !serialNumber.isEmpty() || !rawOcrText.isEmpty();
    }
}