package systems.cauldron.completion.provider;

import systems.cauldron.completion.CompletionProvider;
import systems.cauldron.completion.config.CompletionRequest;
import systems.cauldron.completion.config.SamplingConfig;
import systems.cauldron.completion.config.TerminationConfig;
import systems.cauldron.completion.tokenizer.Gpt3Tokenizer;
import systems.cauldron.completion.tokenizer.Tokenizer;
import systems.cauldron.completion.utility.HttpUtility;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.SubmissionPublisher;
import java.util.stream.Stream;

public class GooseAiCompletionProvider extends CompletionProvider {

    public enum Engine {
        GPT_J_6B,
        GPT_NEO_20B,
        GPT_NEO_2_7B,
        GPT_NEO_1_3B,
        GPT_NEO_125M,
        FAIRSEQ_13B,
        FAIRSEQ_6_7B,
        FAIRSEQ_2_7B,
        FAIRSEQ_1_3B,
        FAIRSEQ_125M,
    }

    private static final int MAX_TOKENS_LIMIT = 2048;
    private static final int STOP_SEQUENCE_LIMIT = 4;
    private static final double TEMPERATURE_LIMIT = 5.0; //TODO: figure out actual temp limit for OpenAI
    private static final double TOP_P_LIMIT = 1.0;

    private static final String COMPLETION_ENDPOINT_TEMPLATE = "https://api.goose.ai/v1/engines/%s/completions";

    private final URI completionEndpoint;
    private final String apiToken;
    private final Tokenizer tokenizer;

    public GooseAiCompletionProvider(String apiToken, Engine engine) {
        String engineId = switch (engine) {
            case GPT_J_6B -> "gpt-j-6b";
            case GPT_NEO_20B -> "gpt-neo-20b";
            case GPT_NEO_2_7B -> "gpt-neo-2-7b";
            case GPT_NEO_1_3B -> "gpt-neo-1-3b";
            case GPT_NEO_125M -> "gpt-neo-125m";
            case FAIRSEQ_13B -> "fairseq-13b";
            case FAIRSEQ_6_7B -> "fairseq-6-7b";
            case FAIRSEQ_2_7B -> "fairseq-2-7b";
            case FAIRSEQ_1_3B -> "fairseq-1-3b";
            case FAIRSEQ_125M -> "fairseq-125m";
        };
        this.completionEndpoint = URI.create(String.format(COMPLETION_ENDPOINT_TEMPLATE, engineId));
        this.apiToken = apiToken;
        this.tokenizer = Gpt3Tokenizer.getInstance();
    }

    @Override
    public void complete(CompletionRequest request, SubmissionPublisher<String> completionTokenHandler) {
        TerminationConfig terminationConfig = request.terminationConfig();
        int promptTokenCount = getTokenCount(request.prompt());
        int requestMaxTokenCount = promptTokenCount + terminationConfig.maxTokens();
        if (requestMaxTokenCount > MAX_TOKENS_LIMIT) {
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
        JsonObject requestJson = buildRequest(request);
        HttpRequest httpRequest = HttpUtility.buildRequest(requestJson, completionEndpoint, apiToken);
        HttpUtility.buildClient()
                .sendAsync(httpRequest, HttpResponse.BodyHandlers.ofLines())
                .thenAccept(response -> {
                    int statusCode = response.statusCode();
                    if (statusCode != 200) {
                        throw new RuntimeException("unexpected status code: " + statusCode);
                    }
                    meter.addRequestCount(1);
                    meter.addSentTokenCount(promptTokenCount);
                    response.body()
                            .filter(line -> !line.isEmpty())
                            .forEach(line -> {
                                if (line.startsWith("data: ")) {
                                    String dataValue = line.substring(6);
                                    if ("[DONE]".equals(dataValue)) {
                                        completionTokenHandler.close();
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
                                                int receivedTokenCount = getTokenCount(completionText);
                                                meter.addReceivedTokenCount(receivedTokenCount);
                                                completionTokenHandler.submit(completionText);
                                            }
                                        }
                                    }
                                }
                            });
                })
                .exceptionally(throwable -> {
                    completionTokenHandler.closeExceptionally(throwable);
                    return null;
                });
    }

    private int getTokenCount(String prompt) {
        List<String> tokens = tokenizer.tokenize(prompt);
        return tokens.size();
    }

    private static JsonObject buildRequest(CompletionRequest request) {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder()
                .add("stream", true)
                .add("prompt", request.prompt())
                .add("max_tokens", request.terminationConfig().maxTokens());
        if (request.terminationConfig().stopSequences().length != 0) {
            JsonArrayBuilder jsonStopSequences = Json.createArrayBuilder();
            Stream.of(request.terminationConfig().stopSequences())
                    .forEach(jsonStopSequences::add);
            objectBuilder.add("stop", jsonStopSequences);
        }
        return objectBuilder.build();
    }
}
