package com.moharajli.miniexchange.engine;

import com.moharajli.miniexchange.domain.Trade;
import com.moharajli.miniexchange.domain.OrderBook;
import com.moharajli.miniexchange.domain.Order;
import com.moharajli.miniexchange.domain.Side;
import com.moharajli.miniexchange.common.Validation;

import java.util.Optional;
import java.util.List;
import java.time.Clock;


public class MatchingEngine {
    private final OrderBook book;
    private final Clock clock;

    public MatchingEngine(OrderBook book, Clock clock) {
        Validation.requireNotNull(book, "Order Book");
        Validation.requireNotNull(clock, "Clock");
        this.book = book;
        this.clock = clock;
    }

    public List<Trade> submit(Order incoming) {
        Optional<Order> resting = bestOpposite(incoming.side());

        if (resting.isPresent() && crosses(incoming, resting.get()))
        {
            Trade trade = createTrade(incoming, resting.get());
            removeBestOpposite(incoming.side());
            return (List.of(trade));
        }

        book.add(incoming);
        return (List.of());
    }

    private Optional<Order> bestOpposite(Side side) {
        return (switch (side) {
            case BUY -> book.bestAsk();
            case SELL -> book.bestBid();
        });
    }

    private boolean crosses(Order incoming, Order resting) {
        return (switch (incoming.side()) {
            case BUY -> incoming.price() >= resting.price();
            case SELL -> incoming.price() <= resting.price();
        });
    }

    private void removeBestOpposite(Side side) {
        switch (side) {
            case BUY -> book.removeBestAsk();
            case SELL -> book.removeBestBid();
        }
    }

    private Trade createTrade(Order incoming, Order resting) {
        Order buyer = incoming.side() == Side.BUY ? incoming : resting;
        Order seller = incoming.side() == Side.SELL ? incoming : resting;

        long quantity = Math.min(incoming.quantity(), resting.quantity());

        return (new Trade(buyer.id(), seller.id(), quantity, resting.price(), clock.instant()));
    }
}
