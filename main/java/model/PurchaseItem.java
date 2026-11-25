package model;

import java.math.BigDecimal;

/**
 * Model representing an item line in a Purchase Order.
 */
public class PurchaseItem {
    private int id;
    private int purchaseId;
    private int productId;
    private String productName;
    private String productBarcode;
    private int quantity;
    private BigDecimal unitCost;
    private BigDecimal subtotal;

    public PurchaseItem() {}

    public PurchaseItem(int productId, String productName, int quantity, BigDecimal unitCost) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitCost = unitCost;
        this.subtotal = unitCost.multiply(BigDecimal.valueOf(quantity));
    }

    public PurchaseItem(int id, int purchaseId, int productId, int quantity, BigDecimal unitCost, BigDecimal subtotal) {
        this.id = id;
        this.purchaseId = purchaseId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitCost = unitCost;
        this.subtotal = subtotal;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPurchaseId() {
        return purchaseId;
    }

    public void setPurchaseId(int purchaseId) {
        this.purchaseId = purchaseId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductBarcode() {
        return productBarcode;
    }

    public void setProductBarcode(String productBarcode) {
        this.productBarcode = productBarcode;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        recalculateSubtotal();
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
        recalculateSubtotal();
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    private void recalculateSubtotal() {
        if (unitCost != null && quantity > 0) {
            this.subtotal = unitCost.multiply(BigDecimal.valueOf(quantity));
        }
    }
}

