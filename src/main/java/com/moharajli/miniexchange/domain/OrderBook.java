package com.moharajli.miniexchange.domain;

import java.util.Comparator;
import java.util.Optional;

public class OrderBook {

    private final BookSide bids = new BookSide(Comparator.reverseOrder());
    private final BookSide asks = new BookSide(Comparator.naturalOrder());

    private BookSide sideFor(Side side) {
        return (switch (side) {
            case BUY -> bids;
            case SELL -> asks;
        });
    }

    public void add(Order order) {
        sideFor(order.side()).add(order);
    }

    public Optional<Order> bestBid() {
        return (bids.best());
    }

    public Optional<Order> bestAsk() {
        return (asks.best());
    }

    public void removeBestBid() {
        bids.removeBest();
    }

    public void removeBestAsk() {
        asks.removeBest();
    }
}
