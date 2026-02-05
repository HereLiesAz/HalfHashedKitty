package hashkitty.java.sniffer;

import com.jcraft.jsch.*;
import hashkitty.java.model.RemoteConnection;

import java.io.InputStream;
import java.util.function.Consumer;

/**
 * Manages remote packet sniffing sessions over SSH.
 * <p>
 * This class uses the JSch library to establish SSH connections to remote devices
 * (like a Raspberry Pi running Kali Linux). It executes a packet capture command
 * (e.g., `tcpdump`) and streams the output back to the application in real-time.
 * </p>
 */
public class SniffManager {

    /** The active SSH session. */
    private Session session;
    /** The channel used for executing the command. */
    private ChannelExec channel;
    /** Callback to stream text output (stdout/stderr) back to the UI. */
    private final Consumer<String> onOutput;

    /**
     * Constructs a new SniffManager.
     *
     * @param onOutput A callback function to be executed when output is received from the remote process.
     */
    public SniffManager(Consumer<String> onOutput) {
        this.onOutput = onOutput;
    }

    /**
     * Starts a new remote sniffing session in a background thread.
     * <p>
     * This method:
     * 1. Connects to the host using JSch.
     * 2. Authenticates using the provided password.
     * 3. Runs `tcpdump` on the remote interface `wlan0` (hardcoded for now).
     * 4. Streams the output until the process ends or is stopped.
     * </p>
     *
     * @param connection The remote connection details (user and host).
     * @param password   The password for the SSH connection.
     * @param userInfo   The JSch UserInfo implementation for handling host key verification.
     */
    public void startSniffing(RemoteConnection connection, String password, UserInfo userInfo) {
        // Prevent multiple simultaneous sessions.
        if (session != null && session.isConnected()) {
            onOutput.accept("Error: A session is already active. Please stop it first.");
            return;
        }

        // Run network operations in a background thread.
        new Thread(() -> {
            try {
                // Parse "user@host".
                String[] connParts = connection.getConnectionString().split("@");
                if (connParts.length != 2) {
                    onOutput.accept("Error: Invalid connection string format. Expected user@host.");
                    return;
                }
                String user = connParts[0];
                String host = connParts[1];

                JSch jsch = new JSch();
                onOutput.accept("Initializing SSH session for " + connection.getConnectionString() + "...");
                session = jsch.getSession(user, host, 22);
                session.setPassword(password);

                // Assign the UserInfo for interaction (e.g. host key confirmation).
                session.setUserInfo(userInfo);

                // Note: We removed "StrictHostKeyChecking=no" to improve security.
                // The UserInfo callback will be used if the host key is unknown.

                onOutput.accept("Connecting to " + host + "...");
                // Set connection timeout to 30 seconds.
                session.connect(30000);
                onOutput.accept("Connection established successfully.");

                // The command to run remotely.
                // -i wlan0: Listen on wireless interface.
                // -l: Line buffered output.
                // -U: Packet buffered output (write as soon as received).
                // sudo is used assuming the user needs privileges to capture packets.
                String command = "sudo tcpdump -i wlan0 -l -U";
                onOutput.accept("Executing remote command: " + command);

                channel = (ChannelExec) session.openChannel("exec");
                channel.setCommand(command);
                channel.setInputStream(null); // No input to send to remote.

                // Get the output stream to read from remote command.
                InputStream in = channel.getInputStream();
                channel.connect(5000); // 5-second timeout for channel connection.
                onOutput.accept("Sniffing process started on remote host.");

                // Read loop.
                byte[] tmp = new byte[1024];
                while (true) {
                    while (in.available() > 0) {
                        int i = in.read(tmp, 0, 1024);
                        if (i < 0) break;
                        // Send chunk to UI callback.
                        onOutput.accept(new String(tmp, 0, i));
                    }
                    if (channel.isClosed()) {
                        if (in.available() > 0) continue;
                        int exitStatus = channel.getExitStatus();
                        String exitMessage = "Sniffing process finished with exit code: " + exitStatus;
                        if (exitStatus != 0) {
                            exitMessage += ". (This could be due to permissions issues, e.g., needing sudo password, or an invalid interface).";
                        }
                        onOutput.accept(exitMessage);
                        break;
                    }
                    try {
                        Thread.sleep(500);
                    } catch (Exception ee) {
                        // Ignored
                    }
                }
            } catch (JSchException e) {
                // Specific SSH error handling.
                String errorMessage = e.getMessage();
                if (errorMessage.contains("Auth fail")) {
                    onOutput.accept("SSH Error: Authentication failed. Please check your password.");
                } else if (errorMessage.contains("UnknownHostException")) {
                    onOutput.accept("SSH Error: Unknown host. Could not resolve " + connection.getConnectionString());
                } else if (errorMessage.contains("Connection timed out")) {
                    onOutput.accept("SSH Error: Connection timed out. Check host address and network.");
                } else if (errorMessage.contains("reject HostKey")) {
                    onOutput.accept("SSH Error: Host key rejected by user.");
                } else {
                    onOutput.accept("SSH Error: " + errorMessage);
                }
                e.printStackTrace();
            } catch (Exception e) {
                onOutput.accept("An unexpected error occurred: " + e.getMessage());
                e.printStackTrace();
            } finally {
                // Ensure cleanup happens.
                stopSniffing();
            }
        }).start();
    }

    /**
     * Stops the currently active sniffing session and disconnects from the remote host.
     */
    public void stopSniffing() {
        if (channel != null && channel.isConnected()) {
            channel.disconnect();
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
            onOutput.accept("Disconnected from remote host.");
        }
        session = null;
        channel = null;
    }
}
