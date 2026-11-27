package com.moharajli.miniexchange.persistence;

import com.moharajli.miniexchange.domain.OwnerId;
import com.moharajli.miniexchange.domain.Trade;
import com.moharajli.miniexchange.domain.TradeSide;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class JdbcTradeRepositoryTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    private static final Instant EXECUTED_AT = Instant.parse("2026-11-26T10:15:30Z");

    private JdbcTradeRepository repository;

    @BeforeAll
    static void createSchema() throws IOException, SQLException {
        String schema;
        try (InputStream in = JdbcTradeRepositoryTest.class.getResourceAsStream("/db/schema.sql")) {
            schema = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        try (Connection connection = connectToContainer(); Statement statement = connection.createStatement()) {
            statement.execute(schema);
        }
    }

    @BeforeEach
    void setUp() throws SQLException {
        try (Connection connection = connectToContainer(); Statement statement = connection.createStatement()) {
            statement.execute("TRUNCATE trades RESTART IDENTITY");
        }
        repository = new JdbcTradeRepository(JdbcTradeRepositoryTest::connectToContainer);
    }

    @AfterEach
    void tearDown() {
        repository.close();
    }

    @Test
    void savesTrade() throws SQLException {
        Trade trade = new Trade(new TradeSide(2, new OwnerId(7)), new TradeSide(1, new OwnerId(9)),
                100, 15000, EXECUTED_AT);

        repository.save(trade);

        try (Connection connection = connectToContainer();
             Statement statement = connection.createStatement();
             ResultSet row = statement.executeQuery("SELECT * FROM trades")) {
            assertTrue(row.next());
            assertEquals(2, row.getLong("buy_order_id"));
            assertEquals(1, row.getLong("sell_order_id"));
            assertEquals(7, row.getLong("buy_owner_id"));
            assertEquals(9, row.getLong("sell_owner_id"));
            assertEquals(100, row.getLong("quantity"));
            assertEquals(15000, row.getLong("price"));
            assertEquals(EXECUTED_AT, row.getObject("executed_at", OffsetDateTime.class).toInstant());
            assertFalse(row.next());
        }
    }

    @Test
    void databaseRejectsNonPositiveQuantity() throws SQLException {
        try (Connection connection = connectToContainer(); Statement statement = connection.createStatement()) {
            assertThrows(SQLException.class, () -> statement.executeUpdate("""
                    INSERT INTO trades (buy_order_id, sell_order_id, buy_owner_id, sell_owner_id,
                                        quantity, price, executed_at)
                    VALUES (2, 1, 7, 9, 0, 15000, now())
                    """));
        }
    }

    private static Connection connectToContainer() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
