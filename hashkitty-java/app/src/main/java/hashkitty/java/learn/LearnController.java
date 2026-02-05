package hashkitty.java.learn;

import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.net.URL;

/**
 * Controller class for the "Learn" tab.
 * <p>
 * This simple controller manages a WebView to display educational content (e.g., HTML help files)
 * to the user. It allows the application to serve as a self-contained knowledge base.
 * </p>
 */
public class LearnController {

    /** The JavaFX WebView component defined in the FXML. */
    @FXML
    private WebView webView;

    /**
     * Initializes the controller.
     * Loads the default help page into the WebView.
     */
    @FXML
    public void initialize() {
        // Get the WebEngine associated with the WebView.
        WebEngine webEngine = webView.getEngine();

        // Attempt to locate the local HTML resource.
        URL url = getClass().getResource("/html/learn.html");

        if (url != null) {
            // Load the local file.
            webEngine.load(url.toExternalForm());
        } else {
            // Fallback content if the file is missing.
            webEngine.loadContent("<html><body><h1>Learn Hashcat</h1><p>Content not found.</p></body></html>");
        }
    }
}
