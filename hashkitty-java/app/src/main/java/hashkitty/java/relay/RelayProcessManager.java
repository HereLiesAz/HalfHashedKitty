package hashkitty.java.relay;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * Manages the lifecycle of an external Relay Server process.
 * <p>
 * While the Desktop App can run the Relay Server internally (in-process), this class supports
 * scenarios where the Relay Server is launched as a separate operating system process.
 * This might be useful for decoupling, stability, or testing purposes, or if the user
 * wants to run the specific standalone binary/script.
 * </p>
 */
public class RelayProcessManager {

    /** The reference to the running process object. Null if not running. */
    private Process relayProcess;

    /** Callback for logging status messages. */
    private final Consumer<String> onStatusUpdate;

    /**
     * Constructs a new RelayProcessManager.
     *
     * @param onStatusUpdate A callback for status and error messages to be displayed in the UI.
     */
    public RelayProcessManager(Consumer<String> onStatusUpdate) {
        this.onStatusUpdate = onStatusUpdate;
    }

    /**
     * Attempts to start the external relay server process.
     * <p>
     * It intelligently searches for the server executable or start script in common locations
     * relative to the application's working directory. It handles platform differences (Windows vs Unix).
     * </p>
     */
    public void startRelay() {
        // Check if already running.
        if (relayProcess != null && relayProcess.isAlive()) {
            onStatusUpdate.accept("Relay process is already running.");
            return;
        }

        try {
            // Determine the current operating system.
            String os = System.getProperty("os.name").toLowerCase();
            boolean isWin = os.contains("win");

            // Define the names of the script/executable to look for.
            // On Windows, we look for 'server.bat' (script) or 'server.exe'.
            // On Unix, we look for 'server' (script or binary).
            String scriptName = isWin ? "server.bat" : "server";
            String exeName = isWin ? "server.exe" : "server";

            // Attempt to locate the file.
            File executable = findExecutable(scriptName, exeName);

            if (executable == null) {
                // If not found, log an error and abort.
                onStatusUpdate.accept("Error: Server executable not found. Looked for " + exeName + " or " + scriptName);
                return;
            }

            // Ensure the file is executable (important for Unix-like systems).
            if (!executable.canExecute()) {
                executable.setExecutable(true);
            }

            // Prepare the process builder.
            ProcessBuilder pb = new ProcessBuilder(executable.getAbsolutePath());
            // Redirect stderr to stdout so we capture all logs.
            pb.redirectErrorStream(true);

            onStatusUpdate.accept("Starting standalone relay server from: " + executable.getAbsolutePath());
            // Start the process.
            relayProcess = pb.start();

            // Start a background thread to read the process's output stream.
            // This prevents the process from blocking if its output buffer fills up
            // and allows us to display the logs in the main app.
            new Thread(() -> {
                try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(relayProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        onStatusUpdate.accept("Relay: " + line);
                    }
                } catch (IOException e) {
                    // This is expected when the process is destroyed/stopped.
                }
            }).start();

        } catch (IOException e) {
            onStatusUpdate.accept("Error starting relay process: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Searches for the executable file in a predefined list of relative paths.
     *
     * @param scriptName The name of the shell/batch script.
     * @param exeName    The name of the binary executable.
     * @return The File object if found, or null otherwise.
     */
    private File findExecutable(String scriptName, String exeName) {
        // List of potential locations relative to the working directory.
        String[] locations = {
            exeName,                  // Current dir (binary)
            scriptName,               // Current dir (script)
            "bin/" + exeName,         // bin subdir (standard distribution layout)
            "bin/" + scriptName,      // bin subdir
            "../bin/" + exeName,      // sibling bin (development layout)
            "../bin/" + scriptName    // sibling bin
        };

        // Iterate and check existence.
        for (String loc : locations) {
            File f = new File(loc);
            if (f.exists() && !f.isDirectory()) {
                return f;
            }
        }
        return null;
    }

    /**
     * Stops the external relay process if it is running.
     * This forcibly kills the process.
     */
    public void stopRelay() {
        if (relayProcess != null && relayProcess.isAlive()) {
            relayProcess.destroy();
            onStatusUpdate.accept("Standalone relay server stopped.");
        }
        relayProcess = null;
    }
}
