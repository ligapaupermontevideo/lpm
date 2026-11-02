package com.liga.calendar;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A confirmed deck reservation. {@code cedula} is always the normalized
 * (digits-only, zero-padded) form from {@link CedulaValidator#normalize}.
 */
public record DeckReservation(String cedula, String fullName, String deckId, String deckName,
                               LocalDate date, LocalDateTime createdAt) {
}
