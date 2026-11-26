package com.moharajli.miniexchange.network;

import com.moharajli.miniexchange.common.IdGenerator;
import com.moharajli.miniexchange.domain.OrderBook;
import com.moharajli.miniexchange.domain.Side;
import com.moharajli.miniexchange.engine.MatchingEngine;
import com.moharajli.miniexchange.domain.OwnerId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandExecutorTest {

    private CommandExecutor executor;

    @BeforeEach
    void setUp() {
        MatchingEngine engine = new MatchingEngine(new OrderBook(), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        executor = new CommandExecutor(engine, new IdGenerator());
    }

    @Test
    void placeIsAcceptedWithNewId() {
        assertEquals(List.of("ACCEPTED 1"), executor.execute(new PlaceOrder(Side.BUY, 100, 15000), new OwnerId(1)));
    }

    @Test
    void placeReportsTrades() {
        executor.execute(new PlaceOrder(Side.SELL, 100, 15000), new OwnerId(1));

        List<String> replies = executor.execute(new PlaceOrder(Side.BUY, 100, 15000), new OwnerId(1));

        assertEquals(List.of("ACCEPTED 2", "TRADE 2 1 100 15000"), replies);
    }

    @Test
    void cancelKnownOrder() {
        executor.execute(new PlaceOrder(Side.BUY, 100, 15000), new OwnerId(1));

        assertEquals(List.of("CANCELLED 1"), executor.execute(new CancelOrder(1), new OwnerId(1)));
    }

    @Test
    void cancelUnknownOrder() {
        assertEquals(List.of("UNKNOWN 99"), executor.execute(new CancelOrder(99), new OwnerId(1)));
    }

    @Test
    void showEmptyBook() {
        assertEquals(List.of("BOOK", "END"), executor.execute(new ShowBook(), new OwnerId(1)));
    }

    @Test
    void showBookAsLadder() {
        executor.execute(new PlaceOrder(Side.BUY, 100, 15000), new OwnerId(1));
        executor.execute(new PlaceOrder(Side.BUY, 50, 15000), new OwnerId(1));
        executor.execute(new PlaceOrder(Side.BUY, 80, 14900), new OwnerId(1));
        executor.execute(new PlaceOrder(Side.SELL, 50, 15100), new OwnerId(1));
        executor.execute(new PlaceOrder(Side.SELL, 30, 15200), new OwnerId(1));

        assertEquals(List.of(
                "BOOK",
                "ASK 30@15200",
                "ASK 50@15100",
                "BID 150@15000",
                "BID 80@14900",
                "END"), executor.execute(new ShowBook(), new OwnerId(1)));
    }

    @Test
    void cancelByOtherOwnerIsUnknown() {
        executor.execute(new PlaceOrder(Side.BUY, 100, 15000), new OwnerId(1));

        assertEquals(List.of("UNKNOWN 1"), executor.execute(new CancelOrder(1), new OwnerId(2)));
    }
}
