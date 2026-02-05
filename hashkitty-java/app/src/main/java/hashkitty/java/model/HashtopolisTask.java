package hashkitty.java.model;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a single cracking task retrieved from a Hashtopolis server.
 * <p>
 * This class acts as a Data Transfer Object (DTO) for parsing JSON responses from the Hashtopolis API.
 * It is used by the `HashtopolisClient` to deserialize incoming task data into a Java object
 * that can be manipulated and displayed by the application.
 * </p>
 * <p>
 * The fields in this class are annotated with `@SerializedName` to map the JSON property names
 * (which may differ in casing or convention) to the Java field names.
 * </p>
 */
public class HashtopolisTask {

    /**
     * The unique identifier for the task in the Hashtopolis database.
     * Mapped from the JSON field "taskId".
     */
    @SerializedName("taskId")
    private int taskId;

    /**
     * The human-readable name of the task (e.g., "MD5 Crack 2023").
     * Mapped from the JSON field "taskName".
     */
    @SerializedName("taskName")
    private String taskName;

    /**
     * The alias or name of the hashlist associated with this task.
     * Mapped from the JSON field "hashlistAlias".
     */
    @SerializedName("hashlistAlias")
    private String hashlistAlias;

    /**
     * Retrieves the unique task ID.
     * <p>
     * This getter is required for JavaFX's `PropertyValueFactory` to access the private field
     * when populating a TableView column bound to "taskId".
     * </p>
     *
     * @return The integer ID of the task.
     */
    public int getTaskId() {
        return taskId;
    }

    /**
     * Retrieves the name of the task.
     * <p>
     * This getter is required for JavaFX's `PropertyValueFactory` to access the private field
     * when populating a TableView column bound to "taskName".
     * </p>
     *
     * @return The name string of the task.
     */
    public String getTaskName() {
        return taskName;
    }

    /**
     * Retrieves the hashlist alias associated with the task.
     * <p>
     * This getter is required for JavaFX's `PropertyValueFactory` to access the private field
     * when populating a TableView column bound to "hashlistAlias".
     * </p>
     *
     * @return The hashlist alias string.
     */
    public String getHashlistAlias() {
        return hashlistAlias;
    }
}
