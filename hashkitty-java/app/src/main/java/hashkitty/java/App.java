package hashkitty.java;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import hashkitty.java.attack.AttackParams;
import hashkitty.java.hashcat.HashcatManager;
import hashkitty.java.model.RemoteConnection;
import hashkitty.java.relay.RelayClient;
import hashkitty.java.attack.AttackController;
import hashkitty.java.relay.RelayProcessManager;
import hashkitty.java.settings.SettingsController;
import hashkitty.java.sniffer.SniffController;
import hashkitty.java.sniffer.SniffManager;
import hashkitty.java.util.ErrorUtil;
import hashkitty.java.util.HibpUtil;
import hashkitty.java.util.HhkUtil;
import hashkitty.java.util.NetworkUtil;
import hashkitty.java.util.QRCodeUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.lingala.zip4j.exception.ZipException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;

/**
 * The main entry point and controller for the HashKitty JavaFX desktop application.
 * <p>
 * This class extends {@link Application} and manages the primary lifecycle of the GUI.
 * It coordinates the various modules (Hashcat, Relay, Sniffer) and manages global state.
 * </p>
 */
public class App extends Application {

    /** Default port for the embedded Relay Server. */
    private static final int RELAY_PORT = 5001;
    /** Size (width/height) for the generated QR code. */
    private static final int QR_CODE_SIZE = 150;

    // UI Components
    /** Text area for scrolling logs/status messages. */
    private TextArea statusLog;
    /** Label to display the most recently cracked password prominently. */
    private Label crackedPasswordLabel;
    /** Button to trigger HIBP check for the cracked password. */
    private Button hibpCheckButton;

    // Managers and Controllers
    /** Manages the execution of Hashcat processes. */
    private HashcatManager hashcatManager;
    /** Manages packet sniffing operations. */
    private SniffManager sniffManager;
    /** Manages the lifecycle of the local Relay Server process/thread. */
    private RelayProcessManager relayProcessManager;
    /** Client to connect to the Relay Server (even the local one). */
    private RelayClient relayClient;

    // Helpers
    /** Gson instance for JSON parsing. */
    private final Gson gson = new Gson();

    // Stage and Scene
    /** Reference to the primary window stage. */
    private Stage primaryStage;
    /** Reference to the main scene. */
    private Scene mainScene;

    // State
    /** Unique ID for the relay room this instance is hosting/joined to. */
    private String roomId;
    /** Observable list of saved remote connections (e.g., SSH hosts). Shared with Settings/Sniff tabs. */
    private final ObservableList<RemoteConnection> remoteConnections = FXCollections.observableArrayList();
    /** Stores the last cracked password for HIBP checking. */
    private String lastCrackedPassword;

    /**
     * The main entry point for JavaFX applications.
     *
     * @param primaryStage The primary stage for this application.
     */
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("HashKitty");

        // Initialize default remote connections for demonstration/testing.
        remoteConnections.add(new RemoteConnection("pwn-pi", "pi@192.168.1.10"));
        remoteConnections.add(new RemoteConnection("cloud-cracker", "user@some-vps.com"));

        // Initialize the HashcatManager with callbacks for UI updates.
        hashcatManager = new HashcatManager(this::displayCrackedPassword, this::updateStatus, () -> {});

        // Initialize the RelayProcessManager.
        relayProcessManager = new RelayProcessManager(this::updateStatus);

        // Generate a random 8-character Room ID for this session.
        roomId = UUID.randomUUID().toString().substring(0, 8);

