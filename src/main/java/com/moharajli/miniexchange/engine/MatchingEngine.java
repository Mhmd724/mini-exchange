package com.moharajli.miniexchange.engine;

import com.moharajli.miniexchange.domain.Trade;
import com.moharajli.miniexchange.domain.OrderBook;
import com.moharajli.miniexchange.domain.BookDepth;
import com.moharajli.miniexchange.domain.Order;
import com.moharajli.miniexchange.domain.Side;
import com.moharajli.miniexchange.common.Validation;

import java.util.Optional;
import java.util.ArrayList;
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
        long remaining = incoming.quantity();
        List<Trade> trades = new ArrayList<>();

        while (remaining > 0) {
            Optional<Order> resting = bestOpposite(incoming.side());
            if (resting.isEmpty() || !crosses(incoming, resting.get())) {
                break ;
            }

            long quantity = Math.min(remaining, resting.get().quantity());
            trades.add(createTrade(incoming, resting.get(), quantity));
            fillBestOpposite(incoming.side(), quantity);
            remaining -= quantity;
        }

        if (remaining > 0) {
            book.add(incoming.withQuantity(remaining));      // the leftover rests
        }
        return trades;
    }

    public Optional<Order> cancel(long id) {
        return book.cancel(id);                 // the book owns its data; the engine only routes
    }

    public BookDepth depth() {
        return book.depth();
    }

    private Optional<Order> bestOpposite(Side side) {
        return switch (side) {
            case BUY -> book.bestAsk();
            case SELL -> book.bestBid();
        };
    }

    private boolean crosses(Order incoming, Order resting) {
        return switch (incoming.side()) {
            case BUY -> incoming.price() >= resting.price();
            case SELL -> incoming.price() <= resting.price();
        };
    }

    private void fillBestOpposite(Side side, long quantity) {
        switch (side) {
            case BUY -> book.fillBestAsk(quantity);
            case SELL -> book.fillBestBid(quantity);
        }
    }

    private Trade createTrade(Order incoming, Order resting, long quantity) {
        Order buyer = incoming.side() == Side.BUY ? incoming : resting;
        Order seller = incoming.side() == Side.SELL ? incoming : resting;

        return new Trade(buyer.id(), seller.id(), quantity, resting.price(), clock.instant());
    }
}
