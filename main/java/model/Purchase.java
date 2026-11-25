package model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Model representing a Purchase Order transaction.
 */
public class Purchase {
    private int id;
    private String purchaseNumber;
    private int supplierId;
    private String supplierName;
    private int userId;
    private String userName;
    private BigDecimal totalAmount;
    private String status; // COMPLETED, CANCELLED
    private String notes;
    private Timestamp purchaseDate;
    private Timestamp createdAt;
    private List<PurchaseItem> items = new ArrayList<>();

    public Purchase() {}

    public Purchase(int id, String purchaseNumber, int supplierId, String supplierName, int userId, 
                    BigDecimal totalAmount, String status, String notes, Timestamp purchaseDate) {
        this.id = id;
        this.purchaseNumber = purchaseNumber;
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.status = status;
        this.notes = notes;
        this.purchaseDate = purchaseDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPurchaseNumber() {
        return purchaseNumber;
    }

    public void setPurchaseNumber(String purchaseNumber) {
        this.purchaseNumber = purchaseNumber;
    }

    public int getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(int supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Timestamp getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(Timestamp purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public List<PurchaseItem> getItems() {
        return items;
    }

    public void setItems(List<PurchaseItem> items) {
        this.items = items;
    }

    public void addItem(PurchaseItem item) {
        this.items.add(item);
    }
}

