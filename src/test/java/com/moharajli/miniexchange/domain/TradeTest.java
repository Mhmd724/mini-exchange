package com.moharajli.miniexchange.domain;

import org.junit.jupiter.api.Test;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


class TradeTest {
    private static final Instant EXECUTED_AT = Instant.EPOCH;

    @Test
    void createsValidTrade() {
        Trade trade = new Trade(new TradeSide(1, new OwnerId(1)), new TradeSide(2, new OwnerId(1)), 100, 15000, EXECUTED_AT);

        assertEquals(1, trade.buy().orderId());
        assertEquals(2, trade.sell().orderId());
        assertEquals(100, trade.quantity());
        assertEquals(15000, trade.price());
        assertEquals(EXECUTED_AT, trade.executedAt());
    }

    @Test
    void rejectsZeroQuantity() {
        assertThrows(IllegalArgumentException.class, () -> new Trade(new TradeSide(1, new OwnerId(1)), new TradeSide(2, new OwnerId(1)), 0, 15000, EXECUTED_AT));
    }

    @Test
    void rejectsNegativePrice() {
        assertThrows(IllegalArgumentException.class, () -> new Trade(new TradeSide(1, new OwnerId(1)), new TradeSide(2, new OwnerId(1)), 100, -1, EXECUTED_AT));
    }

    @Test
    void rejectsSameBuyAndSellOrder() {
        assertThrows(IllegalArgumentException.class, () -> new Trade(new TradeSide(1, new OwnerId(1)), new TradeSide(1, new OwnerId(1)), 100, 15000, EXECUTED_AT));
    }

    @Test
    void rejectsNullExecutionTime() {
        assertThrows(IllegalArgumentException.class, () -> new Trade(new TradeSide(1, new OwnerId(1)), new TradeSide(2, new OwnerId(1)), 100, 15000, null));
    }

    @Test
    void rejectsMissingBuySide() {
        TradeSide sell = new TradeSide(2, new OwnerId(1));
        assertThrows(IllegalArgumentException.class, () -> new Trade(null, sell, 100, 15000, EXECUTED_AT));
    }
}
