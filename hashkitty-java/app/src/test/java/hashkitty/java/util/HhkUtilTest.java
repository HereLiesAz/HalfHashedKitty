package hashkitty.java.util;

import hashkitty.java.model.RemoteConnection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class HhkUtilTest {

    @TempDir
    Path tempDir;

    @Test
    void testJsonImportExport() throws IOException {
        // 1. Setup
        RemoteConnection originalConnection = new RemoteConnection("test-pi", "test@1.2.3.4");
        File testFile = tempDir.resolve("test_connection.json").toFile();

        // 2. Export the connection to a JSON file
        HhkUtil.exportSingleConnection(testFile, originalConnection);
        assertTrue(testFile.exists(), "Exported JSON file should exist.");

        // 3. Import the connection from the JSON file
        RemoteConnection importedConnection = HhkUtil.importSingleConnection(testFile);

        // 4. Assert that the imported data matches the original data
        assertNotNull(importedConnection, "Imported connection should not be null.");
        assertEquals(originalConnection.getName(), importedConnection.getName(), "The name of the connection should match.");
        assertEquals(originalConnection.getConnectionString(), importedConnection.getConnectionString(), "The connection string should match.");
    }
}