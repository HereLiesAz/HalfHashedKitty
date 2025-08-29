package com.hereliesaz.connectionmanager.ui;

import com.hereliesaz.connectionmanager.docker.DockerManager;

import javax.swing.*;
import java.awt.*;

public class ServicesPanel extends JPanel {
    public ServicesPanel() {
        setLayout(new BorderLayout());

        JTextArea outputArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(outputArea);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton startButton = new JButton("Start Services");
        JButton stopButton = new JButton("Stop Services");
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        add(buttonPanel, BorderLayout.NORTH);

        startButton.addActionListener(e -> DockerManager.runDockerCompose("up -d", outputArea));
        stopButton.addActionListener(e -> DockerManager.runDockerCompose("down", outputArea));
    }
}
