package hashkitty.java.model;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a single cracking task from a Hashtopolis server.
 * This class is used by Gson to deserialize the JSON response from the API.
 */
public class HashtopolisTask {

    @SerializedName("taskId")
    private int taskId;

    @SerializedName("taskName")
    private String taskName;

    @SerializedName("hashlistAlias")
    private String hashlistAlias;

    // Getters are needed for the TableView's PropertyValueFactory
    public int getTaskId() {
        return taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getHashlistAlias() {
        return hashlistAlias;
    }
}