package com.liga.calendar;

import java.util.List;

/**
 * A section of the site linked from the home page.
 * <p>
 * To add a new section, add an entry to {@link #ALL} and point its
 * {@code url} at the new controller's mapping — no other wiring is needed
 * for it to show up on the home page.
 * <p>
 * {@code icon} is a key into the inline SVG icons defined in
 * {@code templates/fragments/icons.html} (e.g. {@code "calendar"}), not an
 * emoji — emoji are drawn by each OS's own font, so the same character can
 * look completely different on a phone versus a desktop browser. Add a new
 * {@code th:case} there for any new icon key used here.
 */
public record Section(String name, String description, String url, String icon) {

    public static final List<Section> ALL = List.of(
            new Section("Calendario", "Fechas oficiales y más", "/calendario", "calendar"),
            new Section("Reserva de decks", "Mazos disponibles para las fechas oficiales de la liga", "/prestamo-mazos", "cards"),
            new Section("Accesorios", "Fundas, playmats y accesorios para tus partidas", "/accesorios", "bag"),
            new Section("Tokens", "Tokens y fichas para tus mazos", "/tokens", "coin"),
            new Section("Merchandising", "Remeras, stickers y otros productos de la liga", "/merchandising", "shirt")
    );
}
