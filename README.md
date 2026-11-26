# MiniExchange

![CI](https://github.com/Mhmd724/mini-exchange/actions/workflows/ci.yml/badge.svg)

A multithreaded **order-matching engine** in Java — a small stock exchange.
Clients connect over TCP and send BUY / SELL / CANCEL orders; the engine matches them by **price-time priority**.
Trades will be stored in PostgreSQL (in progress).

> 🚧 **Work in progress** — built day by day. See the roadmap below.

## Features
- **Limit orders** with price-time priority: best price first, then first come first served
- **Partial fills** and **multi-level sweeps**; trades execute at the resting order's price
- **Cancel** — only the client that placed an order can cancel it
- **Full order book depth** (`BOOK`), like a trader's screen
- **TCP server**, one thread per client, plain-text protocol (testable with `nc`)
- **Trade notifications** to both sides — the resting order's owner is told when it fills
- **Thread-safe engine**, proven by a stress test (8 threads, 40,000 orders, no lost trades)
- **77 automated tests**, run by GitHub Actions on every push

## Tech stack
- Java 21
- Maven
- JUnit 5
- PostgreSQL (via JDBC) — in progress
- Bash
- GitHub Actions (CI)

No frameworks (no Spring) — the concurrency and networking code stay visible on purpose.

## Run it
Requirements: JDK 21 and Maven.

```bash
mvn -q compile
java -cp target/classes com.moharajli.miniexchange.Main    # listens on port 9000
```
In other terminals, connect as traders:
```bash
nc localhost 9000
```

## Protocol
One command per line.

| Command | Reply |
|---|---|
| `BUY <qty> <price>` | `ACCEPTED <id>`, then one `TRADE` line per fill |
| `SELL <qty> <price>` | same as BUY |
| `CANCEL <id>` | `CANCELLED <id>` or `UNKNOWN <id>` |
| `BOOK` | `BOOK`, `ASK qty@price`…, `BID qty@price`…, `END` |
| anything invalid | `ERROR <reason>` |

A trade is reported as `TRADE <buyOrderId> <sellOrderId> <qty> <price>`, to **both** the buyer and the seller.

## Example session
```
Trader A                          Trader B
--------                          --------
SELL 100 15000
ACCEPTED 1
                                  BUY 60 15000
                                  ACCEPTED 2
                                  TRADE 2 1 60 15000
TRADE 2 1 60 15000     ← A is notified of the fill
                                  BOOK
                                  BOOK
                                  ASK 40@15000
                                  END
                                  CANCEL 1
                                  UNKNOWN 1   ← only A can cancel order 1
CANCEL 1
CANCELLED 1
```

## Design highlights
- **Layered packages** — `domain` (orders, book), `engine` (matching), `network` (protocol, server), `common` (tools). Packages by meaning, not by kind.
- **One lock, short and in memory** — the engine is guarded by a single `ReentrantLock`; socket writes happen *after* it is released, so a slow client never blocks matching.
- **Price-time priority** — each side of the book is a `TreeMap` of price levels, each level a FIFO queue; an id → price index makes cancel fast.
- **Ports and adapters for notifications** — the executor talks to a `Notifier` interface; a `SessionRegistry` delivers to connected clients.
- **Type safety** — `sealed` command types (exhaustive `switch`), value types like `OwnerId` and `TradeSide` so ids can't be swapped.

## Project structure
```
src/main/java/com/moharajli/miniexchange/
├── common/    Validation, IdGenerator
├── domain/    Order, Trade, TradeSide, OwnerId, Side, OrderBook, BookSide, PriceLevel, BookDepth
├── engine/    MatchingEngine
├── network/   CommandParser, CommandExecutor, ClientHandler, ExchangeServer,
│              ClientSession, SessionRegistry, Notifier, Command (+ PlaceOrder, CancelOrder, ShowBook)
└── Main.java  wiring
```

## Build & test
```bash
mvn test
```

## Known limitations (V1)
- The order book lives in memory — it is lost on restart (persistence is in progress).
- A client's identity is its connection: after reconnecting, it can no longer cancel its old orders.
- One instrument only; prices and quantities are whole numbers.

## Roadmap
- [x] Project setup (Maven, JUnit 5, PostgreSQL driver)
- [x] Domain model: Order, Trade, OrderBook
- [x] Matching engine (full and partial fills, multi-level sweeps, cancel)
- [x] TCP server with multiple clients
- [x] Order ownership and trade notifications to both sides
- [x] Thread safety under load (lock + stress test)
- [x] CI with GitHub Actions
- [ ] Persist orders and trades in PostgreSQL
- [ ] Run scripts, logging, full documentation
