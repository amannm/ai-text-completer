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

    /*
    data: {"id": "cmpl-4IngrvKGxnQYPS9DER2neTvOVVvpW", "object": "text_completion", "created": 1640386261, "choices": [{"text": " Th", "index": 0, "logprobs": null, "finish_reason": null}], "model": "davinci:2020-05-03"}


     */


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