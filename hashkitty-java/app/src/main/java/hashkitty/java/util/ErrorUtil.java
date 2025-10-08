package hashkitty.java.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

/**
 * A utility class for displaying standardized error dialogs.
 */
public class ErrorUtil {

    /**
     * Shows an error dialog with a specified title and message.
     * This method is thread-safe and can be called from any thread.
     *
     * @param title   The title of the error dialog.
     * @param message The main message content of the error dialog.
     */
    public static void showError(String title, String message) {
        // Ensure the dialog is shown on the JavaFX Application Thread
        if (Platform.isFxApplicationThread()) {
            showAlert(title, message);
        } else {
            Platform.runLater(() -> showAlert(title, message));
        }
    }

    private static void showAlert(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
