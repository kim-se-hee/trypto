package ksh.tryptocollector.ingest;

import java.util.concurrent.TimeUnit;

public class ConnectionLiveness {
    private volatile long lastReceivedNanos = System.nanoTime();

    public void recordReceive() {
        lastReceivedNanos = System.nanoTime();
    }

    public long secondsSinceLastReceive() {
        return TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - lastReceivedNanos);
    }
}
