package hashkitty.java.server;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import hashkitty.java.hashcat.HashcatManager;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.function.Consumer;

public class DirectServer extends WebSocketServer {

    private final Gson gson = new Gson();
    private final HashcatManager hashcatManager;
    private final Consumer<String> onStatusUpdate;
    private WebSocket connectedClient = null;

    public DirectServer(int port, Consumer<String> onStatusUpdate, Consumer<String> onPasswordCracked) {
        super(new InetSocketAddress(port));
        this.onStatusUpdate = onStatusUpdate;
        this.hashcatManager = new HashcatManager(crackedPassword -> {
            onPasswordCracked.accept(crackedPassword);
            sendCrackedPassword(crackedPassword);
        });
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        // Only allow one client at a time in direct mode
        if (connectedClient != null && connectedClient.isOpen()) {
            onStatusUpdate.accept("Direct connection already active. Closing new connection from " + conn.getRemoteSocketAddress());
            conn.close(1013, "Server busy"); // Service Restart
            return;
        }
        this.connectedClient = conn;
        System.out.println("New direct connection: " + conn.getRemoteSocketAddress());
        onStatusUpdate.accept("Direct client connected: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        if (conn.equals(connectedClient)) {
            System.out.println("Direct connection closed: " + conn.getRemoteSocketAddress());
            onStatusUpdate.accept("Direct client disconnected.");
            this.connectedClient = null;
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            Message msg = gson.fromJson(message, Message.class);
            if ("attack".equalsIgnoreCase(msg.getType())) {
                handleAttack(msg);
            }
        } catch (JsonSyntaxException e) {
            System.err.println("Failed to parse message: " + message);
        }
    }

    private void handleAttack(Message msg) {
        onStatusUpdate.accept("Starting direct attack on hash: " + msg.getHash());
        try {
            // This logic is similar to the RelayServer, but simpler.
            String wordlistPath = "/app/test-hashes-short.txt"; // Using a test wordlist for now
            hashcatManager.startCracking(msg.getHash(), msg.getMode(), "Dictionary", wordlistPath);
        } catch (IOException e) {
            e.printStackTrace();
            onStatusUpdate.accept("Error starting hashcat: " + e.getMessage());
        }
    }

    private void sendCrackedPassword(String password) {
        if (connectedClient != null && connectedClient.isOpen()) {
            Message response = new Message();
            response.setType("cracked");
            response.setPayload(password);
            String jsonResponse = gson.toJson(response);

            System.out.println("Sending cracked password to direct client.");
            connectedClient.send(jsonResponse);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
        onStatusUpdate.accept("Server error: " + ex.getMessage());
        if (conn != null && conn.equals(connectedClient)) {
            this.connectedClient = null;
        }
    }

    @Override
    public void onStart() {
        System.out.println("Direct connection server started on port " + getPort());
        onStatusUpdate.accept("Direct connection server started on port " + getPort());
    }

    // Inner class for message deserialization
    private static class Message {
        private String type;
        private String hash;
        private String mode;
        private String payload;

        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getHash() { return hash; }
        public String getMode() { return mode; }
        public void setPayload(String payload) { this.payload = payload; }
    }
}