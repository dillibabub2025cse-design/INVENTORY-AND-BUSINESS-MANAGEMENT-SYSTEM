package model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Model representing an Inventory Product.
 */
public class Product {
    private int id;
    private String barcode;
    private String name;
    private int categoryId;
    private String categoryName; // Joined from categories table
    private String description;
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;
    private int currentStock;
    private int minStockLevel;
    private boolean active;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Product() {}

    public Product(int id, String barcode, String name, int categoryId, String description, 
                   BigDecimal purchasePrice, BigDecimal sellingPrice, int currentStock, 
                   int minStockLevel, boolean active) {
        this.id = id;
        this.barcode = barcode;
        this.name = name;
        this.categoryId = categoryId;
        this.description = description;
        this.purchasePrice = purchasePrice;
        this.sellingPrice = sellingPrice;
        this.currentStock = currentStock;
        this.minStockLevel = minStockLevel;
        this.active = active;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public int getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(int currentStock) {
        this.currentStock = currentStock;
    }

    public int getMinStockLevel() {
        return minStockLevel;
    }

    public void setMinStockLevel(int minStockLevel) {
        this.minStockLevel = minStockLevel;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isLowStock() {
        return currentStock <= minStockLevel && currentStock > 0;
    }

    public boolean isOutOfStock() {
        return currentStock <= 0;
    }

    public String getStockStatus() {
        if (isOutOfStock()) {
            return "OUT OF STOCK";
        } else if (isLowStock()) {
            return "LOW STOCK";
        }
        return "IN STOCK";
    }

    @Override
    public String toString() {
        return name != null ? name : "";
    }
}

