package service;

import dao.InventoryDAO;
import dao.ProductDAO;
import exception.ValidationException;
import model.Product;
import model.StockMovement;
import model.StockMovementType;

import java.util.List;
import java.util.Map;

/**
 * Service managing Inventory audits, stock movements, adjustments, and valuations.
 */
public class InventoryService {

    private final InventoryDAO inventoryDAO;
    private final ProductDAO productDAO;

    public InventoryService() {
        this.inventoryDAO = new InventoryDAO();
        this.productDAO = new ProductDAO();
    }

    public InventoryService(InventoryDAO inventoryDAO, ProductDAO productDAO) {
        this.inventoryDAO = inventoryDAO;
        this.productDAO = productDAO;
    }

    public List<Product> getCurrentStockList() {
        return productDAO.findAll();
    }

    public List<StockMovement> getStockMovements(Integer productId, int limit) {
        return inventoryDAO.getMovements(productId, limit);
    }

    public Map<String, Object> getInventoryValuation() {
        return inventoryDAO.getInventoryValuation();
    }

    public boolean adjustStock(int productId, int qtyDelta, StockMovementType type, String reason, int userId) {
        if (qtyDelta == 0) {
            throw new ValidationException("Adjustment quantity cannot be zero.");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new ValidationException("Adjustment reason must be provided for audit compliance.");
        }
        Product product = productDAO.findById(productId);
        if (product == null) {
            throw new ValidationException("Product with ID " + productId + " does not exist.");
        }
        return inventoryDAO.recordAdjustment(productId, qtyDelta, type, reason.trim(), userId);
    }
}

