package dao;

import config.DBConnection;
import exception.DatabaseOperationException;
import model.Expense;
import model.ExpenseCategory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Business Expenses and Expense Categories.
 */
public class ExpenseDAO {

    public List<Expense> findAll() {
        List<Expense> list = new ArrayList<>();
        String sql = "SELECT e.*, c.name AS category_name, u.full_name AS user_name " +
                     "FROM expenses e " +
                     "JOIN expense_categories c ON e.category_id = c.id " +
                     "JOIN users u ON e.user_id = u.id " +
                     "ORDER BY e.expense_date DESC, e.id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error listing expenses: " + e.getMessage(), e);
        }
        return list;
    }

    public Expense findById(int id) {
        String sql = "SELECT e.*, c.name AS category_name, u.full_name AS user_name " +
                     "FROM expenses e " +
                     "JOIN expense_categories c ON e.category_id = c.id " +
                     "JOIN users u ON e.user_id = u.id " +
                     "WHERE e.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching expense by ID: " + e.getMessage(), e);
        }
        return null;
    }

    public List<Expense> findByDateRange(Date start, Date end) {
        List<Expense> list = new ArrayList<>();
        String sql = "SELECT e.*, c.name AS category_name, u.full_name AS user_name " +
                     "FROM expenses e " +
                     "JOIN expense_categories c ON e.category_id = c.id " +
                     "JOIN users u ON e.user_id = u.id " +
                     "WHERE e.expense_date BETWEEN ? AND ? " +
                     "ORDER BY e.expense_date ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, start);
            ps.setDate(2, end);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching expenses by date range: " + e.getMessage(), e);
        }
        return list;
    }

    public boolean create(Expense expense) {
        String sql = "INSERT INTO expenses (title, category_id, amount, expense_date, notes, user_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, expense.getTitle());
            ps.setInt(2, expense.getCategoryId());
            ps.setBigDecimal(3, expense.getAmount());
            ps.setDate(4, expense.getExpenseDate());
            ps.setString(5, expense.getNotes());
            ps.setInt(6, expense.getUserId());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        expense.setId(keys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error recording expense: " + e.getMessage(), e);
        }
        return false;
    }

    public boolean update(Expense expense) {
        String sql = "UPDATE expenses SET title = ?, category_id = ?, amount = ?, expense_date = ?, notes = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, expense.getTitle());
            ps.setInt(2, expense.getCategoryId());
            ps.setBigDecimal(3, expense.getAmount());
            ps.setDate(4, expense.getExpenseDate());
            ps.setString(5, expense.getNotes());
            ps.setInt(6, expense.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error updating expense: " + e.getMessage(), e);
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM expenses WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error deleting expense: " + e.getMessage(), e);
        }
    }

    public BigDecimal getTotalExpenses(Date start, Date end) {
        String sql = "SELECT SUM(amount) FROM expenses WHERE expense_date BETWEEN ? AND ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, start);
            ps.setDate(2, end);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal total = rs.getBigDecimal(1);
                    return total != null ? total : BigDecimal.ZERO;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error calculating total expenses: " + e.getMessage(), e);
        }
        return BigDecimal.ZERO;
    }

    public List<ExpenseCategory> findAllCategories() {
        List<ExpenseCategory> list = new ArrayList<>();
        String sql = "SELECT * FROM expense_categories ORDER BY id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ExpenseCategory ec = new ExpenseCategory();
                ec.setId(rs.getInt("id"));
                ec.setName(rs.getString("name"));
                ec.setDescription(rs.getString("description"));
                list.add(ec);
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching expense categories: " + e.getMessage(), e);
        }
        return list;
    }

    public boolean createCategory(String name, String description) {
        String sql = "INSERT INTO expense_categories (name, description) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, description);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error creating expense category: " + e.getMessage(), e);
        }
    }

    private Expense mapRow(ResultSet rs) throws SQLException {
        Expense e = new Expense();
        e.setId(rs.getInt("id"));
        e.setTitle(rs.getString("title"));
        e.setCategoryId(rs.getInt("category_id"));
        e.setCategoryName(rs.getString("category_name"));
        e.setAmount(rs.getBigDecimal("amount"));
        e.setExpenseDate(rs.getDate("expense_date"));
        e.setNotes(rs.getString("notes"));
        e.setUserId(rs.getInt("user_id"));
        e.setUserName(rs.getString("user_name"));
        e.setCreatedAt(rs.getTimestamp("created_at"));
        return e;
    }
}

