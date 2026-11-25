package com.moharajli.miniexchange.network;

import com.moharajli.miniexchange.common.IdGenerator;
import com.moharajli.miniexchange.common.Validation;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExchangeServer implements AutoCloseable {

    private final ServerSocket serverSocket;
    private final ExecutorService clients = Executors.newCachedThreadPool();
    private final IdGenerator sessions = new IdGenerator();
    private final CommandParser parser;
    private final CommandExecutor executor;

    public ExchangeServer(int port, CommandParser parser, CommandExecutor executor) throws IOException {
        Validation.requireNotNull(parser, "Command parser");
        Validation.requireNotNull(executor, "Command executor");
        this.parser = parser;
        this.executor = executor;
        this.serverSocket = new ServerSocket(port);
    }

    public int port() {
        return serverSocket.getLocalPort();
    }

    public void serve() {
        while (!serverSocket.isClosed()) {
            try {
                Socket client = serverSocket.accept();
                clients.submit(new ClientHandler(client, sessions.next(), parser, executor));
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
