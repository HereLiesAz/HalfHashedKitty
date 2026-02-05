package hashkitty.java.server;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import hashkitty.java.hashcat.HashcatManager;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * A WebSocket server implementation that acts as a relay, facilitating real-time communication
 * between multiple clients organized into "rooms".
 * <p>
 * This server enables the "Anywhere Access" feature of Half-Hashed Kitty. By having both the
 * Desktop App (controller) and the Android App (remote) connect to this relay and join the
 * same room ID, they can exchange messages (attack commands, status updates) without needing
 * a direct peer-to-peer connection (which is often blocked by NATs/Firewalls).
 * </p>
 * <p>
 * <b>Logic Flow:</b>
 * <ol>
 *     <li>Clients connect via WebSocket.</li>
 *     <li>Client sends a "join" message with a {@code roomId}.</li>
 *     <li>Server validates and associates the client connection with that room.</li>
 *     <li>When a client sends a message, the server broadcasts it to all *other* clients in that room (enforcing the room associated with the session).</li>
 * </ol>
 * </p>
 */
public class RelayServer extends WebSocketServer {

    /**
     * Stores the active rooms and their connected clients.
     * Key: Room ID (String).
     * Value: A thread-safe Set of WebSocket connections.
     */
    private final Map<String, Set<WebSocket>> rooms = new ConcurrentHashMap<>();

    /**
     * Stores the room ID associated with each WebSocket connection.
     * Key: WebSocket connection.
     * Value: Room ID (String).
     */
    private final Map<WebSocket, String> connectionRoomMap = new ConcurrentHashMap<>();

    /**
     * Gson instance for JSON operations.
     */
    private final Gson gson = new Gson();

    /**
     * Manager for Hashcat operations.
     * NOTE: In a pure relay scenario (cloud hosted), this might not be used directly,
     * but if the Desktop App hosts the relay, it can also act as a worker directly.
     */
    private final HashcatManager hashcatManager;

    /**
     * Callback for general status logging.
     */
    private final Consumer<String> onStatusUpdate;

    /**
     * Tracks the room ID currently running an attack (if this instance is handling the attack locally).
     */
    private String currentAttackingRoomId;

    /**
     * Constructs a new RelayServer.
     *
     * @param port              The TCP port to listen on.
     * @param onStatusUpdate    Callback for logging status messages.
     * @param onPasswordCracked Callback for when a password is recovered (if running locally).
     */
    public RelayServer(int port, Consumer<String> onStatusUpdate, Consumer<String> onPasswordCracked) {
        super(new InetSocketAddress(port));
        this.onStatusUpdate = onStatusUpdate;
        // Initialize HashcatManager for local execution capability.
        this.hashcatManager = new HashcatManager(onPasswordCracked, onStatusUpdate, null);
    }

