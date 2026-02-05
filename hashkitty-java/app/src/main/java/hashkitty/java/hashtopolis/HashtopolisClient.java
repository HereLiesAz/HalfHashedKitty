package hashkitty.java.hashtopolis;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import hashkitty.java.model.HashtopolisTask;
import okhttp3.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

/**
 * A client wrapper for the Hashtopolis API.
 * <p>
 * This class handles communication with a Hashtopolis server, allowing the application
 * to retrieve assigned tasks for a specific agent.
 * </p>
 */
public class HashtopolisClient {

    /** The HTTP client instance (OkHttp). */
    private final OkHttpClient client;
    /** JSON processor (Gson). */
    private final Gson gson;
    /** The base URL of the Hashtopolis server (e.g., "https://hashtopolis.example.com/api"). */
    private final String baseUrl;
    /** The secret key used for authentication/Voucher. */
    private final String apiKey;

    /**
     * Constructs a new HashtopolisClient.
     *
     * @param baseUrl The API endpoint URL.
     * @param apiKey  The access key/voucher.
     */
    public HashtopolisClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }

    /**
     * Retrieves the list of tasks assigned to the agent associated with the API key.
     *
     * @return A list of {@link HashtopolisTask} objects. Returns an empty list on failure.
     */
    public List<HashtopolisTask> getTasks() {
        // Construct the full URL.
        // NOTE: This endpoint structure is hypothetical/simplified for this implementation.
        // A real Hashtopolis API call involves a more complex action/request payload structure.
        String url = baseUrl + "/tasks?key=" + apiKey;

        // Build the request.
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("Hashtopolis API Error: " + response.code());
                return Collections.emptyList();
            }

            // Parse the response body.
            if (response.body() != null) {
                String responseBody = response.body().string();
                Type listType = new TypeToken<List<HashtopolisTask>>() {}.getType();
                return gson.fromJson(responseBody, listType);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }
}
