package util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

/**
 * Cryptographic utility for password hashing and verification using PBKDF2WithHmacSHA256.
 * Incorporates secure random salting and a high iteration count (65,536) for robust resistance against brute-force attacks.
 * Also provides seamless migration verification for legacy hashes.
 */
public class PasswordUtil {

    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_BYTE_LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generates a 16-byte cryptographically secure random salt formatted as a 32-character hexadecimal string.
     */
    public static String generateSalt() {
        byte[] salt = new byte[SALT_BYTE_LENGTH];
        RANDOM.nextBytes(salt);
        return bytesToHex(salt);
    }

    /**
     * Hashes a password using PBKDF2WithHmacSHA256 with the specified salt and 65,536 iterations.
     *
     * @param password Raw plaintext password
     * @param saltHex  Hex-encoded salt string
     * @return Hexadecimal representation of the 256-bit derived key
     */
    public static String hashPassword(String password, String saltHex) {
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null.");
        }
        if (saltHex == null || saltHex.trim().isEmpty()) {
            throw new IllegalArgumentException("Salt cannot be null or empty.");
        }
        try {
            byte[] saltBytes = hexToBytes(saltHex);
            KeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error computing PBKDF2WithHmacSHA256 hash: " + e.getMessage(), e);
        }
    }

    /**
     * Verifies whether a raw password matches the stored PBKDF2WithHmacSHA256 hash using constant-time comparison.
     */
    public static boolean verifyPassword(String rawPassword, String saltHex, String storedHash) {
        if (rawPassword == null || saltHex == null || storedHash == null) {
            return false;
        }
        String computed = hashPassword(rawPassword, saltHex);
        return slowEquals(computed, storedHash);
    }

    /**
     * Computes legacy SHA-256 hash: SHA-256(salt + ":" + password).
     * Used solely for transparent, seamless on-login migration of existing accounts.
     */
    public static String hashLegacySha256(String password, String saltHex) {
        if (password == null || saltHex == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = saltHex + ":" + password;
            byte[] hashBytes = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    /**
     * Verifies if a password matches a legacy SHA-256 hash during migration.
     * Supports pure unsalted SHA-256 (the original seeded format: SHA-256(password))
     * as well as salted variants (salt + ":" + password and salt + password).
     */
    public static boolean verifyLegacySha256(String rawPassword, String saltHex, String storedHash) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }

        // 1. Check pure unsalted SHA-256 (e.g. original seeded admin123 -> 240be...)
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] pureBytes = digest.digest(rawPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            if (slowEquals(bytesToHex(pureBytes), storedHash)) {
                return true;
            }
        } catch (NoSuchAlgorithmException ignored) {}

        // 2. Check salt + ":" + password
        if (saltHex != null && !saltHex.isEmpty()) {
            String legacyColon = hashLegacySha256(rawPassword, saltHex);
            if (slowEquals(legacyColon, storedHash)) {
                return true;
            }

            // 3. Check salt + password
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] spBytes = digest.digest((saltHex + rawPassword).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                if (slowEquals(bytesToHex(spBytes), storedHash)) {
                    return true;
                }
            } catch (NoSuchAlgorithmException ignored) {}
        }

        return false;
    }

    /**
     * Constant-time comparison to protect against timing attacks.
     */
    private static boolean slowEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] aBytes = a.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int diff = aBytes.length ^ bBytes.length;
        for (int i = 0; i < aBytes.length && i < bBytes.length; i++) {
            diff |= aBytes[i] ^ bBytes[i];
        }
        return diff == 0;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have an even length.");
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
