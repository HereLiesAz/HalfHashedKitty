package hashkitty.java.attack;

import hashkitty.java.App;
import hashkitty.java.hashcat.HashcatManager;
import hashkitty.java.util.ErrorUtil;
import hashkitty.java.util.NormalizationUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;

/**
 * Controller class for the "Attack" tab in the user interface.
 * <p>
 * This class handles all user interactions related to configuring and launching Hashcat attacks.
 * It manages the inputs for hash files, attack modes (Dictionary, Mask), rule files, and advanced options.
 * It dynamically modifies the UI based on the selected attack mode.
 * </p>
 */
public class AttackController {

    // FXML injected fields mapping to UI components defined in Attack.fxml
    @FXML private TextField hashFileField;
    @FXML private ComboBox<String> hashModeField;
    @FXML private ComboBox<String> attackModeSelector;
    @FXML private TextField ruleFileField;
    @FXML private VBox attackInputsContainer;
    @FXML private CheckBox forceCheckbox;
    @FXML private CheckBox optimizedKernelsCheckbox;
    @FXML private ComboBox<String> workloadProfileSelector;

    // References to main application components
    private HashcatManager hashcatManager;
    private Stage primaryStage;
    private App app;

    // Dynamic UI fields (created programmatically based on attack mode)
    private TextField wordlistField;
    private TextField maskField;

