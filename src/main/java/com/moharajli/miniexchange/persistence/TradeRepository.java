package com.moharajli.miniexchange.persistence;

import com.moharajli.miniexchange.domain.Trade;

public interface TradeRepository {

    void save(Trade trade);
}
