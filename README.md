# MiniExchange

A multithreaded **order-matching engine** in Java — a small stock exchange.
Clients connect over TCP and send BUY / SELL / CANCEL orders; the engine matches them by **price-time priority** and stores trades in PostgreSQL.

> 🚧 **Work in progress** — built day by day. See the roadmap below.

## Tech stack
- Java 21
- Maven
- JUnit 5
- PostgreSQL (via JDBC)
- Bash

No frameworks (no Spring) — the concurrency and database code stay visible on purpose.

## Build & test
Requirements: JDK 21 and Maven.

```bash
mvn test
```

## Roadmap
- [x] Project setup (Maven, JUnit 5, PostgreSQL driver)
- [ ] Domain model: Order, Trade, OrderBook
- [ ] Matching engine (full and partial fills, cancel)
- [ ] TCP server with multiple clients
- [ ] Thread safety under load
- [ ] Persist orders and trades in PostgreSQL
- [ ] Run scripts, logging, full documentation
