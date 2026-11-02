package com.liga.calendar;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ProductCatalogTest {

    @Test
    void forSectionReturnsMerchandisingProducts() {
        assertThat(ProductCatalog.forSection("merchandising")).isNotEmpty();
    }

    @Test
    void forSectionReturnsEmptyListForUnknownSection() {
        assertThat(ProductCatalog.forSection("no-existe")).isEmpty();
    }

    @Test
    void findByIdLocatesProductAcrossSections() {
        Optional<Product> product = ProductCatalog.findById("remera-liga");

        assertThat(product).isPresent();
        assertThat(product.get().name()).isEqualTo("Remera Liga Pauper Montevideo");
    }

    @Test
    void findByIdReturnsEmptyForUnknownId() {
        assertThat(ProductCatalog.findById("no-existe")).isEmpty();
    }
}
