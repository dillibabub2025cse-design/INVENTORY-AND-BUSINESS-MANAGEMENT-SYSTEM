package exception;

/**
 * Thrown when an inventory reduction request exceeds available product stock.
 */
public class InsufficientStockException extends AppException {
    private final int productId;
    private final int requested;
    private final int available;

    public InsufficientStockException(String message) {
        super(message);
        this.productId = -1;
        this.requested = 0;
        this.available = 0;
    }

    public InsufficientStockException(int productId, int requested, int available) {
        super(String.format("Insufficient stock for Product ID %d: Requested %d, Available %d", productId, requested, available));
        this.productId = productId;
        this.requested = requested;
        this.available = available;
    }

    public int getProductId() {
        return productId;
    }

    public int getRequested() {
        return requested;
    }

    public int getAvailable() {
        return available;
    }
}

