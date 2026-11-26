package com.moharajli.miniexchange.engine;

import com.moharajli.miniexchange.domain.BookDepth;
import com.moharajli.miniexchange.domain.Order;
import com.moharajli.miniexchange.domain.OrderBook;
import com.moharajli.miniexchange.domain.Side;
import com.moharajli.miniexchange.domain.Trade;
import com.moharajli.miniexchange.domain.OwnerId;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MatchingEngineConcurrencyTest {

    private static final int THREADS = 8;
    private static final int ORDERS_PER_THREAD = 5000;

    @Test
    void concurrentOrdersAllMatchWithoutLoss() throws Exception {
        MatchingEngine engine = new MatchingEngine(new OrderBook(), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        AtomicLong ids = new AtomicLong();
        AtomicLong traded = new AtomicLong();
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        List<Future<?>> tasks = new ArrayList<>();

        for (int t = 0; t < THREADS; t++) {
            Side side = t % 2 == 0 ? Side.BUY : Side.SELL;
            tasks.add(pool.submit(() -> {
                for (int i = 0; i < ORDERS_PER_THREAD; i++) {
                    List<Trade> trades = engine.submit(new Order(ids.incrementAndGet(), new OwnerId(1), side, 1, 15000));
                    for (Trade trade : trades) {
                        traded.addAndGet(trade.quantity());
                    }
                }
            }));
        }
        for (Future<?> task : tasks) {
            task.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertEquals(THREADS / 2 * ORDERS_PER_THREAD, traded.get());
        assertEquals(new BookDepth(List.of(), List.of()), engine.depth());
    }
}
