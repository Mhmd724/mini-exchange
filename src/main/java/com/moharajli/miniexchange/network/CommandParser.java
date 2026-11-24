package com.moharajli.miniexchange.network;

import com.moharajli.miniexchange.common.Validation;
import com.moharajli.miniexchange.domain.Side;

import java.util.Locale;

public class CommandParser {

    public Command parse(String line) {
        Validation.requireTrue(!line.isBlank(), "empty command");

        String[] tokens = line.trim().split("\\s+");
        String name = tokens[0].toUpperCase(Locale.ROOT);

        return switch (name) {
            case "BUY" -> placeOrder(Side.BUY, tokens);
            case "SELL" -> placeOrder(Side.SELL, tokens);
            case "CANCEL" -> cancelOrder(tokens);
            case "BOOK" -> showBook(tokens);
            default -> throw new IllegalArgumentException("unknown command: " + tokens[0]);
        };
    }

    private Command placeOrder(Side side, String[] tokens) {
        requirePartCount(tokens, 3);
        return new PlaceOrder(side, parseNumber(tokens[1]), parseNumber(tokens[2]));
    }

    private Command cancelOrder(String[] tokens) {
        requirePartCount(tokens, 2);
        return new CancelOrder(parseNumber(tokens[1]));
    }

    private Command showBook(String[] tokens) {
        requirePartCount(tokens, 1);
        return new ShowBook();
    }

    private void requirePartCount(String[] tokens, int expected) {
        Validation.requireTrue(tokens.length == expected,
                "expected " + expected + " parts for " + tokens[0].toUpperCase(Locale.ROOT)
                        + ", got " + tokens.length);
    }

    private long parseNumber(String token) {
        try {
            return Long.parseLong(token);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("not a number: " + token, e);
        }
    }
}
