package hashkitty.java.util;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Consumer;

/**
 * Manages the downloading and launching of installers for required software.
 */
public class SetupManager {

    private final Consumer<String> onStatusUpdate;

    public SetupManager(Consumer<String> onStatusUpdate) {
        this.onStatusUpdate = onStatusUpdate;
    }

    public void downloadAndInstallHashcat() {
        final String hashcatUrl = "https://hashcat.net/files/hashcat-6.2.6.7z";
        final String destDir = System.getProperty("user.home") + File.separator + "hashcat";

        onStatusUpdate.accept("Starting Hashcat download from " + hashcatUrl);
        try {
            File tempArchive = FileUtil.downloadFileToTemp(hashcatUrl);
            onStatusUpdate.accept("Download complete. Unpacking to " + destDir);
            unpack7z(tempArchive, new File(destDir));
            onStatusUpdate.accept("Hashcat successfully unpacked to: " + destDir);
            onStatusUpdate.accept("Please add this directory to your system's PATH to run hashcat from any terminal.");
            if (!tempArchive.delete()) {
                onStatusUpdate.accept("Warning: Could not delete temporary archive " + tempArchive.getAbsolutePath());
            }
        } catch (IOException e) {
            onStatusUpdate.accept("Error during Hashcat setup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void openNvidiaDriversPage() {
        String url = "https://www.nvidia.com/Download/index.aspx";
        openUrlInBrowser(url);
    }

    public void openAmdDriversPage() {
        String url = "https://www.amd.com/en/support";
        openUrlInBrowser(url);
    }

    public void openIntelDriversPage() {
        String url = "https://www.intel.com/content/www/us/en/download-center/home.html";
        openUrlInBrowser(url);
    }

    private void unpack7z(File archive, File destination) throws IOException {
        new File(destination, "hashcat-6.2.6").mkdirs(); // Create the base directory
        try (SevenZFile sevenZFile = new SevenZFile(archive)) {
            SevenZArchiveEntry entry;
            while ((entry = sevenZFile.getNextEntry()) != null) {
                File outputFile = new File(destination, entry.getName());
                if (entry.isDirectory()) {
                    if (!outputFile.exists()) {
                        outputFile.mkdirs();
                    }
                } else {
                    // Ensure parent directory exists
                    outputFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                        byte[] content = new byte[(int) entry.getSize()];
                        sevenZFile.read(content);
                        fos.write(content);
                    }
                }
            }
        }
    }

    private void openUrlInBrowser(String urlString) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI(urlString));
                onStatusUpdate.accept("Opened " + urlString + " in your browser.");
            } catch (IOException | URISyntaxException e) {
                onStatusUpdate.accept("Error: Could not open URL in browser.");
                e.printStackTrace();
            }
        } else {
            onStatusUpdate.accept("Error: Cannot open browser automatically. Please go to: " + urlString);
        }
    }
}