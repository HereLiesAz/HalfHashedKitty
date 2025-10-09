package hashkitty.java.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A utility for cleaning and normalizing hash files.
 */
public class NormalizationUtil {

    /**
     * Reads a hash file, extracts the hash from each line, and writes the cleaned hashes to a new temporary file.
     * It handles formats like 'user:hash' or just 'hash' per line.
     *
     * @param inputFile The original hash file.
     * @return A new temporary file containing only the normalized hashes.
     * @throws IOException if there is an error reading the input file or writing the temporary file.
     */
    public static File normalizeHashFile(File inputFile) throws IOException {
        List<String> hashes = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmedLine = line.trim();
                if (trimmedLine.isEmpty()) {
                    continue; // Skip empty lines
                }

                // For formats like user:hash or hash:pass, we assume the hash is the first part.
                // This is a simple heuristic. JtR potfiles have the hash before the last colon.
                // Other dumps might have user:hash. We will take everything before the last colon
                // if one exists, otherwise we take the whole line.
                int lastColonIndex = trimmedLine.lastIndexOf(':');
                String hash;
                if (lastColonIndex != -1) {
                    hash = trimmedLine.substring(0, lastColonIndex);
                } else {
                    hash = trimmedLine;
                }

                // A simple check to avoid adding what are likely usernames or non-hash data
                if (!hash.isEmpty() && isLikelyHash(hash)) {
                    hashes.add(hash);
                } else if (lastColonIndex == -1 && !trimmedLine.isEmpty()) {
                    // If there was no colon, add the whole line if it looks like a hash
                     if(isLikelyHash(trimmedLine)) {
                         hashes.add(trimmedLine);
                     }
                }
            }
        }

        if (hashes.isEmpty()) {
            throw new IOException("No valid hashes could be extracted from the input file.");
        }

        File tempFile = File.createTempFile("normalized_hashes_", ".txt");
        tempFile.deleteOnExit();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            for (String hash : hashes) {
                writer.write(hash);
                writer.newLine();
            }
        }

        return tempFile;
    }

    /**
     * A very basic check to see if a string is likely a hash.
     * This can be expanded with more rules.
     */
    private static boolean isLikelyHash(String s) {
        // Most common hashes are hex, bcrypt starts with $, etc.
        // This regex checks for common hash characters: hex, $, ., /
        return s.matches("^[a-fA-F0-9\\$\\./]+$");
    }
}
