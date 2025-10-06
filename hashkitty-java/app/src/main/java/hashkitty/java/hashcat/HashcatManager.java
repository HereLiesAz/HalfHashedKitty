package hashkitty.java.hashcat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class HashcatManager {

    private Process hashcatProcess;
    private final Consumer<String> onCrackedPassword;

    public HashcatManager(Consumer<String> onCrackedPassword) {
        this.onCrackedPassword = onCrackedPassword;
    }

    public void startCracking(String hash, String mode, String attackMode, String target) throws IOException {
        if (hashcatProcess != null && hashcatProcess.isAlive()) {
            System.out.println("A hashcat process is already running.");
            return;
        }

        // Construct the hashcat command based on the attack mode
        List<String> command = new ArrayList<>();
        command.add("hashcat");
        command.add("-m");
        command.add(mode);
        command.add("--potfile-disable"); // Potfiles store cracked hashes, disabling for simplicity

        if ("Dictionary".equalsIgnoreCase(attackMode)) {
            command.add("-a");
            command.add("0"); // Dictionary attack mode
        } else if ("Mask".equalsIgnoreCase(attackMode)) {
            command.add("-a");
            command.add("3"); // Mask attack mode (aka brute-force)
        } else {
            throw new IllegalArgumentException("Unsupported attack mode: " + attackMode);
        }

        command.add(hash);      // The hash to crack
        command.add(target);    // The wordlist file or mask

        ProcessBuilder pb = new ProcessBuilder(command);

        // Redirect error stream to output stream for easier monitoring
        pb.redirectErrorStream(true);

        // Start the process
        hashcatProcess = pb.start();
        System.out.println("Hashcat process started with command: " + String.join(" ", command));

        // Read the output of the process in a new thread
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(hashcatProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("hashcat: " + line); // Log all output for debugging
                    // A simple way to check for a cracked password in the output
                    if (line.startsWith(hash + ":")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) {
                            onCrackedPassword.accept(parts[1]);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    hashcatProcess.waitFor();
                    System.out.println("Hashcat process finished.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void stopCracking() {
        if (hashcatProcess != null && hashcatProcess.isAlive()) {
            hashcatProcess.destroyForcibly(); // Use destroyForcibly for a more immediate stop
            System.out.println("Hashcat process stopped.");
        }
    }
}