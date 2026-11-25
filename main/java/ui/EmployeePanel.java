package ui;

import exception.AppException;
import model.Employee;
import model.User;
import service.EmployeeService;
import util.ConsoleFormatter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

/**
 * Modern Human Resources and Employee Management panel.
 */
public class EmployeePanel extends JPanel {

    private final EmployeeService employeeService;
    private final User currentUser;

    private JTable employeeTable;
    private DefaultTableModel tableModel;

    public EmployeePanel(EmployeeService employeeService, User currentUser) {
        this.employeeService = employeeService;
        this.currentUser = currentUser;
        initUI();
        refreshTable();
    }

    private void initUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(Theme.APP_BG);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        JLabel title = new JLabel("Employee & Staff Records");
        title.setFont(Theme.FONT_TITLE_LARGE);
        title.setForeground(Theme.TEXT_PRIMARY);

        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionButtons.setOpaque(false);

        JButton addBtn = Theme.createPrimaryButton("+ Add Employee");
        addBtn.addActionListener(e -> showAddEmployeeDialog());

        JButton editBtn = Theme.createSecondaryButton("✏ Edit");
        editBtn.addActionListener(e -> showEditEmployeeDialog());

        JButton statusBtn = Theme.createSecondaryButton("🔄 Change Status");
        statusBtn.addActionListener(e -> handleStatusChange());

        JButton loginBtn = Theme.createPrimaryButton("🔑 Provision Login");
        loginBtn.addActionListener(e -> handleProvisionLogin());

