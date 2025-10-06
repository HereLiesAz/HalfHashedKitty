package hashkitty.java.util;

import com.google.gson.Gson;
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

public class HhkUtil {

    private static final String JSON_FILE_NAME = "remotes.json";
    private static final Gson gson = new Gson();

    /**
     * Exports a list of remote connections to a password-protected .hhk (zip) file.
     *
     * @param file The target .hhk file to create.
     * @param password The password for the archive.
     * @param connections The list of connections to export.
     * @throws IOException if there is an error writing the temporary JSON file or creating the zip file.
     */
    public static void exportConnections(File file, String password, List<RemoteConnection> connections) throws IOException {
        // 1. Serialize the list of connections to JSON
        String jsonContent = gson.toJson(connections);

        // 2. Write JSON to a temporary file
        Path tempJsonFile = Files.createTempFile("hashkitty-export", ".json");
        Files.writeString(tempJsonFile, jsonContent);

        // 3. Create a password-protected zip file with the JSON data
        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setEncryptFiles(true);
        zipParameters.setEncryptionMethod(EncryptionMethod.AES);
        // AES key strength is determined by password length, zip4j handles this automatically

        try (ZipFile zipFile = new ZipFile(file, password.toCharArray())) {
            zipFile.addFile(tempJsonFile.toFile(), zipParameters);
        }

        // 4. Clean up the temporary file
        Files.delete(tempJsonFile);
    }

    /**
     * Imports a list of remote connections from a password-protected .hhk (zip) file.
     *
     * @param file The .hhk file to import from.
     * @param password The password for the archive.
     * @return A list of imported RemoteConnection objects.
     * @throws IOException if there is an error reading the file.
     * @throws ZipException if the password is incorrect or the file is not a valid zip.
     */
    public static List<RemoteConnection> importConnections(File file, String password) throws IOException, ZipException {
        Path tempDir = Files.createTempDirectory("hashkitty-import");

        // 1. Extract the contents of the zip file
        try (ZipFile zipFile = new ZipFile(file, password.toCharArray())) {
            zipFile.extractAll(tempDir.toString());
        }

        // 2. Find and read the JSON file
        File jsonFile = new File(tempDir.toFile(), JSON_FILE_NAME);
        if (!jsonFile.exists()) {
             // Fallback for older exports that might not have the standard name
            File[] files = tempDir.toFile().listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null && files.length > 0) {
                jsonFile = files[0];
            } else {
                 throw new IOException("Could not find remotes.json in the archive.");
            }
        }
        String jsonContent = Files.readString(jsonFile.toPath());

        // 3. Deserialize the JSON back into a list of connections
        Type connectionListType = new TypeToken<List<RemoteConnection>>() {}.getType();
        List<RemoteConnection> importedConnections = gson.fromJson(jsonContent, connectionListType);

        // 4. Clean up the temporary directory
        Files.walk(tempDir)
                .map(Path::toFile)
                .forEach(File::delete);

        return importedConnections;
    }
}