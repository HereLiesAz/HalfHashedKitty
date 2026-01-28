package hashkitty.java.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Entry point for the standalone Relay Server.
 */
public class ServerApp {

    private static final int DEFAULT_PORT = 5001;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number provided. Using default port " + DEFAULT_PORT);
            }
        }

        System.out.println("Starting HashKitty Relay Server on port " + port + "...");

        RelayServer server = new RelayServer(port,
            status -> System.out.println("[Status] " + status),
            cracked -> System.out.println("[CRACKED] " + cracked)
        );

        server.start();

        System.out.println("Server started. Press Enter to stop.");

        // Keep the server running until user input or termination
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            // Check if System.console() is null, which happens when running as background process or without tty.
            // In that case, we just join the server thread or sleep forever.
            if (System.console() == null) {
                // Daemon mode / No console
                Thread.sleep(Long.MAX_VALUE);
            } else {
                reader.readLine();
                System.out.println("Stopping server...");
                server.stop();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
