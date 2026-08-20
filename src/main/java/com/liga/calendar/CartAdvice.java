package com.liga.calendar;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Makes the cart's item count available to every page (for the header's
 * cart badge) without every controller having to add it explicitly.
 */
@ControllerAdvice
public class CartAdvice {

    private final Cart cart;

    public CartAdvice(Cart cart) {
        this.cart = cart;
    }

    @ModelAttribute("cartItemCount")
    public int cartItemCount() {
        return cart.totalItems();
    }
}
