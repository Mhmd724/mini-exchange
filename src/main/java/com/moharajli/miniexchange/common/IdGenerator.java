package com.moharajli.miniexchange.common;

import java.util.concurrent.atomic.AtomicLong;

public final class IdGenerator {

    private final AtomicLong last = new AtomicLong();   // STARTS WITH 0

    public long next() {
        return (last.incrementAndGet());
    }
}
