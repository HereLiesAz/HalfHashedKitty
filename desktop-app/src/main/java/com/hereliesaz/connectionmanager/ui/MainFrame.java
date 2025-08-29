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

        ServicesPanel servicesPanel = new ServicesPanel();
        tabbedPane.addTab("Services", servicesPanel);

        tasksPanel = new TasksPanel(this); // Pass the frame reference
        tabbedPane.addTab("Tasks", tasksPanel);

        add(tabbedPane);

        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (tabbedPane.getSelectedComponent() == tasksPanel) {
                    String apiKey = connectionPanel.getApiKey();
                    if (apiKey != null && !apiKey.isEmpty()) {
                        apiClient = new HashtopolisApiClient("http://localhost:8080", apiKey);
                        tasksPanel.setApiClient(apiClient);
                        tasksPanel.refreshTasks();
                    } else {
                        JOptionPane.showMessageDialog(MainFrame.this, "Please enter an API key in the Connection tab.", "API Key Required", JOptionPane.WARNING_MESSAGE);
                        tabbedPane.setSelectedComponent(connectionPanel);
                    }
                }
            }
        });
    }
}
