package com.moharajli.miniexchange.network;

import com.moharajli.miniexchange.common.Validation;
import com.moharajli.miniexchange.domain.OwnerId;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final OwnerId ownerId;
    private final CommandParser parser;
    private final CommandExecutor executor;
    private final SessionRegistry sessions;

    public ClientHandler(Socket socket, OwnerId ownerId, CommandParser parser,
                         CommandExecutor executor, SessionRegistry sessions) {
        Validation.requireNotNull(socket, "Socket");
        Validation.requireNotNull(ownerId, "Owner id");
        Validation.requireNotNull(parser, "Command parser");
        Validation.requireNotNull(executor, "Command executor");
        Validation.requireNotNull(sessions, "Session registry");
        this.socket = socket;
        this.ownerId = ownerId;
        this.parser = parser;
        this.executor = executor;
        this.sessions = sessions;
    }

    @Override
    public void run() {
        try (socket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            ClientSession session = new ClientSession(out);
            sessions.register(ownerId, session);
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    session.send(handle(line));
                }
            } finally {
                sessions.unregister(ownerId);
            }
        } catch (IOException e) {
            System.err.println("client connection error: " + e.getMessage());
        }
    }

    private List<String> handle(String line) {
        try {
            return executor.execute(parser.parse(line), ownerId);
        } catch (IllegalArgumentException e) {
            return List.of("ERROR " + e.getMessage());
        }
    }
}
