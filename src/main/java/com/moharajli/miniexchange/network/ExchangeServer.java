package com.moharajli.miniexchange.network;

import com.moharajli.miniexchange.common.IdGenerator;
import com.moharajli.miniexchange.common.Validation;
import com.moharajli.miniexchange.domain.OwnerId;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExchangeServer implements AutoCloseable {

    private final ServerSocket serverSocket;
    private final ExecutorService clients = Executors.newCachedThreadPool();
    private final IdGenerator ownerIds = new IdGenerator();
    private final CommandParser parser;
    private final CommandExecutor executor;
    private final SessionRegistry sessions;

    public ExchangeServer(int port, CommandParser parser, CommandExecutor executor,
                          SessionRegistry sessions) throws IOException {
        Validation.requireNotNull(parser, "Command parser");
        Validation.requireNotNull(executor, "Command executor");
        Validation.requireNotNull(sessions, "Session registry");
        this.parser = parser;
        this.executor = executor;
        this.sessions = sessions;
        this.serverSocket = new ServerSocket(port);
    }

    public int port() {
        return serverSocket.getLocalPort();
    }

    public void serve() {
        while (!serverSocket.isClosed()) {
            try {
                Socket client = serverSocket.accept();
                OwnerId owner = new OwnerId(ownerIds.next());
                clients.submit(new ClientHandler(client, owner, parser, executor, sessions));
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    System.err.println("accept failed: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        serverSocket.close();
        clients.shutdownNow();
    }
}
