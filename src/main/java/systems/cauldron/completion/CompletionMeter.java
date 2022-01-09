package systems.cauldron.completion;

import java.util.concurrent.atomic.AtomicLong;

public class CompletionMeter {

    private final AtomicLong requestCount;
    private final AtomicLong sentTokenCount;
    private final AtomicLong receivedTokenCount;

    public CompletionMeter() {
        this.requestCount = new AtomicLong();
        this.sentTokenCount = new AtomicLong();
        this.receivedTokenCount = new AtomicLong();
    }

    public long getRequestCount() {
        return requestCount.get();
    }

    public long getSentTokenCount() {
        return sentTokenCount.get();
    }

    public long getReceivedTokenCount() {
        return receivedTokenCount.get();
    }

    public void addRequestCount(long count) {
        requestCount.addAndGet(count);
    }

    public void addSentTokenCount(long count) {
        sentTokenCount.addAndGet(count);
    }

    public void addReceivedTokenCount(long count) {
        receivedTokenCount.addAndGet(count);
    }
}
