package com.moharajli.miniexchange.domain;

import com.moharajli.miniexchange.common.Validation;

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
        Map.Entry<Long, Deque<Order>> level = bestLevel("remove");
        Deque<Order> queue = level.getValue();

        queue.pollFirst();

        if (queue.isEmpty()) {
            levels.remove(level.getKey());
        }
    }

    void fillBest(long quantity) {
        Deque<Order> queue = bestLevel("fill").getValue();
        Order front = queue.peekFirst();

        Validation.requirePositive(quantity, "fill quantity");
        Validation.requireTrue(quantity <= front.quantity(), "fill exceeds order quantity: " + quantity);

        if (quantity == front.quantity()) {
            removeBest();
        } else {
            queue.pollFirst();
            queue.addFirst(front.withQuantity(front.quantity() - quantity));
        }
    }

    private Map.Entry<Long, Deque<Order>> bestLevel(String action) {
        if (levels.isEmpty()) {
            throw new IllegalStateException("no order to " + action);
        }
        return levels.firstEntry();
    }
}