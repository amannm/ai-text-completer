package systems.cauldron.completion;

import systems.cauldron.completion.config.CompletionRequest;
import systems.cauldron.completion.provider.Ai21CompletionProvider;
import systems.cauldron.completion.provider.OpenAiCompletionProvider;

import java.util.concurrent.SubmissionPublisher;

public abstract class CompletionProvider {

    public enum Type {
        OPENAI_DAVINCI,
        OPENAI_CURIE,
        OPENAI_BABBAGE,
        OPENAI_ADA,
        AI21_J1_LARGE,
        AI21_J1_JUMBO
    }

    protected final CompletionMeter meter = new CompletionMeter();

    public static CompletionProvider create(String apiToken, Type type) {
        return switch (type) {
            case OPENAI_DAVINCI -> new OpenAiCompletionProvider(apiToken, OpenAiCompletionProvider.Engine.DAVINCI);
            case OPENAI_CURIE -> new OpenAiCompletionProvider(apiToken, OpenAiCompletionProvider.Engine.CURIE);
            case OPENAI_BABBAGE -> new OpenAiCompletionProvider(apiToken, OpenAiCompletionProvider.Engine.BABBAGE);
            case OPENAI_ADA -> new OpenAiCompletionProvider(apiToken, OpenAiCompletionProvider.Engine.ADA);
            case AI21_J1_LARGE -> new Ai21CompletionProvider(apiToken, Ai21CompletionProvider.Engine.J1_LARGE);
            case AI21_J1_JUMBO -> new Ai21CompletionProvider(apiToken, Ai21CompletionProvider.Engine.J1_JUMBO);
        };
    }

    public CompletionMeter getMeter() {
        return meter;
    }

    public abstract void complete(CompletionRequest request, SubmissionPublisher<String> completionTokenHandler);
}
