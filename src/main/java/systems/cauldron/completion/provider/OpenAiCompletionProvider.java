package systems.cauldron.completion.provider;

import systems.cauldron.completion.CompletionProvider;
import systems.cauldron.completion.utility.HttpUtility;

import javax.json.*;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class OpenAiCompletionProvider implements CompletionProvider {

    public enum Engine {
        DAVINCI,
        CURIE,
        BABBAGE,
        ADA
    }

    private static final int MAX_TOKENS_LIMIT = 2048;
    private static final int STOP_SEQUENCE_LIMIT = 4;
    private static final String COMPLETION_ENDPOINT_TEMPLATE = "https://api.openai.com/v1/engines/%s/completions";

    private final URI completionEndpoint;
    private final String apiToken;

    public OpenAiCompletionProvider(String apiToken, Engine engine) {
        this.apiToken = apiToken;
        String engineId = switch (engine) {
            case DAVINCI -> "davinci";
            case CURIE -> "curie";
            case BABBAGE -> "babbage";
            case ADA -> "ada";
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
        executeRequest(requestJson, completionTokenHandler);
    }

    public void executeRequest(JsonObject jsonObject, Consumer<String> handler) {
        HttpRequest request = HttpUtility.buildRequest(jsonObject, completionEndpoint, apiToken);
        HttpUtility.buildClient()
                .sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                .thenAccept(response -> {
                    int statusCode = response.statusCode();
                    if (statusCode != 200) {
                        throw new RuntimeException("unexpected status code: " + statusCode);
                    }
                    response.body()
                            .filter(line -> !line.isEmpty())
                            .forEach(line -> {
                                if (line.startsWith("data: ")) {
                                    String dataValue = line.substring(6);
                                    if ("[DONE]".equals(dataValue)) {
                                        handler.accept(null);
                                    } else {
                                        JsonObject jsonResponse;
                                        try (JsonReader reader = Json.createReader(new StringReader(dataValue))) {
                                            jsonResponse = reader.readObject();
                                        }
                                        JsonArray choices = jsonResponse.getJsonArray("choices");
                                        if (!choices.isEmpty()) {
                                            JsonObject choice = choices.getJsonObject(0);
                                            String completionText = choice.getString("text");
                                            if (!completionText.isEmpty()) {
                                                handler.accept(completionText);
                                            }
                                        }
                                    }
                                }
                            });
                });
    }

    private static JsonObject buildRequest(CompletionRequest request) {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder()
                .add("stream", true)
                .add("logprobs", JsonValue.NULL)
                .add("echo", false)
                .add("prompt", request.prompt())
                .add("max_tokens", request.maxTokens())
                .add("n", 1)
                .add("best_of", 1)
                .add("temperature", 1.0)
                .add("top_p", 1.0)
                .add("presence_penalty", 0.0)
                .add("frequency_penalty", 0.0)
                .add("logit_bias", Json.createObjectBuilder()
                        .add("50256", -100));
        if (request.stopSequences().length != 0) {
            JsonArrayBuilder jsonStopSequences = Json.createArrayBuilder();
            Stream.of(request.stopSequences()).forEach(jsonStopSequences::add);
            objectBuilder.add("stop", jsonStopSequences);
        }
        return objectBuilder.build();
    }
}
