package com.liga.calendar;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Holds every deck reservation made through the site.
 * <p>
 * There's no database yet, same as {@link ProductCatalog} — reservations
 * live only in memory and are lost on restart. That's an acceptable
 * trade-off for now since every confirmed reservation is also sent to the
 * league over WhatsApp, which is the actual record the league acts on.
 */
@Component
public class DeckReservationStore {

    public enum ReserveResult {
        OK,
        PERSON_ALREADY_RESERVED,
        DECK_ALREADY_TAKEN
    }

    private final List<DeckReservation> reservations = new CopyOnWriteArrayList<>();
    private final Object lock = new Object();

    /**
     * Atomically checks both uniqueness rules and records the reservation
     * if they pass: one reservation per person per date, and one
     * reservation per physical deck per date.
     */
    public ReserveResult reserve(DeckReservation reservation) {
        synchronized (lock) {
            for (DeckReservation existing : reservations) {
                if (!existing.date().equals(reservation.date())) {
                    continue;
                }
                if (existing.cedula().equals(reservation.cedula())) {
                    return ReserveResult.PERSON_ALREADY_RESERVED;
                }
                if (existing.deckId().equals(reservation.deckId())) {
                    return ReserveResult.DECK_ALREADY_TAKEN;
                }
            }
            reservations.add(reservation);
            return ReserveResult.OK;
        }
    }

    public List<DeckReservation> all() {
        return reservations.stream()
                .sorted(Comparator.comparing(DeckReservation::date).thenComparing(DeckReservation::createdAt))
                .toList();
    }

    /** Which deck ids are already taken, grouped by the date they're taken for. */
    public Map<LocalDate, Set<String>> reservedDeckIdsByDate() {
        return reservations.stream()
                .collect(Collectors.groupingBy(DeckReservation::date,
                        Collectors.mapping(DeckReservation::deckId, Collectors.toSet())));
    }
}
