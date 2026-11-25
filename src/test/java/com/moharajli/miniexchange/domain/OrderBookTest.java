package com.moharajli.miniexchange.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderBookTest {

    private OrderBook book;

    @BeforeEach
    void setUp() {
        book = new OrderBook();
    }

    @Test
    void emptyBookHasNoBestBidOrAsk() {
        assertTrue(book.bestBid().isEmpty());
        assertTrue(book.bestAsk().isEmpty());
    }

    @Test
    void bestBidIsHighestPrice() {
        Order low = new Order(1, 1, Side.BUY, 100, 14900);
        Order high = new Order(2, 1, Side.BUY, 100, 15100);
        Order middle = new Order(3, 1, Side.BUY, 100, 15000);

        book.add(low);
        book.add(high);
        book.add(middle);

        assertEquals(high, book.bestBid().orElseThrow());
    }

    @Test
    void bestAskIsLowestPrice() {
        Order low = new Order(1, 1, Side.SELL, 100, 14900);
        Order high = new Order(2, 1, Side.SELL, 100, 15100);
        Order middle = new Order(3, 1, Side.SELL, 100, 15000);

        book.add(high);
        book.add(low);
        book.add(middle);

        assertEquals(low, book.bestAsk().orElseThrow());
    }

    @Test
    void samePriceEarliestFirst() {
        Order first = new Order(1, 1, Side.BUY, 100, 15000);
        Order second = new Order(2, 1, Side.BUY, 100, 15000);

        book.add(first);
        book.add(second);

        assertEquals(first, book.bestBid().orElseThrow());
    }

    @Test
    void ordersGoToTheirOwnSide() {
        Order order = new Order(1, 1, Side.BUY, 100, 15000);

        book.add(order);

        assertTrue(book.bestAsk().isEmpty());
    }

    @Test
    void fullFillOfBestBidExposesNextBest() {
        Order lower = new Order(1, 1, Side.BUY, 100, 15000);
        Order higher = new Order(2, 1, Side.BUY, 100, 15100);

        book.add(lower);
        book.add(higher);

        assertEquals(higher, book.bestBid().orElseThrow());     //  guard
        book.fillBestBid(100);

        assertEquals(lower, book.bestBid().orElseThrow());      //  result
    }

    @Test
    void fullFillExposesNextPriceLevel() {
        Order cheaper = new Order(1, 1, Side.SELL, 100, 15000);
        Order pricier = new Order(2, 1, Side.SELL, 100, 15100);

        book.add(pricier);
        book.add(cheaper);

        assertEquals(cheaper, book.bestAsk().orElseThrow());    //  guard
        book.fillBestAsk(100);

        assertEquals(pricier, book.bestAsk().orElseThrow());    //  result
    }

    @Test
    void fullFillKeepsTimePriority() {
        Order first = new Order(1, 1, Side.SELL, 100, 15000);
        Order second = new Order(2, 1, Side.SELL, 100, 15000);

        book.add(first);
        book.add(second);

        assertEquals(first, book.bestAsk().orElseThrow());      //  guard
        book.fillBestAsk(100);

        assertEquals(second, book.bestAsk().orElseThrow());     //  result
    }

    @Test
    void fullFillOfOnlyOrderEmptiesTheSide() {
        Order sell = new Order(1, 1, Side.SELL, 100, 15000);
        Order buy = new Order(2, 1, Side.BUY, 100, 15000);

        book.add(sell);
        book.add(buy);

        book.fillBestAsk(100);
        book.fillBestBid(100);

        assertTrue(book.bestAsk().isEmpty());
        assertTrue(book.bestBid().isEmpty());
    }

    @Test
    void fillOnEitherEmptySideFails() {
        assertThrows(IllegalStateException.class, () -> book.fillBestBid(10));
        assertThrows(IllegalStateException.class, () -> book.fillBestAsk(10));
    }

    @Test
    void partialFillReducesBestOrder() {
        Order buy = new Order(1, 1, Side.BUY, 100, 15000);

        book.add(buy);

        book.fillBestBid(60);
        assertEquals(new Order(1, 1, Side.BUY, 40, 15000), book.bestBid().orElseThrow());
    }

    @Test
    void partialFillKeepsTimePriority() {
        Order first = new Order(1, 1, Side.BUY, 100, 15000);
        Order second = new Order(2, 1, Side.BUY, 100, 15000);

        book.add(first);
        book.add(second);

        book.fillBestBid(60);
        assertEquals(new Order(1, 1, Side.BUY, 40, 15000), book.bestBid().orElseThrow());
    }

    @Test
    void fillMoreThanBestFails() {
        Order buy = new Order(1, 1, Side.BUY, 100, 15000);

        book.add(buy);

        assertThrows(IllegalArgumentException.class, () -> book.fillBestBid(150));
        assertEquals(buy, book.bestBid().orElseThrow());
    }

        @Test
    void cancelRemovesRestingOrder() {
        Order buy = new Order(1, 1, Side.BUY, 100, 15000);
        book.add(buy);

        Optional<Order> cancelled = book.cancel(1, 1);

        assertEquals(Optional.of(buy), cancelled);
        assertTrue(book.bestBid().isEmpty());
    }

    @Test
    void cancelUnknownIdReturnsEmpty() {
        assertTrue(book.cancel(99, 1).isEmpty());
    }

    @Test
    void cancelMiddleOrderKeepsQueueOrder() {
        Order first = new Order(1, 1, Side.BUY, 100, 15000);
        Order second = new Order(2, 1, Side.BUY, 100, 15000);
        Order third = new Order(3, 1, Side.BUY, 100, 15000);
        book.add(first);
        book.add(second);
        book.add(third);

        book.cancel(2, 1);

        assertEquals(first, book.bestBid().orElseThrow());      //  #1 still first
        book.fillBestBid(100);
        assertEquals(third, book.bestBid().orElseThrow());      //  #2 is gone, #3 is next
    }

    @Test
    void cancelLastOrderAtPriceRemovesLevel() {
        Order cheaper = new Order(1, 1, Side.SELL, 100, 15000);
        Order pricier = new Order(2, 1, Side.SELL, 100, 15100);
        book.add(cheaper);
        book.add(pricier);

        book.cancel(1, 1);

        assertEquals(pricier, book.bestAsk().orElseThrow());
    }

    @Test
    void cancelAfterPartialFillReturnsRemaining() {
        book.add(new Order(1, 1, Side.BUY, 100, 15000));
        book.fillBestBid(40);

        Optional<Order> cancelled = book.cancel(1, 1);

        assertEquals(Optional.of(new Order(1, 1, Side.BUY, 60, 15000)), cancelled);
        assertTrue(book.bestBid().isEmpty());
    }

    @Test
    void cancelAfterFullFillReturnsEmpty() {
        book.add(new Order(1, 1, Side.BUY, 100, 15000));
        book.fillBestBid(100);

        assertTrue(book.cancel(1, 1).isEmpty());
    }

    @Test
    void cancelTwiceReturnsEmptySecondTime() {
        book.add(new Order(1, 1, Side.BUY, 100, 15000));

        book.cancel(1, 1);

        assertTrue(book.cancel(1, 1).isEmpty());
    }

    @Test
    void depthOfEmptyBookIsEmpty() {
        assertEquals(new BookDepth(List.of(), List.of()), book.depth());
    }

    @Test
    void depthSumsQuantitiesPerLevel() {
        book.add(new Order(1, 1, Side.BUY, 100, 15000));
        book.add(new Order(2, 1, Side.BUY, 50, 15000));

        assertEquals(List.of(new PriceLevel(15000, 150)), book.depth().bids());
    }

    @Test
    void depthListsLevelsBestFirst() {
        book.add(new Order(1, 1, Side.BUY, 80, 14900));
        book.add(new Order(2, 1, Side.BUY, 100, 15000));
        book.add(new Order(3, 1, Side.SELL, 30, 15200));
        book.add(new Order(4, 1, Side.SELL, 50, 15100));

        BookDepth depth = book.depth();

        assertEquals(List.of(new PriceLevel(15000, 100), new PriceLevel(14900, 80)), depth.bids());
        assertEquals(List.of(new PriceLevel(15100, 50), new PriceLevel(15200, 30)), depth.asks());
    }

    @Test
    void cancelByOtherOwnerKeepsOrder() {
        Order order = new Order(1, 1, Side.BUY, 100, 15000);
        book.add(order);

        assertTrue(book.cancel(1, 2).isEmpty());
        assertEquals(order, book.bestBid().orElseThrow());
    }
}
