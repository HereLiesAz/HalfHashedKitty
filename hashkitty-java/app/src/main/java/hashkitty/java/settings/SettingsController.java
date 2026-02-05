package hashkitty.java.settings;

import hashkitty.java.App;
import hashkitty.java.model.RemoteConnection;
import hashkitty.java.util.ErrorUtil;
import hashkitty.java.util.HhkUtil;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;

/**
 * Controller for the "Settings" tab.
 * <p>
 * This class manages application-wide configuration and data management.
 * Features include:
 * <ul>
 *     <li>Managing the list of saved remote connections (add/remove).</li>
 *     <li>Switching between Light and Dark themes.</li>
 *     <li>Importing and Exporting configuration data via the .hhk format.</li>
 * </ul>
 * </p>
 */
public class SettingsController {

    // The UI list view displaying saved connections.
    @FXML
    private ListView<RemoteConnection> connectionsList;

    // Reference to the main application.
    private App app;

    // The data model for the list view.
    private ObservableList<RemoteConnection> remoteConnections;

    /**
     * Injects dependencies from the main App class.
     *
     * @param app               The main application instance.
     * @param remoteConnections The shared list of remote connections.
     */
    public void initData(App app, ObservableList<RemoteConnection> remoteConnections) {
        this.app = app;
        this.remoteConnections = remoteConnections;
        // Bind the ListView to the ObservableList.
        connectionsList.setItems(this.remoteConnections);
    }

    /**
     * Handler for "Add Connection" button.
     * Opens a dialog to create a new remote connection.
     */
    @FXML
    private void addConnection() {
        if (app != null) {
            app.showAddRemoteDialog();
        }
    }

    /**
     * Handler for "Remove Connection" button.
     * Deletes the currently selected item from the list.
     */
    @FXML
    private void removeConnection() {
        RemoteConnection selected = connectionsList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            remoteConnections.remove(selected);
        } else {
            ErrorUtil.showError("Selection Error", "No connection selected to remove.");
        }
    }

    /**
     * Handler for "Dark Theme" button.
     */
    @FXML
    private void setDarkTheme() {
        if (app != null) {
            app.applyTheme("Dark");
        }
    }

    /**
     * Handler for "Light Theme" button.
     */
    @FXML
    private void setLightTheme() {
        if (app != null) {
            app.applyTheme("Light");
        }
    }

    /**
     * Handler for "Import Config" button.
     * Delegates to App to handle the import logic.
     */
    @FXML
    private void importSettings() {
        if (app != null) {
            app.handleImport();
        }
    }

    /**
     * Handler for "Export Config" button.
     * Delegates to App to handle the export logic.
     */
    @FXML
    private void exportSettings() {
        if (app != null) {
            app.handleExport();
        }
    }
}
