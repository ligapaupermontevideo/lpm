package com.liga.calendar;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DeckReservationStoreTest {

    private final DeckReservationStore store = new DeckReservationStore();
    private final LocalDate date = LocalDate.of(2026, 11, 14);
    private final LocalDate otherDate = LocalDate.of(2026, 11, 21);

    private DeckReservation reservation(String cedula, String deckId, LocalDate date) {
        return new DeckReservation(cedula, "Juan Perez", deckId, "Mono Red", date, LocalDateTime.now());
    }

    @Test
    void firstReservationForAPersonAndDeckSucceeds() {
        assertThat(store.reserve(reservation("12345672", "deck-1", date)))
                .isEqualTo(DeckReservationStore.ReserveResult.OK);
        assertThat(store.all()).hasSize(1);
    }

    @Test
    void samePersonCannotReserveTwiceOnTheSameDate() {
        store.reserve(reservation("12345672", "deck-1", date));

        DeckReservationStore.ReserveResult result = store.reserve(reservation("12345672", "deck-2", date));

        assertThat(result).isEqualTo(DeckReservationStore.ReserveResult.PERSON_ALREADY_RESERVED);
        assertThat(store.all()).hasSize(1);
    }

    @Test
    void samePersonCanReserveAgainOnADifferentDate() {
        store.reserve(reservation("12345672", "deck-1", date));

        DeckReservationStore.ReserveResult result = store.reserve(reservation("12345672", "deck-2", otherDate));

        assertThat(result).isEqualTo(DeckReservationStore.ReserveResult.OK);
        assertThat(store.all()).hasSize(2);
    }

    @Test
    void sameDeckCannotBeReservedTwiceOnTheSameDate() {
        store.reserve(reservation("12345672", "deck-1", date));

        DeckReservationStore.ReserveResult result = store.reserve(reservation("41234563", "deck-1", date));

        assertThat(result).isEqualTo(DeckReservationStore.ReserveResult.DECK_ALREADY_TAKEN);
        assertThat(store.all()).hasSize(1);
    }

    @Test
    void sameDeckCanBeReservedAgainOnADifferentDate() {
        store.reserve(reservation("12345672", "deck-1", date));

        DeckReservationStore.ReserveResult result = store.reserve(reservation("41234563", "deck-1", otherDate));

        assertThat(result).isEqualTo(DeckReservationStore.ReserveResult.OK);
        assertThat(store.all()).hasSize(2);
    }

    @Test
    void reservedDeckIdsByDateGroupsByDate() {
        store.reserve(reservation("12345672", "deck-1", date));
        store.reserve(reservation("41234563", "deck-2", date));
        store.reserve(reservation("33534165", "deck-1", otherDate));

        assertThat(store.reservedDeckIdsByDate())
                .containsEntry(date, java.util.Set.of("deck-1", "deck-2"))
                .containsEntry(otherDate, java.util.Set.of("deck-1"));
    }
}
