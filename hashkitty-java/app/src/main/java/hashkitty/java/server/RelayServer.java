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
 * A WebSocket server that acts as a relay, enabling multiple clients to communicate
 * by joining "rooms". Messages are broadcast to all clients in the same room,
 * except for the original sender.
 */
public class RelayServer extends WebSocketServer {

    private final Map<String, Set<WebSocket>> rooms = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();
    private final HashcatManager hashcatManager;
    private final Consumer<String> onStatusUpdate;
    private String currentAttackingRoomId;

    /**
     * Constructs a new RelayServer.
     *
     * @param port              The port number for the server to listen on.
     * @param onStatusUpdate    A callback for status and error messages.
     * @param onPasswordCracked A callback for when a password is cracked.
     */
    public RelayServer(int port, Consumer<String> onStatusUpdate, Consumer<String> onPasswordCracked) {
        super(new InetSocketAddress(port));
        this.onStatusUpdate = onStatusUpdate;
        this.hashcatManager = new HashcatManager(onPasswordCracked, onStatusUpdate, null); // No UI to update on complete
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("New connection: " + conn.getRemoteSocketAddress());
        onStatusUpdate.accept("Client connected: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Closed connection: " + conn.getRemoteSocketAddress());
        onStatusUpdate.accept("Client disconnected: " + conn.getRemoteSocketAddress());
        removeConnectionFromAllRooms(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            Message msg = gson.fromJson(message, Message.class);

            if (msg.getRoomId() == null || msg.getRoomId().isEmpty()) return;

            if ("join".equalsIgnoreCase(msg.getType())) {
                joinRoom(conn, msg.getRoomId());
            } else if ("attack".equalsIgnoreCase(msg.getType())) {
                handleAttack(msg);
            }

            broadcastToRoom(conn, msg.getRoomId(), message);

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
        this.currentAttackingRoomId = msg.getRoomId();
        onStatusUpdate.accept("Starting attack from relay on hash: " + msg.getHash());
        try {
            String attackMode = "Dictionary";
            String wordlistPath = "/app/test-hashes-short.txt"; // Using a test wordlist
            hashcatManager.startAttackWithString(msg.getHash(), msg.getMode(), attackMode, wordlistPath, null);
        } catch (IOException e) {
            onStatusUpdate.accept("Error starting hashcat: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Broadcasts a cracked password to all clients in the current attacking room.
     * @param password The cracked password to send.
     */
    private void broadcastCrackedPassword(String password) {
        if (currentAttackingRoomId != null) {
            Message response = new Message();
            response.setType("cracked");
            response.setRoomId(currentAttackingRoomId);
            response.setPayload(password);
            String jsonResponse = gson.toJson(response);

            Set<WebSocket> clients = rooms.get(currentAttackingRoomId);
            if (clients != null) {
                System.out.println("Broadcasting cracked password to room " + currentAttackingRoomId);
                for (WebSocket client : clients) {
                    if (client != null && client.isOpen()) {
                        client.send(jsonResponse);
                    }
                }
            }
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
        onStatusUpdate.accept("Server Error: " + ex.getMessage());
        if (conn != null) {
            removeConnectionFromAllRooms(conn);
        }
    }

    @Override
    public void onStart() {
        System.out.println("Relay server started on port " + getPort());
        onStatusUpdate.accept("Relay server started on port " + getPort());
    }

    /**
     * Adds a client's WebSocket connection to a specified room.
     * @param conn The client's WebSocket connection.
     * @param roomId The ID of the room to join.
     */
    private void joinRoom(WebSocket conn, String roomId) {
        removeConnectionFromAllRooms(conn);
        rooms.computeIfAbsent(roomId, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(conn);
        System.out.println("Client " + conn.getRemoteSocketAddress() + " joined room " + roomId);
        onStatusUpdate.accept("Client joined room: " + roomId);
    }

    /**
     * Broadcasts a message to all clients in a room except the sender.
     * @param sender The WebSocket connection of the message sender.
     * @param roomId The room to broadcast to.
     * @param message The message to send.
     */
    private void broadcastToRoom(WebSocket sender, String roomId, String message) {
        Set<WebSocket> clients = rooms.get(roomId);
        if (clients != null) {
            for (WebSocket client : clients) {
                if (client != null && client.isOpen() && !client.equals(sender)) {
                    client.send(message);
                }
            }
        }
    }

    /**
     * Removes a client's WebSocket connection from any room it might be in.
     * @param conn The connection to remove.
     */
    private void removeConnectionFromAllRooms(WebSocket conn) {
        for (Map.Entry<String, Set<WebSocket>> entry : rooms.entrySet()) {
            if (entry.getValue().remove(conn)) {
                System.out.println("Client " + conn.getRemoteSocketAddress() + " removed from room " + entry.getKey());
                if (entry.getValue().isEmpty()) {
                    rooms.remove(entry.getKey());
                }
                break;
            }
        }
    }

    /**
     * Inner class for deserializing incoming JSON messages.
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
    }
}