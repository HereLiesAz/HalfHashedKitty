package hashkitty.java.relay;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.function.Consumer;

/**
 * A WebSocket client responsible for connecting to a Relay Server.
 * <p>
 * This client enables the Desktop Application to communicate with the Relay Server (which might be
 * running locally or remotely). It handles the full lifecycle of the connection:
 * <ul>
 *     <li>Connecting to the WebSocket endpoint.</li>
 *     <li>Sending the initial "join" command to subscribe to a specific room.</li>
 *     <li>Listening for incoming messages (commands from Android or status updates).</li>
 *     <li>Sending messages (like cracked password notifications).</li>
 * </ul>
 * </p>
 */
public class RelayClient extends WebSocketClient {

    /** The unique ID of the room this client intends to join upon connection. */
    private final String roomId;

    /** Callback to invoke when a valid JSON message is received from the relay. */
    private final Consumer<Message> onMessageReceived;

    /** Callback to invoke for general status logging (connection success, errors, etc.). */
    private final Consumer<String> onStatusUpdate;

    /** Gson instance for JSON serialization/deserialization. */
    private final Gson gson = new Gson();

    /**
     * Constructs a new RelayClient instance.
     *
     * @param serverUri       The URI of the relay server (e.g., "ws://localhost:5001/ws").
     * @param roomId          The unique room ID to join (must match the ID used by the Android client).
     * @param onMessageReceived A functional callback to handle parsed messages.
     * @param onStatusUpdate  A functional callback to handle log messages.
     */
    public RelayClient(URI serverUri, String roomId, Consumer<Message> onMessageReceived, Consumer<String> onStatusUpdate) {
        super(serverUri);
        this.roomId = roomId;
        this.onMessageReceived = onMessageReceived;
        this.onStatusUpdate = onStatusUpdate;
    }

    /**
     * Triggered when the WebSocket connection is successfully established.
     *
     * @param handshakedata The handshake data from the server.
     */
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        onStatusUpdate.accept("Connected to relay server. Joining room: " + roomId);
        // Construct a "join" message to inform the relay server which room we want to enter.
        Message joinMessage = new Message();
        joinMessage.setType("join");
        joinMessage.setRoomId(roomId);
        // Serialize and send the message.
        send(gson.toJson(joinMessage));
    }

    /**
     * Triggered when a text message is received from the server.
     *
     * @param message The raw message string.
     */
    @Override
    public void onMessage(String message) {
        // Log the raw message reception.
        onStatusUpdate.accept("Relay Client Received: " + message);
        try {
            // Attempt to parse the JSON string into a Message object.
            Message msg = gson.fromJson(message, Message.class);
            // Pass the parsed object to the consumer callback.
            onMessageReceived.accept(msg);
        } catch (JsonSyntaxException e) {
            // Handle malformed JSON.
            onStatusUpdate.accept("Error parsing message from relay: " + e.getMessage());
        }
    }

    /**
     * Triggered when the connection is closed.
     *
     * @param code   The closure code (e.g., 1000 for normal).
     * @param reason The reason string provided by the peer.
     * @param remote True if the closure was initiated by the remote server.
     */
    @Override
    public void onClose(int code, String reason, boolean remote) {
        onStatusUpdate.accept("Disconnected from relay. Reason: " + reason);
    }

    /**
     * Triggered when an error occurs (e.g., network failure).
     *
     * @param ex The exception describing the error.
     */
    @Override
    public void onError(Exception ex) {
        onStatusUpdate.accept("Relay client error: " + ex.getMessage());
        ex.printStackTrace();
    }

    /**
     * Sends a structured message object to the relay server.
     * This helper method handles JSON serialization.
     *
     * @param message The Message object to send.
     */
    public void sendMessage(Message message) {
        if (isOpen()) {
            // Serialize the object to JSON and send it.
            send(gson.toJson(message));
        } else {
            // Warn if trying to send while disconnected.
            onStatusUpdate.accept("Cannot send message: Relay client is not connected.");
        }
    }

    /**
     * A Data Transfer Object (DTO) representing the standard message format used by the relay.
     * This static inner class ensures type safety for JSON operations.
     */
    public static class Message {
        /** The type of the message (e.g., "join", "attack", "cracked"). */
        private String type;
        /** The room ID associated with the message. */
        private String roomId;
        /** The target hash (if type is "attack"). */
        private String hash;
        /** The hash mode (if type is "attack"). */
        private String mode;
        /** Generic payload (e.g., cracked password content). */
        private String payload;

        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getRoomId() { return roomId; }
        public void setRoomId(String roomId) { this.roomId = roomId; }
        public String getHash() { return hash; }
        public void setHash(String hash) { this.hash = hash; }
        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        public String getPayload() { return payload; }
        public void setPayload(String payload) { this.payload = payload; }
    }
}
