package com.liga.calendar;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Holds every sign-up made through the site for the league's official
 * dates.
 * <p>
 * Backed by a JSON file (see {@link JsonFileStore}) so sign-ups survive an
 * app restart or redeploy, not just in-memory while the process runs —
 * same persistence approach as {@link DeckReservationStore}. Every
 * confirmed sign-up is also sent to the league over WhatsApp, which
 * remains the authoritative record either way; the file just saves people
 * from having to re-send theirs after a restart.
 */
@Component
public class EventSignupStore {

    private static final Logger log = LoggerFactory.getLogger(EventSignupStore.class);

    public enum SignUpResult {
        OK,
        ALREADY_SIGNED_UP
    }

    private final List<EventSignup> signups = new CopyOnWriteArrayList<>();
    private final Object lock = new Object();

    // Left null when this store is built directly (as tests do, and as
    // Spring does too when liga.data.dir is unset/blank), which keeps
    // persistence off and signups in-memory only — exactly the old
    // behavior. Spring sets it from liga.data.dir when running the app.
    // Package-private so EventSignupStoreTest can set it directly and
    // call loadFromDisk() to exercise persistence without a Spring context.
    @Value("${liga.data.dir:}")
    String dataDir;

    private JsonFileStore<EventSignup> fileStore;

    @PostConstruct
    void loadFromDisk() {
        if (dataDir == null || dataDir.isBlank()) {
            return;
        }
        fileStore = new JsonFileStore<>(dataDir, "signups.json", EventSignup.class);
        signups.addAll(fileStore.readAll());
    }

    /** One sign-up per person per date. */
    public SignUpResult signUp(EventSignup signup) {
        synchronized (lock) {
            if (isSignedUp(signup.cedula(), signup.date())) {
                return SignUpResult.ALREADY_SIGNED_UP;
            }
            signups.add(signup);
            if (fileStore != null) {
                fileStore.writeAll(signups);
            }
            return SignUpResult.OK;
        }
    }

    /**
     * Read-only check for whether this person is already signed up for this
     * date, without recording anything — lets a caller validate before
     * committing to a longer flow (e.g. picking a deck to reserve) without
     * a throwaway write.
     */
    public boolean isSignedUp(String cedula, LocalDate date) {
        return signups.stream().anyMatch(existing -> existing.date().equals(date) && existing.cedula().equals(cedula));
    }

    public List<EventSignup> all() {
        return signups.stream()
                .sorted(Comparator.comparing(EventSignup::date).thenComparing(EventSignup::createdAt))
                .toList();
    }
}
