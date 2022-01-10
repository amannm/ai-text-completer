package systems.cauldron.completion.config;

public record TerminationConfig(int maxTokens, String[] stopSequences) {
}
