package com.moharajli.miniexchange.engine;

import com.moharajli.miniexchange.domain.OrderBook;
import com.moharajli.miniexchange.domain.Side;
import com.moharajli.miniexchange.domain.Order;
import com.moharajli.miniexchange.domain.Trade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchingEngineTest {

    private static final Instant NOW = Instant.EPOCH;

    private OrderBook book;
    private MatchingEngine engine;

    @BeforeEach
    void setUp() {
        book = new OrderBook();
        engine = new MatchingEngine(book, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void orderRestsWhenBookIsEmpty() {
        Order order = new Order(1, Side.BUY, 100, 15000);

        List<Trade> trades = engine.submit(order);

        assertTrue(trades.isEmpty());
        assertEquals(order, book.bestBid().orElseThrow());
    }

    @Test
    void buyMatchesRestingSell() {
        Order sell = new Order(1, Side.SELL, 100, 15000);
        Order buy = new Order(2, Side.BUY, 100, 15000);

        engine.submit(sell);

        assertEquals(sell, book.bestAsk().orElseThrow());   //  guard

        List<Trade> trades = engine.submit(buy);

        assertEquals(List.of(new Trade(2, 1, 100, 15000, NOW)), trades);

        assertTrue(book.bestAsk().isEmpty());
        assertTrue(book.bestBid().isEmpty());
    }

    @Test
    void sellMatchesRestingBuy() {
        Order sell = new Order(1, Side.SELL, 100, 15000);
        Order buy = new Order(2, Side.BUY, 100, 15000);

        engine.submit(buy);

        assertEquals(buy, book.bestBid().orElseThrow());   //  guard

        List<Trade> trades = engine.submit(sell);

        assertEquals(List.of(new Trade(2, 1, 100, 15000, NOW)), trades);

        assertTrue(book.bestAsk().isEmpty());
        assertTrue(book.bestBid().isEmpty());
    }

    @Test
    void tradeUsesRestingPrice() {
        Order sell = new Order(1, Side.SELL, 100, 15000);
        Order buy = new Order(2, Side.BUY, 100, 15100);

        engine.submit(sell);

        assertEquals(sell, book.bestAsk().orElseThrow());   //  guard

        List<Trade> trades = engine.submit(buy);

        assertEquals(List.of(new Trade(2, 1, 100, 15000, NOW)), trades);

        assertTrue(book.bestAsk().isEmpty());
        assertTrue(book.bestBid().isEmpty());
    }

    @Test
    void noTradeWhenPricesDoNotCross() {
        Order firstSell = new Order(1, Side.SELL, 100, 15000);
        Order secondSell = new Order(2, Side.SELL, 100, 15100);
        Order buy = new Order(3, Side.BUY, 100, 14900);

        engine.submit(firstSell);
        engine.submit(secondSell);

        List<Trade> trades = engine.submit(buy);

        assertTrue(trades.isEmpty());
    
        assertEquals(firstSell, book.bestAsk().orElseThrow());
        assertEquals(buy, book.bestBid().orElseThrow());
    }
}