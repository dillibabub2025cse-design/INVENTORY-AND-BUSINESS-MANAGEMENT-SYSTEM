package dao;

import config.DBConnection;
import exception.DatabaseOperationException;
import exception.InsufficientStockException;
import exception.ValidationException;
import model.Sale;
import model.SaleItem;
import model.StockMovementType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Sales Orders, Invoice items, stock deductions, and
 * sales cancellations.
 */
public class SalesDAO {

    /**
     * Atomically creates a sale, verifies inventory stock, inserts sale items,
     * decreases product stock, and logs stock movements. Rollbacks on any failure.
     */
    public boolean createSale(Sale sale) {
        String insertSaleSql = "INSERT INTO sales (invoice_number, customer_id, user_id, subtotal, discount_amount, tax_amount, total_amount, payment_method, status, sale_date) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String insertItemSql = "INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, cost_price, subtotal) "
                +
                "VALUES (?, ?, ?, ?, ?, ?)";
        String getStockSql = "SELECT name, current_stock, purchase_price FROM products WHERE id = ? FOR UPDATE";
        String updateStockSql = "UPDATE products SET current_stock = current_stock - ? WHERE id = ? AND current_stock >= ?";
        String insertMovementSql = "INSERT INTO stock_movements (product_id, movement_type, quantity, previous_stock, new_stock, reference_id, notes, created_by) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin Transaction

            // 1. Validate Stock Availability for all items before any writes
            for (SaleItem item : sale.getItems()) {
                try (PreparedStatement psStock = conn.prepareStatement(getStockSql)) {
                    psStock.setInt(1, item.getProductId());
                    try (ResultSet rs = psStock.executeQuery()) {
                        if (!rs.next()) {
                            throw new ValidationException("Product ID " + item.getProductId() + " does not exist.");
                        }
                        String productName = rs.getString("name");
                        int availableStock = rs.getInt("current_stock");
                        if (availableStock < item.getQuantity()) {
                            throw new InsufficientStockException(
                                    "Insufficient stock for '" + productName + "'! Available: " + availableStock
                                            + ", Requested: " + item.getQuantity());
                        }
                        // Capture cost price for accurate profit calculation
                        if (item.getCostPrice() == null) {
                            item.setCostPrice(rs.getBigDecimal("purchase_price"));
                        }
                    }
                }
            }

