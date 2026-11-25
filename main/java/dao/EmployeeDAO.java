package dao;

import config.DBConnection;
import exception.DatabaseOperationException;
import model.Employee;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Employee records.
 */
public class EmployeeDAO {

    public List<Employee> findAll() {
        List<Employee> list = new ArrayList<>();
        String sql = "SELECT e.*, u.username FROM employees e " +
                     "LEFT JOIN users u ON e.user_id = u.id " +
                     "ORDER BY e.id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error retrieving employees: " + e.getMessage(), e);
        }
        return list;
    }

    public Employee findById(int id) {
        String sql = "SELECT e.*, u.username FROM employees e " +
                     "LEFT JOIN users u ON e.user_id = u.id " +
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
            throw new DatabaseOperationException("Error retrieving employee: " + e.getMessage(), e);
        }
        return null;
    }

    public Employee findByUserId(int userId) {
        String sql = "SELECT e.*, u.username FROM employees e " +
                     "LEFT JOIN users u ON e.user_id = u.id " +
                     "WHERE e.user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error retrieving employee by user ID: " + e.getMessage(), e);
        }
        return null;
    }

    public boolean create(Employee emp) {
        String sql = "INSERT INTO employees (user_id, first_name, last_name, email, phone, department, designation, salary, hire_date, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (emp.getUserId() != null) {
                ps.setInt(1, emp.getUserId());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, emp.getFirstName());
            ps.setString(3, emp.getLastName());
            ps.setString(4, emp.getEmail());
            ps.setString(5, emp.getPhone());
            ps.setString(6, emp.getDepartment());
            ps.setString(7, emp.getDesignation());
            ps.setBigDecimal(8, emp.getSalary());
            ps.setDate(9, emp.getHireDate());
            ps.setString(10, emp.getStatus());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        emp.setId(keys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error creating employee: " + e.getMessage(), e);
        }
        return false;
    }

    public boolean update(Employee emp) {
        String sql = "UPDATE employees SET user_id = ?, first_name = ?, last_name = ?, email = ?, phone = ?, " +
                     "department = ?, designation = ?, salary = ?, hire_date = ?, status = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (emp.getUserId() != null) {
                ps.setInt(1, emp.getUserId());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, emp.getFirstName());
            ps.setString(3, emp.getLastName());
            ps.setString(4, emp.getEmail());
            ps.setString(5, emp.getPhone());
            ps.setString(6, emp.getDepartment());
            ps.setString(7, emp.getDesignation());
            ps.setBigDecimal(8, emp.getSalary());
            ps.setDate(9, emp.getHireDate());
            ps.setString(10, emp.getStatus());
            ps.setInt(11, emp.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error updating employee: " + e.getMessage(), e);
        }
    }

    public boolean updateStatus(int empId, String status) {
        String sql = "UPDATE employees SET status = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, empId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error updating employee status: " + e.getMessage(), e);
        }
    }

    private Employee mapRow(ResultSet rs) throws SQLException {
        Employee emp = new Employee();
        emp.setId(rs.getInt("id"));
        int userId = rs.getInt("user_id");
        if (!rs.wasNull()) {
            emp.setUserId(userId);
            emp.setUsername(rs.getString("username"));
        }
        emp.setFirstName(rs.getString("first_name"));
        emp.setLastName(rs.getString("last_name"));
        emp.setEmail(rs.getString("email"));
        emp.setPhone(rs.getString("phone"));
        emp.setDepartment(rs.getString("department"));
        emp.setDesignation(rs.getString("designation"));
        emp.setSalary(rs.getBigDecimal("salary"));
        emp.setHireDate(rs.getDate("hire_date"));
        emp.setStatus(rs.getString("status"));
        emp.setCreatedAt(rs.getTimestamp("created_at"));
        return emp;
    }
}

