package com.moharajli.miniexchange.domain;

import com.moharajli.miniexchange.common.Validation;
import java.time.Instant;

public record Trade(long buyOrderId, long sellOrderId, long quantity, long price, Instant executedAt) {
    
    public Trade {
        Validation.requirePositive(quantity, "Quantity");
        Validation.requirePositive(price, "Price");
        Validation.requireTrue(buyOrderId != sellOrderId, "An order cannot trade with itself");
        Validation.requireNotNull(executedAt, "Execution time");
    }
}
