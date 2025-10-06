package hashkitty.java.sniffer;

import com.jcraft.jsch.*;
import hashkitty.java.model.RemoteConnection;

import java.io.InputStream;
import java.util.function.Consumer;

/**
 * Manages remote packet sniffing sessions over SSH.
 * This class uses the JSch library to connect to a remote device,
 * execute a sniffing command (e.g., tcpdump), and stream the output back.
 */
public class SniffManager {

    private Session session;
    private ChannelExec channel;
    private final Consumer<String> onOutput;

    /**
     * Constructs a new SniffManager.
     *
     * @param onOutput A callback function to be executed with output from the sniffing process or status messages.
     */
    public SniffManager(Consumer<String> onOutput) {
        this.onOutput = onOutput;
    }

    /**
     * Starts a new remote sniffing session in a background thread.
     * It establishes an SSH connection and executes a predefined sniffing command.
     *
     * @param connection The remote connection details (user and host).
     * @param password   The password for the SSH connection.
     */
    public void startSniffing(RemoteConnection connection, String password) {
        if (session != null && session.isConnected()) {
            onOutput.accept("Error: A session is already active. Please stop it first.");
            return;
        }

        new Thread(() -> {
            try {
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

                session.setConfig("StrictHostKeyChecking", "no");

                onOutput.accept("Connecting to " + host + "...");
                session.connect(30000); // 30-second timeout
                onOutput.accept("Connection established successfully.");

                String command = "sudo tcpdump -i wlan0 -l -U";
                onOutput.accept("Executing remote command: " + command);

                channel = (ChannelExec) session.openChannel("exec");
                channel.setCommand(command);
                channel.setInputStream(null);

                InputStream in = channel.getInputStream();
                channel.connect(5000); // 5-second timeout for channel connection
                onOutput.accept("Sniffing process started on remote host.");

                byte[] tmp = new byte[1024];
                while (true) {
                    while (in.available() > 0) {
                        int i = in.read(tmp, 0, 1024);
                        if (i < 0) break;
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
                    Thread.sleep(500);
                }
            } catch (JSchException e) {
                String errorMessage = e.getMessage();
                if (errorMessage.contains("Auth fail")) {
                    onOutput.accept("SSH Error: Authentication failed. Please check your password.");
                } else if (errorMessage.contains("UnknownHostException")) {
                    onOutput.accept("SSH Error: Unknown host. Could not resolve " + connection.getConnectionString());
                } else if (errorMessage.contains("Connection timed out")) {
                    onOutput.accept("SSH Error: Connection timed out. Check host address and network.");
                } else {
                    onOutput.accept("SSH Error: " + errorMessage);
                }
                e.printStackTrace();
            } catch (Exception e) {
                onOutput.accept("An unexpected error occurred: " + e.getMessage());
                e.printStackTrace();
            } finally {
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