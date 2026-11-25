package ui;

import exception.AppException;
import model.User;
import service.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Modern login authentication dialog for the Swing GUI.
 */
public class LoginDialog extends JDialog {

    private final AuthService authService;
    private User authenticatedUser = null;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel errorLabel;

    public LoginDialog(JFrame parent, AuthService authService) {
        super(parent, "Inventory & Business Management System - Login", true);
        this.authService = authService;
        initUI();
    }

    private void initUI() {
        setSize(460, 520);
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(Theme.APP_BG);
        setLayout(new BorderLayout());

        // Header Banner
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(Theme.SIDEBAR_BG);
        headerPanel.setBorder(new EmptyBorder(24, 20, 24, 20));
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("INVENTORY & ERP SYSTEM");
        titleLabel.setFont(Theme.FONT_TITLE_LARGE);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLabel = new JLabel("Enterprise Management Suite");
        subLabel.setFont(Theme.FONT_BODY);
        subLabel.setForeground(Theme.TEXT_MUTED);
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(subLabel);

        add(headerPanel, BorderLayout.NORTH);

        // Body Form Panel
        JPanel cardPanel = Theme.createCardPanel();
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));

        JLabel userLabel = new JLabel("Username");
        userLabel.setFont(Theme.FONT_HEADER);
        userLabel.setForeground(Theme.TEXT_PRIMARY);

        usernameField = Theme.createTextField(20);
        usernameField.setMaximumSize(new Dimension(380, 36));

        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(Theme.FONT_HEADER);
        passLabel.setForeground(Theme.TEXT_PRIMARY);

        passwordField = new JPasswordField(20);
        passwordField.setFont(Theme.FONT_BODY);
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Theme.BORDER, 1),
                new EmptyBorder(6, 10, 6, 10)
        ));
        passwordField.setMaximumSize(new Dimension(380, 36));

        errorLabel = new JLabel(" ");
        errorLabel.setFont(Theme.FONT_SMALL_BOLD);
        errorLabel.setForeground(Theme.DANGER);
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton loginBtn = Theme.createPrimaryButton("Sign In to System");
        loginBtn.setMaximumSize(new Dimension(380, 42));
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginBtn.addActionListener(e -> attemptLogin());

        // Enter key listener
        passwordField.addActionListener(e -> attemptLogin());
        usernameField.addActionListener(e -> passwordField.requestFocusInWindow());

        // Quick demo filler
        JPanel demoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        demoPanel.setBackground(Theme.CARD_BG);
        JLabel demoLabel = new JLabel("Demo Accounts:");
        demoLabel.setFont(Theme.FONT_SMALL);
        demoLabel.setForeground(Theme.TEXT_MUTED);

        JButton fillAdminBtn = new JButton("Admin");
        fillAdminBtn.setFont(Theme.FONT_SMALL);
        fillAdminBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        fillAdminBtn.addActionListener(e -> {
            usernameField.setText("admin");
            passwordField.setText("admin123");
            errorLabel.setText(" ");
        });

        JButton fillStaffBtn = new JButton("Staff");
        fillStaffBtn.setFont(Theme.FONT_SMALL);
        fillStaffBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        fillStaffBtn.addActionListener(e -> {
            usernameField.setText("staff");
            passwordField.setText("staff123");
            errorLabel.setText(" ");
        });

        demoPanel.add(demoLabel);
        demoPanel.add(fillAdminBtn);
        demoPanel.add(fillStaffBtn);

        cardPanel.add(Box.createVerticalStrut(10));
        cardPanel.add(userLabel);
        cardPanel.add(Box.createVerticalStrut(6));
        cardPanel.add(usernameField);
        cardPanel.add(Box.createVerticalStrut(16));
        cardPanel.add(passLabel);
        cardPanel.add(Box.createVerticalStrut(6));
        cardPanel.add(passwordField);
        cardPanel.add(Box.createVerticalStrut(12));
        cardPanel.add(errorLabel);
        cardPanel.add(Box.createVerticalStrut(12));
        cardPanel.add(loginBtn);
        cardPanel.add(Box.createVerticalStrut(20));
        cardPanel.add(demoPanel);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Theme.APP_BG);
        wrapper.setBorder(new EmptyBorder(20, 24, 20, 24));
        wrapper.add(cardPanel, BorderLayout.CENTER);

        add(wrapper, BorderLayout.CENTER);
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter both username and password.");
            return;
        }

        try {
            authenticatedUser = authService.login(username, password);
            dispose();
        } catch (Throwable ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : ex.toString();
            errorLabel.setText(msg);
            JOptionPane.showMessageDialog(this, msg, "Login Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    public User getAuthenticatedUser() {
        return authenticatedUser;
    }
}

