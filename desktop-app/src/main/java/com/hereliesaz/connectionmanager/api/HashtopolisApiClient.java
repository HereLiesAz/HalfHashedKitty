package com.hereliesaz.connectionmanager.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class HashtopolisApiClient {
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private final String baseUrl;
    private final String apiKey;

    public HashtopolisApiClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    public List<HashtopolisApiModels.Task> listTasks() throws IOException {
        JsonObject jsonRequest = new JsonObject();
        jsonRequest.addProperty("section", "task");
        jsonRequest.addProperty("request", "listTasks");
        jsonRequest.addProperty("accessKey", apiKey);

        RequestBody body = RequestBody.create(jsonRequest.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(baseUrl + "/api/server.php")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String responseBody = response.body().string();
            HashtopolisApiModels.TaskListResponse taskListResponse = gson.fromJson(responseBody, HashtopolisApiModels.TaskListResponse.class);

            if (taskListResponse != null && taskListResponse.getTasks() != null) {
                return taskListResponse.getTasks();
            } else {
                return Collections.emptyList();
            }
        }
    }

    public HashtopolisApiModels.CreateTaskResponse createTask(HashtopolisApiModels.CreateTaskRequest taskRequest) throws IOException {
        JsonObject jsonRequest = gson.toJsonTree(taskRequest).getAsJsonObject();
        jsonRequest.addProperty("section", "task");
        jsonRequest.addProperty("request", "createTask");
        jsonRequest.addProperty("accessKey", apiKey);

        RequestBody body = RequestBody.create(jsonRequest.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(baseUrl + "/api/server.php")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String responseBody = response.body().string();
            return gson.fromJson(responseBody, HashtopolisApiModels.CreateTaskResponse.class);
        }
    }
}
