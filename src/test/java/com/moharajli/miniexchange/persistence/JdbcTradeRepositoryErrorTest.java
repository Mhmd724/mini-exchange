package com.moharajli.miniexchange.persistence;

import com.moharajli.miniexchange.domain.OwnerId;
import com.moharajli.miniexchange.domain.Trade;
import com.moharajli.miniexchange.domain.TradeSide;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcTradeRepositoryErrorTest {

    @Test
    void connectionErrorsAreRetryable() {
        RepositoryException error = saveWithFailingConnection("08001");

        assertTrue(error.retryable());
    }

    @Test
    void badDataErrorsAreNotRetryable() {
        RepositoryException error = saveWithFailingConnection("23514");

        assertFalse(error.retryable());
    }

    private static RepositoryException saveWithFailingConnection(String sqlState) {
        JdbcTradeRepository repository = new JdbcTradeRepository(() -> {
            throw new SQLException("simulated failure", sqlState);
        });
        Trade trade = new Trade(new TradeSide(2, new OwnerId(7)), new TradeSide(1, new OwnerId(9)),
                100, 15000, Instant.EPOCH);

        return assertThrows(RepositoryException.class, () -> repository.save(trade));
    }
}
