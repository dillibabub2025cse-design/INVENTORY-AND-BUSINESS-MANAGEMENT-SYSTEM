package service;

import dao.UserDAO;
import exception.ValidationException;
import model.User;
import util.PasswordUtil;

import java.util.List;

/**
 * Service managing user accounts, credential issuance, and security administration.
 */
public class UserService {

    private final UserDAO userDAO;

    public UserService() {
        this.userDAO = new UserDAO();
    }

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public List<User> getAllUsers() {
        return userDAO.findAll();
    }

    public User getUserById(int id) {
        User u = userDAO.findById(id);
        if (u == null) {
            throw new ValidationException("User with ID " + id + " does not exist.");
        }
        return u;
    }

    public boolean createUser(String username, String rawPassword, String fullName, String email, int roleId) {
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("Username cannot be empty.");
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new ValidationException("Password must be at least 6 characters.");
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
        user.setFullName(fullName != null ? fullName.trim() : username.trim());
        user.setEmail(email != null ? email.trim() : "");
        user.setRoleId(roleId);
        user.setActive(true);

        return userDAO.create(user);
    }

    public boolean resetPassword(int userId, String newPassword) {
        getUserById(userId);
        if (newPassword == null || newPassword.length() < 6) {
            throw new ValidationException("Password must be at least 6 characters.");
        }
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(newPassword, salt);
        return userDAO.updatePassword(userId, hash, salt);
    }

    public boolean toggleUserStatus(int userId, boolean active) {
        getUserById(userId);
        return userDAO.toggleActive(userId, active);
    }
}

