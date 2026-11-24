package com.moharajli.miniexchange.network;

import com.moharajli.miniexchange.common.Validation;

public record CancelOrder(long orderId) implements Command {

    public CancelOrder {
        Validation.requirePositive(orderId, "Order id");
    }
}
