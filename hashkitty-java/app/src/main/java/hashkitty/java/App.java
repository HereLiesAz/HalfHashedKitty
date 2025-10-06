package hashkitty.java;

import hashkitty.java.hashcat.HashcatManager;
import hashkitty.java.model.RemoteConnection;
import hashkitty.java.server.DirectServer;
import hashkitty.java.server.RelayServer;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The main class for the HashKitty JavaFX desktop application.
 * This class sets up the user interface, manages application state,
 * and coordinates the various backend managers and servers.
 */
public class App extends Application {

    private static final int RELAY_PORT = 5001;
    private static final int DIRECT_PORT = 5002;
    private static final int QR_CODE_SIZE = 150;

    private TextArea statusLog;
    private Label crackedPasswordLabel;
    private TextField wordlistField;
    private TextField maskField;
    private VBox attackInputsContainer;
    private HashcatManager hashcatManager;
    private SniffManager sniffManager;
    private Stage primaryStage;
    private final ObservableList<RemoteConnection> remoteConnections = FXCollections.observableArrayList();

    private RelayServer relayServer;
    private DirectServer directServer;
    private VBox connectionBox;

    /**
     * The main entry point for the JavaFX application.
     * This method is called after the init method has returned, and it's where
     * the primary stage and the application's UI are set up.
     *
     * @param primaryStage The primary stage for this application, onto which
     *                     the application scene can be set.
     */
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("HashKitty");

        remoteConnections.add(new RemoteConnection("pwn-pi", "pi@192.168.1.10"));
        remoteConnections.add(new RemoteConnection("cloud-cracker", "user@some-vps.com"));

        hashcatManager = new HashcatManager(this::displayCrackedPassword, this::updateStatus);

