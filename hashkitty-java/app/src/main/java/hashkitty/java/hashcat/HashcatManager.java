package hashkitty.java.hashcat;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages the execution and lifecycle of the Hashcat process.
 * <p>
 * This class is responsible for:
 * <ul>
 *     <li>Constructing the command-line arguments for Hashcat.</li>
 *     <li>Spawning the external process.</li>
 *     <li>Monitoring the process's standard output for progress updates (percentage, speed).</li>
 *     <li>Detecting when a password has been cracked.</li>
 *     <li>Providing methods to gracefully stop the process.</li>
 * </ul>
 * </p>
 */
public class HashcatManager {

    /** The active Hashcat process. Null if no attack is running. */
    private Process process;

    /** Callback to invoke when a password is successfully cracked. */
    private final Consumer<String> onPasswordCracked;

    /** Callback to invoke for general status log messages. */
    private final Consumer<String> onStatusUpdate;

    /** Callback to invoke when the process terminates (finishes or is stopped). */
    private final Runnable onComplete;

    /**
     * Regex pattern to parse the progress status line from Hashcat.
     * Example output: "Speed.Dev.#1.....:  1500.0 kH/s  (5.24ms)" or "Progress.......: 1024/2048 (50.00%)"
     * Matches "Progress", followed by any number of dots/chars, a colon, space, numbers, and percentage.
     */
    private static final Pattern PROGRESS_PATTERN = Pattern.compile("Progress\\.+:\\s+\\d+/\\d+\\s+\\((\\d+\\.\\d+)%\\)");

    /**
     * Regex pattern to detect a cracked password in the output.
     * Hashcat often outputs the cracked hash:password pair to stdout when not using --quiet.
     * However, reliably getting it from stdout depends on the mode.
     * We often rely on the 'potfile' check or specific output format.
     */
    // private static final Pattern CRACKED_PATTERN = Pattern.compile("^(.*?):(.*)$"); // Too broad

    /**
     * Constructs a new HashcatManager.
     *
     * @param onPasswordCracked Callback for successful cracks.
     * @param onStatusUpdate    Callback for status messages.
     * @param onComplete        Callback for process completion.
     */
    public HashcatManager(Consumer<String> onPasswordCracked, Consumer<String> onStatusUpdate, Runnable onComplete) {
        this.onPasswordCracked = onPasswordCracked;
        this.onStatusUpdate = onStatusUpdate;
        this.onComplete = onComplete;
    }

    /**
     * Starts a Hashcat attack using a file-based target (dictionary or mask).
     *
     * @param hashFile   The path to the file containing hashes to crack.
     * @param mode       The Hashcat hash mode (e.g., "0" for MD5).
     * @param attackMode The attack mode ("Dictionary" or "Mask").
     * @param target     The path to the wordlist (for Dictionary) or the mask string (for Mask).
     * @param ruleFile   (Optional) Path to a rule file.
     * @param force      Whether to append the --force flag (ignore warnings).
     * @param optimizedKernels Whether to append -O (optimized kernels).
     * @param workloadProfile (Optional) The workload profile (1-4).
     * @throws IOException If the process fails to start.
     */
    public void startAttackWithFile(String hashFile, String mode, String attackMode, String target, String ruleFile,
                                    boolean force, boolean optimizedKernels, String workloadProfile) throws IOException {
        startAttackInternal(hashFile, mode, attackMode, target, ruleFile, force, optimizedKernels, workloadProfile);
    }

    /**
     * Starts a Hashcat attack using a direct string hash (wraps it into a temporary file).
     * Used primarily for remote attacks where the hash is received as a string.
     *
     * @param hashString The hash string to crack.
     * @param mode       The Hashcat hash mode.
     * @param attackMode The attack mode.
     * @param target     The wordlist path or mask string.
     * @param ruleFile   (Optional) Rule file path.
     * @throws IOException If file creation or process start fails.
     */
    public void startAttackWithString(String hashString, String mode, String attackMode, String target, String ruleFile) throws IOException {
        // Create a temporary file to store the single hash.
        File tempHashFile = File.createTempFile("hashkitty-target", ".txt");
        // Write the hash string to the temp file.
        java.nio.file.Files.writeString(tempHashFile.toPath(), hashString);
        // Ensure the file is deleted when JVM exits.
        tempHashFile.deleteOnExit();

        // Delegate to the internal starter using the temp file path.
        // Defaults: force=true (often needed for temp setups), optimized=false, workload=default.
        startAttackInternal(tempHashFile.getAbsolutePath(), mode, attackMode, target, ruleFile, true, false, null);
    }

