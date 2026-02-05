package hashkitty.java.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import hashkitty.java.model.RemoteConnection;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Utility class for handling HashKitty's custom file format (.hhk).
 * <p>
 * The .hhk format is essentially an AES-encrypted ZIP archive containing JSON data.
 * It is used for securely exporting and importing configuration data, such as
 * the list of saved remote connections.
 * </p>
 */
public class HhkUtil {

    /** The standard name of the JSON file inside the archive. */
    private static final String JSON_FILE_NAME = "remotes.json";

    /** Gson instance configured for pretty printing. */
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Exports a list of remote connections to a password-protected .hhk (zip) file.
     *
     * @param file        The target .hhk file to create.
     * @param password    The password used to encrypt the archive.
     * @param connections The list of RemoteConnection objects to export.
     * @throws IOException if there is an error writing the temporary files.
     */
    public static void exportConnections(File file, String password, List<RemoteConnection> connections) throws IOException {
        // Serialize the list of connections to a JSON string.
        String jsonContent = gson.toJson(connections);

        // Create a temporary file to hold the JSON content.
        Path tempJsonFile = Files.createTempFile("hashkitty-export", ".json");
        Files.writeString(tempJsonFile, jsonContent);

        // Configure Zip parameters for AES encryption.
        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setEncryptFiles(true);
        zipParameters.setEncryptionMethod(EncryptionMethod.AES);

        // Create the ZipFile and add the temporary JSON file to it.
        try (ZipFile zipFile = new ZipFile(file, password.toCharArray())) {
            // This adds the file to the root of the zip.
            zipFile.addFile(tempJsonFile.toFile(), zipParameters);
        }

        // Clean up the temporary file.
        Files.delete(tempJsonFile);
    }

    /**
     * Imports a list of remote connections from a password-protected .hhk (zip) file.
     *
     * @param file     The .hhk file to import from.
     * @param password The password to decrypt the archive.
     * @return A list of imported RemoteConnection objects.
     * @throws IOException  if there is an error reading the file or the archive structure is invalid.
     * @throws ZipException if the password is incorrect or the file is corrupted.
     */
    public static List<RemoteConnection> importConnections(File file, String password) throws IOException, ZipException {
        // Create a temporary directory to extract the archive contents.
        Path tempDir = Files.createTempDirectory("hashkitty-import");

        // Extract the zip file using the provided password.
        try (ZipFile zipFile = new ZipFile(file, password.toCharArray())) {
            zipFile.extractAll(tempDir.toString());
        }

        // Locate the JSON file inside the extracted directory.
        File jsonFile = new File(tempDir.toFile(), JSON_FILE_NAME);
        if (!jsonFile.exists()) {
            // Fallback: If the exact name doesn't match, look for any .json file.
            File[] files = tempDir.toFile().listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null && files.length > 0) {
                jsonFile = files[0];
            } else {
                 throw new IOException("Could not find remotes.json in the archive.");
            }
        }

        // Read the JSON content.
        String jsonContent = Files.readString(jsonFile.toPath());

        // Deserialize the JSON back into a List of RemoteConnection objects.
        // TypeToken is used to handle generic types with Gson.
        Type connectionListType = new TypeToken<List<RemoteConnection>>() {}.getType();
        List<RemoteConnection> importedConnections = gson.fromJson(jsonContent, connectionListType);

        // Clean up: delete the temporary directory and its contents.
        Files.walk(tempDir)
                .map(Path::toFile)
                .sorted((o1, o2) -> -o1.compareTo(o2)) // Reverse order to delete files before directories
                .forEach(File::delete);

        return importedConnections;
    }

    /**
     * Exports a single remote connection to a raw JSON file (unencrypted).
     *
     * @param file       The target .json file.
     * @param connection The connection to export.
     * @throws IOException if there is an error writing the file.
     */
    public static void exportSingleConnection(File file, RemoteConnection connection) throws IOException {
        String jsonContent = gson.toJson(connection);
        Files.writeString(file.toPath(), jsonContent);
    }

    /**
     * Imports a single remote connection from a raw JSON file (unencrypted).
     *
     * @param file The .json file to read.
     * @return The imported RemoteConnection object.
     * @throws IOException if there is an error reading the file.
     */
    public static RemoteConnection importSingleConnection(File file) throws IOException {
        String jsonContent = Files.readString(file.toPath());
        return gson.fromJson(jsonContent, RemoteConnection.class);
    }
}
