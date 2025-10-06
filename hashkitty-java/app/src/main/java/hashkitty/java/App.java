package hashkitty.java;

import hashkitty.java.hashcat.HashcatManager;
import hashkitty.java.model.RemoteConnection;
import hashkitty.java.relay.RelayClient;
import hashkitty.java.relay.RelayProcessManager;
import hashkitty.java.sniffer.SniffManager;
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
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
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
import java.util.UUID;

/**
 * The main class for the HashKitty JavaFX desktop application.
 */
public class App extends Application {

    private static final int RELAY_PORT = 5001;
    private static final int QR_CODE_SIZE = 150;

    private TextArea statusLog;
    private Label crackedPasswordLabel;
    private TextField wordlistField;
    private TextField maskField;
    private VBox attackInputsContainer;
    private HashcatManager hashcatManager;
    private SniffManager sniffManager;
    private RelayProcessManager relayProcessManager;
    private RelayClient relayClient;
    private Stage primaryStage;
    private Scene mainScene;
    private String roomId;
    private final ObservableList<RemoteConnection> remoteConnections = FXCollections.observableArrayList();

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("HashKitty");

        // --- Initialize Backend Managers ---
        remoteConnections.add(new RemoteConnection("pwn-pi", "pi@192.168.1.10"));
        remoteConnections.add(new RemoteConnection("cloud-cracker", "user@some-vps.com"));
        hashcatManager = new HashcatManager(this::displayCrackedPassword, this::updateStatus);
        relayProcessManager = new RelayProcessManager(this::updateStatus);
        roomId = UUID.randomUUID().toString().substring(0, 8); // Create a unique room ID

