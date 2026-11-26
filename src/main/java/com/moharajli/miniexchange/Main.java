package com.moharajli.miniexchange;

import com.moharajli.miniexchange.common.IdGenerator;
import com.moharajli.miniexchange.domain.OrderBook;
import com.moharajli.miniexchange.engine.MatchingEngine;
import com.moharajli.miniexchange.network.CommandExecutor;
import com.moharajli.miniexchange.network.CommandParser;
import com.moharajli.miniexchange.network.ExchangeServer;
import com.moharajli.miniexchange.network.SessionRegistry;

import java.io.IOException;
import java.time.Clock;

public final class Main {

    private static final int PORT = 9000;

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        SessionRegistry sessions = new SessionRegistry();
        MatchingEngine engine = new MatchingEngine(new OrderBook(), Clock.systemUTC());
        CommandExecutor executor = new CommandExecutor(engine, new IdGenerator(), sessions);

        try (ExchangeServer server = new ExchangeServer(PORT, new CommandParser(), executor, sessions)) {
            System.out.println("MiniExchange listening on port " + server.port());
            server.serve();
        }
    }
}
