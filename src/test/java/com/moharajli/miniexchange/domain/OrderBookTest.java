package com.moharajli.miniexchange.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
