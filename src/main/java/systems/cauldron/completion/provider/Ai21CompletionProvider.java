package systems.cauldron.completion.provider;

import systems.cauldron.completion.CompletionProvider;
import systems.cauldron.completion.utility.HttpUtility;

import javax.json.*;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class Ai21CompletionProvider implements CompletionProvider {

    public enum Engine {
        J1_LARGE,
        J1_JUMBO;
    }

    private static final int MAX_TOKENS_LIMIT = 2048;
    private static final int STOP_SEQUENCE_LIMIT = 4;
    private static final String COMPLETION_ENDPOINT_TEMPLATE = "https://api.ai21.com/studio/v1/%s/complete";

    private final URI completionEndpoint;
    private final String apiToken;

    public Ai21CompletionProvider(String apiToken, Engine engine) {
        this.apiToken = apiToken;
        String engineId = switch (engine) {
            case J1_LARGE -> "j1-large";
            case J1_JUMBO -> "j1-jumbo";
        };
        this.completionEndpoint = URI.create(String.format(COMPLETION_ENDPOINT_TEMPLATE, engineId));
    }

    @Override
    public void complete(CompletionRequest request, Consumer<String> completionTokenHandler) {
        if (request.maxTokens() > MAX_TOKENS_LIMIT) {
            throw new IllegalArgumentException("maximum tokens requested cannot exceed " + MAX_TOKENS_LIMIT);
        }
        if (request.stopSequences().length > STOP_SEQUENCE_LIMIT) {
            throw new IllegalArgumentException("number of stop sequences in request cannot exceed " + STOP_SEQUENCE_LIMIT);
        }
        JsonObject requestJson = buildRequest(request);
        executeRequest(requestJson)
                .thenAccept(response -> {
                    JsonArray completions = response.getJsonArray("completions");
                    if (completions.isEmpty()) {
                        completionTokenHandler.accept(null);
                        return;
                    }
                    JsonObject completion = completions.getJsonObject(0);
                    JsonObject data = completion.getJsonObject("data");
                    JsonObject finishReason = completion.getJsonObject("finishReason");
                    String reason = finishReason.getString("reason");
                    String stopSequence;
                    if ("stop".equals(reason)) {
                        stopSequence = finishReason.getString("sequence");
                    } else {
                        stopSequence = null;
                    }
                    String text = data.getString("text");
                    data.getJsonArray("tokens").stream().map(JsonValue::asJsonObject).forEach(token -> {
                        if (stopSequence != null) {
                            JsonObject generatedToken = token.getJsonObject("generatedToken");
                            String tokenValue = generatedToken.getString("token");
                            if (stopSequence.equals(tokenValue)) {
                                return;
                            }
                        }
                        JsonObject textRange = token.getJsonObject("textRange");
                        int start = textRange.getInt("start");
                        int end = textRange.getInt("end");
                        completionTokenHandler.accept(text.substring(start, end));
                    });
                });
    }

    private CompletableFuture<JsonObject> executeRequest(JsonObject jsonObject) {
        HttpRequest request = HttpUtility.buildRequest(jsonObject, completionEndpoint, apiToken);
        return HttpUtility.buildClient()
                .sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(response -> {
                    int statusCode = response.statusCode();
                    if (statusCode != 200) {
                        throw new RuntimeException("unexpected status code: " + statusCode);
                    }
                    try (JsonReader reader = Json.createReader(response.body())) {
                        return reader.readObject();
                    }
                });
    }

    private static JsonObject buildRequest(CompletionRequest request) {
        JsonArrayBuilder jsonStopSequences = Json.createArrayBuilder();
        Stream.of(request.stopSequences()).forEach(jsonStopSequences::add);
        return Json.createObjectBuilder()
                .add("prompt", request.prompt())
                .add("maxTokens", request.maxTokens())
                .add("stopSequences", jsonStopSequences)
                .add("numResults", 1)
                .add("topKReturn", 0)
                .add("temperature", 1.0)
                .add("topP", 1.0)
                .build();
    }
}
