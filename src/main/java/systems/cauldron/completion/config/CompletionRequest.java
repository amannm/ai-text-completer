package systems.cauldron.completion.config;

public record CompletionRequest(String prompt, TerminationConfig terminationConfig, SamplingConfig samplingConfig) {
}
