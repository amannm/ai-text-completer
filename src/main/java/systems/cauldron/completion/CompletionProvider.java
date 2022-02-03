package systems.cauldron.completion;

import systems.cauldron.completion.config.CompletionRequest;
import systems.cauldron.completion.provider.Ai21CompletionProvider;
import systems.cauldron.completion.provider.GooseAiCompletionProvider;
import systems.cauldron.completion.provider.OpenAiCompletionProvider;

import java.util.concurrent.SubmissionPublisher;

public abstract class CompletionProvider {

    public enum Type {
        OPENAI_DAVINCI,
        OPENAI_CURIE,
        OPENAI_BABBAGE,
        OPENAI_ADA,
        AI21_J1_LARGE,
        AI21_J1_JUMBO,
        GOOSEAI_GPT_J_6B,
        GOOSEAI_GPT_NEO_20B,
        GOOSEAI_GPT_NEO_2_7B,
        GOOSEAI_GPT_NEO_1_3B,
        GOOSEAI_GPT_NEO_125M,
        GOOSEAI_FAIRSEQ_13B,
        GOOSEAI_FAIRSEQ_6_7B,
        GOOSEAI_FAIRSEQ_2_7B,
        GOOSEAI_FAIRSEQ_1_3B,
        GOOSEAI_FAIRSEQ_125M,
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
            case GOOSEAI_GPT_J_6B -> new GooseAiCompletionProvider(apiToken, GooseAiCompletionProvider.Engine.GPT_J_6B);
            case GOOSEAI_GPT_NEO_20B -> new GooseAiCompletionProvider(apiToken, GooseAiCompletionProvider.Engine.GPT_NEO_20B);
            case GOOSEAI_GPT_NEO_2_7B -> new GooseAiCompletionProvider(apiToken, GooseAiCompletionProvider.Engine.GPT_NEO_2_7B);
            case GOOSEAI_GPT_NEO_1_3B -> new GooseAiCompletionProvider(apiToken, GooseAiCompletionProvider.Engine.GPT_NEO_1_3B);
            case GOOSEAI_GPT_NEO_125M -> new GooseAiCompletionProvider(apiToken, GooseAiCompletionProvider.Engine.GPT_NEO_125M);
            case GOOSEAI_FAIRSEQ_13B -> new GooseAiCompletionProvider(apiToken, GooseAiCompletionProvider.Engine.FAIRSEQ_13B);
            case GOOSEAI_FAIRSEQ_6_7B -> new GooseAiCompletionProvider(apiToken, GooseAiCompletionProvider.Engine.FAIRSEQ_6_7B);
            case GOOSEAI_FAIRSEQ_2_7B -> new GooseAiCompletionProvider(apiToken, GooseAiCompletionProvider.Engine.FAIRSEQ_2_7B);
            case GOOSEAI_FAIRSEQ_1_3B -> new GooseAiCompletionProvider(apiToken, GooseAiCompletionProvider.Engine.FAIRSEQ_1_3B);
            case GOOSEAI_FAIRSEQ_125M -> new GooseAiCompletionProvider(apiToken, GooseAiCompletionProvider.Engine.FAIRSEQ_125M);
        };
    }

    public CompletionMeter getMeter() {
        return meter;
    }

    public abstract void complete(CompletionRequest request, SubmissionPublisher<String> completionTokenHandler);
}