        // --- UI Setup ---
        BorderPane mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(10));
        VBox topVBox = createConnectionBox();
        mainLayout.setTop(topVBox);
        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(
            new Tab("Attack", createAttackConfigBox()),
            new Tab("Sniff", createSniffBox()),
            new Tab("Settings", createSettingsBox()),
            new Tab("Learn", createLearnBox())
        );
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        mainLayout.setCenter(tabPane);
        mainLayout.setBottom(createResultsBox());

        // --- Start Services & Show UI ---
        mainScene = new Scene(mainLayout, 600, 800);
        primaryStage.setScene(mainScene);

        relayProcessManager.startRelay();
        connectToRelay(); // Connect the client after starting the process

        primaryStage.show();

        primaryStage.setOnCloseRequest(e -> {
            stopAllServices();
            Platform.exit();
            System.exit(0);
        });
    }

    private void stopAllServices() {
        hashcatManager.stopCracking();
        if (sniffManager != null) sniffManager.stopSniffing();
        if (relayClient != null) relayClient.close();
        if (relayProcessManager != null) relayProcessManager.stopRelay();
    }

    /**
     * Connects the internal WebSocket client to the standalone relay server.
     */
    private void connectToRelay() {
        try {
            URI serverUri = new URI("ws://localhost:" + RELAY_PORT + "/ws");
            relayClient = new RelayClient(serverUri, roomId, this::handleRelayMessage, this::updateStatus);
            updateStatus("Attempting to connect to local relay server...");
            relayClient.connect();
            updateQRCode();
        } catch (URISyntaxException e) {
            updateStatus("Error: Invalid relay server URI.");
            e.printStackTrace();
        }
    }

    /**
     * Handles messages received from the relay server.
     * This is where commands from the mobile client are processed.
     * @param message The message received from the relay.
     */
    private void handleRelayMessage(RelayClient.Message message) {
        if ("attack".equalsIgnoreCase(message.getType())) {
            updateStatus("Received remote attack command for hash: " + message.getHash());
            try {
                // For now, assume remote attacks are always dictionary attacks with a default wordlist
                String wordlistPath = "/app/test-hashes-short.txt";
                hashcatManager.startCracking(message.getHash(), message.getMode(), "Dictionary", wordlistPath);
            } catch (IOException e) {
                updateStatus("Error starting remote attack: " + e.getMessage());
            }
        }
    }

    private void updateQRCode() {
        String ipAddress = NetworkUtil.getLocalIpAddress();
        ImageView qrCodeView = (ImageView) mainScene.getRoot().lookup("#qrCodeView");
        Label connectionLabel = (Label) mainScene.getRoot().lookup("#connectionLabel");
        if (ipAddress != null && qrCodeView != null && connectionLabel != null) {
            // The connection string now includes the room ID for the mobile client
            String connectionString = "ws://" + ipAddress + ":" + RELAY_PORT + "/ws?roomId=" + roomId;
            qrCodeView.setImage(QRCodeUtil.generateQRCodeImage(connectionString, QR_CODE_SIZE, QR_CODE_SIZE));
            connectionLabel.setText("Scan to join room: " + roomId);
        } else if (connectionLabel != null) {
            connectionLabel.setText("Could not determine local IP address.");
        }
    }

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

    private VBox createAttackConfigBox() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 20, 10));
        TextField hashField = new TextField();
        hashField.setPromptText("Enter hash here");
        TextField hashModeField = new TextField();
        hashModeField.setPromptText("e.g., 0 for MD5");
        ComboBox<String> attackModeSelector = new ComboBox<>();
        attackModeSelector.getItems().addAll("Dictionary", "Mask");
        attackModeSelector.setValue("Dictionary");
        grid.add(new Label("Hash:"), 0, 0);
        grid.add(hashField, 1, 0);
        grid.add(new Label("Hash Mode:"), 0, 1);
        grid.add(hashModeField, 1, 1);
        grid.add(new Label("Attack Mode:"), 0, 2);
        grid.add(attackModeSelector, 1, 2);
        attackInputsContainer = new VBox(10);
        createDictionaryInput();
        attackModeSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            if ("Dictionary".equals(newVal)) createDictionaryInput(); else createMaskInput();
        });
        Button startButton = new Button("Start Local Attack");
        startButton.setOnAction(e -> {
            try {
                String hash = hashField.getText();
                String mode = hashModeField.getText();
                String attackMode = attackModeSelector.getValue();
                String target = "Dictionary".equals(attackMode) ? wordlistField.getText() : maskField.getText();
                if (hash.isEmpty() || mode.isEmpty() || target.isEmpty()) {
                    updateStatus("Error: Hash, Mode, and Wordlist/Mask cannot be empty.");
                    return;
                }
                updateStatus("Starting " + attackMode + " attack...");
                hashcatManager.startCracking(hash, mode, attackMode, target);
            } catch (IOException ex) {
                updateStatus("Error starting hashcat process: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
        Button stopButton = new Button("Stop Attack");
        stopButton.setOnAction(e -> hashcatManager.stopCracking());
        HBox buttonBox = new HBox(20, startButton, stopButton);
        buttonBox.setAlignment(Pos.CENTER);
        VBox box = new VBox(20, grid, attackInputsContainer, buttonBox);
        box.setAlignment(Pos.TOP_CENTER);
        box.setPadding(new Insets(20));
        return box;
    }

    private VBox createSettingsBox() {
        VBox settingsLayout = new VBox(20);
        settingsLayout.setPadding(new Insets(20));
        settingsLayout.setAlignment(Pos.TOP_LEFT);
        Label remotesLabel = new Label("Saved Remotes");
        remotesLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        ListView<RemoteConnection> remotesList = new ListView<>(remoteConnections);
        remotesList.setPrefHeight(100);
        Button addButton = new Button("Add");
        addButton.setOnAction(e -> showAddRemoteDialog());
        Button removeButton = new Button("Remove");
        removeButton.setOnAction(e -> {
            RemoteConnection selected = remotesList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                remoteConnections.remove(selected);
                updateStatus("Settings: Removed remote '" + selected.getName() + "'.");
            }
        });
        HBox remoteButtons = new HBox(10, addButton, removeButton);
        VBox remotesBox = new VBox(10, remotesLabel, remotesList, remoteButtons);
        Label configLabel = new Label("Configuration Management");
        configLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Button importButton = new Button("Import from .hhk file");
        importButton.setOnAction(e -> handleImport());
        Button exportButton = new Button("Export to .hhk file");
        exportButton.setOnAction(e -> handleExport());
        HBox configButtons = new HBox(10, importButton, exportButton);
        VBox configBox = new VBox(10, configLabel, configButtons);
        Label themeLabel = new Label("Appearance");
        themeLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        ComboBox<String> themeSelector = new ComboBox<>();
        themeSelector.getItems().addAll("Light", "Dark");
        themeSelector.setValue("Light");
        themeSelector.valueProperty().addListener((obs, oldVal, newVal) -> applyTheme(newVal));
        VBox themeBox = new VBox(10, themeLabel, themeSelector);
        settingsLayout.getChildren().addAll(remotesBox, new Separator(), configBox, new Separator(), themeBox);
        return settingsLayout;
    }

    private void applyTheme(String themeName) {
        mainScene.getStylesheets().clear();
        if ("Dark".equals(themeName)) {
            try {
                String css = this.getClass().getResource("/styles/dark-theme.css").toExternalForm();
                mainScene.getStylesheets().add(css);
                updateStatus("Applied Dark Theme.");
            } catch (Exception e) {
                updateStatus("Error: Could not load dark theme stylesheet.");
                e.printStackTrace();
            }
        } else {
            updateStatus("Applied Light Theme.");
        }
    }

    private VBox createSniffBox() {
        VBox sniffLayout = new VBox(20);
        sniffLayout.setPadding(new Insets(20));
        sniffLayout.setAlignment(Pos.TOP_CENTER);
        Label titleLabel = new Label("Remote Packet Sniffing");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        HBox remoteSelectionBox = new HBox(10);
        remoteSelectionBox.setAlignment(Pos.CENTER);
        Label remoteLabel = new Label("Target Remote:");
        ComboBox<RemoteConnection> remoteSelector = new ComboBox<>(remoteConnections);
        remoteSelectionBox.getChildren().addAll(remoteLabel, remoteSelector);
        TextArea sniffOutput = new TextArea();
        sniffOutput.setEditable(false);
        sniffOutput.setPromptText("Sniffing output will appear here...");
        sniffOutput.setPrefHeight(200);
        sniffManager = new SniffManager(output -> Platform.runLater(() -> sniffOutput.appendText(output)));
        Button startSniffButton = new Button("Start Sniffing");
        Button stopSniffButton = new Button("Stop Sniffing");
        startSniffButton.setOnAction(e -> {
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
        });
        stopSniffButton.setOnAction(e -> sniffManager.stopSniffing());
        HBox controlButtons = new HBox(20, startSniffButton, stopSniffButton);
        controlButtons.setAlignment(Pos.CENTER);
        sniffLayout.getChildren().addAll(titleLabel, remoteSelectionBox, controlButtons, new Label("Output:"), sniffOutput);
        return sniffLayout;
    }

    private ScrollPane createLearnBox() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        Label hashTitle = new Label("What is a Hash?");
        hashTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Text hashText = new Text("A 'hash function' is like a magic machine that turns your message into a secret code of a fixed length. Key properties: 1. The same message ALWAYS creates the same hash. 2. You can't turn the hash back into the original message.");
        hashText.setWrappingWidth(500);
        Label hashcatTitle = new Label("What is Hashcat?");
        hashcatTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Text hashcatText = new Text("Hashcat is the world's fastest password cracker. If you have a hash, you can use hashcat to try millions of password guesses per second until it finds one that creates a matching hash.");
        hashcatText.setWrappingWidth(500);
        content.getChildren().addAll(hashTitle, hashText, new Separator(), hashcatTitle, hashcatText);
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        return scrollPane;
    }

    private void handleExport() {
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
                    updateStatus("Error exporting connections: " + ex.getMessage());
                }
            });
        }
    }

    private void handleImport() {
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
                    updateStatus("Error importing: Invalid password or corrupted file.");
                } catch (IOException ex) {
                    updateStatus("Error importing connections: " + ex.getMessage());
                }
            });
        }
    }

    private void showAddRemoteDialog() {
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

    private void createDictionaryInput() {
        attackInputsContainer.getChildren().clear();
        Label wordlistLabel = new Label("Wordlist:");
        wordlistField = new TextField();
        wordlistField.setPromptText("Path to wordlist file");
        Button chooseFileButton = new Button("...");
        chooseFileButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Wordlist File");
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                wordlistField.setText(file.getAbsolutePath());
            }
        });
        HBox dicBox = new HBox(5, wordlistField, chooseFileButton);
        attackInputsContainer.getChildren().addAll(wordlistLabel, dicBox);
    }

    private void createMaskInput() {
        attackInputsContainer.getChildren().clear();
        Label maskLabel = new Label("Mask:");
        maskField = new TextField();
        maskField.setPromptText("e.g., ?d?d?d?d");
        attackInputsContainer.getChildren().addAll(maskLabel, maskField);
    }

    private VBox createResultsBox() {
        statusLog = new TextArea();
        statusLog.setEditable(false);
        statusLog.setPrefHeight(100);
        crackedPasswordLabel = new Label("Cracked Password: N/A");
        crackedPasswordLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        VBox box = new VBox(10, new Label("Status Log"), statusLog, crackedPasswordLabel);
        box.setPadding(new Insets(10));
        return box;
    }

    private void updateStatus(String message) {
        Platform.runLater(() -> statusLog.appendText(message + "\n"));
    }

    private void displayCrackedPassword(String password) {
        Platform.runLater(() -> {
            crackedPasswordLabel.setText("Cracked Password: " + password);
            updateStatus("SUCCESS: Password found! -> " + password);
            // Send cracked password back to the mobile client
            if (relayClient != null && relayClient.isOpen()) {
                RelayClient.Message crackedMessage = new RelayClient.Message();
                crackedMessage.setType("cracked");
                crackedMessage.setRoomId(roomId);
                crackedMessage.setPayload(password);
                relayClient.sendMessage(crackedMessage);
            }
        });
    }
}