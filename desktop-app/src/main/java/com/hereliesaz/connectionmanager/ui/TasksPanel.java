package com.hereliesaz.connectionmanager.ui;

import com.hereliesaz.connectionmanager.api.HashtopolisApiClient;
import com.hereliesaz.connectionmanager.api.HashtopolisApiModels;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.List;

public class TasksPanel extends JPanel {
    private HashtopolisApiClient apiClient;
    private JTable tasksTable;
    private DefaultTableModel tableModel;
    private Frame owner;

    public TasksPanel(Frame owner) {
        this.owner = owner;
        setLayout(new BorderLayout());

        // Title and Instructions
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        JLabel titleLabel = new JLabel("Hashtopolis Tasks");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(titleLabel);

        JLabel instructionsLabel = new JLabel("This panel displays the tasks from your Hashtopolis server.");
        instructionsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(instructionsLabel);
        add(topPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new Object[]{"ID", "Name", "Type", "Priority"}, 0);
        tasksTable = new JTable(tableModel);
        add(new JScrollPane(tasksTable), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton newTaskButton = new JButton("New Task");
        newTaskButton.setToolTipText("Open a dialog to create a new task.");
        JButton refreshButton = new JButton("Refresh");
        refreshButton.setToolTipText("Refresh the list of tasks from the server.");
        buttonPanel.add(newTaskButton);
        buttonPanel.add(refreshButton);
        add(buttonPanel, BorderLayout.SOUTH);

        newTaskButton.addActionListener(e -> {
            if (apiClient != null) {
                NewTaskDialog dialog = new NewTaskDialog(owner, apiClient);
                dialog.setVisible(true);
            }
        });
        refreshButton.addActionListener(e -> refreshTasks());
    }

    public void setApiClient(HashtopolisApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void refreshTasks() {
        if (apiClient == null) {
            return;
        }

        new SwingWorker<List<HashtopolisApiModels.Task>, Void>() {
            @Override
            protected List<HashtopolisApiModels.Task> doInBackground() throws Exception {
                try {
                    return apiClient.listTasks();
                } catch (IOException e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(TasksPanel.this, "Error fetching tasks: " + e.getMessage(), "API Error", JOptionPane.ERROR_MESSAGE));
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    List<HashtopolisApiModels.Task> tasks = get();
                    if (tasks != null) {
                        tableModel.setRowCount(0); // Clear existing data
                        for (HashtopolisApiModels.Task task : tasks) {
                            tableModel.addRow(new Object[]{task.getId(), task.getName(), task.getType(), task.getPriority()});
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }
}
