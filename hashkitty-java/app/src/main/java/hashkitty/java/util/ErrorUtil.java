package hashkitty.java.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

/**
 * A utility class for displaying standardized error dialogs to the user.
 * <p>
 * This class handles the thread safety requirement of JavaFX, ensuring that
 * UI dialogs are always created and shown on the JavaFX Application Thread,
 * regardless of which thread calls the method.
 * </p>
 */
public class ErrorUtil {

    /**
     * Shows a modal error dialog with a specified title and message.
     * This method is thread-safe.
     *
     * @param title   The title of the error dialog window.
     * @param message The main body text explaining the error.
     */
    public static void showError(String title, String message) {
        // Check if we are currently running on the JavaFX Application Thread.
        if (Platform.isFxApplicationThread()) {
            // Safe to interact with UI directly.
            showAlert(title, message);
        } else {
            // We are on a background thread; schedule the UI update on the FX thread.
            Platform.runLater(() -> showAlert(title, message));
        }
    }

    /**
     * Internal helper to construct and show the Alert.
     * Must be called on the FX Application Thread.
     *
     * @param title   The dialog title.
     * @param message The dialog content.
     */
    private static void showAlert(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(title);
        alert.setContentText(message);
        // showAndWait blocks the UI until the user dismisses the dialog.
        alert.showAndWait();
    }
}
