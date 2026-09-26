package medistore.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Passwords are stored as SHA-256 hashes, never as readable text.
 *
 * Even in a college project it is worth doing: anyone who opens the database
 * file sees a hash, not the staff members' actual passwords.
 */
public final class Passwords {

    private Passwords() {
    }

    public static String hash(String plainText) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(plainText.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is missing from this Java installation", e);
        }
    }

    public static boolean matches(String plainText, String storedHash) {
        return hash(plainText).equals(storedHash);
    }
}
