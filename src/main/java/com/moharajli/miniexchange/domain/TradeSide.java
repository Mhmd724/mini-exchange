package com.moharajli.miniexchange.domain;

import com.moharajli.miniexchange.common.Validation;

public record TradeSide(long orderId, OwnerId owner) {

    public TradeSide {
        Validation.requirePositive(orderId, "Order id");
        Validation.requireNotNull(owner, "Owner");
    }

    public static TradeSide of(Order order) {
        return new TradeSide(order.id(), order.ownerId());
    }
}