    /**
     * Constructs the Hashcat command list based on the provided parameters.
     * Exposed for testing purposes.
     */
    public List<String> buildCommand(String mode, String attackMode, String ruleFile,
                                     boolean force, boolean optimizedKernels, String workloadProfile) {
        List<String> command = new ArrayList<>();
        command.add("hashcat"); // Assumes 'hashcat' is in the system PATH.

        // Add Hashcat mode.
        command.add("-m");
        command.add(mode);

        // Set Attack Mode (-a).
        command.add("-a");
        if ("Dictionary".equalsIgnoreCase(attackMode)) {
            command.add("0"); // Mode 0: Straight (Dictionary)
        } else if ("Mask".equalsIgnoreCase(attackMode)) {
            command.add("3"); // Mode 3: Brute-force/Mask
        } else {
            command.add("0"); // Default fallback
        }

        // Add status auto-update (every 5 seconds) to keep stdout flowing.
        command.add("--status");
        command.add("--status-timer=5");

        // Optional: Force flag.
        if (force) command.add("--force");

        // Optional: Optimized Kernels.
        if (optimizedKernels) command.add("-O");

        // Optional: Workload Profile.
        if (workloadProfile != null && !workloadProfile.isEmpty()) {
            command.add("-w");
            command.add(workloadProfile);
        }

        // Optional: Rules (only valid for dictionary mode usually, but hashcat allows combining).
        if (ruleFile != null && !ruleFile.isEmpty()) {
            command.add("-r");
            command.add(ruleFile);
        }

        return command;
    }

    /**
     * Internal method to construct the command and launch the process.
     */
    private void startAttackInternal(String hashFilePath, String mode, String attackMode, String target, String ruleFile,
                                     boolean force, boolean optimizedKernels, String workloadProfile) throws IOException {
        // Prevent multiple concurrent instances managed by this object.
        if (process != null && process.isAlive()) {
            throw new IOException("Hashcat is already running.");
        }

        // Build the base command.
        List<String> command = buildCommand(mode, attackMode, ruleFile, force, optimizedKernels, workloadProfile);

        // Append positional arguments which are not part of buildCommand signature in test but needed for execution.
        command.add(hashFilePath);
        command.add(target);

        // Log the constructed command for debugging.
        onStatusUpdate.accept("Executing: " + String.join(" ", command));

        // Configure the process builder.
        ProcessBuilder pb = new ProcessBuilder(command);
        // Merge stderr into stdout to capture error messages and status in one stream.
        pb.redirectErrorStream(true);

        // Start the process.
        process = pb.start();

        // Start a background thread to consume the output stream.
        // This is crucial to prevent the process from blocking.
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Check for progress updates.
                    Matcher matcher = PROGRESS_PATTERN.matcher(line);
                    if (matcher.find()) {
                        // Extract percentage.
                        onStatusUpdate.accept("Progress: " + matcher.group(1) + "%");
                    }

                    // Attempt to detect cracked passwords.
                    // Hashcat prints "hash:password" to stdout.
                    // This is a naive check; a more robust way is to use --outfile or --show.
                    // For now, we look for a colon separator where the first part matches a hash format (simplified).
                    // Actually, a safer heuristic for this MVP is: if line contains ':' and isn't a status line.
                    if (line.contains(":") && !line.startsWith("Session") && !line.startsWith("Status") && !line.startsWith("Time")) {
                         // Parse the password (right side of the last colon).
                         int lastColon = line.lastIndexOf(':');
                         if (lastColon != -1 && lastColon < line.length() - 1) {
                             String potentialPassword = line.substring(lastColon + 1);
                             onPasswordCracked.accept(potentialPassword);
                         }
                    }

                    // Echo everything to the status log (optional, maybe too verbose).
                    // onStatusUpdate.accept(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                // When the process ends, notify the callback.
                if (onComplete != null) {
                    onComplete.run();
                }
                onStatusUpdate.accept("Hashcat process finished.");
            }
        }).start();
    }

    /**
     * Stops the running Hashcat process if it exists and is alive.
     */
    public void stopCracking() {
        if (process != null && process.isAlive()) {
            // Forcibly destroy the process.
            process.destroy();
            onStatusUpdate.accept("Hashcat process stopped by user.");
        }
    }
}
