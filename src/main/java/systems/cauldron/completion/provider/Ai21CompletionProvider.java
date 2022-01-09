package systems.cauldron.completion.provider;

import systems.cauldron.completion.CompletionProvider;
import systems.cauldron.completion.utility.HttpUtility;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
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
    private static final double TEMPERATURE_LIMIT = 5.0;
    private static final double TOP_P_LIMIT = 1.0;
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
        validate(request);
        JsonObject requestJson = buildRequest(request);
        executeRequest(requestJson)
                .thenAccept(response -> {
                    JsonArray completions = response.getJsonArray("completions");
                    if (!completions.isEmpty()) {
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
                        data.getJsonArray("tokens").stream()
                                .map(JsonValue::asJsonObject)
                                .forEach(token -> {
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
                    }
                    completionTokenHandler.accept(null);
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
        Stream.of(request.terminationConfig().stopSequences())
                .forEach(jsonStopSequences::add);
        return Json.createObjectBuilder()
                .add("prompt", request.prompt())
                .add("maxTokens", request.terminationConfig().maxTokens())
                .add("stopSequences", jsonStopSequences)
                .add("numResults", 1)
                .add("topKReturn", 0)
                .add("temperature", request.samplingConfig().temperature())
                .add("topP", request.samplingConfig().topP())
                .build();
    }

    private static void validate(CompletionRequest request) {
        TerminationConfig terminationConfig = request.terminationConfig();
        if (terminationConfig.maxTokens() > MAX_TOKENS_LIMIT) {
            throw new IllegalArgumentException("maximum tokens requested cannot exceed " + MAX_TOKENS_LIMIT);
        }
        if (terminationConfig.stopSequences().length > STOP_SEQUENCE_LIMIT) {
            throw new IllegalArgumentException("number of stop sequences in request cannot exceed " + STOP_SEQUENCE_LIMIT);
        }
        SamplingConfig samplingConfig = request.samplingConfig();
        if (samplingConfig.temperature() > TEMPERATURE_LIMIT) {
            throw new IllegalArgumentException("temperature cannot exceed " + TEMPERATURE_LIMIT);
        }
        if (samplingConfig.topP() > TOP_P_LIMIT) {
            throw new IllegalArgumentException("top-p cannot exceed " + TOP_P_LIMIT);
        }
    }
}
