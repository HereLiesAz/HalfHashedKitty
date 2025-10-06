package hashkitty.java.relay;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * Manages the lifecycle of the external gokitty-relay executable.
 * This class handles starting and stopping the standalone relay server process.
 */
public class RelayProcessManager {

    private Process relayProcess;
    private final Consumer<String> onStatusUpdate;

    /**
     * Constructs a new RelayProcessManager.
     *
     * @param onStatusUpdate A callback for status and error messages.
     */
    public RelayProcessManager(Consumer<String> onStatusUpdate) {
        this.onStatusUpdate = onStatusUpdate;
    }

    /**
     * Starts the external gokitty-relay process.
     * It assumes the executable is located in the application's working directory.
     */
    public void startRelay() {
        if (relayProcess != null && relayProcess.isAlive()) {
            onStatusUpdate.accept("Relay process is already running.");
            return;
        }

        try {
            String os = System.getProperty("os.name").toLowerCase();
            String executableName = "gokitty-relay";
            if (os.contains("win")) {
                executableName += ".exe";
            }

            File executable = new File(executableName);
            if (!executable.exists()) {
                onStatusUpdate.accept("Error: gokitty-relay executable not found. Please place it next to the application.");
                return;
            }

            executable.setExecutable(true);

            ProcessBuilder pb = new ProcessBuilder(executable.getAbsolutePath());
            pb.redirectErrorStream(true);

            onStatusUpdate.accept("Starting standalone relay server...");
            relayProcess = pb.start();

            new Thread(() -> {
                try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(relayProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        onStatusUpdate.accept("Relay: " + line);
                    }
                } catch (IOException e) {
                    // This might happen when the process is destroyed
                }
            }).start();

        } catch (IOException e) {
            onStatusUpdate.accept("Error starting relay process: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Stops the external gokitty-relay process.
     */
    public void stopRelay() {
        if (relayProcess != null && relayProcess.isAlive()) {
            relayProcess.destroy();
            onStatusUpdate.accept("Standalone relay server stopped.");
        }
        relayProcess = null;
    }
}