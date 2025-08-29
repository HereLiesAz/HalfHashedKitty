package com.hereliesaz.connectionmanager.docker;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class DockerManager {
    public static void runDockerCompose(String command, JTextArea outputArea) {
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
