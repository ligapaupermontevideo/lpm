package com.liga.calendar;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CartAdviceTest {

    @Test
    void cartItemCountReflectsCartTotal() {
        Cart cart = new Cart();
        cart.add("a");
        cart.add("a");
        CartAdvice advice = new CartAdvice(cart);

        assertThat(advice.cartItemCount()).isEqualTo(2);
    }

    @Test
    void cartItemCountIsZeroForEmptyCart() {
        CartAdvice advice = new CartAdvice(new Cart());

        assertThat(advice.cartItemCount()).isZero();
    }
}
