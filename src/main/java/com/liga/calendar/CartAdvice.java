package com.liga.calendar;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Makes the cart's item count and on/off state available to every page (for
 * the header's cart link and badge) without every controller having to add
 * them explicitly.
 */
@ControllerAdvice
public class CartAdvice {

    private final Cart cart;
    private final SectionAccess sectionAccess;

    public CartAdvice(Cart cart, SectionAccess sectionAccess) {
        this.cart = cart;
        this.sectionAccess = sectionAccess;
    }

    @ModelAttribute("cartItemCount")
    public int cartItemCount() {
        return cart.totalItems();
    }

    @ModelAttribute("cartEnabled")
    public boolean cartEnabled() {
        return sectionAccess.isFeatureEnabled("carrito");
    }
}
