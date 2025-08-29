package com.hereliesaz.connectionmanager.api;

import java.util.List;

public class HashtopolisApiModels {

    public static class Task {
        // Using Integer to handle potential nulls from Gson
        private Integer taskId;
        private Integer supertaskId;
        private String name;
        private int type;
        private int priority;

        public int getId() {
            return (taskId != null) ? taskId : supertaskId;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return (type == 0) ? "Task" : "Supertask";
        }

        public int getPriority() {
            return priority;
        }
    }

    public static class TaskListResponse {
        private List<Task> tasks;

        public List<Task> getTasks() {
            return tasks;
        }
    }

    public static class CreateTaskRequest {
        private String name;
        private int hashlistId;
        private String attackCmd;
        private int chunksize;
        private int statusTimer;
        private int priority;
        private int maxAgents;
        // Assuming crackerVersionId is required, defaulting to a common value
        private int crackerVersionId = 2;

        public CreateTaskRequest(String name, int hashlistId, String attackCmd, int chunksize, int statusTimer, int priority, int maxAgents) {
            this.name = name;
            this.hashlistId = hashlistId;
            this.attackCmd = attackCmd;
            this.chunksize = chunksize;
            this.statusTimer = statusTimer;
            this.priority = priority;
            this.maxAgents = maxAgents;
        }
    }

    public static class CreateTaskResponse {
        private int taskId;

        public int getTaskId() {
            return taskId;
        }
    }
}
