package hashkitty.java.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class RemoteConnection {
    private final StringProperty name;
    private final StringProperty connectionString;

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

    // Override toString() to display a user-friendly format in the ListView
    @Override
    public String toString() {
        return getName() + " (" + getConnectionString() + ")";
    }
}