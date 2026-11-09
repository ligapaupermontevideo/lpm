package com.liga.calendar;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
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
 * Backed by a JSON file (see {@link JsonFileStore}) so reservations
 * survive an app restart or redeploy, not just in-memory while the
 * process runs — same persistence approach as {@link EventSignupStore}.
 * Every confirmed reservation is also sent to the league over WhatsApp,
 * which remains the authoritative record either way; the file just saves
 * people from having to re-send theirs after a restart.
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

    // See EventSignupStore#dataDir for why this is left null/blank in
    // tests and when liga.data.dir is unset, and why it's package-private.
    @Value("${liga.data.dir:}")
    String dataDir;

    private JsonFileStore<DeckReservation> fileStore;

    @PostConstruct
    void loadFromDisk() {
        if (dataDir == null || dataDir.isBlank()) {
            return;
        }
        fileStore = new JsonFileStore<>(dataDir, "deck-reservations.json", DeckReservation.class);
        reservations.addAll(fileStore.readAll());
    }

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
            if (fileStore != null) {
                fileStore.writeAll(reservations);
            }
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
