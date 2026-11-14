package dao;

import config.DBConnection;
import exception.DatabaseOperationException;
import model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for users and credentials management.
 */
public class UserDAO {

    public User findByUsername(String username) {
        String sql = "SELECT u.*, r.role_name FROM users u " +
                     "JOIN roles r ON u.role_id = r.id " +
                     "WHERE u.username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching user by username: " + e.getMessage(), e);
        }
        return null;
    }

    public User findById(int id) {
        String sql = "SELECT u.*, r.role_name FROM users u " +
                     "JOIN roles r ON u.role_id = r.id " +
                     "WHERE u.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching user by ID: " + e.getMessage(), e);
        }
        return null;
    }

    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT u.*, r.role_name FROM users u " +
                     "JOIN roles r ON u.role_id = r.id " +
                     "ORDER BY u.id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error listing users: " + e.getMessage(), e);
        }
        return list;
    }

    public boolean create(User user) {
        String sql = "INSERT INTO users (username, password_hash, salt, full_name, email, role_id, is_active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getSalt());
            ps.setString(4, user.getFullName());
            ps.setString(5, user.getEmail());
            ps.setInt(6, user.getRoleId());
            ps.setBoolean(7, user.isActive());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        user.setId(keys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error creating user: " + e.getMessage(), e);
        }
        return false;
    }

    public boolean update(User user) {
        String sql = "UPDATE users SET full_name = ?, email = ?, role_id = ?, is_active = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setInt(3, user.getRoleId());
            ps.setBoolean(4, user.isActive());
            ps.setInt(5, user.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error updating user: " + e.getMessage(), e);
        }
    }

    public boolean updatePassword(int userId, String hash, String salt) {
        String sql = "UPDATE users SET password_hash = ?, salt = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setString(2, salt);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error updating password: " + e.getMessage(), e);
        }
    }

    public boolean toggleActive(int userId, boolean active) {
        String sql = "UPDATE users SET is_active = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error toggling user status: " + e.getMessage(), e);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setSalt(rs.getString("salt"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setRoleId(rs.getInt("role_id"));
        user.setRoleName(rs.getString("role_name"));
        user.setActive(rs.getBoolean("is_active"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        user.setUpdatedAt(rs.getTimestamp("updated_at"));
        return user;
    }
}

