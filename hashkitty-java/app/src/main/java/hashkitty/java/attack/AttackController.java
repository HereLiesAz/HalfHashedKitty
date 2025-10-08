package hashkitty.java.attack;

import hashkitty.java.App;
import hashkitty.java.hashcat.HashcatManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class AttackController {

    @FXML
    private TextField hashFileField;
    @FXML
    private TextField hashModeField;
    @FXML
    private ComboBox<String> attackModeSelector;
    @FXML
    private TextField ruleFileField;
    @FXML
    private VBox attackInputsContainer;

    private HashcatManager hashcatManager;
    private Stage primaryStage;
    private App app;

    // These fields are for the dynamic input area
    private TextField wordlistField;
    private TextField maskField;

    @FXML
    public void initialize() {
        attackModeSelector.getItems().addAll("Dictionary", "Mask");
        attackModeSelector.setValue("Dictionary");

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
    private void startAttack() {
        try {
            String hashFile = hashFileField.getText();
            String mode = hashModeField.getText();
            String attackMode = attackModeSelector.getValue();
            String ruleFile = ruleFileField.getText();
            String target;

            if ("Dictionary".equals(attackMode)) {
                target = wordlistField.getText();
            } else { // Mask attack
                target = maskField.getText();
            }

            if (hashFile.isEmpty() || mode.isEmpty() || target.isEmpty()) {
                app.updateStatus("Error: Hash File, Hash Mode, and Wordlist/Mask cannot be empty.");
                return;
            }

            app.updateStatus("Starting " + attackMode + " attack...");
            hashcatManager.startAttackWithFile(hashFile, mode, attackMode, target, ruleFile.isEmpty() ? null : ruleFile);
        } catch (IOException ex) {
            app.updateStatus("Error starting hashcat process: " + ex.getMessage());
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
}
