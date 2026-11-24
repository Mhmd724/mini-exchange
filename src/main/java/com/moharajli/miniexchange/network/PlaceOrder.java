package com.moharajli.miniexchange.network;

import com.moharajli.miniexchange.common.Validation;
import com.moharajli.miniexchange.domain.Side;

public record PlaceOrder(Side side, long quantity, long price) implements Command {

    public PlaceOrder {
        Validation.requireNotNull(side, "Side");
        Validation.requirePositive(quantity, "Quantity");
        Validation.requirePositive(price, "Price");
    }
}
