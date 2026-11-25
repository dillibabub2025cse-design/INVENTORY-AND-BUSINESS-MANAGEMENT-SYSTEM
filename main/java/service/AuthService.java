package service;

import dao.UserDAO;
import exception.AuthenticationException;
import exception.ValidationException;
import model.User;
import util.PasswordUtil;

/**
 * Service managing user authentication, sessions, role-based permissions,
 * and seamless cryptographic password upgrade to PBKDF2WithHmacSHA256.
 */
public class AuthService {

    private final UserDAO userDAO;
    private User currentUser;

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Authenticates a user using username and raw password.
     * Verifies against PBKDF2WithHmacSHA256. If a legacy SHA-256 hash is detected,
     * it transparently upgrades the record to PBKDF2 without user disruption.
     * Plaintext fallback is strictly prohibited.
     */
    public User login(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new ValidationException("Username and password cannot be empty.");
        }

        User user = userDAO.findByUsername(username.trim());
        if (user == null) {
            throw new AuthenticationException("Invalid username or password.");
        }

        if (!user.isActive()) {
            throw new AuthenticationException("Account is deactivated. Please contact the system administrator.");
        }

        // Primary: Verify using PBKDF2WithHmacSHA256
        boolean valid = PasswordUtil.verifyPassword(password, user.getSalt(), user.getPasswordHash());

        // Secondary: If not valid with PBKDF2, check for legacy SHA-256 hash to migrate
        if (!valid && PasswordUtil.verifyLegacySha256(password, user.getSalt(), user.getPasswordHash())) {
            // Transparently migrate to PBKDF2WithHmacSHA256
            String newSalt = PasswordUtil.generateSalt();
            String newPbkdf2Hash = PasswordUtil.hashPassword(password, newSalt);
            userDAO.updatePassword(user.getId(), newPbkdf2Hash, newSalt);
            user.setPasswordHash(newPbkdf2Hash);
            user.setSalt(newSalt);
            valid = true;
        }

        if (!valid) {
            throw new AuthenticationException("Invalid username or password.");
        }

        this.currentUser = user;
        return user;
    }

    public void logout() {
        this.currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    public void requireAdmin() {
        if (!isAdmin()) {
            throw new AuthenticationException("Access Denied: Administrator role required for this action.");
        }
    }

    public void requireLogin() {
        if (!isLoggedIn()) {
            throw new AuthenticationException("Please log in to continue.");
        }
    }

    public boolean changePassword(String oldPassword, String newPassword) {
        requireLogin();
        if (newPassword == null || newPassword.length() < 6) {
            throw new ValidationException("New password must be at least 6 characters long.");
        }

        boolean validOld = PasswordUtil.verifyPassword(oldPassword, currentUser.getSalt(), currentUser.getPasswordHash());
        if (!validOld && PasswordUtil.verifyLegacySha256(oldPassword, currentUser.getSalt(), currentUser.getPasswordHash())) {
            validOld = true;
        }

        if (!validOld) {
            throw new AuthenticationException("Incorrect current password.");
        }

        String newSalt = PasswordUtil.generateSalt();
        String newHash = PasswordUtil.hashPassword(newPassword, newSalt);
        boolean success = userDAO.updatePassword(currentUser.getId(), newHash, newSalt);
        if (success) {
            currentUser.setPasswordHash(newHash);
            currentUser.setSalt(newSalt);
        }
        return success;
    }
}
