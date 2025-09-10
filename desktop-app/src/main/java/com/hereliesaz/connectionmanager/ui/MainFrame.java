package com.hereliesaz.connectionmanager.ui;

import com.hereliesaz.connectionmanager.api.HashtopolisApiClient;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class MainFrame extends JFrame {
    private ConnectionPanel connectionPanel;
    private TasksPanel tasksPanel;
    private HashtopolisApiClient apiClient;

    public MainFrame(String connectionString) {
        setTitle("Half-Hashed Kitty - Desktop Manager");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();

        connectionPanel = new ConnectionPanel(connectionString);
        tabbedPane.addTab("Connection", connectionPanel);

        add(tabbedPane);
    }
}
