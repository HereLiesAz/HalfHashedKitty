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

public class RelayServer extends WebSocketServer {

    private final Map<String, Set<WebSocket>> rooms = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();
    private final HashcatManager hashcatManager;
    private final Consumer<String> onStatusUpdate;
    private String currentAttackingRoomId;

    public RelayServer(int port, Consumer<String> onStatusUpdate, Consumer<String> onPasswordCracked) {
        super(new InetSocketAddress(port));
        this.onStatusUpdate = onStatusUpdate;
        this.hashcatManager = new HashcatManager(crackedPassword -> {
            // This callback is executed when hashcat finds a password
            onPasswordCracked.accept(crackedPassword);
            broadcastCrackedPassword(crackedPassword);
        });
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

            // For most messages, we just broadcast them
            broadcastToRoom(conn, msg.getRoomId(), message);

        } catch (JsonSyntaxException e) {
            System.err.println("Failed to parse message: " + message);
        }
    }

    private void handleAttack(Message msg) {
        this.currentAttackingRoomId = msg.getRoomId();
        onStatusUpdate.accept("Starting attack on hash: " + msg.getHash());
        try {
            // For now, we'll assume a wordlist is provided.
            // A real implementation would need more robust logic for different attack types.
            String wordlistPath = "/app/test-hashes-short.txt"; // Using a test wordlist
            hashcatManager.startCracking(msg.getHash(), msg.getMode(), wordlistPath);
        } catch (IOException e) {
            e.printStackTrace();
            onStatusUpdate.accept("Error starting hashcat: " + e.getMessage());
        }
    }

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
        if (conn != null) {
            removeConnectionFromAllRooms(conn);
        }
    }

    @Override
    public void onStart() {
        System.out.println("Relay server started on port " + getPort());
        onStatusUpdate.accept("Relay server started on port " + getPort());
    }

    private void joinRoom(WebSocket conn, String roomId) {
        removeConnectionFromAllRooms(conn);
        rooms.computeIfAbsent(roomId, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(conn);
        System.out.println("Client " + conn.getRemoteSocketAddress() + " joined room " + roomId);
        onStatusUpdate.accept("Client joined room: " + roomId);
    }

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

    // Inner class for message deserialization
    private static class Message {
        private String type;
        private String roomId;
        private String hash;
        private String mode;
        private String payload;

        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getRoomId() { return roomId; }
        public void setRoomId(String roomId) { this.roomId = roomId; }
        public String getHash() { return hash; }
        public String getMode() { return mode; }
        public void setPayload(String payload) { this.payload = payload; }
    }
}