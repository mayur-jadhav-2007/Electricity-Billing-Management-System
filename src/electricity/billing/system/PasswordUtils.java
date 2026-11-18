package electricity.billing.system;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Enterprise Password Security Utility.
 * Implements PBKDF2WithHmacSHA256 (NIST SP 800-63B approved) password hashing
 * with backward-compatible lazy migration for legacy plaintext passwords.
 */
public class PasswordUtils {

    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_BYTES = 16;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String PREFIX = "$pbkdf2$";

    /**
     * Hashes a plaintext password using PBKDF2 with HMAC-SHA256 and a cryptographically secure salt.
     * Output format: $pbkdf2$iterations$saltBase64$hashBase64
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_BYTES];
            random.nextBytes(salt);

            PBEKeySpec spec = new PBEKeySpec(plainPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();

            String saltB64 = Base64.getEncoder().encodeToString(salt);
            String hashB64 = Base64.getEncoder().encodeToString(hash);

            return PREFIX + ITERATIONS + "$" + saltB64 + "$" + hashB64;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    /**
     * Checks if a stored password string is already securely hashed.
     */
    public static boolean isHashed(String storedPassword) {
        if (storedPassword == null) return false;
        return storedPassword.startsWith(PREFIX) ||
               storedPassword.startsWith("$2a$") ||
               storedPassword.startsWith("$2b$") ||
               storedPassword.startsWith("$2y$");
    }

    /**
     * Verifies entered password against stored credential.
     * Supports both modern PBKDF2 hashes and legacy plaintext passwords.
     */
    public static boolean verifyPassword(String candidatePassword, String storedPassword) {
        if (candidatePassword == null || storedPassword == null) {
            return false;
        }

        // 1. PBKDF2 verification
        if (storedPassword.startsWith(PREFIX)) {
            String[] parts = storedPassword.split("\\$");
            // format: "" [0], "pbkdf2" [1], iterations [2], salt [3], hash [4]
            if (parts.length != 5) {
                return false;
            }

            try {
                int iterations = Integer.parseInt(parts[2]);
                byte[] salt = Base64.getDecoder().decode(parts[3]);
                byte[] expectedHash = Base64.getDecoder().decode(parts[4]);

                PBEKeySpec spec = new PBEKeySpec(candidatePassword.toCharArray(), salt, iterations, expectedHash.length * 8);
                SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
                byte[] candidateHash = factory.generateSecret(spec).getEncoded();

                return slowEquals(candidateHash, expectedHash);
            } catch (Exception e) {
                return false;
            }
        }

        // 2. Legacy plaintext comparison
        return candidatePassword.equals(storedPassword);
    }

    /**
     * Constant-time byte array comparison to prevent timing attacks.
     */
    private static boolean slowEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }
}
