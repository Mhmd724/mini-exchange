package com.moharajli.miniexchange.common;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdGeneratorTest {

    @Test
    void firstIdIsOne() {
        assertEquals(1, new IdGenerator().next());
    }

    @Test
    void idsIncreaseByOne() {
        IdGenerator ids = new IdGenerator();
        ids.next();

        assertEquals(2, ids.next());
    }

    @Test
    void idsAreUniqueAcrossThreads() throws InterruptedException {
        IdGenerator ids = new IdGenerator();
        Set<Long> seen = ConcurrentHashMap.newKeySet();
        ExecutorService pool = Executors.newFixedThreadPool(8);

        for (int i = 0; i < 8000; i++) {
            pool.submit(() -> seen.add(ids.next()));
        }
        pool.shutdown();

        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(8000, seen.size());
    }
}
