package hashkitty.java.hashtopolis;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import hashkitty.java.model.HashtopolisTask;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

/**
 * A client for interacting with a Hashtopolis server API.
 */
public class HashtopolisClient {

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    /**
     * Fetches a list of tasks from the Hashtopolis server.
     *
     * @param serverUrl The base URL of the Hashtopolis server.
     * @param apiKey    The user's API key.
     * @return A list of {@link HashtopolisTask} objects.
     * @throws IOException if there is a network error or the server returns a non-successful response.
     */
    public List<HashtopolisTask> getTasks(String serverUrl, String apiKey) throws IOException {
        // Ensure the URL has a trailing slash
        if (!serverUrl.endsWith("/")) {
            serverUrl += "/";
        }

        String url = serverUrl + "api/v1/tasks?accessKey=" + apiKey;

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response + " - " + response.body().string());
            }

            String responseBody = response.body().string();
            Type taskListType = new TypeToken<List<HashtopolisTask>>() {}.getType();
            return gson.fromJson(responseBody, taskListType);
        }
    }
}