package com.moharajli.miniexchange.domain;


import com.moharajli.miniexchange.common.Validation;

public record Order(long id, Side side, long quantity, long price) {

    public Order {
        Validation.requireNotNull(side, "Side");
        Validation.requirePositive(quantity, "Quantity");
        Validation.requirePositive(price, "Price");
    }
}