        BorderPane mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(10));

        VBox serverControlBox = createServerControlBox();
        this.connectionBox = createConnectionBox();
        VBox topVBox = new VBox(15, serverControlBox, this.connectionBox);
        mainLayout.setTop(topVBox);

        TabPane tabPane = new TabPane();
        Tab attackTab = new Tab("Attack", createAttackConfigBox());
        Tab sniffTab = new Tab("Sniff", createSniffBox());
        Tab settingsTab = new Tab("Settings", createSettingsBox());
        Tab learnTab = new Tab("Learn", createLearnBox());
        tabPane.getTabs().addAll(attackTab, sniffTab, settingsTab, learnTab);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        mainLayout.setCenter(tabPane);

        mainLayout.setBottom(createResultsBox());

        updateServerAndQRCode("Relay");

        primaryStage.setScene(new Scene(mainLayout, 600, 850));
        primaryStage.show();

        primaryStage.setOnCloseRequest(e -> {
            stopAllServices();
            Platform.exit();
            System.exit(0);
        });
    }

    /**
     * Gracefully stops all running background services.
     * This includes hashcat processes, sniffing sessions, and WebSocket servers.
     * This method is called when the application is closing.
     */
    private void stopAllServices() {
        hashcatManager.stopCracking();
        if (sniffManager != null) sniffManager.stopSniffing();
        if (relayServer != null) try { relayServer.stop(100); } catch (Exception ex) { ex.printStackTrace(); }
        if (directServer != null) try { directServer.stop(100); } catch (Exception ex) { ex.printStackTrace(); }
    }

    /**
     * Creates the UI component for selecting the server mode (Relay or Direct).
     * @return A VBox containing the server mode selector.
     */
    private VBox createServerControlBox() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-border-color: lightgray; -fx-border-width: 1;");
        ComboBox<String> serverModeSelector = new ComboBox<>();
        serverModeSelector.getItems().addAll("Relay", "Direct");
        serverModeSelector.setValue("Relay");
        serverModeSelector.valueProperty().addListener((obs, oldVal, newVal) -> updateServerAndQRCode(newVal));
        box.getChildren().addAll(new Label("Server Mode:"), serverModeSelector);
        return box;
    }

    /**
     * Stops the currently running server and starts a new one based on the selected mode.
     * It also updates the QR code and connection string displayed in the UI.
     *
     * @param mode The server mode to start ("Relay" or "Direct").
     */
    private void updateServerAndQRCode(String mode) {
        if (relayServer != null) try { relayServer.stop(100); } catch (Exception e) { e.printStackTrace(); }
        if (directServer != null) try { directServer.stop(100); } catch (Exception e) { e.printStackTrace(); }
        relayServer = null;
        directServer = null;

        if ("Relay".equals(mode)) {
            relayServer = new RelayServer(RELAY_PORT, this::updateStatus, this::displayCrackedPassword);
            relayServer.start();
        } else {
            directServer = new DirectServer(DIRECT_PORT, this::updateStatus, this::displayCrackedPassword);
            directServer.start();
        }

        ImageView qrCodeView = (ImageView) connectionBox.lookup("#qrCodeView");
        Label connectionLabel = (Label) connectionBox.lookup("#connectionLabel");
        int port = "Relay".equals(mode) ? RELAY_PORT : DIRECT_PORT;
        String ipAddress = NetworkUtil.getLocalIpAddress();

        if (ipAddress != null) {
            String connectionString = "ws://" + ipAddress + ":" + port;
            qrCodeView.setImage(QRCodeUtil.generateQRCodeImage(connectionString, QR_CODE_SIZE, QR_CODE_SIZE));
            connectionLabel.setText(mode + " server running at: " + connectionString);
        } else {
            connectionLabel.setText("Could not determine local IP address.");
            qrCodeView.setImage(null);
        }
    }

    /**
     * Creates the UI component that displays the connection QR code and server address.
     * @return A VBox containing the connection information.
     */
    private VBox createConnectionBox() {
        ImageView qrCodeView = new ImageView();
        qrCodeView.setId("qrCodeView");
        Label connectionLabel = new Label("Initializing...");
        connectionLabel.setId("connectionLabel");
        VBox box = new VBox(10, new Label("Connection Info"), qrCodeView, connectionLabel);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-border-color: lightgray; -fx-border-width: 1;");
        return box;
    }

    /**
     * Creates the UI for the "Attack" tab.
     * This includes input fields for the hash and mode, a selector for the attack type,
     * and controls to start and stop the hashcat process.
     * @return A VBox containing the attack configuration UI.
     */
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
            if ("Dictionary".equals(newVal)) {
                createDictionaryInput();
            } else {
                createMaskInput();
            }
        });

        Button startButton = new Button("Start Attack");
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

    /**
     * Creates the UI for the "Settings" tab.
     * This includes controls for managing saved remote connections and for
     * importing/exporting configurations.
     * @return A VBox containing the settings UI.
     */
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

        settingsLayout.getChildren().addAll(remotesBox, new Separator(), configBox);
        return settingsLayout;
    }

    /**
     * Creates the UI for the "Sniff" tab.
     * This includes controls for selecting a remote target and starting/stopping
     * a remote packet sniffing session.
     * @return A VBox containing the sniffing UI.
     */
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

    /**
     * Creates the UI for the "Learn" tab.
     * This section provides educational content about hashing and password cracking.
     * @return A ScrollPane containing the educational text content.
     */
    private ScrollPane createLearnBox() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        Label hashTitle = new Label("What is a Hash?");
        hashTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Text hashText = new Text("Imagine you have a secret message. A 'hash function' is like a magic machine that turns your message into a secret code. This code is called a 'hash'.\n\nKey things about hashes:\n1. It's always the same length, no matter how long your message is.\n2. The same message ALWAYS creates the same hash.\n3. You can't turn the hash back into the original message. It's a one-way street!\n\nComputers use hashes to store passwords safely. Instead of saving your actual password, they save its hash. When you log in, they hash the password you typed and check if the hashes match.");
        hashText.setWrappingWidth(500);
        Label hashcatTitle = new Label("What is Hashcat?");
        hashcatTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Text hashcatText = new Text("Hashcat is a famous computer program known as the world's fastest 'password cracker'.\n\nIf you have a hash but don't know the original password, you can use hashcat to try and find it. It's like having a super-fast robot that can try millions or even billions of password guesses per second until it finds one that creates the matching hash.");
        hashcatText.setWrappingWidth(500);
        Label howTitle = new Label("How Does Hashcat Work?");
        howTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Text howText = new Text("Hashcat uses a few main methods (or 'attack modes'):\n\n1. Dictionary Attack: Hashcat takes a huge list of words (a 'dictionary' or 'wordlist') and hashes every single one to see if it finds a match. This is the most common type of attack.\n\n2. Brute-Force (Mask) Attack: Hashcat tries every possible combination of letters, numbers, and symbols. You can give it a pattern (a 'mask'), like '????' for any four-letter password, or '?d?d?d?d' for any four-digit number.\n\n3. Hybrid Attack: A mix of both! It might take a word from a dictionary and add numbers or symbols to the end.");
        howText.setWrappingWidth(500);
        Label hashtopolisTitle = new Label("What is Hashtopolis?");
        hashtopolisTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Text hashtopolisText = new Text("Imagine you have a really hard hash to crack, and one computer isn't fast enough. Hashtopolis is a tool that lets you link lots of computers together to work on the same problem at the same time!\n\nIt's like having a whole team of robots all trying to guess the password at once. It's a 'distributed cracking' system that manages all the computers (agents) and gives them jobs to do, making the process much, much faster.");
        hashtopolisText.setWrappingWidth(500);
        content.getChildren().addAll(hashTitle, hashText, new Separator(), hashcatTitle, hashcatText, new Separator(), howTitle, howText, new Separator(), hashtopolisTitle, hashtopolisText);
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        return scrollPane;
    }

    /**
     * Handles the logic for exporting remote connections to a .hhk file.
     * Prompts the user for a file location and a password.
     */
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
                    updateStatus("Successfully exported " + remoteConnections.size() + " connections to " + file.getName());
                } catch (IOException ex) {
                    updateStatus("Error exporting connections: " + ex.getMessage());
                    ex.printStackTrace();
                }
            });
        }
    }

    /**
     * Handles the logic for importing remote connections from a .hhk file.
     * Prompts the user for a file and a password.
     */
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
                    ex.printStackTrace();
                }
            });
        }
    }

    /**
     * Shows a dialog window to add a new remote connection to the list.
     */
    private void showAddRemoteDialog() {
        Dialog<RemoteConnection> dialog = new Dialog<>();
        dialog.setTitle("Add New Remote");
        dialog.setHeaderText("Enter the details for the new remote connection.");
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
        result.ifPresent(remote -> {
            remoteConnections.add(remote);
            updateStatus("Settings: Added new remote '" + remote.getName() + "'.");
        });
    }

    /**
     * Creates the UI controls for a dictionary-based attack.
     */
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

    /**
     * Creates the UI control for a mask-based attack.
     */
    private void createMaskInput() {
        attackInputsContainer.getChildren().clear();
        Label maskLabel = new Label("Mask:");
        maskField = new TextField();
        maskField.setPromptText("e.g., ?d?d?d?d");
        attackInputsContainer.getChildren().addAll(maskLabel, maskField);
    }

    /**
     * Creates the UI component for displaying status messages and cracked passwords.
     * @return A VBox containing the status log and result label.
     */
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

    /**
     * Updates the status log with a new message. This method is thread-safe
     * and can be called from background threads.
     * @param message The message to append to the status log.
     */
    private void updateStatus(String message) {
        Platform.runLater(() -> statusLog.appendText(message + "\n"));
    }

    /**
     * Updates the UI to display a newly cracked password. This method is thread-safe.
     * @param password The cracked password to display.
     */
    private void displayCrackedPassword(String password) {
        Platform.runLater(() -> {
            crackedPasswordLabel.setText("Cracked Password: " + password);
            updateStatus("SUCCESS: Password found! -> " + password);
        });
    }
}