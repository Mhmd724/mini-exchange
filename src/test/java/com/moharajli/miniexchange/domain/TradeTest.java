package com.moharajli.miniexchange.domain;

import org.junit.jupiter.api.Test;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


class TradeTest {
    private static final Instant EXECUTED_AT = Instant.EPOCH;

    @Test
    void createsValidTrade() {
        Trade trade = new Trade(1, 2, 100, 15000, EXECUTED_AT);

        assertEquals(1, trade.buyOrderId());
        assertEquals(2, trade.sellOrderId());
        assertEquals(100, trade.quantity());
        assertEquals(15000, trade.price());
        assertEquals(EXECUTED_AT, trade.executedAt());
    }

    @Test
    void rejectsZeroQuantity() {
        assertThrows(IllegalArgumentException.class, () -> new Trade(1, 2, 0, 15000, EXECUTED_AT));
    }

    @Test
    void rejectsNegativePrice() {
        assertThrows(IllegalArgumentException.class, () -> new Trade(1, 2, 100, -1, EXECUTED_AT));
    }

    @Test
    void rejectsSameBuyAndSellOrder() {
        assertThrows(IllegalArgumentException.class, () -> new Trade(1, 1, 100, 15000, EXECUTED_AT));
    }

    @Test
    void rejectsNullExecutionTime() {
        assertThrows(IllegalArgumentException.class, () -> new Trade(1, 2, 100, 15000, null));
    }
}
