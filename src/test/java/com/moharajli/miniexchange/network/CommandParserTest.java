package com.moharajli.miniexchange.network;

import com.moharajli.miniexchange.domain.Side;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommandParserTest {

    private CommandParser parser;

    @BeforeEach
    void setUp() {
        parser = new CommandParser();
    }

    @Test
    void parsesBuy() {
        assertEquals(new PlaceOrder(Side.BUY, 100, 15000), parser.parse("BUY 100 15000"));
    }

    @Test
    void parsesSell() {
        assertEquals(new PlaceOrder(Side.SELL, 50, 15100), parser.parse("SELL 50 15100"));
    }

    @Test
    void parsesCancel() {
        assertEquals(new CancelOrder(7), parser.parse("CANCEL 7"));
    }

    @Test
    void parsesBook() {
        assertEquals(new ShowBook(), parser.parse("BOOK"));
    }

    @Test
    void toleratesExtraSpacesAndLowercase() {
        assertEquals(new PlaceOrder(Side.BUY, 100, 15000), parser.parse("  buy   100  15000 "));
    }

    @Test
    void rejectsEmptyLine() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(""));
    }

    @Test
    void rejectsBlankLine() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("   "));
    }

    @Test
    void rejectsUnknownCommand() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("HELLO"));
    }

    @Test
    void rejectsWrongPartCount() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("BUY 100"));
    }

    @Test
    void rejectsNonNumber() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("BUY abc 15000"));
    }

    @Test
    void rejectsNonPositiveQuantity() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("BUY 0 15000"));
    }
}
