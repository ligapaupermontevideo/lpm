package com.liga.calendar;

/**
 * A person who helps run the league, shown as a card on the
 * "Colaboradores" section.
 * <p>
 * {@code photo} is a path under {@code /img/colaboradores/} (e.g.
 * {@code "/img/colaboradores/juan.jpg"}); leave it {@code null} to fall
 * back to an initials avatar instead of a photo. {@code instagramUrl} is
 * optional too (e.g. {@code "https://instagram.com/liga.pauper.mvd"}) —
 * leave it {@code null} to show the card without a social link, since not
 * every collaborator wants to share one.
 */
public record Collaborator(String id, String name, String role, String bio, String photo, String instagramUrl) {

    /** First letter of the first and last name, for the fallback avatar shown when {@link #photo} is null. */
    public String initials() {
        String[] parts = name.trim().split("\\s+");
        String first = parts.length > 0 && !parts[0].isEmpty() ? parts[0].substring(0, 1) : "";
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }
}
