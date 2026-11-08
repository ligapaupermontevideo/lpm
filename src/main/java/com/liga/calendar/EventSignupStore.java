package com.liga.calendar;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Holds every sign-up made through the site for the league's official
 * dates.
 * <p>
 * There's no database yet, same as {@link DeckReservationStore} —
 * sign-ups live only in memory and are lost on restart. That's an
 * acceptable trade-off for now since every confirmed sign-up is also sent
 * to the league over WhatsApp, which is the actual record the league acts
 * on.
 */
@Component
public class EventSignupStore {

    public enum SignUpResult {
        OK,
        ALREADY_SIGNED_UP
    }

    private final List<EventSignup> signups = new CopyOnWriteArrayList<>();
    private final Object lock = new Object();

    /** One sign-up per person per date. */
    public SignUpResult signUp(EventSignup signup) {
        synchronized (lock) {
            boolean alreadySignedUp = signups.stream()
                    .anyMatch(existing -> existing.date().equals(signup.date())
                            && existing.cedula().equals(signup.cedula()));
            if (alreadySignedUp) {
                return SignUpResult.ALREADY_SIGNED_UP;
            }
            signups.add(signup);
            return SignUpResult.OK;
        }
    }

    public List<EventSignup> all() {
        return signups.stream()
                .sorted(Comparator.comparing(EventSignup::date).thenComparing(EventSignup::createdAt))
                .toList();
    }
}
