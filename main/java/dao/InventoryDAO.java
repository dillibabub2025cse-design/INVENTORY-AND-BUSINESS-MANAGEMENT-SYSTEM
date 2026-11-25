package dao;

import config.DBConnection;
import exception.DatabaseOperationException;
import exception.ValidationException;
import model.StockMovement;
import model.StockMovementType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for Inventory operations, movements audit trail, and valuation.
 */
public class InventoryDAO {

    /**
     * Records a manual stock adjustment atomically and logs to stock_movements.
     */
    public boolean recordAdjustment(int productId, int qtyDelta, StockMovementType type, String reason, int userId) {
        String getStockSql = "SELECT name, current_stock FROM products WHERE id = ? FOR UPDATE";
        String updateStockSql = "UPDATE products SET current_stock = current_stock + ? WHERE id = ? AND (current_stock + ?) >= 0";
        String insertMovementSql = "INSERT INTO stock_movements (product_id, movement_type, quantity, previous_stock, new_stock, reference_id, notes, created_by) " +
                                   "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin Transaction

            int currentStock = 0;
            String prodName = "";
            try (PreparedStatement ps = conn.prepareStatement(getStockSql)) {
                ps.setInt(1, productId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new ValidationException("Product ID " + productId + " not found.");
                    }
                    prodName = rs.getString("name");
                    currentStock = rs.getInt("current_stock");
                }
            }

            int newStock = currentStock + qtyDelta;
            if (newStock < 0) {
                throw new ValidationException("Adjustment would cause negative stock for '" + prodName + 
                        "'. Current: " + currentStock + ", Delta: " + qtyDelta);
            }

            // Update stock
            try (PreparedStatement psUpdate = conn.prepareStatement(updateStockSql)) {
                psUpdate.setInt(1, qtyDelta);
                psUpdate.setInt(2, productId);
                psUpdate.setInt(3, qtyDelta);
                int affected = psUpdate.executeUpdate();
                if (affected == 0) {
                    throw new SQLException("Failed to update stock for Product ID " + productId);
                }
            }

            // Insert movement record
            try (PreparedStatement psMovement = conn.prepareStatement(insertMovementSql)) {
                psMovement.setInt(1, productId);
                psMovement.setString(2, type.name());
                psMovement.setInt(3, qtyDelta);
                psMovement.setInt(4, currentStock);
                psMovement.setInt(5, newStock);
                psMovement.setString(6, "ADJ-" + System.currentTimeMillis());
                psMovement.setString(7, reason);
                psMovement.setInt(8, userId);
                psMovement.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {}
            }
            throw new DatabaseOperationException("Error executing stock adjustment: " + e.getMessage(), e);
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
     * Retrieves stock movement history optionally filtered by product ID.
     */
    public List<StockMovement> getMovements(Integer productId, int limit) {
        List<StockMovement> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT sm.*, p.name AS product_name, p.barcode AS product_barcode, u.full_name AS user_name " +
            "FROM stock_movements sm " +
            "JOIN products p ON sm.product_id = p.id " +
            "LEFT JOIN users u ON sm.created_by = u.id "
        );
        if (productId != null) {
            sql.append("WHERE sm.product_id = ? ");
        }
        sql.append("ORDER BY sm.id DESC LIMIT ?");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int paramIdx = 1;
            if (productId != null) {
                ps.setInt(paramIdx++, productId);
            }
            ps.setInt(paramIdx, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StockMovement sm = new StockMovement();
                    sm.setId(rs.getInt("id"));
                    sm.setProductId(rs.getInt("product_id"));
                    sm.setProductName(rs.getString("product_name"));
                    sm.setProductBarcode(rs.getString("product_barcode"));
                    sm.setMovementType(StockMovementType.valueOf(rs.getString("movement_type")));
                    sm.setQuantity(rs.getInt("quantity"));
                    sm.setPreviousStock(rs.getInt("previous_stock"));
                    sm.setNewStock(rs.getInt("new_stock"));
                    sm.setReferenceId(rs.getString("reference_id"));
                    sm.setNotes(rs.getString("notes"));
                    sm.setCreatedByName(rs.getString("user_name"));
                    sm.setCreatedAt(rs.getTimestamp("created_at"));
                    list.add(sm);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching stock movements: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Calculates total inventory valuation based on purchase cost and retail price.
     */
    public Map<String, Object> getInventoryValuation() {
        Map<String, Object> metrics = new HashMap<>();
        String sql = "SELECT " +
                     "COUNT(*) AS total_items, " +
                     "SUM(current_stock) AS total_units, " +
                     "SUM(current_stock * purchase_price) AS total_cost_valuation, " +
                     "SUM(current_stock * selling_price) AS total_retail_valuation " +
                     "FROM products WHERE is_active = TRUE";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int totalItems = rs.getInt("total_items");
                int totalUnits = rs.getInt("total_units");
                BigDecimal costValuation = rs.getBigDecimal("total_cost_valuation");
                BigDecimal retailValuation = rs.getBigDecimal("total_retail_valuation");

                if (costValuation == null) costValuation = BigDecimal.ZERO;
                if (retailValuation == null) retailValuation = BigDecimal.ZERO;
                BigDecimal potentialProfit = retailValuation.subtract(costValuation);

                metrics.put("total_items", totalItems);
                metrics.put("total_units", totalUnits);
                metrics.put("cost_valuation", costValuation);
                metrics.put("retail_valuation", retailValuation);
                metrics.put("potential_profit", potentialProfit);
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error calculating inventory valuation: " + e.getMessage(), e);
        }
        return metrics;
    }
}

