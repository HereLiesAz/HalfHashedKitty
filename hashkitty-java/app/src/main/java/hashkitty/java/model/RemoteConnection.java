package hashkitty.java.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Represents a saved remote connection, typically for an SSH target.
 * This class uses JavaFX properties to allow for easy data binding with UI components.
 */
public class RemoteConnection {
    private final StringProperty name;
    private final StringProperty connectionString;

    /**
     * Constructs a new RemoteConnection.
     *
     * @param name             A user-friendly name for the connection (e.g., "pwn-pi").
     * @param connectionString The actual connection string (e.g., "user@192.168.1.100").
     */
    public RemoteConnection(String name, String connectionString) {
        this.name = new SimpleStringProperty(name);
        this.connectionString = new SimpleStringProperty(connectionString);
    }

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public String getConnectionString() {
        return connectionString.get();
    }

    public StringProperty connectionStringProperty() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString.set(connectionString);
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