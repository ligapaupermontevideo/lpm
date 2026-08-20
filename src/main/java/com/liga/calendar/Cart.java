package com.liga.calendar;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A shopping cart tied to the visitor's browser session — no login
 * required, and nothing is persisted once the session ends. Spring gives
 * each session its own instance of this bean.
 */
@Component
@SessionScope
public class Cart {

    private final Map<String, Integer> quantities = new LinkedHashMap<>();

    public void add(String productId) {
        quantities.merge(productId, 1, Integer::sum);
    }

    public void remove(String productId) {
        quantities.remove(productId);
    }

    public void setQuantity(String productId, int quantity) {
        if (quantity <= 0) {
            quantities.remove(productId);
        } else {
            quantities.put(productId, quantity);
        }
    }

    public Map<String, Integer> quantities() {
        return quantities;
    }

    public int totalItems() {
        return quantities.values().stream().mapToInt(Integer::intValue).sum();
    }

    public boolean isEmpty() {
        return quantities.isEmpty();
    }

    public void clear() {
        quantities.clear();
    }
}
