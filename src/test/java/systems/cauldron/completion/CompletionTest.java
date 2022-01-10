package systems.cauldron.completion;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import systems.cauldron.completion.config.CompletionRequest;
import systems.cauldron.completion.config.SamplingConfig;
import systems.cauldron.completion.config.TerminationConfig;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.SubmissionPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled
public class CompletionTest {

    private final static Logger LOG = LogManager.getLogger(CompletionTest.class);

    @Test
    public void basicOpenAiTest() {
        String apiToken = Optional.ofNullable(System.getenv("OPENAI_API_TOKEN"))
                .orElseThrow(() -> new AssertionError("missing required environment variable"));
        CompletionProvider provider = CompletionProvider.create(apiToken, CompletionProvider.Type.OPENAI_DAVINCI);
        executeHelloWorldTest(provider);
    }

    @Test
    public void basicAi21Test() {
        String apiToken = Optional.ofNullable(System.getenv("AI21_API_TOKEN"))
                .orElseThrow(() -> new AssertionError("missing required environment variable"));
        CompletionProvider provider = CompletionProvider.create(apiToken, CompletionProvider.Type.AI21_J1_JUMBO);
        executeHelloWorldTest(provider);
    }

    private void executeHelloWorldTest(CompletionProvider provider) {
        String prompt = "His first program simply printed 'Hello";
        TerminationConfig terminationConfig = new TerminationConfig(3, new String[]{"\n"});
        SamplingConfig samplingConfig = new SamplingConfig(1.0, 1.0);
        CompletionRequest request = new CompletionRequest(prompt, terminationConfig, samplingConfig);
        CopyOnWriteArrayList<String> results = new CopyOnWriteArrayList<>();
        SubmissionPublisher<String> publisher = new SubmissionPublisher<>();
        provider.complete(request, publisher);
        publisher.consume(results::add).join();
        String finalCompletion = String.join("", results);
        LOG.info("completion: |{}| => \"{}\"", String.join("|", results), finalCompletion);
        String finalPrompt = prompt + finalCompletion;
        LOG.info("final prompt: {}", finalPrompt);
        String normalizedFinalResult = finalCompletion.toLowerCase(Locale.ROOT);
        assertTrue(normalizedFinalResult.contains("world"));
        CompletionMeter meter = provider.getMeter();
        long receivedTokenCount = meter.getReceivedTokenCount();
        assertTrue(receivedTokenCount > 0);
        long sentTokenCount = meter.getSentTokenCount();
        assertTrue(sentTokenCount > 0);
        long requestCount = meter.getRequestCount();
        assertEquals(1, requestCount);
    }
}