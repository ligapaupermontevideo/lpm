package com.liga.calendar;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CartTest {

    @Test
    void addIncreasesQuantityForRepeatedProduct() {
        Cart cart = new Cart();

        cart.add("a");
        cart.add("a");
        cart.add("b");

        assertThat(cart.quantities()).containsEntry("a", 2).containsEntry("b", 1);
        assertThat(cart.totalItems()).isEqualTo(3);
    }

    @Test
    void setQuantityUpdatesExistingItem() {
        Cart cart = new Cart();
        cart.add("a");

        cart.setQuantity("a", 5);

        assertThat(cart.quantities()).containsEntry("a", 5);
    }

    @Test
    void setQuantityToZeroOrLessRemovesItem() {
        Cart cart = new Cart();
        cart.add("a");

        cart.setQuantity("a", 0);

        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    void removeDeletesEntry() {
        Cart cart = new Cart();
        cart.add("a");

        cart.remove("a");

        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    void clearEmptiesCart() {
        Cart cart = new Cart();
        cart.add("a");
        cart.add("b");

        cart.clear();

        assertThat(cart.isEmpty()).isTrue();
        assertThat(cart.totalItems()).isZero();
    }
}