    /**
     * Initializes the controller class.
     * This method is automatically called after the fxml file has been loaded.
     */
    @FXML
    public void initialize() {
        // Populate the Attack Mode selector.
        attackModeSelector.getItems().addAll("Dictionary", "Mask", "Brute-force");
        // Set default value.
        attackModeSelector.setValue("Dictionary");

        // Populate the Hash Mode selector with common types.
        // Format: "ID - Description"
        hashModeField.getItems().addAll(
                "0 - MD5",
                "1000 - NTLM",
                "1800 - sha512crypt, SHA-512 (Unix)",
                "3200 - bcrypt, Blowfish (Unix)",
                "22000 - WPA-PBKDF2-PMKID+EAPOL",
                "13100 - Kerberos 5 TGS-REP etype 23"
        );

        // Populate the Workload Profile selector.
        workloadProfileSelector.getItems().addAll(
                "1 - Low",
                "2 - Default",
                "3 - High",
                "4 - Nightmare"
        );
        workloadProfileSelector.setValue("2 - Default");

        // Add a listener to change the input fields when the attack mode changes.
        attackModeSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            if ("Dictionary".equals(newVal)) {
                createDictionaryInput();
            } else {
                // "Mask" or "Brute-force" (treated similarly for now)
                createMaskInput();
            }
        });

        // Initialize the view with Dictionary inputs by default.
        createDictionaryInput();
    }

    /**
     * Injects dependencies from the main App class.
     *
     * @param app             The main application instance.
     * @param hashcatManager  The manager for hashcat processes.
     * @param primaryStage    The primary stage (window) for showing dialogs.
     */
    public void initData(App app, HashcatManager hashcatManager, Stage primaryStage) {
        this.app = app;
        this.hashcatManager = hashcatManager;
        this.primaryStage = primaryStage;
    }

    /**
     * Handler for the "..." button next to Hash File.
     * Opens a file chooser to select a hash file.
     */
    @FXML
    private void chooseHashFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Hash File");
        File selectedFile = fileChooser.showOpenDialog(primaryStage);
        if (selectedFile != null) {
            // Pre-process the file to clean/normalize hashes.
            normalizeAndSetHashFile(selectedFile);
        }
    }

    /**
     * Handler for the "..." button next to Rule File.
     * Opens a file chooser to select a rule file.
     */
    @FXML
    private void chooseRuleFile() {
        File file = new FileChooser().showOpenDialog(primaryStage);
        if (file != null) {
            ruleFileField.setText(file.getAbsolutePath());
        }
    }

    /**
     * Handler for the "Identify" button.
     * Attempts to automatically detect the hash type based on the content of the selected file.
     */
    @FXML
    private void identifyHash() {
        String hashFilePath = hashFileField.getText();
        // Validation: Ensure a file is selected.
        if (hashFilePath == null || hashFilePath.isEmpty()) {
            ErrorUtil.showError("Input Error", "Please select a hash file first.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(hashFilePath))) {
            // Read the first line to analyze.
            String firstLine = reader.readLine();
            if (firstLine == null || firstLine.isEmpty()) {
                ErrorUtil.showError("File Error", "The selected hash file is empty.");
                return;
            }

            // Perform heuristic analysis.
            String identifiedMode = identifyHashType(firstLine.trim());

            if (identifiedMode != null) {
                // Try to find the matching description in our ComboBox.
                Optional<String> match = hashModeField.getItems().stream()
                        .filter(item -> item.startsWith(identifiedMode + " "))
                        .findFirst();

                if (match.isPresent()) {
                    // Select the matching item.
                    hashModeField.setValue(match.get());
                    app.updateStatus("Identified hash type: " + match.get());
                } else {
                    // If the mode is valid but not in our preset list, just set the ID.
                    hashModeField.setValue(identifiedMode);
                    app.updateStatus("Identified hash mode: " + identifiedMode);
                }
            } else {
                ErrorUtil.showError("Identification Failed", "Could not automatically identify the hash type.");
            }

        } catch (IOException e) {
            ErrorUtil.showError("File Error", "Could not read the hash file: " + e.getMessage());
        }
    }

    /**
     * Handler for the "Import Potfile" button.
     * Allows users to import existing cracked hashes from John the Ripper.
     */
    @FXML
    private void importPotfile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select John the Ripper Potfile");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JtR Potfile", "*.pot"));
        File potfile = fileChooser.showOpenDialog(primaryStage);

        if (potfile != null) {
            normalizeAndSetHashFile(potfile);
        }
    }

    /**
     * Handler for the "Start Local Attack" button.
     * Validates inputs and initiates the attack via HashcatManager.
     */
    @FXML
    private void startAttack() {
        try {
            // Retrieve values from UI fields.
            String hashFile = hashFileField.getText();
            String modeInput = hashModeField.getValue();
            String attackMode = attackModeSelector.getValue();
            String ruleFile = ruleFileField.getText();
            String target;

            // Determine the target (wordlist or mask) based on mode.
            if ("Dictionary".equals(attackMode)) {
                target = wordlistField.getText();
            } else { // Mask attack
                target = maskField.getText();
            }

            // Basic Validation.
            if (hashFile.isEmpty() || modeInput == null || modeInput.isEmpty() || target.isEmpty()) {
                ErrorUtil.showError("Missing Information", "Hash File, Hash Mode, and Wordlist/Mask cannot be empty.");
                return;
            }

            // Parse the mode ID (extract "0" from "0 - MD5").
            String mode = modeInput.split(" ")[0];

            // Get advanced options.
            boolean force = forceCheckbox.isSelected();
            boolean optimizedKernels = optimizedKernelsCheckbox.isSelected();
            String workloadProfileInput = workloadProfileSelector.getValue();
            String workloadProfile = workloadProfileInput != null ? workloadProfileInput.split(" ")[0] : null;

            app.updateStatus("Starting " + attackMode + " attack...");

            // Start the process.
            hashcatManager.startAttackWithFile(
                    hashFile,
                    mode,
                    attackMode,
                    target,
                    ruleFile.isEmpty() ? null : ruleFile,
                    force,
                    optimizedKernels,
                    workloadProfile
            );

        } catch (IOException ex) {
            ErrorUtil.showError("Hashcat Error", "Error starting hashcat process: " + ex.getMessage());
        }
    }

    /**
     * Handler for the "Stop Attack" button.
     */
    @FXML
    private void stopAttack() {
        hashcatManager.stopCracking();
    }

    /**
     * Dynamically creates the UI elements for a Dictionary attack.
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
     * Dynamically creates the UI elements for a Mask attack.
     */
    private void createMaskInput() {
        attackInputsContainer.getChildren().clear();

        Label maskLabel = new Label("Mask:");
        maskField = new TextField();
        maskField.setPromptText("e.g., ?d?d?d?d");

        // Create helper buttons for inserting mask characters.
        HBox maskHelperButtons = new HBox(5);
        maskHelperButtons.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label helperLabel = new Label("Append:");
        Button lowerAlphaButton = new Button("?l");
        lowerAlphaButton.setOnAction(e -> maskField.appendText("?l"));
        Button upperAlphaButton = new Button("?u");
        upperAlphaButton.setOnAction(e -> maskField.appendText("?u"));
        Button digitsButton = new Button("?d");
        digitsButton.setOnAction(e -> maskField.appendText("?d"));
        Button specialButton = new Button("?s");
        specialButton.setOnAction(e -> maskField.appendText("?s"));
        Button allButton = new Button("?a");
        allButton.setOnAction(e -> maskField.appendText("?a"));

        maskHelperButtons.getChildren().addAll(helperLabel, lowerAlphaButton, upperAlphaButton, digitsButton, specialButton, allButton);

        VBox maskLayout = new VBox(10, maskLabel, maskField, maskHelperButtons);
        attackInputsContainer.getChildren().add(maskLayout);
    }

    /**
     * A simple heuristic-based method to identify a hash type from a string.
     *
     * @param hash The hash string to analyze.
     * @return A string representing the suggested hashcat mode ID, or null if not identified.
     */
    private String identifyHashType(String hash) {
        // NTLM (or MD5) - 32 hex chars.
        if (hash.length() == 32 && hash.matches("^[a-fA-F0-9]{32}$")) {
            return "1000"; // Default to NTLM as it's common in password dumps.
        }
        // SHA-1 - 40 hex chars.
        if (hash.length() == 40 && hash.matches("^[a-fA-F0-9]{40}$")) {
            return "100";
        }
        // bcrypt - Starts with $2a$, $2b$, or $2y$.
        if (hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$")) {
            return "3200";
        }
        // sha512crypt - Starts with $6$.
        if (hash.startsWith("$6$")) {
            return "1800";
        }
        // TODO: Add more rules for other hash types.
        return null; // Not identified.
    }

    /**
     * Helper to process a selected file via NormalizationUtil and update the UI.
     *
     * @param inputFile The raw file selected by the user.
     */
    private void normalizeAndSetHashFile(File inputFile) {
        try {
            app.updateStatus("Normalizing and cleaning hash file...");
            // Run the normalization utility.
            File normalizedFile = NormalizationUtil.normalizeHashFile(inputFile);
            // Update the text field with the path to the temp file.
            hashFileField.setText(normalizedFile.getAbsolutePath());
            app.updateStatus("Hash file processed successfully.");
        } catch (IOException e) {
            ErrorUtil.showError("File Processing Error", "Failed to process the selected file: " + e.getMessage());
        }
    }
}
