package com.moharajli.miniexchange.persistence;

import com.moharajli.miniexchange.domain.Trade;

import java.util.List;

@FunctionalInterface
public interface TradeRecorder {

    void record(List<Trade> trades);
}
