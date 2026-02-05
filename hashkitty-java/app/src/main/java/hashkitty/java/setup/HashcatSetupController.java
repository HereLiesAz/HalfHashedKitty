package hashkitty.java.setup;

import hashkitty.java.util.ErrorUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Controller for the "Hashcat Setup" tab.
 * <p>
 * This class provides a guided experience for installing Hashcat and necessary drivers.
 * It handles:
 * <ul>
 *     <li>Opening external browser links for driver downloads.</li>
 *     <li>Downloading the Hashcat binary release from the official website.</li>
 *     <li>Unzipping the downloaded archive.</li>
 * </ul>
 * </p>
 */
public class HashcatSetupController {

    // URL to the latest Hashcat release (hardcoded for simplicity, could be dynamic).
    private static final String HASHCAT_DOWNLOAD_URL = "https://hashcat.net/files/hashcat-6.2.6.7z";

    @FXML private Label statusLabel;
    @FXML private ProgressBar progressBar;

    /**
     * Handler for "NVIDIA Drivers" button.
     */
    @FXML
    private void openNvidiaDrivers() {
        openUrl("https://www.nvidia.com/Download/index.aspx");
    }

    /**
     * Handler for "AMD Drivers" button.
     */
    @FXML
    private void openAmdDrivers() {
        openUrl("https://www.amd.com/en/support");
    }

    /**
     * Handler for "Intel Drivers" button.
     */
    @FXML
    private void openIntelDrivers() {
        openUrl("https://www.intel.com/content/www/us/en/download-center/home.html");
    }

    /**
     * Handler for "Download Hashcat" button.
     * Starts the download and extraction process in a background thread.
     */
    @FXML
    private void downloadHashcat() {
        // Create a directory chooser to let the user pick the install location.
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Install Directory");
        File selectedDirectory = directoryChooser.showDialog(statusLabel.getScene().getWindow());

        if (selectedDirectory != null) {
            statusLabel.setText("Downloading Hashcat...");
            progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);

            // Run in background.
            new Thread(() -> {
                try {
                    // 1. Download the file.
                    // Using a zip source for compatibility with standard Java libraries.
                    String downloadUrl = "https://github.com/hashcat/hashcat/archive/refs/tags/v6.2.6.zip";
                    File zipFile = downloadFile(downloadUrl, selectedDirectory);

                    // 2. Update status.
                    javafx.application.Platform.runLater(() -> statusLabel.setText("Unpacking..."));

                    // 3. Unzip.
                    unzip(zipFile, selectedDirectory);

                    // 4. Cleanup.
                    zipFile.delete();

                    javafx.application.Platform.runLater(() -> {
                        statusLabel.setText("Installation Complete!");
                        progressBar.setProgress(1.0);
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Success");
                        alert.setHeaderText("Hashcat Installed");
                        alert.setContentText("Hashcat has been installed to " + selectedDirectory.getAbsolutePath() + "\n\nPlease add this directory to your PATH.");
                        alert.showAndWait();
                    });

                } catch (Exception e) {
                    javafx.application.Platform.runLater(() -> {
                        statusLabel.setText("Error: " + e.getMessage());
                        progressBar.setProgress(0);
                        ErrorUtil.showError("Installation Failed", e.getMessage());
                    });
                }
            }).start();
        }
    }

    /**
     * Helper to open a URL in the system's default browser.
     */
    private void openUrl(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Downloads a file from a URL to a target directory.
     */
    private File downloadFile(String urlString, File targetDir) throws IOException {
        URL url = new URL(urlString);
        File targetFile = new File(targetDir, "hashcat.zip");

        try (InputStream in = url.openStream();
             ReadableByteChannel rbc = Channels.newChannel(in);
             FileOutputStream fos = new FileOutputStream(targetFile)) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
        return targetFile;
    }

    /**
     * Unzips a file to a target directory.
     * Prevents Zip Slip vulnerability by validating entry paths.
     */
    private void unzip(File zipFile, File targetDir) throws IOException {
        String targetDirPath = targetDir.getCanonicalPath();

        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                File entryDestination = new File(targetDir, entry.getName());

                // Security Check: Zip Slip
                if (!entryDestination.getCanonicalPath().startsWith(targetDirPath + File.separator)) {
                    throw new IOException("Entry is outside of the target dir: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    entryDestination.mkdirs();
                } else {
                    entryDestination.getParentFile().mkdirs();
                    try (InputStream in = zip.getInputStream(entry);
                         FileOutputStream out = new FileOutputStream(entryDestination)) {
                        in.transferTo(out);
                    }
                }
            }
        }
    }
}
