CREATE TABLE IF NOT EXISTS trades (
    id                  BIGSERIAL       PRIMARY KEY,
    buy_order_id        BIGINT          NOT NULL,
    sell_order_id       BIGINT          NOT NULL,
    buy_owner_id        BIGINT          NOT NULL,
    sell_owner_id       BIGINT          NOT NULL,
    price               BIGINT          NOT NULL,
    quantity            BIGINT          NOT NULL,
    executed_at         TIMESTAMPTZ     NOT NULL,
    CHECK   (buy_order_id <> sell_order_id),
    CHECK   (quantity > 0),
    CHECK   (price > 0)
);
