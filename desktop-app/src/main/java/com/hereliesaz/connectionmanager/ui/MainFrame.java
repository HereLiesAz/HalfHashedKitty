package com.hereliesaz.connectionmanager.ui;

import com.hereliesaz.connectionmanager.api.HashtopolisApiClient;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class MainFrame extends JFrame {
    private ConnectionPanel connectionPanel;
    private TasksPanel tasksPanel;
    private HashtopolisApiClient apiClient;
    private JTabbedPane tabbedPane;

    public MainFrame(String connectionString) {
        setTitle("Half-Hashed Kitty - Desktop Manager");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane();

        connectionPanel = new ConnectionPanel(connectionString);
        tabbedPane.addTab("Connection", connectionPanel);

        tasksPanel = new TasksPanel(this);
        tabbedPane.addTab("Tasks", tasksPanel);
        tabbedPane.setEnabledAt(1, false); // Disable tasks tab until connected

        add(tabbedPane);

        connectionPanel.getConnectButton().addActionListener(e -> connectToHashtopolis());
    }

    private void connectToHashtopolis() {
        String serverUrl = connectionPanel.getServerUrl();
        String apiKey = connectionPanel.getApiKey();

        if (serverUrl == null || serverUrl.isEmpty() || apiKey == null || apiKey.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both Server URL and API Key.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        apiClient = new HashtopolisApiClient(serverUrl, apiKey);
        tasksPanel.setApiClient(apiClient);
        tasksPanel.refreshTasks();

        tabbedPane.setEnabledAt(1, true);
        tabbedPane.setSelectedIndex(1);
    }
}
