package com.hereliesaz.connectionmanager;

import com.hereliesaz.connectionmanager.ui.MainFrame;
import com.hereliesaz.connectionmanager.util.IpUtils;
import com.hereliesaz.connectionmanager.websocket.WebSocketManager;

import javax.swing.*;

import com.formdev.flatlaf.FlatDarkLaf;

public class Main {
    private static final int PORT = 8080;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }

        String ip = IpUtils.getLocalIpAddress();
        if (ip == null) {
            JOptionPane.showMessageDialog(null, "Could not find local IP address. Please check your network connection.", "Network Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String connectionString = ip + ":" + PORT;

        // Start WebSocket server
        WebSocketManager webSocketManager = new WebSocketManager(PORT);
        webSocketManager.start();

        // Create and show the main UI
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame(connectionString);
            frame.setVisible(true);
        });
    }
}
