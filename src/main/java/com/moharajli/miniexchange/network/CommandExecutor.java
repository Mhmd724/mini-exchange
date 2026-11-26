package com.moharajli.miniexchange.network;

import com.moharajli.miniexchange.common.IdGenerator;
import com.moharajli.miniexchange.common.Validation;
import com.moharajli.miniexchange.domain.BookDepth;
import com.moharajli.miniexchange.domain.Order;
import com.moharajli.miniexchange.domain.OwnerId;
import com.moharajli.miniexchange.domain.PriceLevel;
import com.moharajli.miniexchange.domain.Trade;
import com.moharajli.miniexchange.engine.MatchingEngine;

import java.util.ArrayList;
import java.util.List;

public class CommandExecutor {

    private final MatchingEngine engine;
    private final IdGenerator ids;
    private final Notifier notifier;

    public CommandExecutor(MatchingEngine engine, IdGenerator ids, Notifier notifier) {
        Validation.requireNotNull(engine, "Matching engine");
        Validation.requireNotNull(ids, "Id generator");
        Validation.requireNotNull(notifier, "Notifier");
        this.engine = engine;
        this.ids = ids;
        this.notifier = notifier;
    }

    public List<String> execute(Command command, OwnerId ownerId) {
        return switch (command) {
            case PlaceOrder place -> place(place, ownerId);
            case CancelOrder cancel -> cancel(cancel, ownerId);
            case ShowBook show -> showBook();
        };
    }

    private List<String> place(PlaceOrder command, OwnerId ownerId) {
        long id = ids.next();
        List<Trade> trades = engine.submit(new Order(id, ownerId, command.side(), command.quantity(), command.price()));

        List<String> replies = new ArrayList<>();
        replies.add("ACCEPTED " + id);
        for (Trade trade : trades) {
            String line = formatTrade(trade);
            replies.add(line);
            OwnerId counterparty = counterpartyOf(trade, ownerId);
            if (!counterparty.equals(ownerId)) {
                notifier.notifyOwner(counterparty, line);
            }
        }
        return replies;
    }

    private List<String> cancel(CancelOrder command, OwnerId ownerId) {
        String reply = engine.cancel(command.orderId(), ownerId)
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

    private OwnerId counterpartyOf(Trade trade, OwnerId ownerId) {
        return trade.buy().owner().equals(ownerId) ? trade.sell().owner() : trade.buy().owner();
    }

    private String formatTrade(Trade trade) {
        return "TRADE " + trade.buy().orderId() + " " + trade.sell().orderId()
                + " " + trade.quantity() + " " + trade.price();
    }

    private String formatLevel(String side, PriceLevel level) {
        return side + " " + level.quantity() + "@" + level.price();
    }
}
