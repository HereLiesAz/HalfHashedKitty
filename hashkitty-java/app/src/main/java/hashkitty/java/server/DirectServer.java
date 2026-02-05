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
 * A simplified WebSocket server implementation designed for direct, one-to-one communication.
 * <p>
 * Unlike the {@link RelayServer}, which broadcasts messages between multiple clients in a "room",
 * this server is intended for a direct connection between a single Android client and this Desktop instance,
 * typically over a local network (LAN). It acts as a dedicated endpoint for controlling this specific instance.
 * </p>
 * <p>
 * It enforces a single-client policy, rejecting new connections if a client is already connected.
 * </p>
 */
public class DirectServer extends WebSocketServer {

    /**
     * Gson instance for JSON serialization and deserialization.
     */
    private final Gson gson = new Gson();

    /**
     * Manager for handling Hashcat process execution.
     */
    private final HashcatManager hashcatManager;

    /**
     * Callback interface to report status updates back to the UI or console.
     */
    private final Consumer<String> onStatusUpdate;

    /**
     * Holds the reference to the currently connected client.
     * Null if no client is connected.
     */
    private WebSocket connectedClient = null;

    /**
     * Constructs a new DirectServer instance.
     *
     * @param port              The TCP port to listen on.
     * @param onStatusUpdate    Callback to be invoked when there is a status message to log (e.g., "Client connected").
     * @param onPasswordCracked Callback to be invoked when Hashcat successfully cracks a password.
     */
    public DirectServer(int port, Consumer<String> onStatusUpdate, Consumer<String> onPasswordCracked) {
        // Initialize the parent WebSocketServer with the listening address.
        super(new InetSocketAddress(port));
        this.onStatusUpdate = onStatusUpdate;
        // Initialize the HashcatManager. Note that the 'onComplete' callback is null here as we rely on explicit status updates.
        this.hashcatManager = new HashcatManager(onPasswordCracked, onStatusUpdate, null);
    }

    /**
     * triggered when a new WebSocket connection is attempted.
     *
     * @param conn      The WebSocket connection object.
     * @param handshake The handshake data from the client.
     */
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        // Check if there is already an active client connection.
        if (connectedClient != null && connectedClient.isOpen()) {
            // Log the rejection.
            onStatusUpdate.accept("Direct connection already active. Closing new connection from " + conn.getRemoteSocketAddress());
            // Close the new connection with a specific code (1013: Try Again Later) and message.
            conn.close(1013, "Server busy");
            return;
        }
        // Accept the new connection.
        this.connectedClient = conn;
        // Log the connection to stdout and the status callback.
        System.out.println("New direct connection: " + conn.getRemoteSocketAddress());
        onStatusUpdate.accept("Direct client connected: " + conn.getRemoteSocketAddress());
    }

    /**
     * Triggered when a WebSocket connection is closed.
     *
     * @param conn   The WebSocket connection that was closed.
     * @param code   The closure code.
     * @param reason The reason string.
     * @param remote True if the closure was initiated by the remote peer.
     */
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        // Only perform cleanup if the closed connection was the currently tracked client.
        if (conn.equals(connectedClient)) {
            System.out.println("Direct connection closed: " + conn.getRemoteSocketAddress());
            onStatusUpdate.accept("Direct client disconnected.");
            // clear the reference to allow a new connection.
            this.connectedClient = null;
        }
    }

    /**
     * Triggered when a text message is received from a client.
     *
     * @param conn    The WebSocket connection sending the message.
     * @param message The message content (expected to be JSON).
     */
    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            // Deserialize the JSON message into the Message inner class.
            Message msg = gson.fromJson(message, Message.class);
            // Check if the message type is an "attack" command.
            if ("attack".equalsIgnoreCase(msg.getType())) {
                handleAttack(msg);
            }
        } catch (JsonSyntaxException e) {
            // Handle invalid JSON format.
            System.err.println("Failed to parse message: " + message);
            onStatusUpdate.accept("Error: Received a malformed message from a client.");
        }
    }

    /**
     * Processes an "attack" command received from the client.
     *
     * @param msg The parsed message object containing attack details (hash, mode, etc.).
     */
    private void handleAttack(Message msg) {
        // Notify UI/Logs that an attack is starting.
        onStatusUpdate.accept("Starting direct attack on hash: " + msg.getHash());
        try {
            // Hardcoded attack parameters for now (Proof of Concept).
            // In a production version, these should be parsed from the message payload or configuration.
            String attackMode = "Dictionary";
            String wordlistPath = "/app/test-hashes-short.txt"; // Using a bundled test wordlist

            // Instruct HashcatManager to start the attack.
            hashcatManager.startAttackWithString(msg.getHash(), msg.getMode(), attackMode, wordlistPath, null);
        } catch (IOException e) {
            // Handle process launch failures.
            onStatusUpdate.accept("Error starting hashcat: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sends a cracked password back to the connected client.
     * <p>
     * This method constructs a JSON response with type "cracked" and the password as the payload.
     * </p>
     *
     * @param password The plaintext password recovered by Hashcat.
     */
    private void sendCrackedPassword(String password) {
        // Ensure the client is still connected before attempting to send.
        if (connectedClient != null && connectedClient.isOpen()) {
            // Create the response object.
            Message response = new Message();
            response.setType("cracked");
            response.setPayload(password);

            // Serialize to JSON.
            String jsonResponse = gson.toJson(response);

            System.out.println("Sending cracked password to direct client.");
            // Send the JSON string over the WebSocket.
            connectedClient.send(jsonResponse);
        }
    }

    /**
     * Triggered when an error occurs on the WebSocket connection.
     *
     * @param conn The connection where the error occurred (may be null if server-level error).
     * @param ex   The exception.
     */
    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
        onStatusUpdate.accept("Server error: " + ex.getMessage());
        // If the error was on the active client connection, reset the state.
        if (conn != null && conn.equals(connectedClient)) {
            this.connectedClient = null;
        }
    }

    /**
     * Triggered when the server has successfully started listening.
     */
    @Override
    public void onStart() {
        System.out.println("Direct connection server started on port " + getPort());
        onStatusUpdate.accept("Direct connection server started on port " + getPort());
    }

    /**
     * Inner class representing the structure of JSON messages exchanged with the client.
     * Used for Gson serialization/deserialization.
     */
    private static class Message {
        /** The type of message (e.g., "attack", "cracked"). */
        private String type;
        /** The target hash string (for attack requests). */
        private String hash;
        /** The hash mode (e.g., "0" for MD5). */
        private String mode;
        /** General payload field (used for the cracked password in responses). */
        private String payload;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getHash() { return hash; }
        public String getMode() { return mode; }
        public void setPayload(String payload) { this.payload = payload; }
    }
}