            // 2. Insert Sale Header
            int saleId;
            try (PreparedStatement ps = conn.prepareStatement(insertSaleSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, sale.getInvoiceNumber());
                if (sale.getCustomerId() != null) {
                    ps.setInt(2, sale.getCustomerId());
                } else {
                    ps.setNull(2, Types.INTEGER);
                }
                ps.setInt(3, sale.getUserId());
                ps.setBigDecimal(4, sale.getSubtotal());
                ps.setBigDecimal(5, sale.getDiscountAmount());
                ps.setBigDecimal(6, sale.getTaxAmount());
                ps.setBigDecimal(7, sale.getTotalAmount());
                ps.setString(8, sale.getPaymentMethod());
                ps.setString(9, "COMPLETED");
                ps.setTimestamp(10, sale.getSaleDate());

                int affected = ps.executeUpdate();
                if (affected == 0) {
                    throw new SQLException("Failed to create sales header.");
                }
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        saleId = rs.getInt(1);
                        sale.setId(saleId);
                    } else {
                        throw new SQLException("Failed to obtain generated sale ID.");
                    }
                }
            }

            // 3. Insert Items, Deduct Stock, and Record Movement
            for (SaleItem item : sale.getItems()) {
                item.setSaleId(saleId);

                // Fetch current stock
                int currentStock = 0;
                try (PreparedStatement psStock = conn.prepareStatement(getStockSql)) {
                    psStock.setInt(1, item.getProductId());
                    try (ResultSet rs = psStock.executeQuery()) {
                        if (rs.next()) {
                            currentStock = rs.getInt("current_stock");
                        }
                    }
                }

                // Insert sale item
                try (PreparedStatement psItem = conn.prepareStatement(insertItemSql, Statement.RETURN_GENERATED_KEYS)) {
                    psItem.setInt(1, saleId);
                    psItem.setInt(2, item.getProductId());
                    psItem.setInt(3, item.getQuantity());
                    psItem.setBigDecimal(4, item.getUnitPrice());
                    psItem.setBigDecimal(5, item.getCostPrice());
                    psItem.setBigDecimal(6, item.getSubtotal());
                    psItem.executeUpdate();
                }

                // Deduct stock safely
                try (PreparedStatement psUpdate = conn.prepareStatement(updateStockSql)) {
                    psUpdate.setInt(1, item.getQuantity());
                    psUpdate.setInt(2, item.getProductId());
                    psUpdate.setInt(3, item.getQuantity());
                    int updated = psUpdate.executeUpdate();
                    if (updated == 0) {
                        throw new InsufficientStockException(
                                "Stock deduction race condition for Product ID " + item.getProductId());
                    }
                }

                // Log movement
                int newStock = currentStock - item.getQuantity();
                try (PreparedStatement psMovement = conn.prepareStatement(insertMovementSql)) {
                    psMovement.setInt(1, item.getProductId());
                    psMovement.setString(2, StockMovementType.SALE.name());
                    psMovement.setInt(3, -item.getQuantity());
                    psMovement.setInt(4, currentStock);
                    psMovement.setInt(5, newStock);
                    psMovement.setString(6, sale.getInvoiceNumber());
                    psMovement.setString(7, "Sale Invoice " + sale.getInvoiceNumber());
                    psMovement.setInt(8, sale.getUserId());
                    psMovement.executeUpdate();
                }
            }

            conn.commit(); // Transaction Succeeded
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            throw new DatabaseOperationException("Failed to complete sale transaction: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    /**
     * Cancels a sale and restores stock quantities atomically.
     */
    public boolean cancelSale(int saleId, int userId, String reason) {
        String getSaleSql = "SELECT * FROM sales WHERE id = ? FOR UPDATE";
        String getItemsSql = "SELECT * FROM sale_items WHERE sale_id = ?";
        String getStockSql = "SELECT current_stock FROM products WHERE id = ? FOR UPDATE";
        String updateStockSql = "UPDATE products SET current_stock = current_stock + ? WHERE id = ?";
        String updateSaleStatusSql = "UPDATE sales SET status = 'CANCELLED' WHERE id = ?";
        String insertMovementSql = "INSERT INTO stock_movements (product_id, movement_type, quantity, previous_stock, new_stock, reference_id, notes, created_by) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin Transaction

            // 1. Verify Sale and atomically lock & mark as CANCELLED to prevent double
            // restoration
            String invoiceNumber;
            try (PreparedStatement ps = conn.prepareStatement(getSaleSql)) {
                ps.setInt(1, saleId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new ValidationException("Sale Invoice ID #" + saleId + " not found.");
                    }
                    String currentStatus = rs.getString("status");
                    if (!"COMPLETED".equalsIgnoreCase(currentStatus)) {
                        throw new ValidationException(
                                "Sale Invoice #" + saleId + " cannot be cancelled because its status is '"
                                        + currentStatus + "'. Only COMPLETED sales can be cancelled.");
                    }
                    invoiceNumber = rs.getString("invoice_number");
                }
            }

            // Atomically update status to CANCELLED conditional on status = 'COMPLETED'
            String markCancelledSql = "UPDATE sales SET status = 'CANCELLED' WHERE id = ? AND status = 'COMPLETED'";
            try (PreparedStatement ps = conn.prepareStatement(markCancelledSql)) {
                ps.setInt(1, saleId);
                int affected = ps.executeUpdate();
                if (affected == 0) {
                    throw new ValidationException(
                            "Sale Invoice #" + saleId + " could not be cancelled. It may have already been cancelled.");
                }
            }

            // 2. Fetch Items
            List<SaleItem> items = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(getItemsSql)) {
                ps.setInt(1, saleId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        SaleItem item = new SaleItem();
                        item.setProductId(rs.getInt("product_id"));
                        item.setQuantity(rs.getInt("quantity"));
                        items.add(item);
                    }
                }
            }

            // 3. Restore Stock and log movements
            for (SaleItem item : items) {
                int currentStock = 0;
                try (PreparedStatement psStock = conn.prepareStatement(getStockSql)) {
                    psStock.setInt(1, item.getProductId());
                    try (ResultSet rs = psStock.executeQuery()) {
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

                int newStock = currentStock + item.getQuantity();
                try (PreparedStatement psMovement = conn.prepareStatement(insertMovementSql)) {
                    psMovement.setInt(1, item.getProductId());
                    psMovement.setString(2, StockMovementType.SALE_CANCELLED.name());
                    psMovement.setInt(3, item.getQuantity());
                    psMovement.setInt(4, currentStock);
                    psMovement.setInt(5, newStock);
                    psMovement.setString(6, invoiceNumber);
                    psMovement.setString(7, "Sale Cancellation: " + reason);
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
                } catch (SQLException ignored) {
                }
            }
            throw new DatabaseOperationException("Error cancelling sale: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    public List<Sale> findAll() {
        List<Sale> list = new ArrayList<>();
        String sql = "SELECT s.*, c.name AS customer_name, u.full_name AS user_name " +
                "FROM sales s " +
                "LEFT JOIN customers c ON s.customer_id = c.id " +
                "JOIN users u ON s.user_id = u.id " +
                "ORDER BY s.id DESC";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching sales list: " + e.getMessage(), e);
        }
        return list;
    }

    public Sale findById(int id) {
        String sql = "SELECT s.*, c.name AS customer_name, u.full_name AS user_name " +
                "FROM sales s " +
                "LEFT JOIN customers c ON s.customer_id = c.id " +
                "JOIN users u ON s.user_id = u.id " +
                "WHERE s.id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Sale s = mapRow(rs);
                    s.setItems(findItemsBySaleId(s.getId()));
                    return s;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching sale by ID: " + e.getMessage(), e);
        }
        return null;
    }

    public Sale findByInvoiceNumber(String invoiceNumber) {
        String sql = "SELECT s.*, c.name AS customer_name, u.full_name AS user_name " +
                "FROM sales s " +
                "LEFT JOIN customers c ON s.customer_id = c.id " +
                "JOIN users u ON s.user_id = u.id " +
                "WHERE s.invoice_number = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, invoiceNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Sale s = mapRow(rs);
                    s.setItems(findItemsBySaleId(s.getId()));
                    return s;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching sale by invoice: " + e.getMessage(), e);
        }
        return null;
    }

    public List<SaleItem> findItemsBySaleId(int saleId) {
        List<SaleItem> items = new ArrayList<>();
        String sql = "SELECT si.*, pr.name AS product_name, pr.barcode AS product_barcode " +
                "FROM sale_items si " +
                "JOIN products pr ON si.product_id = pr.id " +
                "WHERE si.sale_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, saleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SaleItem item = new SaleItem();
                    item.setId(rs.getInt("id"));
                    item.setSaleId(rs.getInt("sale_id"));
                    item.setProductId(rs.getInt("product_id"));
                    item.setProductName(rs.getString("product_name"));
                    item.setProductBarcode(rs.getString("product_barcode"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setUnitPrice(rs.getBigDecimal("unit_price"));
                    item.setCostPrice(rs.getBigDecimal("cost_price"));
                    item.setSubtotal(rs.getBigDecimal("subtotal"));
                    items.add(item);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching sale items: " + e.getMessage(), e);
        }
        return items;
    }

    public List<Sale> getCustomerPurchaseHistory(int customerId) {
        List<Sale> list = new ArrayList<>();
        String sql = "SELECT s.*, c.name AS customer_name, u.full_name AS user_name " +
                "FROM sales s " +
                "JOIN customers c ON s.customer_id = c.id " +
                "JOIN users u ON s.user_id = u.id " +
                "WHERE s.customer_id = ? " +
                "ORDER BY s.id DESC";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching customer history: " + e.getMessage(), e);
        }
        return list;
    }

    private Sale mapRow(ResultSet rs) throws SQLException {
        Sale s = new Sale();
        s.setId(rs.getInt("id"));
        s.setInvoiceNumber(rs.getString("invoice_number"));
        int custId = rs.getInt("customer_id");
        if (!rs.wasNull()) {
            s.setCustomerId(custId);
            s.setCustomerName(rs.getString("customer_name"));
        } else {
            s.setCustomerName("Walk-in Customer");
        }
        s.setUserId(rs.getInt("user_id"));
        s.setUserName(rs.getString("user_name"));
        s.setSubtotal(rs.getBigDecimal("subtotal"));
        s.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        s.setTaxAmount(rs.getBigDecimal("tax_amount"));
        s.setTotalAmount(rs.getBigDecimal("total_amount"));
        s.setPaymentMethod(rs.getString("payment_method"));
        s.setStatus(rs.getString("status"));
        s.setSaleDate(rs.getTimestamp("sale_date"));
        s.setCreatedAt(rs.getTimestamp("created_at"));
        return s;
    }
}
