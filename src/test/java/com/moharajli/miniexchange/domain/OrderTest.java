package com.moharajli.miniexchange.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


class OrderTest {

    @Test
    void createValidOrder() {
        Order order = new Order(1, Side.BUY, 100, 15000);

        assertEquals(1, order.id());
        assertEquals(Side.BUY, order.side());
        assertEquals(100, order.quantity());
        assertEquals(15000, order.price());
    }

    @Test
    void rejectsZeroQuantity() {
        assertThrows(IllegalArgumentException.class, () -> new Order(1, Side.BUY, 0, 15000));
    }

    @Test
    void rejectsNegativePrice() {
        assertThrows(IllegalArgumentException.class, () -> new Order(1, Side.SELL, 100, -1));
    }

    @Test
    void rejectsNullSide() {
        assertThrows(IllegalArgumentException.class, () -> new Order(1, null, 100, 15000));
    }
}