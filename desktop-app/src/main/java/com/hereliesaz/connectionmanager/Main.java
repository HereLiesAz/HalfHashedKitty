package com.hereliesaz.connectionmanager;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        try {
            String ip = getLocalIpAddress();
            if (ip == null) {
                System.err.println("Could not find local IP address.");
                return;
            }
            int port = 8080;
            String connectionString = ip + ":" + port;

            generateAndShowQrCode(connectionString);
            startWebSocketServer(port);
            createAndShowDockerControls();

        } catch (WriterException | IOException e) {
            e.printStackTrace();
        }
    }

    private static void startWebSocketServer(int port) {
        WebSocketServer server = new WebSocketServer(new InetSocketAddress(port)) {
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

    private static void generateAndShowQrCode(String connectionString) throws WriterException, IOException {
        int size = 400;
        String fileType = "png";
        File qrFile = new File("qr." + fileType);

        Map<EncodeHintType, Object> hintMap = new EnumMap<>(EncodeHintType.class);
        hintMap.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hintMap.put(EncodeHintType.MARGIN, 1);
        hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix byteMatrix = qrCodeWriter.encode(connectionString, BarcodeFormat.QR_CODE, size, size, hintMap);
        int matrixWidth = byteMatrix.getWidth();
        BufferedImage image = new BufferedImage(matrixWidth, matrixWidth, BufferedImage.TYPE_INT_RGB);
        image.createGraphics();

        Graphics2D graphics = (Graphics2D) image.getGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, matrixWidth, matrixWidth);
        graphics.setColor(Color.BLACK);

        for (int i = 0; i < matrixWidth; i++) {
            for (int j = 0; j < matrixWidth; j++) {
                if (byteMatrix.get(i, j)) {
                    graphics.fillRect(i, j, 1, 1);
                }
            }
        }
        ImageIO.write(image, fileType, qrFile);

        displayQRCode(image);
    }

    private static String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress ia = inetAddresses.nextElement();
                    if (!ia.isLoopbackAddress() && ia.isSiteLocalAddress()) {
                        return ia.getHostAddress();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void displayQRCode(BufferedImage image) {
        JFrame frame = new JFrame("Connection QR Code");
        frame.getContentPane().setLayout(new FlowLayout());
        frame.getContentPane().add(new JLabel(new ImageIcon(image)));
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private static void createAndShowDockerControls() {
        JFrame frame = new JFrame("Docker Controls");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

        JPanel panel = new JPanel();
        JButton startButton = new JButton("Start Services");
        JButton stopButton = new JButton("Stop Services");
        JTextArea outputArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(outputArea);

        panel.add(startButton);
        panel.add(stopButton);

        frame.getContentPane().add(BorderLayout.NORTH, panel);
        frame.getContentPane().add(BorderLayout.CENTER, scrollPane);

        startButton.addActionListener(e -> runDockerCompose("up -d", outputArea));
        stopButton.addActionListener(e -> runDockerCompose("down", outputArea));

        frame.setVisible(true);
    }

    private static void runDockerCompose(String command, JTextArea outputArea) {
        new Thread(() -> {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder("docker-compose", command);
                processBuilder.directory(new File("desktop-app"));
                Process process = processBuilder.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    final String finalLine = line;
                    SwingUtilities.invokeLater(() -> outputArea.append(finalLine + "\n"));
                }

                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                while ((line = errorReader.readLine()) != null) {
                    final String finalLine = line;
                    SwingUtilities.invokeLater(() -> outputArea.append("ERROR: " + finalLine + "\n"));
                }

                int exitCode = process.waitFor();
                SwingUtilities.invokeLater(() -> outputArea.append("Process exited with code: " + exitCode + "\n"));

            } catch (IOException | InterruptedException ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> outputArea.append("Error running docker-compose: " + ex.getMessage() + "\n"));
            }
        }).start();
    }
}
