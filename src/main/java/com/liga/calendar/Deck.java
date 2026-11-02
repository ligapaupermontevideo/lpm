package com.liga.calendar;

/**
 * A deck available to reserve, sourced live from the league's Moxfield
 * profile (see {@link MoxfieldDeckService}) rather than stored locally —
 * the league keeps its decklists up to date there, not in this codebase.
 */
public record Deck(String id, String name, String format, String url) {
}
