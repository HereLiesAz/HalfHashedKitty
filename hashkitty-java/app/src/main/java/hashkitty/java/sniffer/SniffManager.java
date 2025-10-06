package hashkitty.java.sniffer;

import com.jcraft.jsch.*;
import hashkitty.java.model.RemoteConnection;

import java.io.InputStream;
import java.util.function.Consumer;

public class SniffManager {

    private Session session;
    private ChannelExec channel;
    private final Consumer<String> onOutput;

    public SniffManager(Consumer<String> onOutput) {
        this.onOutput = onOutput;
    }

    public void startSniffing(RemoteConnection connection, String password) {
        if (session != null && session.isConnected()) {
            onOutput.accept("A session is already active. Please stop it first.");
            return;
        }

        new Thread(() -> {
            try {
                String[] connParts = connection.getConnectionString().split("@");
                if (connParts.length != 2) {
                    onOutput.accept("Invalid connection string format. Expected user@host.");
                    return;
                }
                String user = connParts[0];
                String host = connParts[1];

                JSch jsch = new JSch();
                session = jsch.getSession(user, host, 22);
                session.setPassword(password);

                // For testing purposes, accept all host keys.
                // In a real application, you'd want to manage known_hosts.
                session.setConfig("StrictHostKeyChecking", "no");

                onOutput.accept("Connecting to " + host + "...");
                session.connect(30000); // 30-second timeout
                onOutput.accept("Connection established.");

                // Example command: run tcpdump to capture packets.
                // This command would need to be adapted for the target system (e.g., using airodump-ng for WiFi).
                String command = "sudo tcpdump -i wlan0 -l -U";

                channel = (ChannelExec) session.openChannel("exec");
                channel.setCommand(command);
                channel.setInputStream(null);
                channel.setErrStream(System.err);

                InputStream in = channel.getInputStream();
                channel.connect();
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
                        onOutput.accept("Sniffing process finished with exit code: " + channel.getExitStatus());
                        break;
                    }
                    Thread.sleep(1000);
                }
            } catch (JSchException e) {
                onOutput.accept("SSH Error: " + e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                onOutput.accept("Error: " + e.getMessage());
                e.printStackTrace();
            } finally {
                stopSniffing();
            }
        }).start();
    }

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