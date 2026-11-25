package com.moharajli.miniexchange.network;

import com.moharajli.miniexchange.common.IdGenerator;
import com.moharajli.miniexchange.domain.OrderBook;
import com.moharajli.miniexchange.engine.MatchingEngine;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExchangeServerTest {

	private ExchangeServer server;
	private Thread serverThread;

	@BeforeEach
	void startServer() throws IOException {
		MatchingEngine engine = new MatchingEngine(new OrderBook(), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
		CommandExecutor executor = new CommandExecutor(engine, new IdGenerator());
		server = new ExchangeServer(0, new CommandParser(), executor);
		serverThread = new Thread(server::serve);
		serverThread.start();
	}

	@AfterEach
	void stopServer() throws Exception {
		server.close();
		serverThread.join(2000);
	}

	@Test
	void placesOrderOverTcp() throws IOException {
		try (TestClient client = new TestClient(server.port())) {
			client.send("BUY 100 15000");

			assertEquals("ACCEPTED 1", client.read());
		}
	}

	@Test
	void invalidCommandGetsError() throws IOException {
		try (TestClient client = new TestClient(server.port())) {
			client.send("HELLO");

			assertEquals("ERROR unknown command: HELLO", client.read());
		}
	}

	@Test
	void twoClientsTrade() throws IOException {
		try (TestClient seller = new TestClient(server.port());
				TestClient buyer = new TestClient(server.port())) {
			seller.send("SELL 100 15000");
			assertEquals("ACCEPTED 1", seller.read());

			buyer.send("BUY 100 15000");

			assertEquals("ACCEPTED 2", buyer.read());
			assertEquals("TRADE 2 1 100 15000", buyer.read());
		}
	}

	private static final class TestClient implements AutoCloseable {

		private final Socket socket;
		private final BufferedReader in;
		private final PrintWriter out;

		TestClient(int port) throws IOException {
			socket = new Socket("localhost", port);
			socket.setSoTimeout(2000);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
			out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
		}

		void send(String line) {
			out.print(line + "\n");
			out.flush();
		}

		String read() throws IOException {
			return in.readLine();
		}

		@Override
		public void close() throws IOException {
			socket.close();
		}
	}
}
