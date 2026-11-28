package com.moharajli.miniexchange.persistence;

import com.moharajli.miniexchange.common.Validation;
import com.moharajli.miniexchange.domain.Trade;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class JdbcTradeRepository implements TradeRepository, AutoCloseable {

    private static final String INSERT_TRADE = """
            INSERT INTO trades (buy_order_id, sell_order_id, buy_owner_id, sell_owner_id,
                                quantity, price, executed_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private final ConnectionFactory connections;
    private Connection connection;

    public JdbcTradeRepository(ConnectionFactory connections) {
        Validation.requireNotNull(connections, "Connection factory");
        this.connections = connections;
    }

    @Override
    public void save(Trade trade) {
        Validation.requireNotNull(trade, "Trade");
        try (PreparedStatement statement = connection().prepareStatement(INSERT_TRADE)) {
            statement.setLong(1, trade.buy().orderId());
            statement.setLong(2, trade.sell().orderId());
            statement.setLong(3, trade.buy().owner().value());
            statement.setLong(4, trade.sell().owner().value());
            statement.setLong(5, trade.quantity());
            statement.setLong(6, trade.price());
            statement.setObject(7, OffsetDateTime.ofInstant(trade.executedAt(), ZoneOffset.UTC));
            statement.executeUpdate();
        } catch (SQLException e) {
            boolean retryable = isRetryable(e);
            if (retryable) {
                close();
            }
            throw new RepositoryException("could not save trade: " + e.getMessage(), e, retryable);
        }
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // already broken; nothing more to do
            }
            connection = null;
        }
    }

    private static boolean isRetryable(SQLException e) {
        String state = e.getSQLState();
        boolean badData = state != null && (state.startsWith("22") || state.startsWith("23"));
        return !badData;
    }

    private Connection connection() throws SQLException {
        if (connection == null) {
            connection = connections.open();
        }
        return connection;
    }
}
