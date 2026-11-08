package com.liga.calendar;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CartAdviceTest {

    @Test
    void cartItemCountReflectsCartTotal() {
        Cart cart = new Cart();
        cart.add("a");
        cart.add("a");
        CartAdvice advice = new CartAdvice(cart, new SectionAccess(new SectionProperties()));

        assertThat(advice.cartItemCount()).isEqualTo(2);
    }

    @Test
    void cartItemCountIsZeroForEmptyCart() {
        CartAdvice advice = new CartAdvice(new Cart(), new SectionAccess(new SectionProperties()));

        assertThat(advice.cartItemCount()).isZero();
    }

    @Test
    void cartEnabledIsTrueByDefault() {
        CartAdvice advice = new CartAdvice(new Cart(), new SectionAccess(new SectionProperties()));

        assertThat(advice.cartEnabled()).isTrue();
    }

    @Test
    void cartEnabledIsFalseWhenDisabled() {
        SectionProperties properties = new SectionProperties();
        properties.setEnabled(Map.of("carrito", false));
        CartAdvice advice = new CartAdvice(new Cart(), new SectionAccess(properties));

        assertThat(advice.cartEnabled()).isFalse();
    }
}
