package com.boxstockmanager.app;

public class Product {
    private long id;
    private String name;
    private String brand;
    private String model;
    private String productCode;
    private String barcode;
    private String imei1;
    private String imei2;
    private String serialNumber;
    private String category;
    private String color;
    private String ram;
    private String storage;
    private double purchasePrice;
    private double sellingPrice;
    private int stockQuantity;
    private String unit;
    private String supplierName;
    private String supplierPhone;
    private String purchaseDate;
    private String warranty;
    private String description;
    private String imagePath;
    private String location;
    private String createdAt;

    public Product() {
        this.id = 0;
        this.unit = "Pcs";
        this.stockQuantity = 0;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name != null ? name : ""; }
    public void setName(String name) { this.name = name; }

    public String getBrand() { return brand != null ? brand : ""; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModel() { return model != null ? model : ""; }
    public void setModel(String model) { this.model = model; }

    public String getProductCode() { return productCode != null ? productCode : ""; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getBarcode() { return barcode != null ? barcode : ""; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getImei1() { return imei1 != null ? imei1 : ""; }
    public void setImei1(String imei1) { this.imei1 = imei1; }

    public String getImei2() { return imei2 != null ? imei2 : ""; }
    public void setImei2(String imei2) { this.imei2 = imei2; }

    public String getSerialNumber() { return serialNumber != null ? serialNumber : ""; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getCategory() { return category != null ? category : "General"; }
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

    public String getUnit() { return unit != null ? unit : "Pcs"; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getSupplierName() { return supplierName != null ? supplierName : ""; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getSupplierPhone() { return supplierPhone != null ? supplierPhone : ""; }
    public void setSupplierPhone(String supplierPhone) { this.supplierPhone = supplierPhone; }

    public String getPurchaseDate() { return purchaseDate != null ? purchaseDate : ""; }
    public void setPurchaseDate(String purchaseDate) { this.purchaseDate = purchaseDate; }

    public String getWarranty() { return warranty != null ? warranty : ""; }
    public void setWarranty(String warranty) { this.warranty = warranty; }

    public String getDescription() { return description != null ? description : ""; }
    public void setDescription(String description) { this.description = description; }

    public String getImagePath() { return imagePath != null ? imagePath : ""; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getLocation() { return location != null ? location : ""; }
    public void setLocation(String location) { this.location = location; }

    public String getCreatedAt() { return createdAt != null ? createdAt : ""; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getStockStatus() {
        if (stockQuantity <= 0) {
            return "Out of Stock";
        } else if (stockQuantity <= 5) {
            return "Low Stock";
        } else {
            return "In Stock";
        }
    }
}