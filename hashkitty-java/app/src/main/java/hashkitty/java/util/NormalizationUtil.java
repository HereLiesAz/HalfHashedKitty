package hashkitty.java.util;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

/**
 * A utility class for pre-processing hash lists before sending them to Hashcat.
 * <p>
 * This class handles the extraction of hashes from mixed-format files (like "username:hash" or
 * "email:hash:salt") and creates a clean, deduplicated file containing only the hash strings.
 * This ensures compatibility with Hashcat modes that expect raw hashes.
 * </p>
 */
public class NormalizationUtil {

    /**
     * Reads an input file, attempts to extract valid hashes, removes duplicates, and writes
     * the result to a temporary file.
     *
     * @param inputFile The raw file provided by the user (e.g., a potfile or dump).
     * @return A {@link File} object pointing to the temporary file containing clean hashes.
     * @throws IOException If file reading/writing fails or if no valid hashes are found.
     */
    public static File normalizeHashFile(File inputFile) throws IOException {
        // Use a Set to automatically handle deduplication of hashes.
        Set<String> hashes = new HashSet<>();

        // Read the input file line by line.
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmedLine = line.trim();
                // Skip empty lines.
                if (trimmedLine.isEmpty()) continue;

                // Heuristic: Attempt to extract the hash part.
                // Many formats are colon-separated (e.g., user:hash).
                // We assume the hash is the *last* component if there are colons.
                // NOTE: This is a simple heuristic and might not work for all formats (e.g. colon in salt).
                int lastColonIndex = trimmedLine.lastIndexOf(':');
                String hash = "";

                if (lastColonIndex != -1 && lastColonIndex < trimmedLine.length() - 1) {
                    // Extract substring after the last colon.
                    hash = trimmedLine.substring(lastColonIndex + 1);
                } else {
                    // No colon found, assume the whole line is the hash.
                    hash = trimmedLine;
                }

                // Validate the candidate hash string.
                if (!hash.isEmpty() && isLikelyHash(hash)) {
                    hashes.add(hash);
                } else if (lastColonIndex == -1 && !trimmedLine.isEmpty()) {
                    // Fallback: If there was no colon, but strict check failed,
                    // check the whole line again (redundant but safe).
                     if(isLikelyHash(trimmedLine)) {
                         hashes.add(trimmedLine);
                     }
                }
            }
        }

        // Ensure we actually found something.
        if (hashes.isEmpty()) {
            throw new IOException("No valid hashes could be extracted from the input file.");
        }

        // Create a temporary file to store the result.
        File tempFile = File.createTempFile("normalized_hashes_", ".txt");
        // Ensure the temp file is deleted when the VM exits (best effort).
        tempFile.deleteOnExit();

        // Write the deduplicated hashes to the temp file.
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            for (String hash : hashes) {
                writer.write(hash);
                writer.newLine();
            }
        }

        return tempFile;
    }

    /**
     * A basic heuristic check to determine if a string resembles a cryptographic hash.
     *
     * @param s The string to check.
     * @return true if the string looks like a hash, false otherwise.
     */
    private static boolean isLikelyHash(String s) {
        // Regex explanation:
        // ^           : Start of string
        // [a-fA-F0-9] : Hexadecimal characters
        // \\$         : Literal dollar sign (common in modular crypt formats like bcrypt, MD5-crypt)
        // \\.         : Literal dot (used in some salt encodings)
        // /           : Forward slash (used in base64-like encodings)
        // +           : One or more occurrences
        // $           : End of string
        return s.matches("^[a-fA-F0-9\\$\\./]+$");
    }
}