        JButton refreshBtn = Theme.createSecondaryButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> refreshTable());

        actionButtons.add(addBtn);
        actionButtons.add(editBtn);
        actionButtons.add(statusBtn);
        actionButtons.add(loginBtn);
        actionButtons.add(refreshBtn);

        headerRow.add(title, BorderLayout.WEST);
        headerRow.add(actionButtons, BorderLayout.EAST);
        add(headerRow, BorderLayout.NORTH);

        String[] columns = {"ID", "Full Name", "Email", "Phone", "Department", "Designation", "Salary", "Hire Date", "Status", "User Account"};
        tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        employeeTable = new JTable(tableModel);
        Theme.styleTable(employeeTable);
        employeeTable.getColumnModel().getColumn(0).setMaxWidth(60);

        JScrollPane scroll = new JScrollPane(employeeTable);
        scroll.setBorder(new LineBorder(Theme.BORDER, 1));
        add(scroll, BorderLayout.CENTER);
    }

    public void refreshTable() {
        tableModel.setRowCount(0);
        try {
            List<Employee> list = employeeService.getAllEmployees();
            for (Employee e : list) {
                String userDisplay = e.getUsername() != null ? "✔ " + e.getUsername() : "❌ Unlinked";
                tableModel.addRow(new Object[]{
                        e.getId(),
                        e.getFullName(),
                        e.getEmail(),
                        e.getPhone(),
                        e.getDepartment(),
                        e.getDesignation(),
                        ConsoleFormatter.formatCurrency(e.getSalary()),
                        e.getHireDate() != null ? e.getHireDate().toString() : "-",
                        e.getStatus(),
                        userDisplay
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to load employees: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showAddEmployeeDialog() {
        JTextField firstNameField = Theme.createTextField(15);
        JTextField lastNameField = Theme.createTextField(15);
        JTextField emailField = Theme.createTextField(20);
        JTextField phoneField = Theme.createTextField(15);
        JTextField deptField = Theme.createTextField(15);
        JTextField desigField = Theme.createTextField(15);
        JTextField salaryField = Theme.createTextField(10);
        JTextField hireDateField = Theme.createTextField(10);
        hireDateField.setText(LocalDate.now().toString());

        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"ACTIVE", "ON_LEAVE", "TERMINATED"});
        statusCombo.setFont(Theme.FONT_BODY);

        JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
        p.add(new JLabel("First Name:"));
        p.add(firstNameField);
        p.add(new JLabel("Last Name:"));
        p.add(lastNameField);
        p.add(new JLabel("Email Address:"));
        p.add(emailField);
        p.add(new JLabel("Phone Number:"));
        p.add(phoneField);
        p.add(new JLabel("Department:"));
        p.add(deptField);
        p.add(new JLabel("Designation:"));
        p.add(desigField);
        p.add(new JLabel("Monthly Salary:"));
        p.add(salaryField);
        p.add(new JLabel("Hire Date (YYYY-MM-DD):"));
        p.add(hireDateField);
        p.add(new JLabel("Status:"));
        p.add(statusCombo);

        int res = JOptionPane.showConfirmDialog(this, p, "Register New Employee", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            try {
                Employee emp = new Employee();
                emp.setFirstName(firstNameField.getText().trim());
                emp.setLastName(lastNameField.getText().trim());
                emp.setEmail(emailField.getText().trim());
                emp.setPhone(phoneField.getText().trim());
                emp.setDepartment(deptField.getText().trim());
                emp.setDesignation(desigField.getText().trim());
                emp.setSalary(new BigDecimal(salaryField.getText().trim()));
                emp.setHireDate(Date.valueOf(hireDateField.getText().trim()));
                emp.setStatus((String) statusCombo.getSelectedItem());

                boolean success = employeeService.addEmployee(emp);
                if (success) {
                    JOptionPane.showMessageDialog(this, "Employee registered successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    refreshTable();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Registration failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showEditEmployeeDialog() {
        int row = employeeTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an employee to edit.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int empId = (int) tableModel.getValueAt(row, 0);
        Employee emp = employeeService.getEmployeeById(empId);
        if (emp == null) return;

        JTextField firstNameField = Theme.createTextField(15);
        firstNameField.setText(emp.getFirstName());
        JTextField lastNameField = Theme.createTextField(15);
        lastNameField.setText(emp.getLastName());
        JTextField emailField = Theme.createTextField(20);
        emailField.setText(emp.getEmail());
        JTextField phoneField = Theme.createTextField(15);
        phoneField.setText(emp.getPhone());
        JTextField deptField = Theme.createTextField(15);
        deptField.setText(emp.getDepartment());
        JTextField desigField = Theme.createTextField(15);
        desigField.setText(emp.getDesignation());
        JTextField salaryField = Theme.createTextField(10);
        salaryField.setText(emp.getSalary() != null ? emp.getSalary().toString() : "0.00");
        JTextField hireDateField = Theme.createTextField(10);
        hireDateField.setText(emp.getHireDate() != null ? emp.getHireDate().toString() : LocalDate.now().toString());

        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"ACTIVE", "ON_LEAVE", "TERMINATED"});
        statusCombo.setFont(Theme.FONT_BODY);
        statusCombo.setSelectedItem(emp.getStatus());

        JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
        p.add(new JLabel("First Name:"));
        p.add(firstNameField);
        p.add(new JLabel("Last Name:"));
        p.add(lastNameField);
        p.add(new JLabel("Email Address:"));
        p.add(emailField);
        p.add(new JLabel("Phone Number:"));
        p.add(phoneField);
        p.add(new JLabel("Department:"));
        p.add(deptField);
        p.add(new JLabel("Designation:"));
        p.add(desigField);
        p.add(new JLabel("Monthly Salary:"));
        p.add(salaryField);
        p.add(new JLabel("Hire Date (YYYY-MM-DD):"));
        p.add(hireDateField);
        p.add(new JLabel("Status:"));
        p.add(statusCombo);

        int res = JOptionPane.showConfirmDialog(this, p, "Edit Employee Details", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            try {
                emp.setFirstName(firstNameField.getText().trim());
                emp.setLastName(lastNameField.getText().trim());
                emp.setEmail(emailField.getText().trim());
                emp.setPhone(phoneField.getText().trim());
                emp.setDepartment(deptField.getText().trim());
                emp.setDesignation(desigField.getText().trim());
                emp.setSalary(new BigDecimal(salaryField.getText().trim()));
                emp.setHireDate(Date.valueOf(hireDateField.getText().trim()));
                emp.setStatus((String) statusCombo.getSelectedItem());

                boolean success = employeeService.updateEmployee(emp);
                if (success) {
                    JOptionPane.showMessageDialog(this, "Employee updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    refreshTable();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Update failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleStatusChange() {
        int row = employeeTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an employee.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int empId = (int) tableModel.getValueAt(row, 0);
        String[] options = {"ACTIVE", "ON_LEAVE", "TERMINATED"};
        String current = (String) tableModel.getValueAt(row, 8);

        String chosen = (String) JOptionPane.showInputDialog(
                this,
                "Select new employee status:",
                "Update Status",
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                current
        );

        if (chosen != null && !chosen.equals(current)) {
            try {
                employeeService.setEmployeeStatus(empId, chosen);
                JOptionPane.showMessageDialog(this, "Employee status updated to " + chosen, "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshTable();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to update status: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleProvisionLogin() {
        int row = employeeTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an employee to provision a login for.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int empId = (int) tableModel.getValueAt(row, 0);
        Employee emp = employeeService.getEmployeeById(empId);
        if (emp.getUserId() != null) {
            JOptionPane.showMessageDialog(this, "This employee already has a linked login account (" + emp.getUsername() + ").", "Already Provisioned", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JTextField usernameField = Theme.createTextField(15);
        usernameField.setText((emp.getFirstName() + "." + emp.getLastName()).toLowerCase().replaceAll("\\s+", ""));
        JPasswordField passwordField = new JPasswordField(15);
        passwordField.setFont(Theme.FONT_BODY);

        JComboBox<String> roleCombo = new JComboBox<>(new String[]{"2 - STAFF", "1 - ADMIN"});
        roleCombo.setFont(Theme.FONT_BODY);

        JPanel p = new JPanel(new GridLayout(0, 1, 6, 6));
        p.add(new JLabel("Provision login for: " + emp.getFullName()));
        p.add(new JLabel("Desired Username:"));
        p.add(usernameField);
        p.add(new JLabel("Initial Password (min 6 characters):"));
        p.add(passwordField);
        p.add(new JLabel("Access Role:"));
        p.add(roleCombo);

        int res = JOptionPane.showConfirmDialog(this, p, "Provision User Credentials", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            try {
                String username = usernameField.getText().trim();
                String pass = new String(passwordField.getPassword());
                int roleId = roleCombo.getSelectedIndex() == 1 ? 1 : 2;

                User newUser = employeeService.createLoginForEmployee(empId, username, pass, roleId);
                JOptionPane.showMessageDialog(this, "User account successfully created and linked!\nUsername: " + newUser.getUsername(), "Provisioned", JOptionPane.INFORMATION_MESSAGE);
                refreshTable();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Provisioning failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

