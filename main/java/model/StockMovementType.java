package model;

/**
 * Enumeration of inventory transaction movement types.
 */
public enum StockMovementType {
    PURCHASE,
    SALE,
    PURCHASE_CANCELLED,
    SALE_CANCELLED,
    MANUAL_IN,
    MANUAL_OUT,
    ADJUSTMENT
}

