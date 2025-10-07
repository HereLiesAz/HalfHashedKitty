package hashkitty.java.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Represents a saved remote connection, typically for an SSH target.
 * This class uses JavaFX properties to allow for easy data binding with UI components,
 * but separates the core data from the UI properties for robust serialization.
 */
public class RemoteConnection {
    private final String name;
    private final String connectionString;

    private transient StringProperty nameProperty;
    private transient StringProperty connectionStringProperty;

    /**
     * Constructs a new RemoteConnection.
     *
     * @param name             A user-friendly name for the connection (e.g., "pwn-pi").
     * @param connectionString The actual connection string (e.g., "user@192.168.1.100").
     */
    public RemoteConnection(String name, String connectionString) {
        this.name = name;
        this.connectionString = connectionString;
    }

    public String getName() {
        return name;
    }

    public StringProperty nameProperty() {
        if (nameProperty == null) {
            nameProperty = new SimpleStringProperty(name);
        }
        return nameProperty;
    }

    public String getConnectionString() {
        return connectionString;
    }

    public StringProperty connectionStringProperty() {
        if (connectionStringProperty == null) {
            connectionStringProperty = new SimpleStringProperty(connectionString);
        }
        return connectionStringProperty;
    }

    /**
     * Provides a user-friendly string representation for display in UI controls like ListView.
     * @return The formatted string, e.g., "pwn-pi (user@192.168.1.100)".
     */
    @Override
    public String toString() {
        return getName() + " (" + getConnectionString() + ")";
    }
}