    /**
     * Triggered when a new client connects.
     *
     * @param conn      The WebSocket connection.
     * @param handshake The handshake details.
     */
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("New connection: " + conn.getRemoteSocketAddress());
        onStatusUpdate.accept("Client connected: " + conn.getRemoteSocketAddress());
    }

    /**
     * Triggered when a client disconnects.
     *
     * @param conn   The connection.
     * @param code   Close code.
     * @param reason Close reason.
     * @param remote Initiator.
     */
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Closed connection: " + conn.getRemoteSocketAddress());
        onStatusUpdate.accept("Client disconnected: " + conn.getRemoteSocketAddress());
        // Ensure the client is removed from any rooms they were in to prevent memory leaks or stale delivery.
        removeConnectionFromAllRooms(conn);
    }

    /**
     * Triggered when a message is received.
     *
     * @param conn    The sender.
     * @param message The message text (JSON).
     */
    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            // Parse the message.
            Message msg = gson.fromJson(message, Message.class);

            // Handle specific command types.
            if ("join".equalsIgnoreCase(msg.getType())) {
                // Client requesting to join a room.
                if (msg.getRoomId() != null && !msg.getRoomId().isEmpty()) {
                    joinRoom(conn, msg.getRoomId());
                }
            } else {
                // Determine the correct room based on the authenticated session, not the payload.
                String sessionRoomId = connectionRoomMap.get(conn);
                if (sessionRoomId != null) {
                    if ("attack".equalsIgnoreCase(msg.getType())) {
                        // Inject the trusted room ID into the message for internal handling
                        msg.setRoomId(sessionRoomId);
                        // If this server is also the worker (Desktop App hosting relay), handle the attack.
                        handleAttack(msg);
                    }
                    // The core relay function: Broadcast the message to everyone else in the room.
                    broadcastToRoom(conn, sessionRoomId, message);
                } else {
                    // Client tried to send a message without joining a room first.
                    // Silently ignore or log warning.
                    onStatusUpdate.accept("Warning: Client tried to send message without joining a room.");
                }
            }

        } catch (JsonSyntaxException e) {
            System.err.println("Failed to parse message: " + message);
            onStatusUpdate.accept("Error: Received a malformed message from a client.");
        }
    }

    /**
     * Handles an incoming "attack" message by initiating a local Hashcat process.
     * This allows the Relay Server to also function as an attack node.
     *
     * @param msg The attack configuration message.
     */
    private void handleAttack(Message msg) {
        // Store the room ID so we know where to send the result later.
        this.currentAttackingRoomId = msg.getRoomId();
        onStatusUpdate.accept("Starting attack from relay on hash: " + msg.getHash());
        try {
            // Define default attack parameters (currently hardcoded for MVP).
            String attackMode = "Dictionary";
            String wordlistPath = "/app/test-hashes-short.txt"; // Test wordlist location

            // Start the attack.
            hashcatManager.startAttackWithString(msg.getHash(), msg.getMode(), attackMode, wordlistPath, null);
        } catch (IOException e) {
            onStatusUpdate.accept("Error starting hashcat: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Broadcasts a cracked password to all clients in the active room.
     * <p>
     * This method is intended to be called by the `onPasswordCracked` callback passed to the constructor.
     * </p>
     *
     * @param password The recovered password.
     */
    private void broadcastCrackedPassword(String password) {
        if (currentAttackingRoomId != null) {
            // Construct the success message.
            Message response = new Message();
            response.setType("cracked");
            response.setRoomId(currentAttackingRoomId);
            response.setPayload(password);
            String jsonResponse = gson.toJson(response);

            // Find the clients in the room.
            Set<WebSocket> clients = rooms.get(currentAttackingRoomId);
            if (clients != null) {
                System.out.println("Broadcasting cracked password to room " + currentAttackingRoomId);
                // Send to all clients.
                for (WebSocket client : clients) {
                    if (client != null && client.isOpen()) {
                        client.send(jsonResponse);
                    }
                }
            }
        }
    }

    /**
     * Handles server-level errors.
     *
     * @param conn The connection associated with the error (if any).
     * @param ex   The exception.
     */
    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
        onStatusUpdate.accept("Server Error: " + ex.getMessage());
        if (conn != null) {
            // Clean up the connection if it's faulted.
            removeConnectionFromAllRooms(conn);
        }
    }

    /**
     * Triggered when the server starts successfully.
     */
    @Override
    public void onStart() {
        System.out.println("Relay server started on port " + getPort());
        onStatusUpdate.accept("Relay server started on port " + getPort());
    }

    /**
     * Adds a client to a specific room.
     *
     * @param conn   The client's connection.
     * @param roomId The target room ID.
     */
    private void joinRoom(WebSocket conn, String roomId) {
        // Ensure client isn't in multiple rooms simultaneously (simplifies logic).
        removeConnectionFromAllRooms(conn);

        // Add to the new room, creating the Set if it doesn't exist.
        // computeIfAbsent is atomic, ensuring thread safety.
        rooms.computeIfAbsent(roomId, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(conn);

        // Map the connection to the room ID for secure lookup later.
        connectionRoomMap.put(conn, roomId);

        System.out.println("Client " + conn.getRemoteSocketAddress() + " joined room " + roomId);
        onStatusUpdate.accept("Client joined room: " + roomId);
    }

    /**
     * Relays a message to all other peers in the room.
     *
     * @param sender  The connection that originated the message (will not receive the echo).
     * @param roomId  The room to broadcast to.
     * @param message The raw message string.
     */
    private void broadcastToRoom(WebSocket sender, String roomId, String message) {
        Set<WebSocket> clients = rooms.get(roomId);
        if (clients != null) {
            for (WebSocket client : clients) {
                // Check if client is valid, open, and NOT the sender.
                if (client != null && client.isOpen() && !client.equals(sender)) {
                    client.send(message);
                }
            }
        }
    }

    /**
     * Removes a connection from all rooms.
     * Called on disconnect or when switching rooms.
     *
     * @param conn The connection to remove.
     */
    private void removeConnectionFromAllRooms(WebSocket conn) {
        // Use the map for O(1) lookup of the room ID.
        String roomId = connectionRoomMap.remove(conn);

        if (roomId != null) {
            Set<WebSocket> roomClients = rooms.get(roomId);
            if (roomClients != null) {
                roomClients.remove(conn);
                System.out.println("Client " + conn.getRemoteSocketAddress() + " removed from room " + roomId);

                // If the room is now empty, remove the room entry entirely to save memory.
                if (roomClients.isEmpty()) {
                    rooms.remove(roomId);
                }
            }
        }
    }

    /**
     * DTO for JSON messages.
     */
    private static class Message {
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
        public String getMode() { return mode; }
        public void setPayload(String payload) { this.payload = payload; }
        public String getPayload() { return payload; }
    }
}
