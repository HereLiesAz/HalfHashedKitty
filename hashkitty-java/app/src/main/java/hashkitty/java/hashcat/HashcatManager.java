package hashkitty.java.hashcat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.Consumer;

public class HashcatManager {

    private Process hashcatProcess;
    private final Consumer<String> onCrackedPassword;

    public HashcatManager(Consumer<String> onCrackedPassword) {
        this.onCrackedPassword = onCrackedPassword;
    }

    public void startCracking(String hash, String mode, String wordlist) throws IOException {
        if (hashcatProcess != null && hashcatProcess.isAlive()) {
            System.out.println("A hashcat process is already running.");
            return;
        }

        // Construct the hashcat command
        // Example: hashcat -m <mode> <hash> <wordlist> --potfile-disable
        ProcessBuilder pb = new ProcessBuilder(
                "hashcat",
                "-m", mode,
                hash,
                wordlist,
                "--potfile-disable" // Potfiles store cracked hashes, disabling it for simplicity here
        );

        // Redirect error stream to output stream for easier monitoring
        pb.redirectErrorStream(true);

        // Start the process
        hashcatProcess = pb.start();
        System.out.println("Hashcat process started...");

        // Read the output of the process in a new thread
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(hashcatProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // A simple way to check for a cracked password in the output
                    // Hashcat's output format can vary, this is a basic check
                    if (line.contains(hash) && line.contains(":")) {
                        // Assuming the format is hash:password
                        String[] parts = line.split(":");
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
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void stopCracking() {
        if (hashcatProcess != null && hashcatProcess.isAlive()) {
            hashcatProcess.destroy();
            System.out.println("Hashcat process stopped.");
        }
    }
}