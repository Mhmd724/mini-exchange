package com.moharajli.miniexchange.domain;

import java.util.Deque;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Map;

class BookSide {
    
    private final NavigableMap<Long, Deque<Order>> levels;

    BookSide(Comparator<Long> priceOrder) {
        levels = new TreeMap<>(priceOrder);
    }

    void add(Order order) {
        Deque<Order> queue = levels.computeIfAbsent(order.price(), price -> new ArrayDeque<>());
        queue.addLast(order);
    }

    Optional<Order> best() {
        if (levels.isEmpty()) {
            return (Optional.empty());
        }
        return (Optional.of(levels.firstEntry().getValue().peekFirst()));
    }

    void removeBest() {
        if (levels.isEmpty()) {
            throw new IllegalStateException("no order to remove");
        }

        Map.Entry<Long, Deque<Order>> bestLevel = levels.firstEntry();
        Deque<Order> queue = bestLevel.getValue();

        queue.pollFirst();

        if (queue.isEmpty()) {
            levels.remove(bestLevel.getKey());
        }
    }
}