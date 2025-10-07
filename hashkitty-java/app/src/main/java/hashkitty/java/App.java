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
import javafx.scene.text.TextFlow;
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
    private TextField hashFileField;
    private TextField ruleFileField;
    private VBox attackInputsContainer;
    private TextField hashModeField;
    private ComboBox<String> attackModeSelector;
    private Button chooseHashFileButton;
    private Button chooseRuleFileButton;
    private Button startButton;
    private Button stopButton;
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

        remoteConnections.add(new RemoteConnection("pwn-pi", "pi@192.168.1.10"));
        remoteConnections.add(new RemoteConnection("cloud-cracker", "user@some-vps.com"));

        hashcatManager = new HashcatManager(this::displayCrackedPassword, this::updateStatus, () -> setAttackInProgress(false));
        relayProcessManager = new RelayProcessManager(this::updateStatus);
        roomId = UUID.randomUUID().toString().substring(0, 8);

        BorderPane mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(10));
        VBox topVBox = createConnectionBox();
        mainLayout.setTop(topVBox);
        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(
            new Tab("Attack", createAttackConfigBox()),
            new Tab("Sniff", createSniffBox()),
            new Tab("Settings", createSettingsBox()),
            new Tab("Learn", createLearnBox()),
            new Tab("Hashcat Setup", createHashcatSetupBox())
        );
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        mainLayout.setCenter(tabPane);
        mainLayout.setBottom(createResultsBox());

        mainScene = new Scene(mainLayout, 600, 800);
        primaryStage.setScene(mainScene);

        relayProcessManager.startRelay();
        connectToRelay();

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

    private void handleRelayMessage(RelayClient.Message message) {
        if ("attack".equalsIgnoreCase(message.getType())) {
            updateStatus("Received remote attack command for hash: " + message.getHash());
            try {
                String wordlistPath = "/app/test-hashes-short.txt";
                hashcatManager.startAttackWithString(message.getHash(), message.getMode(), "Dictionary", wordlistPath, null);
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

        hashFileField = new TextField();
        hashFileField.setPromptText("Path to hash file");
        chooseHashFileButton = new Button("...");
        chooseHashFileButton.setOnAction(e -> {
            File file = new FileChooser().showOpenDialog(primaryStage);
            if (file != null) hashFileField.setText(file.getAbsolutePath());
        });
        grid.add(new Label("Hash File:"), 0, 0);
        grid.add(new HBox(5, hashFileField, chooseHashFileButton), 1, 0);

        hashModeField = new TextField();
        hashModeField.setPromptText("e.g., 22000 for WPA2");
        grid.add(new Label("Hash Mode:"), 0, 1);
        grid.add(hashModeField, 1, 1);

        attackModeSelector = new ComboBox<>();
        attackModeSelector.getItems().addAll("Dictionary", "Mask");
        attackModeSelector.setValue("Dictionary");
        grid.add(new Label("Attack Mode:"), 0, 2);
        grid.add(attackModeSelector, 1, 2);

        ruleFileField = new TextField();
        ruleFileField.setPromptText("(Optional) Path to rule file");
        chooseRuleFileButton = new Button("...");
        chooseRuleFileButton.setOnAction(e -> {
            File file = new FileChooser().showOpenDialog(primaryStage);
            if (file != null) ruleFileField.setText(file.getAbsolutePath());
        });
        grid.add(new Label("Rule File:"), 0, 3);
        grid.add(new HBox(5, ruleFileField, chooseRuleFileButton), 1, 3);

        attackInputsContainer = new VBox(10);
        createDictionaryInput();
        attackModeSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            if ("Dictionary".equals(newVal)) createDictionaryInput(); else createMaskInput();
        });

        startButton = new Button("Start Local Attack");
        startButton.setOnAction(e -> {
            try {
                String hashFile = hashFileField.getText();
                String mode = hashModeField.getText();
                String attackMode = attackModeSelector.getValue();
                String ruleFile = ruleFileField.getText();
                String target = "Dictionary".equals(attackMode) ? wordlistField.getText() : maskField.getText();

                if (hashFile.isEmpty() || mode.isEmpty() || target.isEmpty()) {
                    updateStatus("Error: Hash File, Hash Mode, and Wordlist/Mask cannot be empty.");
                    return;
                }

                setAttackInProgress(true);
                updateStatus("Starting " + attackMode + " attack...");
                hashcatManager.startAttackWithFile(hashFile, mode, attackMode, target, ruleFile.isEmpty() ? null : ruleFile);
            } catch (IOException ex) {
                updateStatus("Error starting hashcat process: " + ex.getMessage());
                setAttackInProgress(false);
            }
        });
        stopButton = new Button("Stop Attack");
        stopButton.setOnAction(e -> hashcatManager.stopCracking());
        HBox buttonBox = new HBox(20, startButton, stopButton);
        buttonBox.setAlignment(Pos.CENTER);

        VBox box = new VBox(20, grid, attackInputsContainer, buttonBox);
        box.setAlignment(Pos.TOP_CENTER);
        box.setPadding(new Insets(20));
        return box;
    }

    private void setAttackInProgress(boolean inProgress) {
        Platform.runLater(() -> {
            // Disable configuration inputs during an attack
            hashFileField.setDisable(inProgress);
            hashModeField.setDisable(inProgress);
            ruleFileField.setDisable(inProgress);
            attackModeSelector.setDisable(inProgress);
            chooseHashFileButton.setDisable(inProgress);
            chooseRuleFileButton.setDisable(inProgress);

            // Also disable the dynamic inputs (wordlist/mask)
            if (wordlistField != null) wordlistField.setDisable(inProgress);
            if (maskField != null) maskField.setDisable(inProgress);

            // Toggle the start/stop buttons
            startButton.setDisable(inProgress);
            stopButton.setDisable(!inProgress);
        });
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
        HBox hhkButtons = new HBox(10, importButton, exportButton);

        Button importJsonButton = new Button("Import from JSON");
        importJsonButton.setOnAction(e -> handleJsonImport());

        Button exportJsonButton = new Button("Export Selected as JSON");
        exportJsonButton.setOnAction(e -> handleJsonExport(remotesList.getSelectionModel().getSelectedItem()));
        exportJsonButton.disableProperty().bind(remotesList.getSelectionModel().selectedItemProperty().isNull());

        HBox jsonButtons = new HBox(10, importJsonButton, exportJsonButton);

        VBox configButtonsVbox = new VBox(10, hhkButtons, jsonButtons);
        VBox configBox = new VBox(10, configLabel, configButtonsVbox);
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

    private void handleJsonExport(RemoteConnection selected) {
        if (selected == null) {
            updateStatus("No remote connection selected for export.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Single Connection");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files (*.json)", "*.json"));
        fileChooser.setInitialFileName(selected.getName() + ".json");
        File file = fileChooser.showSaveDialog(primaryStage);

        if (file != null) {
            try {
                HhkUtil.exportSingleConnection(file, selected);
                updateStatus("Successfully exported '" + selected.getName() + "' to " + file.getName());
            } catch (IOException ex) {
                updateStatus("Error exporting connection: " + ex.getMessage());
            }
        }
    }

    private void handleJsonImport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Single Connection");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files (*.json)", "*.json"));
        File file = fileChooser.showOpenDialog(primaryStage);

        if (file != null) {
            try {
                RemoteConnection imported = HhkUtil.importSingleConnection(file);
                remoteConnections.add(imported);
                updateStatus("Successfully imported '" + imported.getName() + "' from " + file.getName());
            } catch (IOException ex) {
                updateStatus("Error importing connection: " + ex.getMessage());
            }
        }
    }

    private Node createLearnBox() {
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(20));

        Accordion accordion = new Accordion();

        // --- Hashcat Section ---
        Text hashcatText = new Text(
            "Hashcat is the world's fastest and most advanced password recovery tool.\n\n" +
            "Think of it like this: When you save a password, it's not stored as plain text. It's turned into a unique, scrambled string called a 'hash'. You can't easily turn the hash back into the password.\n\n" +
            "Hashcat takes a hash and tries billions of password combinations per second to find the one that creates the exact same hash. It uses the power of your computer's Graphics Card (GPU) to do this incredibly quickly.\n\n" +
            "It's used by cybersecurity professionals to test the strength of passwords and by law enforcement to recover passwords from digital evidence."
        );
        hashcatText.setWrappingWidth(550); // Ensure text wraps nicely
        TextFlow hashcatFlow = new TextFlow(hashcatText);

        TitledPane hashcatPane = new TitledPane("What is Hashcat?", hashcatFlow);

        // --- Hashtopolis Section ---
        Text hashtopolisText = new Text(
            "Hashtopolis is a tool that manages multiple Hashcat instances, often across many different computers.\n\n" +
            "If Hashcat is a single, powerful worker, then Hashtopolis is the factory manager. It takes a big password-cracking job (called a 'task') and splits it up into smaller pieces. It then sends these pieces out to all the connected Hashcat 'agents' (the workers).\n\n" +
            "This allows you to combine the power of many computers to crack passwords even faster. It's used for large-scale security audits and password recovery operations where a single computer wouldn't be powerful enough."
        );
        hashtopolisText.setWrappingWidth(550);
        TextFlow hashtopolisFlow = new TextFlow(hashtopolisText);

        TitledPane hashtopolisPane = new TitledPane("What is Hashtopolis?", hashtopolisFlow);

        accordion.getPanes().addAll(hashcatPane, hashtopolisPane);
        accordion.setExpandedPane(hashcatPane); // Start with the first pane open

        layout.getChildren().add(accordion);

        ScrollPane scrollPane = new ScrollPane(layout);
        scrollPane.setFitToWidth(true);

        return scrollPane;
    }

    private ScrollPane createHashcatSetupBox() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        Label title = new Label("Setting Up Hashcat for GPU Cracking");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Text intro = new Text("Hashcat uses your Graphics Card (GPU) to crack hashes incredibly fast. To make this work, you need the right drivers installed. If hashcat isn't working, this is the most common reason why.");
        intro.setWrappingWidth(550);

        // --- Step 1: Install Hashcat ---
        Label step1Title = new Label("Step 1: Install Hashcat");
        step1Title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Text step1Text = new Text("Download the latest version from the official website: hashcat.net/hashcat/. Extract the archive to a known location on your computer.");
        step1Text.setWrappingWidth(550);

        // --- Step 2: Install GPU Drivers ---
        Label step2Title = new Label("Step 2: Install GPU Drivers");
        step2Title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Text step2Text = new Text(
            "This is the most important step.\n\n" +
            "NVIDIA Users:\n" +
            "You need the latest 'Game Ready' or 'Studio' drivers. Download them from the official NVIDIA website. Hashcat uses the NVIDIA CUDA platform.\n\n" +
            "AMD Users:\n" +
            "You need the latest 'Adrenalin Edition' drivers. Download them from the official AMD website. Hashcat uses the OpenCL platform, which is included with these drivers.\n\n" +
            "Intel GPU Users:\n" +
            "You need the latest graphics drivers from Intel's website. Hashcat uses the OpenCL platform, which is included with these drivers."
        );
        step2Text.setWrappingWidth(550);

        // --- Step 3: Verify Installation ---
        Label step3Title = new Label("Step 3: Verify Everything Works");
        step3Title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Text step3Text = new Text(
            "Open a command prompt or terminal, navigate to your hashcat directory, and run the benchmark command:\n\n" +
            "hashcat.exe -b\n\n" +
            "If everything is set up correctly, you will see a list of your GPUs and a benchmark running for various hash types. If you see errors about missing DLLs or no devices being found, it means your GPU drivers are not installed correctly."
        );
        step3Text.setWrappingWidth(550);

        content.getChildren().addAll(title, intro, new Separator(), step1Title, step1Text, new Separator(), step2Title, step2Text, new Separator(), step3Title, step3Text);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        return scrollPane;
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