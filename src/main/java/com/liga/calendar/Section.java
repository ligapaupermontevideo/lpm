package com.liga.calendar;

import java.util.List;

/**
 * A section of the site linked from the home page.
 * <p>
 * To add a new section, add an entry to {@link #ALL} and point its
 * {@code url} at the new controller's mapping — no other wiring is needed
 * for it to show up on the home page.
 */
public record Section(String name, String description, String url, String icon) {

    public static final List<Section> ALL = List.of(
            new Section("Calendario", "Eventos y torneos del mes", "/calendario", "📅")
    );
}
