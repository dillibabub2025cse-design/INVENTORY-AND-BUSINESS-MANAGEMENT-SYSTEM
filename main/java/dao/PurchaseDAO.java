package dao;

import config.DBConnection;
import exception.DatabaseOperationException;
import exception.ValidationException;
import model.Purchase;
import model.PurchaseItem;
import model.StockMovementType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Purchase Orders, Item lines, and atomic stock replenishment transactions.
 */
public class PurchaseDAO {

    /**
     * Atomically creates a purchase order, inserts its items, increases product stock, 
     * and logs entries in stock_movements. Rollbacks on any failure.
     */
    public boolean createPurchase(Purchase purchase) {
        String insertPurchaseSql = "INSERT INTO purchases (purchase_number, supplier_id, user_id, total_amount, status, notes, purchase_date) " +
                                   "VALUES (?, ?, ?, ?, ?, ?, ?)";
        String insertItemSql = "INSERT INTO purchase_items (purchase_id, product_id, quantity, unit_cost, subtotal) " +
                               "VALUES (?, ?, ?, ?, ?)";
        String updateStockSql = "UPDATE products SET current_stock = current_stock + ? WHERE id = ?";
        String getStockSql = "SELECT current_stock FROM products WHERE id = ?";
        String insertMovementSql = "INSERT INTO stock_movements (product_id, movement_type, quantity, previous_stock, new_stock, reference_id, notes, created_by) " +
                                   "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin Transaction

            // 1. Insert Purchase Header
            int purchaseId;
            try (PreparedStatement ps = conn.prepareStatement(insertPurchaseSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, purchase.getPurchaseNumber());
                ps.setInt(2, purchase.getSupplierId());
                ps.setInt(3, purchase.getUserId());
                ps.setBigDecimal(4, purchase.getTotalAmount());
                ps.setString(5, "COMPLETED");
                ps.setString(6, purchase.getNotes());
                ps.setTimestamp(7, purchase.getPurchaseDate());

                int affected = ps.executeUpdate();
                if (affected == 0) {
                    throw new SQLException("Failed to insert purchase header.");
                }
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        purchaseId = rs.getInt(1);
                        purchase.setId(purchaseId);
                    } else {
                        throw new SQLException("Failed to obtain generated purchase ID.");
                    }
                }
            }

            // 2. Insert Items, Increase Stock, and Record Movement
            for (PurchaseItem item : purchase.getItems()) {
                item.setPurchaseId(purchaseId);

                // Fetch current stock
                int currentStock = 0;
                try (PreparedStatement psStock = conn.prepareStatement(getStockSql)) {
                    psStock.setInt(1, item.getProductId());
                    try (ResultSet rs = psStock.executeQuery()) {
                        if (rs.next()) {
                            currentStock = rs.getInt("current_stock");
                        } else {
                            throw new SQLException("Product ID " + item.getProductId() + " does not exist.");
                        }
                    }
                }

                // Insert purchase item
                try (PreparedStatement psItem = conn.prepareStatement(insertItemSql, Statement.RETURN_GENERATED_KEYS)) {
                    psItem.setInt(1, purchaseId);
                    psItem.setInt(2, item.getProductId());
                    psItem.setInt(3, item.getQuantity());
                    psItem.setBigDecimal(4, item.getUnitCost());
                    psItem.setBigDecimal(5, item.getSubtotal());
                    psItem.executeUpdate();
                }

                // Increase product stock
                try (PreparedStatement psUpdate = conn.prepareStatement(updateStockSql)) {
                    psUpdate.setInt(1, item.getQuantity());
                    psUpdate.setInt(2, item.getProductId());
                    psUpdate.executeUpdate();
                }

                // Record stock movement log
                int newStock = currentStock + item.getQuantity();
                try (PreparedStatement psMovement = conn.prepareStatement(insertMovementSql)) {
                    psMovement.setInt(1, item.getProductId());
                    psMovement.setString(2, StockMovementType.PURCHASE.name());
                    psMovement.setInt(3, item.getQuantity());
                    psMovement.setInt(4, currentStock);
                    psMovement.setInt(5, newStock);
                    psMovement.setString(6, purchase.getPurchaseNumber());
                    psMovement.setString(7, "Purchase confirmation PO " + purchase.getPurchaseNumber());
                    psMovement.setInt(8, purchase.getUserId());
                    psMovement.executeUpdate();
                }
            }

            conn.commit(); // Transaction Succeeded
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback on failure
                } catch (SQLException ex) {
                    System.err.println("Rollback error: " + ex.getMessage());
                }
            }
            throw new DatabaseOperationException("Failed to complete purchase transaction: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Cancels a purchase order and safely deducts previously added stock, rolling back on error.
     */
    public boolean cancelPurchase(int purchaseId, int userId, String reason) {
        String getPurchaseSql = "SELECT * FROM purchases WHERE id = ? FOR UPDATE";
        String getItemsSql = "SELECT * FROM purchase_items WHERE purchase_id = ?";
        String getStockSql = "SELECT name, current_stock FROM products WHERE id = ? FOR UPDATE";
        String updateStockSql = "UPDATE products SET current_stock = current_stock - ? WHERE id = ?";
        String updatePurchaseStatusSql = "UPDATE purchases SET status = 'CANCELLED', notes = CONCAT(IFNULL(notes,''), ' [CANCELLED: ', ?, ']') WHERE id = ?";
        String insertMovementSql = "INSERT INTO stock_movements (product_id, movement_type, quantity, previous_stock, new_stock, reference_id, notes, created_by) " +
                                   "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin Transaction

            // 1. Verify Purchase Status
            String poNumber;
            try (PreparedStatement ps = conn.prepareStatement(getPurchaseSql)) {
                ps.setInt(1, purchaseId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new ValidationException("Purchase Order #" + purchaseId + " not found.");
                    }
                    String currentStatus = rs.getString("status");
                    if (!"COMPLETED".equalsIgnoreCase(currentStatus)) {
                        throw new ValidationException("Purchase Order #" + purchaseId + " cannot be cancelled because its status is '" + currentStatus + "'. Only COMPLETED purchases can be cancelled.");
                    }
                    poNumber = rs.getString("purchase_number");
                }
            }

            // 2. Fetch items and verify safe stock reversal
            List<PurchaseItem> items = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(getItemsSql)) {
                ps.setInt(1, purchaseId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        PurchaseItem item = new PurchaseItem();
                        item.setProductId(rs.getInt("product_id"));
                        item.setQuantity(rs.getInt("quantity"));
                        item.setUnitCost(rs.getBigDecimal("unit_cost"));
                        items.add(item);
                    }
                }
            }

            // Check if reversal causes negative stock
            for (PurchaseItem item : items) {
                try (PreparedStatement ps = conn.prepareStatement(getStockSql)) {
                    ps.setInt(1, item.getProductId());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String name = rs.getString("name");
                            int stock = rs.getInt("current_stock");
                            if (stock < item.getQuantity()) {
                                throw new ValidationException("Cannot cancel PO: reversing " + item.getQuantity() + 
                                        " units would make stock negative for product '" + name + "' (Current Stock: " + stock + ")");
                            }
                        }
                    }
                }
            }

            // Atomically mark status to CANCELLED conditional on status = 'COMPLETED'
            String markCancelledSql = "UPDATE purchases SET status = 'CANCELLED', notes = CONCAT(IFNULL(notes,''), ' [CANCELLED: ', ?, ']') WHERE id = ? AND status = 'COMPLETED'";
            try (PreparedStatement ps = conn.prepareStatement(markCancelledSql)) {
                ps.setString(1, reason);
                ps.setInt(2, purchaseId);
                int affected = ps.executeUpdate();
                if (affected == 0) {
                    throw new ValidationException("Purchase Order #" + purchaseId + " could not be cancelled. It may have already been cancelled.");
                }
            }

            // Deduct stock and log movements
            for (PurchaseItem item : items) {
                int currentStock = 0;
                try (PreparedStatement ps = conn.prepareStatement(getStockSql)) {
                    ps.setInt(1, item.getProductId());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            currentStock = rs.getInt("current_stock");
                        }
                    }
                }

                try (PreparedStatement psUpdate = conn.prepareStatement(updateStockSql)) {
                    psUpdate.setInt(1, item.getQuantity());
                    psUpdate.setInt(2, item.getProductId());
                    psUpdate.executeUpdate();
                }

                int newStock = currentStock - item.getQuantity();
                try (PreparedStatement psMovement = conn.prepareStatement(insertMovementSql)) {
                    psMovement.setInt(1, item.getProductId());
                    psMovement.setString(2, StockMovementType.PURCHASE_CANCELLED.name());
                    psMovement.setInt(3, -item.getQuantity());
                    psMovement.setInt(4, currentStock);
                    psMovement.setInt(5, newStock);
                    psMovement.setString(6, poNumber);
                    psMovement.setString(7, "Purchase cancellation: " + reason);
                    psMovement.setInt(8, userId);
                    psMovement.executeUpdate();
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {}
            }
            throw new DatabaseOperationException("Error cancelling purchase order: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    public List<Purchase> findAll() {
        List<Purchase> list = new ArrayList<>();
        String sql = "SELECT p.*, s.company_name AS supplier_name, u.full_name AS user_name " +
                     "FROM purchases p " +
                     "JOIN suppliers s ON p.supplier_id = s.id " +
                     "JOIN users u ON p.user_id = u.id " +
                     "ORDER BY p.id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching purchases: " + e.getMessage(), e);
        }
        return list;
    }

    public Purchase findById(int id) {
        String sql = "SELECT p.*, s.company_name AS supplier_name, u.full_name AS user_name " +
                     "FROM purchases p " +
                     "JOIN suppliers s ON p.supplier_id = s.id " +
                     "JOIN users u ON p.user_id = u.id " +
                     "WHERE p.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Purchase p = mapRow(rs);
                    p.setItems(findItemsByPurchaseId(p.getId()));
                    return p;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching purchase: " + e.getMessage(), e);
        }
        return null;
    }

    public List<PurchaseItem> findItemsByPurchaseId(int purchaseId) {
        List<PurchaseItem> items = new ArrayList<>();
        String sql = "SELECT pi.*, pr.name AS product_name, pr.barcode AS product_barcode " +
                     "FROM purchase_items pi " +
                     "JOIN products pr ON pi.product_id = pr.id " +
                     "WHERE pi.purchase_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, purchaseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PurchaseItem item = new PurchaseItem();
                    item.setId(rs.getInt("id"));
                    item.setPurchaseId(rs.getInt("purchase_id"));
                    item.setProductId(rs.getInt("product_id"));
                    item.setProductName(rs.getString("product_name"));
                    item.setProductBarcode(rs.getString("product_barcode"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setUnitCost(rs.getBigDecimal("unit_cost"));
                    item.setSubtotal(rs.getBigDecimal("subtotal"));
                    items.add(item);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching purchase items: " + e.getMessage(), e);
        }
        return items;
    }

    private Purchase mapRow(ResultSet rs) throws SQLException {
        Purchase p = new Purchase();
        p.setId(rs.getInt("id"));
        p.setPurchaseNumber(rs.getString("purchase_number"));
        p.setSupplierId(rs.getInt("supplier_id"));
        p.setSupplierName(rs.getString("supplier_name"));
        p.setUserId(rs.getInt("user_id"));
        p.setUserName(rs.getString("user_name"));
        p.setTotalAmount(rs.getBigDecimal("total_amount"));
        p.setStatus(rs.getString("status"));
        p.setNotes(rs.getString("notes"));
        p.setPurchaseDate(rs.getTimestamp("purchase_date"));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        return p;
    }
}

