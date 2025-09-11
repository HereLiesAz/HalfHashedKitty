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
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridwidth = 1;

        // Instructions
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        JLabel instructionsLabel = new JLabel("Scan this QR code with the Half-Hashed Kitty Android app to connect to this desktop application.");
        add(instructionsLabel, gbc);

        // QR Code
        gbc.gridy = 1;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        try {
            BufferedImage qrImage = generateQrCode(connectionString);
            JLabel qrLabel = new JLabel(new ImageIcon(qrImage));
            add(qrLabel, gbc);
        } catch (WriterException | IOException e) {
            e.printStackTrace();
            add(new JLabel("Error generating QR code."), gbc);
        }
        gbc.weighty = 0.0;

        // Input Panel
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(createInputPanel(), gbc);
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.anchor = GridBagConstraints.WEST;

        // API Key
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("API Key:"), gbc);

        gbc.gridx = 1;
        apiKeyField = new JTextField(30);
        panel.add(apiKeyField, gbc);

        gbc.gridy = 1;
        JLabel apiKeyInstruction = new JLabel("Enter the API key from your Hashtopolis web interface.");
        apiKeyInstruction.setFont(apiKeyInstruction.getFont().deriveFont(10f));
        panel.add(apiKeyInstruction, gbc);

        // Save Button
        gbc.gridx = 1;
        gbc.gridy = 2;
        JButton saveButton = new JButton("Save Key");
        panel.add(saveButton, gbc);

        gbc.gridy = 3;
        JLabel saveButtonInstruction = new JLabel("Click to save the API key.");
        saveButtonInstruction.setFont(saveButtonInstruction.getFont().deriveFont(10f));
        panel.add(saveButtonInstruction, gbc);

        saveButton.addActionListener(e -> {
            this.apiKey = apiKeyField.getText();
            JOptionPane.showMessageDialog(this, "API Key Saved!");
        });

        return panel;
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
