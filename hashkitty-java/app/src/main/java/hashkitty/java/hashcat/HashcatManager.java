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
        List<String> command = buildCommand(mode, attackMode, ruleFile, force, optimizedKernels, workloadProfile);
        command.add(hashFile); // Hash file comes before the target
        command.add(target);   // Target (wordlist or mask) comes last
        startAttackInternal(command, null);
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
        List<String> command = buildCommand(mode, attackMode, ruleFile, false, false, null);
        command.add(hashString); // Hash string as the "file"
        command.add(target);     // Target (wordlist or mask)
        startAttackInternal(command, hashString);
    }

    /**
     * Constructs the base hashcat command list (executable + options).
     * Note: Does NOT add the hash file/string or the target.
     */
    List<String> buildCommand(String mode, String attackMode, String ruleFile, boolean force, boolean optimizedKernels, String workloadProfile) {
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
        } else if ("Mask".equalsIgnoreCase(attackMode) || "Brute-force".equalsIgnoreCase(attackMode)) {
            command.add("-a");
            command.add("3");
        } else {
            throw new IllegalArgumentException("Unsupported attack mode: " + attackMode);
        }

        if (ruleFile != null && !ruleFile.isEmpty()) {
            command.add("-r");
            command.add(ruleFile);
        }

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
                        // Only show error if not just "exhausted" (code 1) or stopped?
                        // Hashcat exit codes: 0=Cracked, 1=Exhausted, -1=Error.
                        // But often checking exit code is enough.
                        // If exitCode is 1 (Exhausted), it's not necessarily an error-error, just failed to crack.
                        if (exitCode != 1) {
                             ErrorUtil.showError("Hashcat Error", "Hashcat exited with code " + exitCode + ". Check console/logs.");
                        } else {
                             onError.accept("Attack finished. Password not found.");
                        }
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
