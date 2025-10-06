package hashkitty.java.relay;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.function.Consumer;

/**
 * A WebSocket client for connecting to the standalone gokitty-relay server.
 * It handles joining a room, receiving messages, and sending messages.
 */
public class RelayClient extends WebSocketClient {

    private final String roomId;
    private final Consumer<Message> onMessageReceived;
    private final Consumer<String> onStatusUpdate;
    private final Gson gson = new Gson();

    /**
     * Constructs a new RelayClient.
     *
     * @param serverUri       The URI of the relay server.
     * @param roomId          The unique room ID to join.
     * @param onMessageReceived A callback for when a message is received from the relay.
     * @param onStatusUpdate  A callback for status and error messages.
     */
    public RelayClient(URI serverUri, String roomId, Consumer<Message> onMessageReceived, Consumer<String> onStatusUpdate) {
        super(serverUri);
        this.roomId = roomId;
        this.onMessageReceived = onMessageReceived;
        this.onStatusUpdate = onStatusUpdate;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        onStatusUpdate.accept("Connected to relay server. Joining room: " + roomId);
        Message joinMessage = new Message();
        joinMessage.setType("join");
        joinMessage.setRoomId(roomId);
        send(gson.toJson(joinMessage));
    }

    @Override
    public void onMessage(String message) {
        onStatusUpdate.accept("Relay Client Received: " + message);
        try {
            Message msg = gson.fromJson(message, Message.class);
            onMessageReceived.accept(msg);
        } catch (JsonSyntaxException e) {
            onStatusUpdate.accept("Error parsing message from relay: " + e.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        onStatusUpdate.accept("Disconnected from relay. Reason: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        onStatusUpdate.accept("Relay client error: " + ex.getMessage());
        ex.printStackTrace();
    }

    /**
     * Sends a message object to the relay server after converting it to JSON.
     * @param message The message object to send.
     */
    public void sendMessage(Message message) {
        if (isOpen()) {
            send(gson.toJson(message));
        } else {
            onStatusUpdate.accept("Cannot send message: Relay client is not connected.");
        }
    }

    /**
     * A simple inner class representing the message structure for JSON serialization.
     */
    public static class Message {
        private String type;
        private String roomId;
        private String hash;
        private String mode;
        private String payload;

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