        // Setup the main layout container.
        BorderPane mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(10));

        // Create the top section (QR Code and Connection info).
        VBox topVBox = createConnectionBox();
        mainLayout.setTop(topVBox);

        // Create the center section (Tabs).
        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(
                new Tab("Attack", loadAttackScreen()),
                new Tab("Wordlist", loadFxmlScreen("Wordlist")),
                new Tab("Mask", loadFxmlScreen("Mask")),
                new Tab("Terminal", loadFxmlScreen("Terminal")),
                new Tab("Sniff", loadSniffScreen()),
                new Tab("Hashtopolis", loadFxmlScreen("Hashtopolis")),
                new Tab("Settings", loadSettingsScreen()),
                new Tab("Learn", loadLearnScreen()),
                new Tab("Hashcat Setup", loadHashcatSetupScreen())
        );
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        mainLayout.setCenter(tabPane);

        // Create the bottom section (Logs and Results).
        mainLayout.setBottom(createResultsBox());

        // Initialize the Scene with the layout.
        mainScene = new Scene(mainLayout, 600, 800);
        primaryStage.setScene(mainScene);

        // Apply the Dark theme by default.
        applyTheme("Dark");

        // Start the embedded Relay Server.
        relayProcessManager.startRelay();
        // Connect this client to the local Relay Server.
        connectToRelay();

        // Display the window.
        primaryStage.show();

        // Handle application close request.
        primaryStage.setOnCloseRequest(e -> {
            stopAllServices();
            Platform.exit();
            System.exit(0);
        });
    }

    /**
     * Gracefully stops all background services and connections.
     */
    private void stopAllServices() {
        // Stop any running Hashcat attack.
        hashcatManager.stopCracking();
        // Stop sniffing if active.
        if (sniffManager != null) sniffManager.stopSniffing();
        // Close the WebSocket client connection.
        if (relayClient != null) relayClient.close();
        // Stop the embedded Relay Server.
        if (relayProcessManager != null) relayProcessManager.stopRelay();
    }

    /**
     * Establishes a WebSocket connection to the local Relay Server.
     */
    private void connectToRelay() {
        try {
            // Construct the URI for localhost.
            URI serverUri = new URI("ws://localhost:" + RELAY_PORT + "/ws");
            // Initialize the RelayClient with callbacks for handling messages and status updates.
            relayClient = new RelayClient(serverUri, roomId, this::handleRelayMessage, this::updateStatus);
            updateStatus("Attempting to connect to local relay server...");
            // Initiate connection.
            relayClient.connect();
            // Update the QR code.
            updateQRCode();
        } catch (URISyntaxException e) {
            ErrorUtil.showError("Connection Error", "Invalid relay server URI.");
            e.printStackTrace();
        }
    }

    /**
     * Handles incoming messages from the Relay Server.
     * This processes commands received from the Android client.
     *
     * @param message The parsed message object (outer envelope).
     */
    private void handleRelayMessage(RelayClient.Message message) {
        // Check if the message is a command to start an attack.
        if ("attack".equalsIgnoreCase(message.getType())) {
            try {
                // Deserialize the payload into AttackParams.
                AttackParams params = gson.fromJson(message.getPayload(), AttackParams.class);

                updateStatus("Received remote attack command for job: " + params.jobId);

                // Map the attack mode ID to string ("0" -> "Dictionary", "3" -> "Mask").
                String attackModeName = "3".equals(params.attackMode) ? "Mask" : "Dictionary";

                // Start the attack using the HashcatManager.
                // We assume file paths provided in params are valid on this machine.
                // Force=true is safer for remote execution to avoid prompts.
                hashcatManager.startAttackWithFile(
                    params.file,
                    params.mode,
                    attackModeName,
                    ("Dictionary".equals(attackModeName) ? params.wordlist : params.wordlist), // target logic depends on mode
                    params.rules,
                    true,
                    false,
                    null
                );
            } catch (JsonSyntaxException | IOException e) {
                ErrorUtil.showError("Remote Attack Error", "Error processing remote attack command: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Generates and updates the QR code displayed in the UI.
     */
    private void updateQRCode() {
        String ipAddress = NetworkUtil.getLocalIpAddress();
        ImageView qrCodeView = (ImageView) mainScene.getRoot().lookup("#qrCodeView");
        Label connectionLabel = (Label) mainScene.getRoot().lookup("#connectionLabel");

        if (ipAddress != null && qrCodeView != null && connectionLabel != null) {
            String connectionString = "ws://" + ipAddress + ":" + RELAY_PORT + "/ws?roomId=" + roomId;
            qrCodeView.setImage(QRCodeUtil.generateQRCodeImage(connectionString, QR_CODE_SIZE, QR_CODE_SIZE));
            connectionLabel.setText("Scan to join room: " + roomId);
        } else if (connectionLabel != null) {
            connectionLabel.setText("Could not determine local IP address.");
        }
    }

    /**
     * Creates the UI container for the connection info (QR code).
     */
    private VBox createConnectionBox() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-border-color: lightgray; -fx-border-width: 1;");

        ImageView qrCodeView = new ImageView();
        qrCodeView.setId("qrCodeView");

        Label connectionLabel = new Label("Initializing Relay Server...");
        connectionLabel.setId("connectionLabel");

        box.getChildren().addAll(new Label("Mobile Connection"), qrCodeView, connectionLabel);
        return box;
    }

    /**
     * Loads the "Attack" screen from FXML and initializes its controller.
     */
    private Node loadAttackScreen() {
        try {
            String fxmlPath = "/fxml/Attack.fxml";
            ResourceBundle bundle = ResourceBundle.getBundle("messages");
            FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxmlPath), bundle);
            Parent root = fxmlLoader.load();

            AttackController controller = fxmlLoader.getController();
            controller.initData(this, hashcatManager, primaryStage);

            return root;
        } catch (IOException e) {
            e.printStackTrace();
            return new Label("Error loading Attack screen: " + e.getMessage());
        }
    }

    /**
     * Helper method to load a generic FXML screen by name.
     */
    private Parent loadFxml(String fxml) throws IOException {
        String fxmlPath = "/fxml/" + fxml + ".fxml";
        ResourceBundle bundle = ResourceBundle.getBundle("messages");
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxmlPath), bundle);
        return fxmlLoader.load();
    }

    private Node loadFxmlScreen(String fxml) {
        try {
            return loadFxml(fxml);
        } catch (IOException e) {
            e.printStackTrace();
            return new Label("Error loading " + fxml + " screen: " + e.getMessage());
        }
    }

    private Node loadLearnScreen() {
        try {
            return loadFxml("Learn");
        } catch (IOException e) {
            e.printStackTrace();
            return new Label("Error loading Learn screen: " + e.getMessage());
        }
    }

    private Node loadSettingsScreen() {
        try {
            String fxmlPath = "/fxml/Settings.fxml";
            ResourceBundle bundle = ResourceBundle.getBundle("messages");
            FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxmlPath), bundle);
            Parent root = fxmlLoader.load();

            SettingsController controller = fxmlLoader.getController();
            controller.initData(this, remoteConnections);

            return root;
        } catch (IOException e) {
            e.printStackTrace();
            return new Label("Error loading Settings screen: " + e.getMessage());
        }
    }

    public void applyTheme(String themeName) {
        mainScene.getStylesheets().clear();
        if ("Dark".equals(themeName)) {
            try {
                String css = this.getClass().getResource("/styles/dark-theme.css").toExternalForm();
                mainScene.getStylesheets().add(css);
                updateStatus("Applied Dark Theme.");
            } catch (Exception e) {
                ErrorUtil.showError("Theme Error", "Could not load dark theme stylesheet.");
                e.printStackTrace();
            }
        } else {
            updateStatus("Applied Light Theme.");
        }
    }

    private Node loadHashcatSetupScreen() {
        try {
            return loadFxml("HashcatSetup");
        } catch (IOException e) {
            e.printStackTrace();
            return new Label("Error loading Hashcat Setup screen: " + e.getMessage());
        }
    }

    private Node loadSniffScreen() {
        try {
            String fxmlPath = "/fxml/Sniff.fxml";
            ResourceBundle bundle = ResourceBundle.getBundle("messages");
            FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxmlPath), bundle);
            Parent root = fxmlLoader.load();
            SniffController controller = fxmlLoader.getController();
            controller.initData(this, remoteConnections);
            return root;
        } catch (IOException e) {
            e.printStackTrace();
            return new Label("Error loading Sniff screen: " + e.getMessage());
        }
    }

    public void handleExport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Connections");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("HashKitty Config", "*.hhk"));
        File file = fileChooser.showSaveDialog(primaryStage);
        if (file != null) {
            TextInputDialog passwordDialog = new TextInputDialog();
            passwordDialog.setTitle("Set Export Password");
            passwordDialog.setHeaderText("Enter a password to protect the .hhk file.");
            passwordDialog.setContentText("Password:");
            Optional<String> result = passwordDialog.showAndWait();
            result.ifPresent(password -> {
                try {
                    HhkUtil.exportConnections(file, password, new ArrayList<>(remoteConnections));
                    updateStatus("Successfully exported connections.");
                } catch (IOException ex) {
                    ErrorUtil.showError("Export Error", "Error exporting connections: " + ex.getMessage());
                }
            });
        }
    }

    public void handleImport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Connections");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("HashKitty Config", "*.hhk"));
        File file = fileChooser.showOpenDialog(primaryStage);
        if (file != null) {
            TextInputDialog passwordDialog = new TextInputDialog();
            passwordDialog.setTitle("Enter Import Password");
            passwordDialog.setHeaderText("Enter the password for " + file.getName());
            passwordDialog.setContentText("Password:");
            Optional<String> result = passwordDialog.showAndWait();
            result.ifPresent(password -> {
                try {
                    List<RemoteConnection> imported = HhkUtil.importConnections(file, password);
                    remoteConnections.clear();
                    remoteConnections.addAll(imported);
                    updateStatus("Successfully imported " + imported.size() + " connections.");
                } catch (ZipException ex) {
                    ErrorUtil.showError("Import Error", "Invalid password or corrupted file.");
                } catch (IOException ex) {
                    ErrorUtil.showError("Import Error", "Error importing connections: " + ex.getMessage());
                }
            });
        }
    }

    public void showAddRemoteDialog() {
        Dialog<RemoteConnection> dialog = new Dialog<>();
        dialog.setTitle("Add New Remote");
        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("e.g., pwn-pi");
        TextField connectionStringField = new TextField();
        connectionStringField.setPromptText("e.g., user@192.168.1.100");

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Connection:"), 0, 1);
        grid.add(connectionStringField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        Node addButton = dialog.getDialogPane().lookupButton(addButtonType);
        addButton.setDisable(true);
        nameField.textProperty().addListener((observable, oldValue, newValue) -> addButton.setDisable(newValue.trim().isEmpty()));

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                return new RemoteConnection(nameField.getText(), connectionStringField.getText());
            }
            return null;
        });

        Optional<RemoteConnection> result = dialog.showAndWait();
        result.ifPresent(remoteConnections::add);
    }

    private VBox createResultsBox() {
        statusLog = new TextArea();
        statusLog.setEditable(false);
        statusLog.setPrefHeight(100);

        crackedPasswordLabel = new Label("Cracked Password: N/A");
        crackedPasswordLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        hibpCheckButton = new Button("Check HIBP for Exposure");
        hibpCheckButton.setDisable(true);
        hibpCheckButton.setOnAction(e -> checkHibp());

        HBox crackedPasswordBox = new HBox(20, crackedPasswordLabel, hibpCheckButton);
        crackedPasswordBox.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(10, new Label("Status Log"), statusLog, crackedPasswordBox);
        box.setPadding(new Insets(10));
        return box;
    }

    public void updateStatus(String message) {
        Platform.runLater(() -> statusLog.appendText(message + "\n"));
    }

    private void displayCrackedPassword(String password) {
        Platform.runLater(() -> {
            lastCrackedPassword = password;
            hibpCheckButton.setDisable(false);
            crackedPasswordLabel.setText("Cracked Password: " + password);
            updateStatus("SUCCESS: Password found! -> " + password);

            if (relayClient != null && relayClient.isOpen()) {
                RelayClient.Message crackedMessage = new RelayClient.Message();
                crackedMessage.setType("cracked");
                crackedMessage.setRoomId(roomId);
                crackedMessage.setPayload(password);
                relayClient.sendMessage(crackedMessage);
            }
        });
    }

    private void checkHibp() {
        if (lastCrackedPassword == null || lastCrackedPassword.isEmpty()) {
            return;
        }
        hibpCheckButton.setDisable(true);
        updateStatus("Checking password '" + lastCrackedPassword + "' against HIBP database...");

        new Thread(() -> {
            try {
                int count = HibpUtil.checkPassword(lastCrackedPassword);
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("HIBP Check Result");
                    if (count > 0) {
                        alert.setHeaderText("Password Found!");
                        alert.setContentText("The password '" + lastCrackedPassword + "' has been found in " + String.format("%,d", count) + " data breaches.\n\nIt is strongly recommended not to use this password.");
                    } else {
                        alert.setHeaderText("Password Not Found");
                        alert.setContentText("Good news! The password '" + lastCrackedPassword + "' was not found in the Have I Been Pwned database.");
                    }
                    alert.showAndWait();
                    updateStatus("HIBP check complete.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    ErrorUtil.showError("HIBP API Error", "Failed to check password: " + e.getMessage());
                });
            } finally {
                Platform.runLater(() -> hibpCheckButton.setDisable(false));
            }
        }).start();
    }
}
