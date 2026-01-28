package hashkitty.java.relay;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * Manages the lifecycle of the standalone relay server process.
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
     * Starts the server process.
     * It looks for 'server' or 'server.bat' (script) or 'server'/'server.exe' (binary)
     * in the current directory or bin/ subdirectory.
     */
    public void startRelay() {
        if (relayProcess != null && relayProcess.isAlive()) {
            onStatusUpdate.accept("Relay process is already running.");
            return;
        }

        try {
            String os = System.getProperty("os.name").toLowerCase();
            boolean isWin = os.contains("win");

            // Prioritize native launcher if using jpackage, or script if using installDist
            String scriptName = isWin ? "server.bat" : "server";
            String exeName = isWin ? "server.exe" : "server";

            File executable = findExecutable(scriptName, exeName);

            if (executable == null) {
                onStatusUpdate.accept("Error: Server executable not found. Looked for " + exeName + " or " + scriptName);
                return;
            }

            if (!executable.canExecute()) {
                executable.setExecutable(true);
            }

            ProcessBuilder pb = new ProcessBuilder(executable.getAbsolutePath());
            pb.redirectErrorStream(true);

            onStatusUpdate.accept("Starting standalone relay server from: " + executable.getAbsolutePath());
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

    private File findExecutable(String scriptName, String exeName) {
        String[] locations = {
            exeName,
            scriptName,
            "bin/" + exeName,
            "bin/" + scriptName,
            "../bin/" + exeName,
            "../bin/" + scriptName
        };

        for (String loc : locations) {
            File f = new File(loc);
            if (f.exists() && !f.isDirectory()) {
                return f;
            }
        }
        return null;
    }

    /**
     * Stops the external relay process.
     */
    public void stopRelay() {
        if (relayProcess != null && relayProcess.isAlive()) {
            relayProcess.destroy();
            onStatusUpdate.accept("Standalone relay server stopped.");
        }
        relayProcess = null;
    }
}
