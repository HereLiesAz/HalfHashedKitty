package hashkitty.java.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;

/**
 * A utility class for common file I/O operations.
 */
public class FileUtil {

    /**
     * Downloads a file from a specified URL and saves it to a temporary file.
     * <p>
     * This is commonly used for downloading remote wordlists provided via URL.
     * The downloaded file is marked with a "hashkitty-wordlist-" prefix.
     * </p>
     *
     * @param urlString The fully qualified URL of the file to download.
     * @return The temporary {@link File} object referencing the downloaded content.
     * @throws IOException If the URL is invalid, connection fails, or writing to disk fails.
     */
    public static File downloadFileToTemp(String urlString) throws IOException {
        URL url = new URL(urlString);
        // Create a temp file with a specific prefix/suffix.
        File tempFile = Files.createTempFile("hashkitty-wordlist-", ".txt").toFile();

        // Use NIO Channels for efficient transfer.
        try (InputStream in = url.openStream();
             ReadableByteChannel rbc = Channels.newChannel(in);
             FileOutputStream fos = new FileOutputStream(tempFile)) {
            // Transfer bytes from the source channel (URL) to the destination file channel.
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }

        return tempFile;
    }
}
