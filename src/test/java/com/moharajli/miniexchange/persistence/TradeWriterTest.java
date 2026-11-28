package com.moharajli.miniexchange.persistence;

import com.moharajli.miniexchange.domain.OwnerId;
import com.moharajli.miniexchange.domain.Trade;
import com.moharajli.miniexchange.domain.TradeSide;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeWriterTest {

    private static final Duration INITIAL_BACKOFF = Duration.ofMillis(1);
    private static final Duration MAX_BACKOFF = Duration.ofMillis(5);

    private final FakeTradeRepository repository = new FakeTradeRepository();
    private TradeWriter writer;

    @AfterEach
    void tearDown() {
        repository.open();
        if (writer != null) {
            writer.close();
        }
    }

    @Test
    void savesRecordedTradesInOrder() {
        writer = startWriter(10);

        writer.record(List.of(trade(1), trade(2), trade(3)));
        writer.close();

        assertEquals(List.of(trade(1), trade(2), trade(3)), repository.saved());
    }

    @Test
    void retriesTheSameTradeUntilTheDatabaseRecovers() {
        repository.failNextSaves(3);
        writer = startWriter(10);

        writer.record(List.of(trade(1)));
        waitUntil(() -> repository.saved().size() == 1);

        assertEquals(4, repository.attempts());
        assertEquals(List.of(trade(1)), repository.saved());
    }

    @Test
    void skipsATradeTheDatabaseRejects() {
        repository.rejectNextSave();
        writer = startWriter(10);

        writer.record(List.of(trade(1), trade(2)));
        writer.close();

        assertEquals(2, repository.attempts());
        assertEquals(List.of(trade(2)), repository.saved());
    }

    @Test
    void recordBlocksWhileTheQueueIsFull() throws Exception {
        repository.hold();
        writer = startWriter(1);
        writer.record(List.of(trade(1)));
        waitUntil(() -> repository.attempts() == 1);
        writer.record(List.of(trade(2)));

        CompletableFuture<Void> third = CompletableFuture.runAsync(() -> writer.record(List.of(trade(3))));
        Thread.sleep(100);
        assertFalse(third.isDone());

        repository.open();
        third.get(2, TimeUnit.SECONDS);
        writer.close();

        assertEquals(List.of(trade(1), trade(2), trade(3)), repository.saved());
    }

    @Test
    void givesUpOnShutdownWhileTheDatabaseIsDown() {
        repository.failNextSaves(Integer.MAX_VALUE);
        writer = startWriter(10);
        writer.record(List.of(trade(1)));
        waitUntil(() -> repository.attempts() >= 1);

        writer.close();

        assertTrue(repository.saved().isEmpty());
    }

    @Test
    void rejectsTradesAfterClose() {
        writer = startWriter(10);
        writer.close();

        assertThrows(IllegalStateException.class, () -> writer.record(List.of(trade(1))));
    }

    @Test
    void rejectsInvalidSettings() {
        assertThrows(IllegalArgumentException.class,
                () -> new TradeWriter(repository, 0, INITIAL_BACKOFF, MAX_BACKOFF));
        assertThrows(IllegalArgumentException.class,
                () -> new TradeWriter(repository, 10, MAX_BACKOFF, INITIAL_BACKOFF));
    }

    private TradeWriter startWriter(int capacity) {
        TradeWriter started = new TradeWriter(repository, capacity, INITIAL_BACKOFF, MAX_BACKOFF);
        started.start();
        return started;
    }

    private static Trade trade(long buyOrderId) {
        return new Trade(new TradeSide(buyOrderId, new OwnerId(1)), new TradeSide(buyOrderId + 1000, new OwnerId(2)),
                10, 100, Instant.EPOCH);
    }

    private static void waitUntil(BooleanSupplier condition) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("condition not met within 2 seconds");
            }
            Thread.onSpinWait();
        }
    }

    private static final class FakeTradeRepository implements TradeRepository {

        private final List<Trade> saved = new CopyOnWriteArrayList<>();
        private final AtomicInteger attempts = new AtomicInteger();
        private final AtomicInteger failuresLeft = new AtomicInteger();
        private volatile boolean rejectNext;
        private volatile CountDownLatch gate = new CountDownLatch(0);

        @Override
        public void save(Trade trade) {
            attempts.incrementAndGet();
            awaitGate();
            if (rejectNext) {
                rejectNext = false;
                throw new RepositoryException("bad data", null, false);
            }
            if (failuresLeft.get() > 0) {
                failuresLeft.decrementAndGet();
                throw new RepositoryException("database down", null, true);
            }
            saved.add(trade);
        }

        void failNextSaves(int count) {
            failuresLeft.set(count);
        }

        void rejectNextSave() {
            rejectNext = true;
        }

        void hold() {
            gate = new CountDownLatch(1);
        }

        void open() {
            gate.countDown();
        }

        List<Trade> saved() {
            return List.copyOf(saved);
        }

        int attempts() {
            return attempts.get();
        }

        private void awaitGate() {
            try {
                gate.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RepositoryException("interrupted", e, true);
            }
        }
    }
}
