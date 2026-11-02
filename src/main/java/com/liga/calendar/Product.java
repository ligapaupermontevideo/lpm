package com.liga.calendar;

import java.math.BigDecimal;

/**
 * A purchasable item shown on a section's product page.
 * <p>
 * {@code icon} is a key into {@code templates/fragments/icons.html}, same
 * as {@link Section#icon()}.
 */
public record Product(String id, String name, String description, BigDecimal price, String icon) {
}
