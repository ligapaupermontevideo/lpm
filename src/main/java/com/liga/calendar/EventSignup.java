package com.liga.calendar;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A confirmed sign-up to play on one of the league's official dates.
 * {@code cedula} is always the normalized (digits-only, zero-padded) form
 * from {@link CedulaValidator#normalize}.
 */
public record EventSignup(String cedula, String fullName, LocalDate date, String eventTitle, LocalDateTime createdAt) {
}
