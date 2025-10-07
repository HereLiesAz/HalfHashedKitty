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

/**
 * A utility class for handling the import and export of application settings
 * to and from password-protected .hhk (zip) files.
 */
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
        String jsonContent = gson.toJson(connections);
        Path tempJsonFile = Files.createTempFile("hashkitty-export", ".json");
        Files.writeString(tempJsonFile, jsonContent);

        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setEncryptFiles(true);
        zipParameters.setEncryptionMethod(EncryptionMethod.AES);

        try (ZipFile zipFile = new ZipFile(file, password.toCharArray())) {
            zipFile.addFile(tempJsonFile.toFile(), zipParameters);
        }

        Files.delete(tempJsonFile);
    }

    /**
     * Imports a list of remote connections from a password-protected .hhk (zip) file.
     *
     * @param file The .hhk file to import from.
     * @param password The password for the archive.
     * @return A list of imported RemoteConnection objects.
     * @throws IOException if there is an error reading the file or the archive is malformed.
     * @throws ZipException if the password is incorrect or the file is not a valid zip archive.
     */
    public static List<RemoteConnection> importConnections(File file, String password) throws IOException, ZipException {
        Path tempDir = Files.createTempDirectory("hashkitty-import");

        try (ZipFile zipFile = new ZipFile(file, password.toCharArray())) {
            zipFile.extractAll(tempDir.toString());
        }

        File jsonFile = new File(tempDir.toFile(), JSON_FILE_NAME);
        if (!jsonFile.exists()) {
            File[] files = tempDir.toFile().listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null && files.length > 0) {
                jsonFile = files[0];
            } else {
                 throw new IOException("Could not find remotes.json in the archive.");
            }
        }
        String jsonContent = Files.readString(jsonFile.toPath());

        Type connectionListType = new TypeToken<List<RemoteConnection>>() {}.getType();
        List<RemoteConnection> importedConnections = gson.fromJson(jsonContent, connectionListType);

        Files.walk(tempDir)
                .map(Path::toFile)
                .sorted((o1, o2) -> -o1.compareTo(o2))
                .forEach(File::delete);

        return importedConnections;
    }

    /**
     * Exports a single remote connection to a JSON file.
     *
     * @param file        The target .json file to create.
     * @param connection  The connection to export.
     * @throws IOException if there is an error writing the file.
     */
    public static void exportSingleConnection(File file, RemoteConnection connection) throws IOException {
        String jsonContent = gson.toJson(connection);
        Files.writeString(file.toPath(), jsonContent);
    }

    /**
     * Imports a single remote connection from a JSON file.
     *
     * @param file The .json file to import from.
     * @return The imported RemoteConnection object.
     * @throws IOException if there is an error reading the file.
     */
    public static RemoteConnection importSingleConnection(File file) throws IOException {
        String jsonContent = Files.readString(file.toPath());
        return gson.fromJson(jsonContent, RemoteConnection.class);
    }
}