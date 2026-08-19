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
            new Section("Calendario", "Eventos y torneos del mes", "/calendario", "📅"),
            new Section("Alquiler de Decks", "Mazos disponibles para alquilar en los eventos", "/alquiler-decks", "🃏"),
            new Section("Accesorios", "Fundas, playmats y accesorios para tus partidas", "/accesorios", "🎒"),
            new Section("Tokens", "Tokens y fichas para tus mazos", "/tokens", "🪙"),
            new Section("Merchandising", "Remeras, stickers y otros productos de la liga", "/merchandising", "👕")
    );
}
