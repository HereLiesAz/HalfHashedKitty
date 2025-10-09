package hashkitty.java.sniffer;

import hashkitty.java.App;
import hashkitty.java.model.RemoteConnection;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.stage.FileChooser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;

public class SniffController {

    @FXML
    private ComboBox<RemoteConnection> remoteSelector;

    @FXML
    private TextArea sniffOutput;

    private SniffManager sniffManager;
    private App app;
    private ObservableList<RemoteConnection> remoteConnections;

    @FXML
    public void initialize() {
        sniffManager = new SniffManager(output -> Platform.runLater(() -> sniffOutput.appendText(output)));
    }

    public void initData(App app, ObservableList<RemoteConnection> remoteConnections) {
        this.app = app;
        this.remoteConnections = remoteConnections;
        remoteSelector.setItems(this.remoteConnections);
    }

    @FXML
    private void startSniffing() {
        RemoteConnection selected = remoteSelector.getValue();
        if (selected == null) {
            sniffOutput.appendText("Please select a remote target first.\n");
            return;
        }
        TextInputDialog passwordDialog = new TextInputDialog();
        passwordDialog.setTitle("SSH Password");
        passwordDialog.setHeaderText("Enter password for " + selected.getConnectionString());
        passwordDialog.setContentText("Password:");
        Optional<String> result = passwordDialog.showAndWait();
        result.ifPresent(password -> sniffManager.startSniffing(selected, password));
    }

    @FXML
    private void stopSniffing() {
        if (sniffManager != null) {
            sniffManager.stopSniffing();
        }
    }

    @FXML
    private void analyzePcap() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select PCAP File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PCAP Files", "*.pcap", "*.pcapng"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File selectedFile = fileChooser.showOpenDialog(sniffOutput.getScene().getWindow());

        if (selectedFile != null) {
            sniffOutput.appendText("Analyzing " + selectedFile.getName() + " for WPA handshakes...\n");

            new Thread(() -> {
                try {
                    ProcessBuilder pb = new ProcessBuilder(
                            "tshark", "-r", selectedFile.getAbsolutePath(),
                            "-Y", "wlan_rsna_eapol.keydes.msgnr == 1 or wlan_rsna_eapol.keydes.msgnr == 2"
                    );
                    pb.redirectErrorStream(true);
                    Process process = pb.start();

                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        StringBuilder output = new StringBuilder();
                        while ((line = reader.readLine()) != null) {
                            output.append(line).append("\n");
                        }
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
}
