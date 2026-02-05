package hashkitty.java.sniffer;

import com.jcraft.jsch.UserInfo;
import hashkitty.java.App;
import hashkitty.java.model.RemoteConnection;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Controller class for the "Sniff" tab in the user interface.
 * <p>
 * This class provides functionality for:
 * <ul>
 *     <li>Starting remote packet sniffing sessions via SSH (using {@link SniffManager}).</li>
 *     <li>Analyzing local PCAP files for WPA handshakes (using external `tshark` command).</li>
 *     <li>Displaying the output of these operations in a text area.</li>
 * </ul>
 * </p>
 */
public class SniffController {

    /** Dropdown to select a saved remote connection (e.g., a Raspberry Pi). */
    @FXML
    private ComboBox<RemoteConnection> remoteSelector;

    /** Text area to display logs and analysis results. */
    @FXML
    private TextArea sniffOutput;

    /** The manager handling the SSH connection and sniffing logic. */
    private SniffManager sniffManager;

    /** Reference to the main application class. */
    private App app;

    /** The list of available remote connections (bound to the ComboBox). */
    private ObservableList<RemoteConnection> remoteConnections;

    /**
     * Initializes the controller. Called automatically after FXML loading.
     */
    @FXML
    public void initialize() {
        // Initialize SniffManager with a callback to update the UI text area.
        // Platform.runLater is crucial here because the callback will come from a background thread.
        sniffManager = new SniffManager(output -> Platform.runLater(() -> sniffOutput.appendText(output + "\n")));
    }

    /**
     * Injects dependencies from the main App class.
     *
     * @param app               The main application instance.
     * @param remoteConnections The shared list of remote connections to populate the dropdown.
     */
    public void initData(App app, ObservableList<RemoteConnection> remoteConnections) {
        this.app = app;
        this.remoteConnections = remoteConnections;
        // Bind the ComboBox items to the observable list.
        remoteSelector.setItems(this.remoteConnections);
    }

    /**
     * Handler for the "Start Sniffing" button.
     * Initiates the SSH connection to the selected target.
     */
    @FXML
    private void startSniffing() {
        RemoteConnection selected = remoteSelector.getValue();
        // Validation: Ensure a target is selected.
        if (selected == null) {
            sniffOutput.appendText("Please select a remote target first.\n");
            return;
        }

        // Prompt the user for the SSH password.
        // Ideally, we might support key-based auth in the future, but password is simple for now.
        TextInputDialog passwordDialog = new TextInputDialog();
        passwordDialog.setTitle("SSH Password");
        passwordDialog.setHeaderText("Enter password for " + selected.getConnectionString());
        passwordDialog.setContentText("Password:");

        // Wait for user input.
        Optional<String> result = passwordDialog.showAndWait();
        // If user entered a password and clicked OK, start the sniffing session.
        result.ifPresent(password -> {
            UserInfo userInfo = new JavaFxUserInfo();
            sniffManager.startSniffing(selected, password, userInfo);
        });
    }

    /**
     * Handler for the "Stop Sniffing" button.
     * Terminates the active SSH session.
     */
    @FXML
    private void stopSniffing() {
        if (sniffManager != null) {
            sniffManager.stopSniffing();
        }
    }

    /**
     * Handler for the "Analyze PCAP" button.
     * Allows the user to open a local capture file and runs a tshark filter against it.
     */
    @FXML
    private void analyzePcap() {
        // Configure file chooser.
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select PCAP File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PCAP Files", "*.pcap", "*.pcapng"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        // Show dialog.
        File selectedFile = fileChooser.showOpenDialog(sniffOutput.getScene().getWindow());

        if (selectedFile != null) {
            sniffOutput.appendText("Analyzing " + selectedFile.getName() + " for WPA handshakes...\n");

            // Run analysis in a background thread to keep UI responsive.
            new Thread(() -> {
                try {
                    // Construct tshark command to filter for EAPOL Key messages 1 and 2 (Handshake start).
                    // This verifies if a handshake was potentially captured.
                    ProcessBuilder pb = new ProcessBuilder(
                            "tshark", "-r", selectedFile.getAbsolutePath(),
                            "-Y", "wlan_rsna_eapol.keydes.msgnr == 1 or wlan_rsna_eapol.keydes.msgnr == 2"
                    );
                    pb.redirectErrorStream(true); // Merge stderr into stdout.
                    Process process = pb.start();

                    // Read output.
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        StringBuilder output = new StringBuilder();
                        while ((line = reader.readLine()) != null) {
                            output.append(line).append("\n");
                        }
                        // Update UI with results.
                        Platform.runLater(() -> {
                            if (output.length() == 0) {
                                sniffOutput.appendText("No WPA handshakes found in the file.\n");
                            } else {
                                sniffOutput.appendText("--- Handshake Information ---\n");
                                sniffOutput.appendText(output.toString());
                                sniffOutput.appendText("-----------------------------\n");
                            }
                        });
                    }

                    int exitCode = process.waitFor();
                    Platform.runLater(() -> sniffOutput.appendText("Analysis finished with exit code: " + exitCode + "\n"));

                } catch (IOException e) {
                    Platform.runLater(() -> sniffOutput.appendText("Error running tshark: " + e.getMessage() + ". Is tshark installed and in your system's PATH?\n"));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Platform.runLater(() -> sniffOutput.appendText("PCAP analysis was interrupted.\n"));
                }
            }).start();
        }
    }

    /**
     * Inner class implementing JSch UserInfo to handle interactive prompts (host keys).
     * This class uses JavaFX Alerts to prompt the user on the UI thread, blocking the SSH thread until response.
     */
    private class JavaFxUserInfo implements UserInfo {
        @Override
        public String getPassphrase() { return null; }

        @Override
        public String getPassword() { return null; }

        @Override
        public boolean promptPassword(String message) { return false; }

        @Override
        public boolean promptPassphrase(String message) { return false; }

        @Override
        public boolean promptYesNo(String message) {
            // Need to prompt user on FX Thread and wait for result.
            AtomicBoolean result = new AtomicBoolean(false);
            CountDownLatch latch = new CountDownLatch(1);

            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("SSH Security Warning");
                alert.setHeaderText("Unknown Host Key");
                alert.setContentText(message);

                Optional<ButtonType> buttonType = alert.showAndWait();
                if (buttonType.isPresent() && buttonType.get() == ButtonType.OK) {
                    result.set(true);
                }
                latch.countDown();
            });

            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return result.get();
        }

        @Override
        public void showMessage(String message) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("SSH Info");
                alert.setContentText(message);
                alert.showAndWait();
            });
        }
    }
}
