package com.liga.calendar;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Static, in-memory product data, keyed by section slug (the section's
 * {@link Section#url()} without the leading slash).
 * <p>
 * There's no database yet — this is placeholder data to get the cart
 * working end to end. Swap it out for a real repository once there's an
 * actual catalog to manage.
 */
public final class ProductCatalog {

    public static final Map<String, List<Product>> BY_SECTION = Map.of(
            "merchandising", List.of(
                    new Product("remera-liga", "Remera Liga Pauper Montevideo",
                            "Remera de algodon con el logo de la liga", new BigDecimal("900"), "shirt"),
                    new Product("sticker-pack", "Pack de Stickers",
                            "Set de 5 stickers con ilustraciones de la liga", new BigDecimal("150"), "coin"),
                    new Product("buff-liga", "Buff / Cuello Liga",
                            "Buff multifuncional con el logo de la liga", new BigDecimal("450"), "bag")
            )
    );

    private ProductCatalog() {
    }

    public static List<Product> forSection(String sectionSlug) {
        return BY_SECTION.getOrDefault(sectionSlug, List.of());
    }

    public static Optional<Product> findById(String productId) {
        return BY_SECTION.values().stream()
                .flatMap(List::stream)
                .filter(p -> p.id().equals(productId))
                .findFirst();
    }
}
