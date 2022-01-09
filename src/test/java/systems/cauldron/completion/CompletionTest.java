package systems.cauldron.completion;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Disabled
public class CompletionTest {

    private final static Logger LOG = LogManager.getLogger(CompletionTest.class);

    @Test
    public void basicOpenAiTest() throws InterruptedException {
        String apiToken = Optional.ofNullable(System.getenv("OPENAI_API_TOKEN"))
                .orElseThrow(() -> new AssertionError("missing required environment variable"));
        CompletionProvider provider = CompletionProvider.create(apiToken, CompletionProvider.Type.OPENAI_DAVINCI);
        executeHelloWorldTest(provider);
    }

    @Test
    public void basicAi21Test() throws InterruptedException {
        String apiToken = Optional.ofNullable(System.getenv("AI21_API_TOKEN"))
                .orElseThrow(() -> new AssertionError("missing required environment variable"));
        CompletionProvider provider = CompletionProvider.create(apiToken, CompletionProvider.Type.AI21_J1_JUMBO);
        executeHelloWorldTest(provider);
    }

    private void executeHelloWorldTest(CompletionProvider provider) throws InterruptedException {
        String prompt = "His first program simply printed 'Hello";
        CompletionProvider.TerminationConfig terminationConfig = new CompletionProvider.TerminationConfig(3, new String[]{"\n"});
        CompletionProvider.SamplingConfig samplingConfig = new CompletionProvider.SamplingConfig(1.0, 1.0);
        CompletionProvider.CompletionRequest request = new CompletionProvider.CompletionRequest(prompt, terminationConfig, samplingConfig);
        CountDownLatch latch = new CountDownLatch(1);
        CopyOnWriteArrayList<String> results = new CopyOnWriteArrayList<>();
        provider.complete(request, completionToken -> {
            if (completionToken == null) {
                latch.countDown();
            } else {
                results.add(completionToken);
            }
        });
        if (!latch.await(60L, TimeUnit.SECONDS)) {
            fail("API request timed out");
        }
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