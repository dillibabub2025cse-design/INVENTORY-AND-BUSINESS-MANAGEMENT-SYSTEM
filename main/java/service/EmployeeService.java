package service;

import dao.EmployeeDAO;
import dao.UserDAO;
import exception.ValidationException;
import model.Employee;
import model.User;
import util.PasswordUtil;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service managing Employees, departmental records, and linked user accounts.
 */
public class EmployeeService {

    private final EmployeeDAO employeeDAO;
    private final UserDAO userDAO;

    public EmployeeService() {
        this.employeeDAO = new EmployeeDAO();
        this.userDAO = new UserDAO();
    }

    public EmployeeService(EmployeeDAO employeeDAO, UserDAO userDAO) {
        this.employeeDAO = employeeDAO;
        this.userDAO = userDAO;
    }

    public List<Employee> getAllEmployees() {
        return employeeDAO.findAll();
    }

    public Employee getEmployeeById(int id) {
        Employee e = employeeDAO.findById(id);
        if (e == null) {
            throw new ValidationException("Employee with ID " + id + " does not exist.");
        }
        return e;
    }

    public boolean addEmployee(Employee emp) {
        validateEmployee(emp);
        return employeeDAO.create(emp);
    }

    public boolean updateEmployee(Employee emp) {
        validateEmployee(emp);
        return employeeDAO.update(emp);
    }

    public boolean setEmployeeStatus(int id, String status) {
        getEmployeeById(id);
        return employeeDAO.updateStatus(id, status.toUpperCase());
    }

    /**
     * Creates a login account for an employee and links the records.
     */
    public User createLoginForEmployee(int employeeId, String username, String rawPassword, int roleId) {
        Employee emp = getEmployeeById(employeeId);
        if (emp.getUserId() != null) {
            throw new ValidationException("Employee already has an associated user account (User ID " + emp.getUserId() + ").");
        }

        if (userDAO.findByUsername(username.trim()) != null) {
            throw new ValidationException("Username '" + username.trim() + "' is already taken.");
        }

        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(rawPassword, salt);

        User user = new User();
        user.setUsername(username.trim());
        user.setPasswordHash(hash);
        user.setSalt(salt);
        user.setFullName(emp.getFullName());
        user.setEmail(emp.getEmail());
        user.setRoleId(roleId);
        user.setActive(true);

        boolean userCreated = userDAO.create(user);
        if (userCreated) {
            emp.setUserId(user.getId());
            employeeDAO.update(emp);
        }
        return user;
    }

    private void validateEmployee(Employee e) {
        if (e.getFirstName() == null || e.getFirstName().trim().isEmpty()) {
            throw new ValidationException("First name is required.");
        }
        if (e.getLastName() == null || e.getLastName().trim().isEmpty()) {
            throw new ValidationException("Last name is required.");
        }
        if (e.getEmail() == null || e.getEmail().trim().isEmpty()) {
            throw new ValidationException("Email address is required.");
        }
        if (e.getPhone() == null || e.getPhone().trim().isEmpty()) {
            throw new ValidationException("Phone number is required.");
        }
        if (e.getSalary() == null || e.getSalary().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Salary must be non-negative.");
        }
    }
}

