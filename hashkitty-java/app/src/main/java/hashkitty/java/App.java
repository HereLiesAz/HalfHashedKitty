package hashkitty.java;

import hashkitty.java.server.RelayServer;
import hashkitty.java.util.NetworkUtil;
import hashkitty.java.util.QRCodeUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class App extends Application {

    private static final int RELAY_PORT = 5001;
    private static final int QR_CODE_SIZE = 250;

    private TextArea statusLog;
    private Label crackedPasswordLabel;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("HashKitty");

        // UI Components
        Label titleLabel = new Label("Scan to Connect");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        ImageView qrCodeView = new ImageView();
        Label connectionLabel = new Label("Initializing...");

        statusLog = new TextArea();
        statusLog.setEditable(false);
        statusLog.setPrefHeight(150);

        crackedPasswordLabel = new Label("Cracked Password: N/A");
        crackedPasswordLabel.setStyle("-fx-font-size: 14px;");

        // Layout
        VBox root = new VBox(15, titleLabel, qrCodeView, connectionLabel, new Label("Status Log:"), statusLog, crackedPasswordLabel);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new javafx.geometry.Insets(20));

        // Get IP and generate QR code
        String ipAddress = NetworkUtil.getLocalIpAddress();
        if (ipAddress != null) {
            String connectionString = "ws://" + ipAddress + ":" + RELAY_PORT;
            qrCodeView.setImage(QRCodeUtil.generateQRCodeImage(connectionString, QR_CODE_SIZE, QR_CODE_SIZE));
            connectionLabel.setText("Connect at: " + connectionString);
        } else {
            connectionLabel.setText("Could not determine local IP address.");
        }

        primaryStage.setScene(new Scene(root, 450, 550));
        primaryStage.show();

        // Ensure the relay server is shut down when the application closes
        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
    }

    private void updateStatus(String message) {
        Platform.runLater(() -> statusLog.appendText(message + "\n"));
    }

    private void displayCrackedPassword(String password) {
        Platform.runLater(() -> crackedPasswordLabel.setText("Cracked Password: " + password));
    }

    public static void main(String[] args) {
        // We need to launch the app to have the UI components ready
        // So we will start the server after the app has been initialized
        // This is a bit of a workaround for passing UI update callbacks to the server
        Application.launch(args);
    }

    @Override
    public void init() throws Exception {
        super.init();
        // Start the relay server in a new thread
        Thread relayThread = new Thread(() -> {
            // Pass in the UI update methods as callbacks
            RelayServer server = new RelayServer(RELAY_PORT, this::updateStatus, this::displayCrackedPassword);
            server.start();
        });
        relayThread.setDaemon(true);
        relayThread.start();
    }
}