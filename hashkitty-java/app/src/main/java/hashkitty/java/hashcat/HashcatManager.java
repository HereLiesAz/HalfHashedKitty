package hashkitty.java.hashcat;

import hashkitty.java.util.ErrorUtil;

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
    private final Runnable onComplete;

    /**
     * Constructs a new HashcatManager.
     *
     * @param onCrackedPassword A callback function to be executed when a password is successfully cracked.
     * @param onError           A callback function to be executed when an error occurs.
     * @param onComplete        A callback function to be executed when the hashcat process completes.
     */
    public HashcatManager(Consumer<String> onCrackedPassword, Consumer<String> onError, Runnable onComplete) {
        this.onCrackedPassword = onCrackedPassword;
        this.onError = onError;
        this.onComplete = onComplete;
    }

    /**
     * Starts a new hashcat cracking process using a hash file.
     *
     * @param hashFile   The path to the file containing hashes.
     * @param mode       The hashcat hash mode (e.g., "22000" for WPA2).
     * @param attackMode The hashcat attack mode ("Dictionary" or "Mask").
     * @param target     The target for the attack (wordlist path or mask pattern).
     * @param ruleFile   The path to a hashcat rule file (can be null).
     * @throws IOException if an I/O error occurs when starting the process.
     */
    public void startAttackWithFile(String hashFile, String mode, String attackMode, String target, String ruleFile, boolean force, boolean optimizedKernels, String workloadProfile) throws IOException {
        List<String> command = buildCommand(mode, attackMode, target, ruleFile, force, optimizedKernels, workloadProfile);
        command.add(hashFile); // Add hash file as the main input
        startAttackInternal(command, null); // Pass null because we don't have a single hash to monitor
    }

    /**
     * Starts a new hashcat cracking process using a single hash string.
     *
     * @param hashString The single hash string to crack.
     * @param mode       The hashcat hash mode.
     * @param attackMode The hashcat attack mode.
     * @param target     The target for the attack.
     * @param ruleFile   The path to a hashcat rule file (can be null).
     * @throws IOException if an I/O error occurs when starting the process.
     */
    public void startAttackWithString(String hashString, String mode, String attackMode, String target, String ruleFile) throws IOException {
        // Remote attacks do not yet support advanced options, so pass default values.
        List<String> command = buildCommand(mode, attackMode, target, ruleFile, false, false, null);
        command.add(hashString); // Add the hash string directly to the command
        startAttackInternal(command, hashString); // Pass the hash string to monitor the output precisely
    }

    /**
     * Constructs the base hashcat command list.
     */
    private List<String> buildCommand(String mode, String attackMode, String target, String ruleFile, boolean force, boolean optimizedKernels, String workloadProfile) {
        List<String> command = new ArrayList<>();
        command.add("hashcat");
        command.add("-m");
        command.add(mode);
        command.add("--potfile-disable");

        if (force) {
            command.add("--force");
        }
        if (optimizedKernels) {
            command.add("-O");
        }
        if (workloadProfile != null && !workloadProfile.isEmpty()) {
            command.add("-w");
            command.add(workloadProfile);
        }

        if ("Dictionary".equalsIgnoreCase(attackMode)) {
            command.add("-a");
            command.add("0");
        } else if ("Mask".equalsIgnoreCase(attackMode)) {
            command.add("-a");
            command.add("3");
        } else {
            throw new IllegalArgumentException("Unsupported attack mode: " + attackMode);
        }

        if (ruleFile != null && !ruleFile.isEmpty()) {
            command.add("-r");
            command.add(ruleFile);
        }

        // The target (wordlist or mask) and hash source (file or string) are added after this
        command.add(target);
        return command;
    }

    /**
     * The internal method that launches and monitors the hashcat process.
     */
    private void startAttackInternal(List<String> command, String hashToMonitor) throws IOException {
        if (hashcatProcess != null && hashcatProcess.isAlive()) {
            ErrorUtil.showError("Process Error", "A hashcat process is already running.");
            return;
        }

        // The hash source (file or string) should be the last argument before the target
        // The buildCommand method doesn't add the hash source, so we add it here at the correct position
        // Let's re-order: hashcat [options] hash target
        int targetIndex = command.size() - 1;
        String target = command.get(targetIndex);
        command.remove(targetIndex);
        if (hashToMonitor != null) { // It's a string hash
             command.add(hashToMonitor);
        } else { // It's a file path, which should be the last argument before the target
             // The calling methods already added the hash file path
        }
        command.add(target);


        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        try {
            hashcatProcess = pb.start();
        } catch (IOException e) {
            ErrorUtil.showError("Process Error", "Failed to start hashcat. Is it installed and in your system's PATH?");
            throw e;
        }

        System.out.println("Hashcat process started with command: " + String.join(" ", command));

        new Thread(() -> {
            try {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(hashcatProcess.getInputStream()))) {
                    String line;
                    boolean found = false;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("hashcat: " + line);

                        boolean isCrackedLine;
                        if (hashToMonitor != null) {
                            isCrackedLine = line.startsWith(hashToMonitor + ":");
                        } else {
                            isCrackedLine = line.contains(":") && !line.startsWith("Status") && !line.startsWith("Session") && !line.startsWith("Input");
                        }

                        if (isCrackedLine) {
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
                        ErrorUtil.showError("Hashcat Error", "Hashcat exited with error code " + exitCode + ". Check parameters and hash file.");
                    }

                } catch (IOException e) {
                    ErrorUtil.showError("I/O Error", "Error reading hashcat output: " + e.getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    ErrorUtil.showError("Process Error", "Hashcat process was interrupted.");
                }
            } finally {
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        }).start();
    }

    /**
     * Forcibly stops the currently running hashcat process.
     */
    public void stopCracking() {
        if (hashcatProcess != null && hashcatProcess.isAlive()) {
            hashcatProcess.destroyForcibly();
            System.out.println("Hashcat process stopped by user.");
            onError.accept("Attack stopped by user.");
        }
    }
}