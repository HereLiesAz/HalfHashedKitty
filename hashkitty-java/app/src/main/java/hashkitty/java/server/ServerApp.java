package hashkitty.java.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * The main entry point for running the HashKitty Relay Server as a standalone application.
 * <p>
 * This class is responsible for initializing and starting the {@link RelayServer}.
 * It parses command-line arguments to determine the listening port and manages the application lifecycle,
 * keeping the process alive until explicitly stopped or interrupted.
 * </p>
 * <p>
 * While the Desktop App (`App.java`) starts an embedded Relay Server, this class is used
 * when the user wants to run *only* the relay server (e.g., on a VPS or a headless Raspberry Pi).
 * </p>
 */
public class ServerApp {

    /**
     * The default TCP port the server will listen on if no argument is provided.
     */
    private static final int DEFAULT_PORT = 5001;

    /**
     * The main method handling application startup.
     *
     * @param args Command-line arguments. The first argument, if present, is interpreted as the port number.
     */
    public static void main(String[] args) {
        // Initialize port with the default value.
        int port = DEFAULT_PORT;

        // Check if the user provided a custom port number as an argument.
        if (args.length > 0) {
            try {
                // Attempt to parse the first argument as an integer.
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                // Inform the user if the provided argument wasn't a valid number and fall back to default.
                System.err.println("Invalid port number provided. Using default port " + DEFAULT_PORT);
            }
        }

        // Log startup message.
        System.out.println("Starting HashKitty Relay Server on port " + port + "...");

        // Instantiate the RelayServer with callbacks for logging to stdout.
        // The first lambda handles general status updates.
        // The second lambda handles notifications when a password is successfully cracked.
        RelayServer server = new RelayServer(port,
            status -> System.out.println("[Status] " + status),
            cracked -> System.out.println("[CRACKED] " + cracked)
        );

        // Start the WebSocket server in a separate thread.
        server.start();

        System.out.println("Server started. Press Enter to stop.");

        // Keep the main thread alive to prevent the application from exiting immediately.
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            // Check if the application is running in an environment with a dedicated console (TTY).
            // If System.console() is null, it typically means we are running in a background process,
            // IDE run configuration, or a container without an attached terminal.
            if (System.console() == null) {
                // If running in daemon/headless mode, put the main thread to sleep indefinitely.
                // This keeps the JVM running so the WebSocket server thread can continue handling requests.
                Thread.sleep(Long.MAX_VALUE);
            } else {
                // If running in an interactive console, wait for the user to press Enter.
                reader.readLine();
                System.out.println("Stopping server...");
                // Gracefully stop the WebSocket server.
                server.stop();
            }
        } catch (Exception e) {
            // Catch and print any unexpected exceptions that occur during runtime.
            e.printStackTrace();
        }
    }
}
