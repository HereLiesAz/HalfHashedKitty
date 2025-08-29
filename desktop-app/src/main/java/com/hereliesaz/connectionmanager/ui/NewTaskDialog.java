package com.hereliesaz.connectionmanager.ui;

import com.hereliesaz.connectionmanager.api.HashtopolisApiClient;
import com.hereliesaz.connectionmanager.api.HashtopolisApiModels;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class NewTaskDialog extends JDialog {
    private JTextField nameField, hashlistIdField, attackCmdField, chunksizeField, statusTimerField, priorityField, maxAgentsField;
    private HashtopolisApiClient apiClient;

    public NewTaskDialog(Frame owner, HashtopolisApiClient apiClient) {
        super(owner, "Create New Task", true);
        this.apiClient = apiClient;

        setLayout(new GridLayout(8, 2, 5, 5));

        add(new JLabel("Name:"));
        nameField = new JTextField();
        add(nameField);

        add(new JLabel("Hashlist ID:"));
        hashlistIdField = new JTextField();
        add(hashlistIdField);

        add(new JLabel("Attack Command:"));
        attackCmdField = new JTextField();
        add(attackCmdField);

        add(new JLabel("Chunk Size:"));
        chunksizeField = new JTextField("600");
        add(chunksizeField);

        add(new JLabel("Status Timer:"));
        statusTimerField = new JTextField("5");
        add(statusTimerField);

        add(new JLabel("Priority:"));
        priorityField = new JTextField("0");
        add(priorityField);

        add(new JLabel("Max Agents:"));
        maxAgentsField = new JTextField("4");
        add(maxAgentsField);

        JButton createButton = new JButton("Create");
        add(createButton);
        JButton cancelButton = new JButton("Cancel");
        add(cancelButton);

        createButton.addActionListener(e -> createTask());
        cancelButton.addActionListener(e -> dispose());

        pack();
        setLocationRelativeTo(owner);
    }

    private void createTask() {
        try {
            String name = nameField.getText();
            int hashlistId = Integer.parseInt(hashlistIdField.getText());
            String attackCmd = attackCmdField.getText();
            int chunksize = Integer.parseInt(chunksizeField.getText());
            int statusTimer = Integer.parseInt(statusTimerField.getText());
            int priority = Integer.parseInt(priorityField.getText());
            int maxAgents = Integer.parseInt(maxAgentsField.getText());

            HashtopolisApiModels.CreateTaskRequest request = new HashtopolisApiModels.CreateTaskRequest(name, hashlistId, attackCmd, chunksize, statusTimer, priority, maxAgents);

            new SwingWorker<HashtopolisApiModels.CreateTaskResponse, Void>() {
                @Override
                protected HashtopolisApiModels.CreateTaskResponse doInBackground() throws Exception {
                    return apiClient.createTask(request);
                }

                @Override
                protected void done() {
                    try {
                        HashtopolisApiModels.CreateTaskResponse response = get();
                        if (response != null && response.getTaskId() > 0) {
                            JOptionPane.showMessageDialog(NewTaskDialog.this, "Task created successfully with ID: " + response.getTaskId());
                            dispose();
                        } else {
                            JOptionPane.showMessageDialog(NewTaskDialog.this, "Failed to create task.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(NewTaskDialog.this, "Error creating task: " + e.getMessage(), "API Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers for numeric fields.", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
