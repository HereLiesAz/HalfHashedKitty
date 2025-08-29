package com.hereliesaz.connectionmanager.websocket;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.net.InetSocketAddress;

public class WebSocketManager {
    private WebSocketServer server;
    private int port;

    public WebSocketManager(int port) {
        this.port = port;
    }

    public void start() {
        server = new WebSocketServer(new InetSocketAddress(port)) {
            @Override
            public void onOpen(WebSocket conn, ClientHandshake handshake) {
                System.out.println("New connection from " + conn.getRemoteSocketAddress().getAddress().getHostAddress());
            }

            @Override
            public void onClose(WebSocket conn, int code, String reason, boolean remote) {
                System.out.println("Closed connection to " + conn.getRemoteSocketAddress().getAddress().getHostAddress());
            }

            @Override
            public void onMessage(WebSocket conn, String message) {
                System.out.println("Received message from " + conn.getRemoteSocketAddress().getAddress().getHostAddress() + ": " + message);
            }

            @Override
            public void onError(WebSocket conn, Exception ex) {
                ex.printStackTrace();
            }

            @Override
            public void onStart() {
                System.out.println("WebSocket server started on port " + getPort());
            }
        };
        server.start();
    }

    public void stop() throws InterruptedException {
        if (server != null) {
            server.stop();
        }
    }
}
