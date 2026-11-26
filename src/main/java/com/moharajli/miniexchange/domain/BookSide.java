package com.moharajli.miniexchange.domain;

import com.moharajli.miniexchange.common.Validation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

class BookSide {

    private final NavigableMap<Long, Deque<Order>> levels;
    private final Map<Long, Long> priceById = new HashMap<>();

    BookSide(Comparator<Long> priceOrder) {
        levels = new TreeMap<>(priceOrder);
    }

    void add(Order order) {
        Deque<Order> queue = levels.computeIfAbsent(order.price(), price -> new ArrayDeque<>());
        queue.addLast(order);
        priceById.put(order.id(), order.price());
    }

    Optional<Order> best() {
        if (levels.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(levels.firstEntry().getValue().peekFirst());
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

    Optional<Order> cancel(long id, OwnerId ownerId) {
        Long price = priceById.get(id);
        if (price == null) {
            return Optional.empty();
        }

        Deque<Order> queue = levels.get(price);
        Iterator<Order> it = queue.iterator();
        while (it.hasNext()) {
            Order order = it.next();
            if (order.id() == id) {
                if (!order.ownerId().equals(ownerId)) {
                    return Optional.empty();
                }
                it.remove();
                if (queue.isEmpty()) {
                    levels.remove(price);
                }
                priceById.remove(id);
                return Optional.of(order);
            }
        }
        return Optional.empty();
    }

    List<PriceLevel> depth() {
        List<PriceLevel> depth = new ArrayList<>();
        for (Map.Entry<Long, Deque<Order>> level : levels.entrySet()) {
            long total = 0;
            for (Order order : level.getValue()) {
                total += order.quantity();
            }
            depth.add(new PriceLevel(level.getKey(), total));
        }
        return depth;
    }

    private void removeBest() {
        Map.Entry<Long, Deque<Order>> level = bestLevel("remove");
        Deque<Order> queue = level.getValue();

        Order removed = queue.pollFirst();
        priceById.remove(removed.id());

        if (queue.isEmpty()) {
            levels.remove(level.getKey());
        }
    }

    private Map.Entry<Long, Deque<Order>> bestLevel(String action) {
        if (levels.isEmpty()) {
            throw new IllegalStateException("no order to " + action);
        }
        return levels.firstEntry();
    }
}
