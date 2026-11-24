package com.moharajli.miniexchange.network;

import com.moharajli.miniexchange.common.IdGenerator;
import com.moharajli.miniexchange.common.Validation;
import com.moharajli.miniexchange.domain.BookDepth;
import com.moharajli.miniexchange.domain.Order;
import com.moharajli.miniexchange.domain.PriceLevel;
import com.moharajli.miniexchange.domain.Trade;
import com.moharajli.miniexchange.engine.MatchingEngine;

import java.util.ArrayList;
import java.util.List;

public class CommandExecutor {

    private final MatchingEngine engine;
    private final IdGenerator ids;

    public CommandExecutor(MatchingEngine engine, IdGenerator ids) {
        Validation.requireNotNull(engine, "Matching engine");
        Validation.requireNotNull(ids, "Id generator");
        this.engine = engine;
        this.ids = ids;
    }

    public List<String> execute(Command command) {
        return switch (command) {
            case PlaceOrder place -> place(place);
            case CancelOrder cancel -> cancel(cancel);
            case ShowBook show -> showBook();
        };
    }

    private List<String> place(PlaceOrder command) {
        long id = ids.next();
        List<Trade> trades = engine.submit(new Order(id, command.side(), command.quantity(), command.price()));

        List<String> replies = new ArrayList<>();
        replies.add("ACCEPTED " + id);
        for (Trade trade : trades) {
            replies.add(formatTrade(trade));
        }
        return replies;
    }

    private List<String> cancel(CancelOrder command) {
        String reply = engine.cancel(command.orderId())
                .map(order -> "CANCELLED " + order.id())
                .orElse("UNKNOWN " + command.orderId());
        return List.of(reply);
    }

    private List<String> showBook() {
        BookDepth depth = engine.depth();

        List<String> lines = new ArrayList<>();
        lines.add("BOOK");
        for (PriceLevel level : depth.asks().reversed()) {
            lines.add(formatLevel("ASK", level));
        }
        for (PriceLevel level : depth.bids()) {
            lines.add(formatLevel("BID", level));
        }
        lines.add("END");
        return lines;
    }

    private String formatTrade(Trade trade) {
        return "TRADE " + trade.buyOrderId() + " " + trade.sellOrderId()
                + " " + trade.quantity() + " " + trade.price();
    }

    private String formatLevel(String side, PriceLevel level) {
        return side + " " + level.quantity() + "@" + level.price();
    }
}
