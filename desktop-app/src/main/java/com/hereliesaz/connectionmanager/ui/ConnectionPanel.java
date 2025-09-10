package com.hereliesaz.connectionmanager.ui;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

public class ConnectionPanel extends JPanel {
    private JTextField apiKeyField;
    private String apiKey;

    public ConnectionPanel(String connectionString) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Instructions
        JLabel instructionsLabel = new JLabel("Scan this QR code with the Half-Hashed Kitty Android app to connect to this desktop application.");
        instructionsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(instructionsLabel);

        // QR Code
        try {
            BufferedImage qrImage = generateQrCode(connectionString);
            JLabel qrLabel = new JLabel(new ImageIcon(qrImage));
            qrLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(qrLabel);
        } catch (WriterException | IOException e) {
            e.printStackTrace();
            add(new JLabel("Error generating QR code."));
        }

        // API Key Input
        JLabel apiKeyLabel = new JLabel("Enter the API key from the Hashtopolis web interface.");
        apiKeyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(apiKeyLabel);

        JPanel apiKeyPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        apiKeyPanel.add(new JLabel("API Key:"));
        apiKeyField = new JTextField(30);
        apiKeyPanel.add(apiKeyField);
        JButton saveButton = new JButton("Save Key");
        apiKeyPanel.add(saveButton);

        add(Box.createRigidArea(new Dimension(0, 10)));
        add(apiKeyPanel);

        saveButton.addActionListener(e -> {
            this.apiKey = apiKeyField.getText();
            JOptionPane.showMessageDialog(this, "API Key Saved!");
        });
    }

    public String getApiKey() {
        return apiKey;
    }

    private BufferedImage generateQrCode(String connectionString) throws WriterException, IOException {
        int size = 400;
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
        return image;
    }
}
