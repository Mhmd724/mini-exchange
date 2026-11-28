package com.moharajli.miniexchange.persistence;

import com.moharajli.miniexchange.common.Validation;
import com.moharajli.miniexchange.domain.Trade;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Saves trades in the background, one by one and in order, on a single thread.
 * While the database is down it retries the same trade with a growing delay.
 * When the queue is full, {@link #record} blocks until there is space.
 */
public final class TradeWriter implements TradeRecorder, AutoCloseable {

    private static final long POLL_MILLIS = 50;

    private final TradeRepository repository;
    private final BlockingQueue<Trade> queue;
    private final Duration initialBackoff;
    private final Duration maxBackoff;
    private final Thread thread;
    private volatile boolean running = true;

    public TradeWriter(TradeRepository repository, int capacity, Duration initialBackoff, Duration maxBackoff) {
        Validation.requireNotNull(repository, "Trade repository");
        Validation.requirePositive(capacity, "Queue capacity");
        Validation.requireNotNull(initialBackoff, "Initial backoff");
        Validation.requireNotNull(maxBackoff, "Max backoff");
        Validation.requirePositive(initialBackoff.toMillis(), "Initial backoff in ms");
        Validation.requireTrue(maxBackoff.compareTo(initialBackoff) >= 0, "Max backoff must not be below initial backoff");
        this.repository = repository;
        this.queue = new ArrayBlockingQueue<>(capacity);
        this.initialBackoff = initialBackoff;
        this.maxBackoff = maxBackoff;
        this.thread = new Thread(this::processQueue, "trade-writer");
    }

    public void start() {
        thread.start();
    }

    @Override
    public void record(List<Trade> trades) {
        Validation.requireNotNull(trades, "Trades");
        for (Trade trade : trades) {
            if (!running) {
                throw new IllegalStateException("Trade writer is closed");
            }
            try {
                queue.put(trade);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while recording a trade", e);
            }
        }
    }

    @Override
    public void close() {
        running = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void processQueue() {
        try {
            while (running || !queue.isEmpty()) {
                Trade trade = queue.poll(POLL_MILLIS, TimeUnit.MILLISECONDS);
                if (trade != null) {
                    saveWithRetry(trade);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void saveWithRetry(Trade trade) throws InterruptedException {
        Duration delay = initialBackoff;
        while (true) {
            try {
                repository.save(trade);
                return;
            } catch (RepositoryException e) {
                if (!e.retryable()) {
                    System.err.println("Trade rejected by the database, skipped: " + trade + " (" + e.getMessage() + ")");
                    return;
                }
                if (!running) {
                    System.err.println("Trade not saved (shutting down): " + trade);
                    return;
                }
                System.err.println("Could not save trade, retrying in " + delay.toMillis() + " ms: " + e.getMessage());
                Thread.sleep(delay.toMillis());
                delay = nextDelay(delay);
            }
        }
    }

    private Duration nextDelay(Duration delay) {
        Duration doubled = delay.multipliedBy(2);
        return doubled.compareTo(maxBackoff) > 0 ? maxBackoff : doubled;
    }
}
