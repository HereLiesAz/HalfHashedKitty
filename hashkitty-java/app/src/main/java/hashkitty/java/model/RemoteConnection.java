package hashkitty.java.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Represents a saved remote connection, typically for an SSH target used in sniffing operations.
 * <p>
 * This class serves as a data model for the application's "Connection Manager" feature.
 * It uses JavaFX properties (StringProperty) to allow for seamless data binding with
 * JavaFX UI components (like TableViews), ensuring that changes to the model are
 * automatically reflected in the UI and vice-versa.
 * </p>
 * <p>
 * However, it also maintains standard String fields (`name` and `connectionString`)
 * separate from the UI properties. This separation is crucial for robust serialization
 * (e.g., to JSON) because JavaFX properties are not inherently serializable by libraries like Gson.
 * The `transient` keyword on the property fields prevents Gson from attempting to serialize them.
 * </p>
 */
public class RemoteConnection {

    /**
     * The user-defined friendly name for this connection.
     * Examples: "Home Lab", "Target Pi", "Cloud Server".
     */
    private final String name;

    /**
     * The connection string required to initiate the connection.
     * Format is typically "username@hostname" or "username@ip_address".
     * Example: "pi@192.168.1.100".
     */
    private final String connectionString;

    /**
     * JavaFX property wrapper for the name.
     * Marked as 'transient' to exclude it from serialization processes (like Gson/JSON).
     * This is lazily initialized only when requested by the UI.
     */
    private transient StringProperty nameProperty;

    /**
     * JavaFX property wrapper for the connection string.
     * Marked as 'transient' to exclude it from serialization processes.
     * This is lazily initialized only when requested by the UI.
     */
    private transient StringProperty connectionStringProperty;

    /**
     * Constructs a new RemoteConnection instance.
     *
     * @param name             A user-friendly name for the connection (e.g., "pwn-pi").
     *                         This is how the user will identify the connection in the UI list.
     * @param connectionString The actual connection string used by the SSH client (e.g., "user@192.168.1.100").
     *                         This string is parsed by the connection handler.
     */
    public RemoteConnection(String name, String connectionString) {
        // Assign the immutable name field.
        this.name = name;
        // Assign the immutable connectionString field.
        this.connectionString = connectionString;
    }

    /**
     * Retrieves the raw string value of the connection name.
     * This method is used when serialization is required or when accessing the data without UI overhead.
     *
     * @return The name of the connection.
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieves the JavaFX StringProperty for the name.
     * This method is used by JavaFX UI components (like TableColumn) to bind to the value.
     * If the property has not been initialized yet (lazy loading), it is created here
     * using the raw `name` value.
     *
     * @return The StringProperty object representing the name.
     */
    public StringProperty nameProperty() {
        // Check if the property object has been created.
        if (nameProperty == null) {
            // Create a new SimpleStringProperty initialized with the raw name value.
            nameProperty = new SimpleStringProperty(name);
        }
        // Return the property object for binding.
        return nameProperty;
    }

    /**
     * Retrieves the raw string value of the connection string.
     * This method is used when serialization is required or when passing the value to the SSH client.
     *
     * @return The connection string (e.g., "user@host").
     */
    public String getConnectionString() {
        return connectionString;
    }

    /**
     * Retrieves the JavaFX StringProperty for the connection string.
     * This method is used by JavaFX UI components to bind to the value.
     * If the property has not been initialized yet (lazy loading), it is created here
     * using the raw `connectionString` value.
     *
     * @return The StringProperty object representing the connection string.
     */
    public StringProperty connectionStringProperty() {
        // Check if the property object has been created.
        if (connectionStringProperty == null) {
            // Create a new SimpleStringProperty initialized with the raw connectionString value.
            connectionStringProperty = new SimpleStringProperty(connectionString);
        }
        // Return the property object for binding.
        return connectionStringProperty;
    }

    /**
     * Provides a user-friendly string representation of the object.
     * This is commonly used when the object is displayed in a simple UI list (like a ListView)
     * where a custom CellFactory hasn't been defined.
     *
     * @return A formatted string combining the name and connection string, e.g., "pwn-pi (user@192.168.1.100)".
     */
    @Override
    public String toString() {
        // Concatenate name and connection string for display.
        return getName() + " (" + getConnectionString() + ")";
    }
}
