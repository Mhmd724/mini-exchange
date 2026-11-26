package com.moharajli.miniexchange.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


class OrderTest {

    @Test
    void createValidOrder() {
        Order order = new Order(1, new OwnerId(1), Side.BUY, 100, 15000);

        assertEquals(1, order.id());
        assertEquals(Side.BUY, order.side());
        assertEquals(100, order.quantity());
        assertEquals(15000, order.price());
    }

    @Test
    void rejectsZeroQuantity() {
        assertThrows(IllegalArgumentException.class, () -> new Order(1, new OwnerId(1), Side.BUY, 0, 15000));
    }

    @Test
    void rejectsNegativePrice() {
        assertThrows(IllegalArgumentException.class, () -> new Order(1, new OwnerId(1), Side.SELL, 100, -1));
    }

    @Test
    void rejectsNullSide() {
        assertThrows(IllegalArgumentException.class, () -> new Order(1, new OwnerId(1), null, 100, 15000));
    }

    @Test
    void withQuantityReturnsCopyWithNewQuantity() {
        Order order = new Order(1, new OwnerId(1), Side.BUY, 100, 15000);

        Order reduced = order.withQuantity(60);

        assertEquals(new Order(1, new OwnerId(1), Side.BUY, 60, 15000), reduced);   // original's copy
        assertEquals(new Order(1, new OwnerId(1), Side.BUY, 100, 15000), order);    // original unchanged
    }

    @Test
    void withQuantityRejectsZero() {
        Order order = new Order(1, new OwnerId(1), Side.BUY, 100, 15000);
        assertThrows(IllegalArgumentException.class, () -> order.withQuantity(0));
    }

    @Test
    void rejectsNonPositiveOwner() {
        assertThrows(IllegalArgumentException.class, () -> new Order(1, new OwnerId(0), Side.BUY, 100, 15000));
    }
}
