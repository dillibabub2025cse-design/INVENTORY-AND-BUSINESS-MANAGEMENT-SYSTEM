package ui;

import exception.AppException;
import model.User;
import service.UserService;
import util.ConsoleFormatter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Modern Security Administration and User Account Management panel.
 */
public class UserManagementPanel extends JPanel {

    private final UserService userService;
    private final User currentUser;

    private JTable userTable;
    private DefaultTableModel tableModel;

    public UserManagementPanel(UserService userService, User currentUser) {
        this.userService = userService;
        this.currentUser = currentUser;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(14, 14));
        setBackground(Theme.APP_BG);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        // Security check
        if (currentUser == null || !currentUser.isAdmin()) {
            JPanel lockCard = new JPanel(new GridBagLayout());
            lockCard.setBackground(Theme.SURFACE);
            lockCard.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(Theme.BORDER, 1),
                    new EmptyBorder(40, 40, 40, 40)
            ));

            JPanel inner = new JPanel(new GridLayout(0, 1, 8, 8));
            inner.setOpaque(false);

            JLabel lockIcon = new JLabel("🔒", SwingConstants.CENTER);
            lockIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));

            JLabel title = new JLabel("Administrator Privileges Required", SwingConstants.CENTER);
            title.setFont(Theme.FONT_TITLE_LARGE);
            title.setForeground(Theme.DANGER);

            JLabel msg = new JLabel("Only system administrators can access user credential administration and RBAC settings.", SwingConstants.CENTER);
            msg.setFont(Theme.FONT_BODY);
            msg.setForeground(Theme.TEXT_MUTED);

            inner.add(lockIcon);
            inner.add(title);
            inner.add(msg);
            lockCard.add(inner);

            add(lockCard, BorderLayout.CENTER);
            return;
        }

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        JLabel title = new JLabel("User Security & RBAC Administration");
        title.setFont(Theme.FONT_TITLE_LARGE);
        title.setForeground(Theme.TEXT_PRIMARY);

        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionButtons.setOpaque(false);

        JButton addBtn = Theme.createPrimaryButton("+ New User Account");
        addBtn.addActionListener(e -> showAddUserDialog());

        JButton passBtn = Theme.createSecondaryButton("🔑 Reset Password");
        passBtn.addActionListener(e -> handleResetPassword());

        JButton toggleBtn = Theme.createSecondaryButton("🔄 Toggle Status");
        toggleBtn.addActionListener(e -> handleToggleStatus());

        JButton refreshBtn = Theme.createSecondaryButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> refreshTable());

        actionButtons.add(addBtn);
        actionButtons.add(passBtn);
        actionButtons.add(toggleBtn);
        actionButtons.add(refreshBtn);

        headerRow.add(title, BorderLayout.WEST);
        headerRow.add(actionButtons, BorderLayout.EAST);
        add(headerRow, BorderLayout.NORTH);

        String[] columns = {"ID", "Username", "Full Name", "Email Address", "Role", "Active Status", "Created On", "Last Modified"};
        tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        userTable = new JTable(tableModel);
        Theme.styleTable(userTable);
        userTable.getColumnModel().getColumn(0).setMaxWidth(60);

        JScrollPane scroll = new JScrollPane(userTable);
        scroll.setBorder(new LineBorder(Theme.BORDER, 1));
        add(scroll, BorderLayout.CENTER);

        refreshTable();
    }

    public void refreshTable() {
        if (currentUser == null || !currentUser.isAdmin()) return;
        tableModel.setRowCount(0);
        try {
            List<User> list = userService.getAllUsers();
            for (User u : list) {
                String status = u.isActive() ? "🟢 ACTIVE" : "🔴 DEACTIVATED";
                tableModel.addRow(new Object[]{
                        u.getId(),
                        u.getUsername(),
                        u.getFullName(),
                        u.getEmail(),
                        u.getRoleName() != null ? u.getRoleName() : (u.getRoleId() == 1 ? "ADMIN" : "STAFF"),
                        status,
                        ConsoleFormatter.formatDateTime(u.getCreatedAt()),
                        ConsoleFormatter.formatDateTime(u.getUpdatedAt())
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to load users: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showAddUserDialog() {
        JTextField userField = Theme.createTextField(15);
        JTextField nameField = Theme.createTextField(20);
        JTextField emailField = Theme.createTextField(20);
        JPasswordField passField = new JPasswordField(15);
        passField.setFont(Theme.FONT_BODY);

        JComboBox<String> roleCombo = new JComboBox<>(new String[]{"2 - STAFF", "1 - ADMIN"});
        roleCombo.setFont(Theme.FONT_BODY);

        JPanel p = new JPanel(new GridLayout(0, 1, 6, 6));
        p.add(new JLabel("Username:"));
        p.add(userField);
        p.add(new JLabel("Full Name:"));
        p.add(nameField);
        p.add(new JLabel("Email Address:"));
        p.add(emailField);
        p.add(new JLabel("Password (min 6 chars, PBKDF2 hashed):"));
        p.add(passField);
        p.add(new JLabel("Role:"));
        p.add(roleCombo);

        int res = JOptionPane.showConfirmDialog(this, p, "Create User Account", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            try {
                String username = userField.getText().trim();
                String fullname = nameField.getText().trim();
                String email = emailField.getText().trim();
                String password = new String(passField.getPassword());
                int roleId = roleCombo.getSelectedIndex() == 1 ? 1 : 2;

                boolean success = userService.createUser(username, password, fullname, email, roleId);
                if (success) {
                    JOptionPane.showMessageDialog(this, "User created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    refreshTable();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to create user: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleResetPassword() {
        int row = userTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a user account to reset password.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int userId = (int) tableModel.getValueAt(row, 0);
        String username = (String) tableModel.getValueAt(row, 1);

        JPasswordField newPassField = new JPasswordField(15);
        newPassField.setFont(Theme.FONT_BODY);

        JPanel p = new JPanel(new GridLayout(0, 1, 6, 6));
        p.add(new JLabel("Reset password for user: " + username));
        p.add(new JLabel("New Password (min 6 characters):"));
        p.add(newPassField);

        int res = JOptionPane.showConfirmDialog(this, p, "Reset Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            try {
                String newPass = new String(newPassField.getPassword());
                boolean success = userService.resetPassword(userId, newPass);
                if (success) {
                    JOptionPane.showMessageDialog(this, "Password for '" + username + "' has been reset using PBKDF2WithHmacSHA256.", "Success", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to reset password: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleToggleStatus() {
        int row = userTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a user account.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int userId = (int) tableModel.getValueAt(row, 0);
        String username = (String) tableModel.getValueAt(row, 1);
        String status = (String) tableModel.getValueAt(row, 5);

        if (currentUser.getId() == userId) {
            JOptionPane.showMessageDialog(this, "You cannot deactivate your own active session account.", "Action Prohibited", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean currentActive = status.contains("ACTIVE");
        boolean newActive = !currentActive;

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to " + (newActive ? "ACTIVATE" : "DEACTIVATE") + " user '" + username + "'?",
                "Confirm User Status Change",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean success = userService.toggleUserStatus(userId, newActive);
                if (success) {
                    JOptionPane.showMessageDialog(this, "User status updated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    refreshTable();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to update status: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

