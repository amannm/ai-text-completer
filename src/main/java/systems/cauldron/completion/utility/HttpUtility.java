package systems.cauldron.completion.utility;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class HttpUtility {

    public static HttpClient buildClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.of(10L, ChronoUnit.SECONDS))
                .build();
    }

    public static HttpRequest buildRequest(JsonObject jsonObject, URI uri, String token) {
        byte[] payload = serializeJson(jsonObject);
        return HttpRequest.newBuilder()
                .uri(uri)
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                .setHeader("Content-Type", "application/json")
                .setHeader("Authorization", String.format("Bearer %s", token))
                .build();
    }

    private static byte[] serializeJson(JsonObject jsonObject) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (JsonWriter writer = Json.createWriter(os)) {
            writer.write(jsonObject);
        }
        return os.toByteArray();
    }
}