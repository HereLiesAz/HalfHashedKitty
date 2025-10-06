package hashkitty.java.hashcat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Manages the execution of the `hashcat` command-line tool.
 * This class handles building the command, running the process,
 * and parsing the output for cracked passwords or errors.
 */
public class HashcatManager {

    private Process hashcatProcess;
    private final Consumer<String> onCrackedPassword;
    private final Consumer<String> onError;

    /**
     * Constructs a new HashcatManager.
     *
     * @param onCrackedPassword A callback function to be executed when a password is successfully cracked.
     * @param onError           A callback function to be executed when an error occurs.
     */
    public HashcatManager(Consumer<String> onCrackedPassword, Consumer<String> onError) {
        this.onCrackedPassword = onCrackedPassword;
        this.onError = onError;
    }

    /**
     * Starts a new hashcat cracking process in a background thread.
     *
     * @param hash       The hash string to be cracked.
     * @param mode       The hashcat hash mode (e.g., "0" for MD5).
     * @param attackMode The hashcat attack mode ("Dictionary" or "Mask").
     * @param target     The target for the attack, either a path to a wordlist file or a mask pattern.
     * @throws IOException if an I/O error occurs when starting the process (e.g., hashcat not found).
     */
    public void startCracking(String hash, String mode, String attackMode, String target) throws IOException {
        if (hashcatProcess != null && hashcatProcess.isAlive()) {
            onError.accept("A hashcat process is already running.");
            return;
        }

        List<String> command = new ArrayList<>();
        command.add("hashcat");
        command.add("-m");
        command.add(mode);
        command.add("--potfile-disable");

        if ("Dictionary".equalsIgnoreCase(attackMode)) {
            command.add("-a");
            command.add("0");
        } else if ("Mask".equalsIgnoreCase(attackMode)) {
            command.add("-a");
            command.add("3");
        } else {
            throw new IllegalArgumentException("Unsupported attack mode: " + attackMode);
        }

        command.add(hash);
        command.add(target);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        try {
            hashcatProcess = pb.start();
        } catch (IOException e) {
            onError.accept("Failed to start hashcat. Is it installed and in your system's PATH?");
            throw e;
        }

        System.out.println("Hashcat process started with command: " + String.join(" ", command));

        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(hashcatProcess.getInputStream()))) {
                String line;
                boolean found = false;
                while ((line = reader.readLine()) != null) {
                    System.out.println("hashcat: " + line);

                    if (line.startsWith(hash + ":")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) {
                            onCrackedPassword.accept(parts[1]);
                            found = true;
                        }
                    }
                }

                int exitCode = hashcatProcess.waitFor();
                System.out.println("Hashcat process finished with exit code: " + exitCode);
                if (exitCode != 0 && !found) {
                     onError.accept("Hashcat exited with error code " + exitCode + ". Check hash, mode, and target.");
                }

            } catch (IOException e) {
                onError.accept("Error reading hashcat output: " + e.getMessage());
                e.printStackTrace();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                onError.accept("Hashcat process was interrupted.");
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Forcibly stops the currently running hashcat process, if one exists.
     */
    public void stopCracking() {
        if (hashcatProcess != null && hashcatProcess.isAlive()) {
            hashcatProcess.destroyForcibly();
            System.out.println("Hashcat process stopped by user.");
            onError.accept("Attack stopped by user.");
        }
    }
}