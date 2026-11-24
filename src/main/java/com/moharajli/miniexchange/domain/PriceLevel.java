package com.moharajli.miniexchange.domain;

import com.moharajli.miniexchange.common.Validation;

public record PriceLevel(long price, long quantity) {

    public PriceLevel {
        Validation.requirePositive(price, "Price");
        Validation.requirePositive(quantity, "Quantity");
    }
}
