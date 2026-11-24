package com.moharajli.miniexchange.domain;

import com.moharajli.miniexchange.common.Validation;

import java.util.List;

public record BookDepth(List<PriceLevel> bids, List<PriceLevel> asks) {

    public BookDepth {
        Validation.requireNotNull(bids, "Bids");
        Validation.requireNotNull(asks, "Asks");
        bids = List.copyOf(bids);
        asks = List.copyOf(asks);
    }
}
