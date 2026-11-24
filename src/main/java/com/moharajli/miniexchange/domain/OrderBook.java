package com.moharajli.miniexchange.domain;

import java.util.Comparator;
import java.util.Optional;

public class OrderBook {

    private final BookSide bids = new BookSide(Comparator.reverseOrder());
    private final BookSide asks = new BookSide(Comparator.naturalOrder());

    public void add(Order order) {
        sideFor(order.side()).add(order);
    }

    public Optional<Order> bestBid() {
        return bids.best();
    }

    public Optional<Order> bestAsk() {
        return asks.best();
    }

    public void fillBestBid(long quantity) {
        bids.fillBest(quantity);
    }

    public void fillBestAsk(long quantity) {
        asks.fillBest(quantity);
    }

    public Optional<Order> cancel(long id) {
        return bids.cancel(id).or(() -> asks.cancel(id));
    }

    public BookDepth depth() {
        return new BookDepth(bids.depth(), asks.depth());
    }

    private BookSide sideFor(Side side) {
        return switch (side) {
            case BUY -> bids;
            case SELL -> asks;
        };
    }
}
