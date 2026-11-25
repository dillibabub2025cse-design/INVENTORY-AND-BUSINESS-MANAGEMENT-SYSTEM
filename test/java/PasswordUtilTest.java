
package test;

import util.PasswordUtil;

/**
 * Unit tests verifying PBKDF2WithHmacSHA256 hashing, unique salts, and legacy
 * migration verification.
 */
public class PasswordUtilTest {

    public static void runTests() {
        System.out.println("Running PasswordUtilTest (PBKDF2WithHmacSHA256)...");

        // Test 1: Salt generation uniqueness and length
        String salt1 = PasswordUtil.generateSalt();
        String salt2 = PasswordUtil.generateSalt();
        assert salt1 != null && salt1.length() == 32 : "Salt length must be 32 hex chars (16 bytes)";
        assert !salt1.equals(salt2) : "Generated salts must be unique";

        // Test 2: PBKDF2 hashing consistency
        String password = "SecureAdminPassword!2026";
        String hash1 = PasswordUtil.hashPassword(password, salt1);
        String hash2 = PasswordUtil.hashPassword(password, salt1);
        assert hash1 != null && hash1.length() == 64 : "PBKDF2 256-bit hash must be 64 hex characters";
        assert hash1.equals(hash2) : "Hashing the same input with the same salt must yield identical hashes";

        // Test 3: Positive PBKDF2 verification
        assert PasswordUtil.verifyPassword(password, salt1, hash1)
                : "Password verification should succeed for valid password";

        // Test 4: Negative PBKDF2 verification (wrong password)
        assert !PasswordUtil.verifyPassword("WrongPassword123", salt1, hash1)
                : "Password verification should fail for invalid password";

        // Test 5: Legacy SHA-256 verification support for migration (salted and pure
        // unsalted)
        String legacySalt = "1234567890abcdef1234567890abcdef";
        String legacyHash = PasswordUtil.hashLegacySha256("admin123", legacySalt);
        assert PasswordUtil.verifyLegacySha256("admin123", legacySalt, legacyHash)
                : "Legacy SHA-256 verification must succeed for migration";
        assert !PasswordUtil.verifyLegacySha256("wrong", legacySalt, legacyHash)
                : "Legacy verification must reject wrong password";

        // Pure unsalted SHA-256 (original seeded hash)
        String pureSha256Admin123 = "240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9";
        assert PasswordUtil.verifyLegacySha256("admin123", legacySalt, pureSha256Admin123)
                : "Pure unsalted legacy SHA-256 must verify for transparent migration";
        assert !PasswordUtil.verifyLegacySha256("wrongpass", legacySalt, pureSha256Admin123)
                : "Pure unsalted legacy SHA-256 must reject incorrect password";

        System.out.println("  [PASS] PasswordUtilTest - All 5 assertions passed.");
    }
}
