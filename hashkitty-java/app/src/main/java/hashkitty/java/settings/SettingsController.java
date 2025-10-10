package hashkitty.java.settings;

import hashkitty.java.App;
import hashkitty.java.model.RemoteConnection;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;

public class SettingsController {

    @FXML
    private ListView<RemoteConnection> remotesList;

    @FXML
    private ComboBox<String> themeSelector;

    private App app;
    private ObservableList<RemoteConnection> remoteConnections;

    @FXML
    public void initialize() {
        // Populate the theme selector
        themeSelector.getItems().addAll("Light", "Dark");
        themeSelector.setValue("Dark"); // Default theme
        // Add a listener to apply the theme when changed
        themeSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (app != null) {
                app.applyTheme(newVal);
            }
        });
    }

    // This method will be called by the App class to pass a reference to itself
    // and the list of remote connections.
    public void initData(App app, ObservableList<RemoteConnection> remoteConnections) {
        this.app = app;
        this.remoteConnections = remoteConnections;
        remotesList.setItems(this.remoteConnections);
    }

    @FXML
    private void addRemote() {
        if (app != null) {
            app.showAddRemoteDialog();
        }
    }

    @FXML
    private void removeRemote() {
        RemoteConnection selected = remotesList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            remoteConnections.remove(selected);
            if (app != null) {
                app.updateStatus("Settings: Removed remote '" + selected.getName() + "'.");
            }
        }
    }

    @FXML
    private void importSettings() {
        if (app != null) {
            app.handleImport();
        }
    }

    @FXML
    private void exportSettings() {
        if (app != null) {
            app.handleExport();
        }
    }
}
