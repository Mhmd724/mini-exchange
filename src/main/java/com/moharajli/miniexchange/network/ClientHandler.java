package com.moharajli.miniexchange.network;

import com.moharajli.miniexchange.common.Validation;

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
    private final long ownerId;
    private final CommandParser parser;
    private final CommandExecutor executor;

    public ClientHandler(Socket socket, long ownerId, CommandParser parser, CommandExecutor executor) {
        Validation.requireNotNull(socket, "Socket");
        Validation.requirePositive(ownerId, "Owner id");
        Validation.requireNotNull(parser, "Command parser");
        Validation.requireNotNull(executor, "Command executor");
        this.socket = socket;
        this.ownerId = ownerId;
        this.parser = parser;
        this.executor = executor;
    }

    @Override
    public void run() {
        try (socket;
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter out = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = in.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                for (String reply : handle(line)) {
                    out.print(reply + "\n");
                }
                out.flush();
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
