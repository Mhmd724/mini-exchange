package com.moharajli.miniexchange.domain;

import com.moharajli.miniexchange.common.Validation;

import java.time.Instant;

public record Trade(TradeSide buy, TradeSide sell, long quantity, long price, Instant executedAt) {

    public Trade {
        Validation.requireNotNull(buy, "Buy side");
        Validation.requireNotNull(sell, "Sell side");
        Validation.requirePositive(quantity, "Quantity");
        Validation.requirePositive(price, "Price");
        Validation.requireTrue(buy.orderId() != sell.orderId(), "An order cannot trade with itself");
        Validation.requireNotNull(executedAt, "Execution time");
    }
}
