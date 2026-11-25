package ui;

import config.DBConnection;
import exception.DatabaseOperationException;
import model.User;
import service.AuthService;

import javax.swing.*;
import java.awt.*;

/**
 * Main Entry Point for the Inventory and Business Management System Swing Desktop Application.
 */
public class SwingApp {

    private static AuthService authService;

    public static void main(String[] args) {
        // Enable modern font rendering and subpixel anti-aliasing
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(SwingApp::bootstrap);
    }

    private static void bootstrap() {
        // Pre-flight database connectivity test
        try {
            if (!DBConnection.testConnection()) {
                showDatabaseErrorAndExit("Database connection failed. Please verify MySQL service is running and credentials in database.local.properties are valid.");
                return;
            }
        } catch (DatabaseOperationException ex) {
            showDatabaseErrorAndExit("Database initialization failure:\n" + ex.getMessage());
            return;
        }

        authService = new AuthService();
        showLoginFlow();
    }

    private static void showLoginFlow() {
        LoginDialog loginDialog = new LoginDialog(null, authService);
        loginDialog.setVisible(true);

        User authenticatedUser = loginDialog.getAuthenticatedUser();
        if (authenticatedUser != null) {
            try {
                MainFrame mainFrame = new MainFrame(authenticatedUser, authService, SwingApp::showLoginFlow);
                mainFrame.setVisible(true);
            } catch (Throwable t) {
                t.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error launching application window:\n" + t.getMessage(), "Application Error", JOptionPane.ERROR_MESSAGE);
                showLoginFlow();
            }
        } else {
            // User closed dialog without logging in
            System.exit(0);
        }
    }

    private static void showDatabaseErrorAndExit(String message) {
        JOptionPane.showMessageDialog(
                null,
                message,
                "Database Connection Error",
                JOptionPane.ERROR_MESSAGE
        );
        System.exit(1);
    }
}

