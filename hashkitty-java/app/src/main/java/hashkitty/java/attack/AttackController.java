package hashkitty.java.attack;

import hashkitty.java.App;
import hashkitty.java.hashcat.HashcatManager;
import hashkitty.java.util.ErrorUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AttackController {

    @FXML
    private TextField hashFileField;
    @FXML
    private ComboBox<String> hashModeField;
    @FXML
    private ComboBox<String> attackModeSelector;
    @FXML
    private TextField ruleFileField;
    @FXML
    private VBox attackInputsContainer;
    @FXML
    private CheckBox forceCheckbox;
    @FXML
    private CheckBox optimizedKernelsCheckbox;
    @FXML
    private ComboBox<String> workloadProfileSelector;

    private HashcatManager hashcatManager;
    private Stage primaryStage;
    private App app;

    // These fields are for the dynamic input area
    private TextField wordlistField;
    private TextField maskField;

    @FXML
    public void initialize() {
        attackModeSelector.getItems().addAll("Dictionary", "Mask", "Brute-force");
        attackModeSelector.setValue("Dictionary");

        // Populate the hash mode selector with common types
        hashModeField.getItems().addAll(
                "0 - MD5",
                "1000 - NTLM",
                "1800 - sha512crypt, SHA-512 (Unix)",
                "3200 - bcrypt, Blowfish (Unix)",
                "22000 - WPA-PBKDF2-PMKID+EAPOL",
                "13100 - Kerberos 5 TGS-REP etype 23"
        );

        // Populate the workload profile selector
        workloadProfileSelector.getItems().addAll(
                "1 - Low",
                "2 - Default",
                "3 - High",
                "4 - Nightmare"
        );
        workloadProfileSelector.setValue("2 - Default");

        // Add listener to switch input fields based on attack mode
        attackModeSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            if ("Dictionary".equals(newVal)) {
                createDictionaryInput();
            } else {
                createMaskInput();
            }
        });

        // Create the initial input field
        createDictionaryInput();
    }

    public void initData(App app, HashcatManager hashcatManager, Stage primaryStage) {
        this.app = app;
        this.hashcatManager = hashcatManager;
        this.primaryStage = primaryStage;
    }

    @FXML
    private void chooseHashFile() {
        File file = new FileChooser().showOpenDialog(primaryStage);
        if (file != null) {
            hashFileField.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void chooseRuleFile() {
        File file = new FileChooser().showOpenDialog(primaryStage);
        if (file != null) {
            ruleFileField.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void identifyHash() {
        String hashFilePath = hashFileField.getText();
        if (hashFilePath == null || hashFilePath.isEmpty()) {
            ErrorUtil.showError("Input Error", "Please select a hash file first.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(hashFilePath))) {
            String firstLine = reader.readLine();
            if (firstLine == null || firstLine.isEmpty()) {
                ErrorUtil.showError("File Error", "The selected hash file is empty.");
                return;
            }

            String identifiedMode = identifyHashType(firstLine.trim());
            if (identifiedMode != null) {
                // Find the full description in the combo box
                Optional<String> match = hashModeField.getItems().stream()
                        .filter(item -> item.startsWith(identifiedMode + " "))
                        .findFirst();

                if (match.isPresent()) {
                    hashModeField.setValue(match.get());
                    app.updateStatus("Identified hash type: " + match.get());
                } else {
                    // If not in the list, just set the mode number
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

    @FXML
    private void importPotfile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select John the Ripper Potfile");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JtR Potfile", "*.pot"));
        File potfile = fileChooser.showOpenDialog(primaryStage);

        if (potfile != null) {
            List<String> hashes = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(potfile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    int lastColonIndex = line.lastIndexOf(':');
                    if (lastColonIndex > 0) { // Ensure there is a colon and the hash part is not empty
                        String hash = line.substring(0, lastColonIndex);
                        if (!hash.isEmpty()) {
                            hashes.add(hash);
                        }
                    }
                }
            } catch (IOException e) {
                ErrorUtil.showError("File Error", "Failed to read the potfile: " + e.getMessage());
                return;
            }

            if (!hashes.isEmpty()) {
                try {
                    File tempFile = File.createTempFile("imported_hashes_", ".txt");
                    tempFile.deleteOnExit(); // Clean up the file when the application closes

                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                        for (String hash : hashes) {
                            writer.write(hash);
                            writer.newLine();
                        }
                    }

                    hashFileField.setText(tempFile.getAbsolutePath());
                    app.updateStatus("Successfully imported " + hashes.size() + " hashes from potfile.");

                } catch (IOException e) {
                    ErrorUtil.showError("File Error", "Failed to create temporary hash file: " + e.getMessage());
                }
            } else {
                ErrorUtil.showError("Import Failed", "No valid hashes were found in the selected potfile.");
            }
        }
    }

    @FXML
    private void startAttack() {
        try {
            String hashFile = hashFileField.getText();
            String modeInput = hashModeField.getValue();
            String attackMode = attackModeSelector.getValue();
            String ruleFile = ruleFileField.getText();
            String target;

            if ("Dictionary".equals(attackMode)) {
                target = wordlistField.getText();
            } else { // Mask attack
                target = maskField.getText();
            }

            if (hashFile.isEmpty() || modeInput == null || modeInput.isEmpty() || target.isEmpty()) {
                ErrorUtil.showError("Missing Information", "Hash File, Hash Mode, and Wordlist/Mask cannot be empty.");
                return;
            }

            // Handle case where user selects "0 - MD5", we only want "0"
            String mode = modeInput.split(" ")[0];
            boolean force = forceCheckbox.isSelected();
            boolean optimizedKernels = optimizedKernelsCheckbox.isSelected();
            String workloadProfileInput = workloadProfileSelector.getValue();
            String workloadProfile = workloadProfileInput != null ? workloadProfileInput.split(" ")[0] : null;

            app.updateStatus("Starting " + attackMode + " attack...");
            hashcatManager.startAttackWithFile(hashFile, mode, attackMode, target, ruleFile.isEmpty() ? null : ruleFile, force, optimizedKernels, workloadProfile);
        } catch (IOException ex) {
            ErrorUtil.showError("Hashcat Error", "Error starting hashcat process: " + ex.getMessage());
        }
    }

    @FXML
    private void stopAttack() {
        hashcatManager.stopCracking();
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
     * @param hash The hash string to analyze.
     * @return A string representing the suggested hashcat mode, or null if not identified.
     */
    private String identifyHashType(String hash) {
        // NTLM (or MD5)
        if (hash.length() == 32 && hash.matches("^[a-fA-F0-9]{32}$")) {
            return "1000"; // Default to NTLM as it's common in password dumps
        }
        // SHA-1
        if (hash.length() == 40 && hash.matches("^[a-fA-F0-9]{40}$")) {
            return "100";
        }
        // bcrypt
        if (hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$")) {
            return "3200";
        }
        // sha512crypt
        if (hash.startsWith("$6$")) {
            return "1800";
        }
        // Could add many more rules here...
        return null; // Not identified
    }
}
