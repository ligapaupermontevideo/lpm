package com.liga.calendar;

import java.util.List;

/**
 * A deck available to reserve, sourced live from the league's Moxfield
 * profile (see {@link MoxfieldDeckService}) rather than stored locally —
 * the league keeps its decklists up to date there, not in this codebase.
 * <p>
 * {@code colors} is the deck's mana colors in WUBRG order (e.g.
 * {@code ["U", "B"]} for a Dimir deck), used to show mana symbols next to
 * its name — see {@link DeckCatalog} for where this is sourced from.
 */
public record Deck(String id, String name, String format, String url, List<String> colors) {
}
