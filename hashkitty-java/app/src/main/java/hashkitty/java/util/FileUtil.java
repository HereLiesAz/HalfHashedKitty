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
 * A utility class for file-related operations.
 */
public class FileUtil {

    /**
     * Downloads a file from a given URL to a temporary local file.
     *
     * @param urlString The URL of the file to download.
     * @return The temporary {@link File} object where the content is stored.
     * @throws IOException if there is an error during the download or file creation.
     */
    public static File downloadFileToTemp(String urlString) throws IOException {
        URL url = new URL(urlString);
        File tempFile = Files.createTempFile("hashkitty-download-", ".tmp").toFile();

        try (InputStream in = url.openStream();
             ReadableByteChannel rbc = Channels.newChannel(in);
             FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }

        return tempFile;
    }
}