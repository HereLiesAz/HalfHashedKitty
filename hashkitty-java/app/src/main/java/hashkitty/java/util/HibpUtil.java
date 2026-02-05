package hashkitty.java.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A utility class to check passwords against the "Have I Been Pwned" (HIBP) Pwned Passwords API.
 * <p>
 * This class implements the k-Anonymity model required by the HIBP API v2/v3.
 * Instead of sending the full password hash to the server (which would be insecure),
 * it sends only the first 5 characters of the SHA-1 hash. The server returns all hashes
 * starting with that prefix, and the client performs the final match locally.
 * </p>
 */
public class HibpUtil {

    /** The base URL for the HIBP Pwned Passwords API. */
    private static final String API_URL = "https://api.pwnedpasswords.com/range/";

    /**
     * Checks if a password has been exposed in a data breach.
     *
     * @param password The plain-text password to check.
     * @return The number of times the password has appeared in known breaches. Returns 0 if safe (not found).
     * @throws Exception If network errors occur or SHA-1 is not supported.
     */
    public static int checkPassword(String password) throws Exception {
        try {
            // 1. Hash the password using SHA-1 (required by HIBP API).
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hashBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            String sha1Hash = bytesToHex(hashBytes);

            // 2. Implement k-Anonymity: Split the hash.
            // Prefix: First 5 characters (sent to API).
            String prefix = sha1Hash.substring(0, 5);
            // Suffix: The rest of the hash (kept local).
            String suffix = sha1Hash.substring(5).toUpperCase();

            // 3. Query the API with the prefix.
            URL url = new URL(API_URL + prefix);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            // Add User-Agent (good practice/required by some APIs).
            conn.setRequestProperty("User-Agent", "HashKitty-Java-App");

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("HIBP API request failed with response code: " + responseCode);
            }

            // 4. Parse the response.
            // The response contains lines in the format: SUFFIX:COUNT
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    // Split line into [suffix, count].
                    String[] parts = line.split(":");
                    // Check if the suffix matches our password's suffix.
                    if (parts.length == 2 && parts[0].equalsIgnoreCase(suffix)) {
                        // Match found! Return the breach count.
                        return Integer.parseInt(parts[1]);
                    }
                }
            }

            // If we finish the loop without returning, the suffix was not in the list.
            return 0;

        } catch (NoSuchAlgorithmException e) {
            // SHA-1 is standard in Java, so this is highly unlikely.
            throw new RuntimeException("SHA-1 algorithm not found", e);
        }
    }

    /**
     * Helper method to convert a byte array to a hexadecimal string.
     *
     * @param hash The byte array.
     * @return The uppercase hex string.
     */
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                // Pad with leading zero if needed.
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString().toUpperCase();
    }
}
