package hashkitty.java.sniffer;

import hashkitty.java.App;
import hashkitty.java.model.RemoteConnection;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;

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
}
