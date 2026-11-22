package com.moharajli.miniexchange.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderBookTest {

    @Test
    void emptyBookHasNoBestBidOrAsk() {
        OrderBook book = new OrderBook();

        assertTrue(book.bestBid().isEmpty());
        assertTrue(book.bestAsk().isEmpty());
    }

    @Test
    void bestBidIsHighestPrice() {
        OrderBook book = new OrderBook();

        Order low = new Order(1, Side.BUY, 100, 14900);
        Order high = new Order(2, Side.BUY, 100, 15100);
        Order middle = new Order(3, Side.BUY, 100, 15000);

        book.add(low);
        book.add(high);
        book.add(middle);

        assertEquals(high, book.bestBid().orElseThrow());
    }

    @Test
    void bestAskIsLowestPrice() {
        OrderBook book = new OrderBook();

        Order low = new Order(1, Side.SELL, 100, 14900);
        Order high = new Order(2, Side.SELL, 100, 15100);
        Order middle = new Order(3, Side.SELL, 100, 15000);

        book.add(high);
        book.add(low);
        book.add(middle);

        assertEquals(low, book.bestAsk().orElseThrow());
    }

    @Test
    void samePriceEarliestFirst() {
        OrderBook book = new OrderBook();

        Order first = new Order(1, Side.BUY, 100, 15000);
        Order second = new Order(2, Side.BUY, 100, 15000);

        book.add(first);
        book.add(second);

        assertEquals(first, book.bestBid().orElseThrow());
    }

    @Test
    void ordersGoToTheirOwnSide() {
        OrderBook book = new OrderBook();

        Order order = new Order(1, Side.BUY, 100, 15000);

        book.add(order);

        assertTrue(book.bestAsk().isEmpty());
    }

    @Test
    void removeBestBidRemovesTheBestOne() {
        OrderBook book = new OrderBook();

        Order lower = new Order(1, Side.BUY, 100, 15000);
        Order higher = new Order(2, Side.BUY, 100, 15100);

        book.add(lower);
        book.add(higher);
        
        assertEquals(higher, book.bestBid().orElseThrow());     //  guard
        book.removeBestBid();

        assertEquals(lower, book.bestBid().orElseThrow());      //  result
    }

    //  "After the only order at the best price is removed, bestAsk() returns the next price's order (and doesn't crash)."
    @Test
    void removingLastOrderAtPriceExposesNextLevel() {
        OrderBook book = new OrderBook();

        Order cheaper = new Order(1, Side.SELL, 100, 15000);
        Order pricier = new Order(2, Side.SELL, 100, 15100);

        book.add(pricier);
        book.add(cheaper);
        
        assertEquals(cheaper, book.bestAsk().orElseThrow());     //  guard
        book.removeBestAsk();

        assertEquals(pricier, book.bestAsk().orElseThrow());      //  result
    }

    @Test
    void removeKeepsTimePriority() {
        OrderBook book = new OrderBook();

        Order first = new Order(1, Side.SELL, 100, 15000);
        Order second = new Order(2, Side.SELL, 100, 15000);

        book.add(first);
        book.add(second);
        
        assertEquals(first, book.bestAsk().orElseThrow());     //  guard
        book.removeBestAsk();

        assertEquals(second, book.bestAsk().orElseThrow());      //  result
    }

    @Test
    void removingTheOnlyOrderEmptiesTheSide() {
        OrderBook book = new OrderBook();

        Order sell = new Order(1, Side.SELL, 100, 15000);
        Order buy = new Order(2, Side.BUY, 100, 15000);

        book.add(sell);
        book.add(buy);
        book.removeBestAsk();
        book.removeBestBid();
        assertTrue(book.bestAsk().isEmpty());
        assertTrue(book.bestBid().isEmpty());
    }

    @Test
    void removingFromEmptySideFails() {
        OrderBook book = new OrderBook();

        assertThrows(IllegalStateException.class, () -> book.removeBestBid());
        assertThrows(IllegalStateException.class, () -> book.removeBestAsk());
    }
}
