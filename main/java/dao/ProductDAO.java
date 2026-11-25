package dao;

import config.DBConnection;
import exception.DatabaseOperationException;
import model.Product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Products and stock levels.
 */
public class ProductDAO {

    public List<Product> findAll() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.*, c.name AS category_name FROM products p " +
                     "JOIN categories c ON p.category_id = c.id " +
                     "ORDER BY p.id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error listing products: " + e.getMessage(), e);
        }
        return list;
    }

    public Product findById(int id) {
        String sql = "SELECT p.*, c.name AS category_name FROM products p " +
                     "JOIN categories c ON p.category_id = c.id " +
                     "WHERE p.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching product by ID: " + e.getMessage(), e);
        }
        return null;
    }

    public Product findByBarcode(String barcode) {
        String sql = "SELECT p.*, c.name AS category_name FROM products p " +
                     "JOIN categories c ON p.category_id = c.id " +
                     "WHERE p.barcode = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, barcode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching product by barcode: " + e.getMessage(), e);
        }
        return null;
    }

    public List<Product> search(String keyword) {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.*, c.name AS category_name FROM products p " +
                     "JOIN categories c ON p.category_id = c.id " +
                     "WHERE p.barcode LIKE ? OR LOWER(p.name) LIKE ? OR LOWER(c.name) LIKE ? " +
                     "ORDER BY p.name ASC";
        String pattern = "%" + keyword.toLowerCase() + "%";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error searching products: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Product> findLowStock() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.*, c.name AS category_name FROM products p " +
                     "JOIN categories c ON p.category_id = c.id " +
                     "WHERE p.current_stock <= p.min_stock_level AND p.current_stock > 0 AND p.is_active = TRUE " +
                     "ORDER BY p.current_stock ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching low-stock products: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Product> findOutOfStock() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.*, c.name AS category_name FROM products p " +
                     "JOIN categories c ON p.category_id = c.id " +
                     "WHERE p.current_stock <= 0 AND p.is_active = TRUE " +
                     "ORDER BY p.name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching out-of-stock products: " + e.getMessage(), e);
        }
        return list;
    }

    public boolean create(Product product) {
        String sql = "INSERT INTO products (barcode, name, category_id, description, purchase_price, selling_price, current_stock, min_stock_level, is_active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, product.getBarcode());
            ps.setString(2, product.getName());
            ps.setInt(3, product.getCategoryId());
            ps.setString(4, product.getDescription());
            ps.setBigDecimal(5, product.getPurchasePrice());
            ps.setBigDecimal(6, product.getSellingPrice());
            ps.setInt(7, product.getCurrentStock());
            ps.setInt(8, product.getMinStockLevel());
            ps.setBoolean(9, product.isActive());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        product.setId(keys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error creating product: " + e.getMessage(), e);
        }
        return false;
    }

    public boolean update(Product product) {
        String sql = "UPDATE products SET barcode = ?, name = ?, category_id = ?, description = ?, " +
                     "purchase_price = ?, selling_price = ?, min_stock_level = ?, is_active = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, product.getBarcode());
            ps.setString(2, product.getName());
            ps.setInt(3, product.getCategoryId());
            ps.setString(4, product.getDescription());
            ps.setBigDecimal(5, product.getPurchasePrice());
            ps.setBigDecimal(6, product.getSellingPrice());
            ps.setInt(7, product.getMinStockLevel());
            ps.setBoolean(8, product.isActive());
            ps.setInt(9, product.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error updating product: " + e.getMessage(), e);
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM products WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error deleting product: " + e.getMessage(), e);
        }
    }

    public boolean hasTransactions(int productId) {
        String sql = "SELECT " +
                     "(SELECT COUNT(*) FROM purchase_items WHERE product_id = ?) + " +
                     "(SELECT COUNT(*) FROM sale_items WHERE product_id = ?) AS total";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setInt(2, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total") > 0;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error checking product transactions: " + e.getMessage(), e);
        }
        return false;
    }

    public static boolean updateStock(Connection conn, int productId, int delta) throws SQLException {
        String sql = "UPDATE products SET current_stock = current_stock + ? WHERE id = ? AND (current_stock + ?) >= 0";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setInt(2, productId);
            ps.setInt(3, delta);
            return ps.executeUpdate() > 0;
        }
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getInt("id"));
        p.setBarcode(rs.getString("barcode"));
        p.setName(rs.getString("name"));
        p.setCategoryId(rs.getInt("category_id"));
        p.setCategoryName(rs.getString("category_name"));
        p.setDescription(rs.getString("description"));
        p.setPurchasePrice(rs.getBigDecimal("purchase_price"));
        p.setSellingPrice(rs.getBigDecimal("selling_price"));
        p.setCurrentStock(rs.getInt("current_stock"));
        p.setMinStockLevel(rs.getInt("min_stock_level"));
        p.setActive(rs.getBoolean("is_active"));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        p.setUpdatedAt(rs.getTimestamp("updated_at"));
        return p;
    }
}

