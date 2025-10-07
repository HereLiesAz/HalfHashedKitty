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

/**
 * A WebSocket server that handles a direct, one-to-one connection with a single client.
 * This is simpler than the RelayServer and is intended for use when the client
 * and server are on the same local network.
 */
public class DirectServer extends WebSocketServer {

    private final Gson gson = new Gson();
    private final HashcatManager hashcatManager;
    private final Consumer<String> onStatusUpdate;
    private WebSocket connectedClient = null;

    /**
     * Constructs a new DirectServer.
     *
     * @param port              The port number for the server to listen on.
     * @param onStatusUpdate    A callback for status and error messages.
     * @param onPasswordCracked A callback for when a password is cracked.
     */
    public DirectServer(int port, Consumer<String> onStatusUpdate, Consumer<String> onPasswordCracked) {
        super(new InetSocketAddress(port));
        this.onStatusUpdate = onStatusUpdate;
        this.hashcatManager = new HashcatManager(onPasswordCracked, onStatusUpdate, null); // No UI to update on complete
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        // Only allow one client at a time in direct mode
        if (connectedClient != null && connectedClient.isOpen()) {
            onStatusUpdate.accept("Direct connection already active. Closing new connection from " + conn.getRemoteSocketAddress());
            conn.close(1013, "Server busy");
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
            onStatusUpdate.accept("Error: Received a malformed message from a client.");
        }
    }

    /**
     * Handles an incoming "attack" message by starting a hashcat process.
     * @param msg The deserialized message containing attack parameters.
     */
    private void handleAttack(Message msg) {
        onStatusUpdate.accept("Starting direct attack on hash: " + msg.getHash());
        try {
            String attackMode = "Dictionary";
            String wordlistPath = "/app/test-hashes-short.txt"; // Using a test wordlist for now
            hashcatManager.startAttackWithString(msg.getHash(), msg.getMode(), attackMode, wordlistPath, null);
        } catch (IOException e) {
            onStatusUpdate.accept("Error starting hashcat: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sends a cracked password back to the connected client.
     * @param password The cracked password to send.
     */
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

    /**
     * Inner class for deserializing incoming JSON messages.
     */
    private static class Message {
        private String type;
        private String hash;
        private String mode;
        private String payload;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getHash() { return hash; }
        public String getMode() { return mode; }
        public void setPayload(String payload) { this.payload = payload; }
    }
}