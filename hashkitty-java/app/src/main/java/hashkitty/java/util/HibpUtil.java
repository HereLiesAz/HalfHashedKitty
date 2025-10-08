package hashkitty.java.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A utility class to check passwords against the Have I Been Pwned (HIBP)
 * Pwned Passwords API.
 */
public class HibpUtil {

    private static final String API_URL = "https://api.pwnedpasswords.com/range/";

    /**
     * Checks if a password has been exposed in a data breach according to the HIBP database.
     *
     * @param password The plain-text password to check.
     * @return The number of times the password has been seen in a breach, or 0 if it has not.
     * @throws Exception if there is an error during the API request or hashing.
     */
    public static int checkPassword(String password) throws Exception {
        try {
            // 1. Hash the password with SHA-1
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hashBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            String sha1Hash = bytesToHex(hashBytes);

            // 2. Split the hash into a prefix (first 5 chars) and suffix
            String prefix = sha1Hash.substring(0, 5);
            String suffix = sha1Hash.substring(5).toUpperCase();

            // 3. Make the API request
            URL url = new URL(API_URL + prefix);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("HIBP API request failed with response code: " + responseCode);
            }

            // 4. Read the response and check for the suffix
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    String[] parts = line.split(":");
                    if (parts.length == 2 && parts[0].equalsIgnoreCase(suffix)) {
                        return Integer.parseInt(parts[1]); // Found the hash suffix
                    }
                }
            }

            return 0; // Suffix not found in the response

        } catch (NoSuchAlgorithmException e) {
            // This should not happen with SHA-1 being a standard algorithm
            throw new RuntimeException("SHA-1 algorithm not found", e);
        }
    }

    /**
     * Converts a byte array to a hexadecimal string.
     */
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString().toUpperCase();
    }
}
