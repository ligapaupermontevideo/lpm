package com.liga.calendar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EventSignupStoreTest {

    private final EventSignupStore store = new EventSignupStore();
    private final LocalDate date = LocalDate.of(2026, 11, 14);
    private final LocalDate otherDate = LocalDate.of(2026, 11, 21);

    private EventSignup signup(String cedula, LocalDate date) {
        return new EventSignup(cedula, "Juan Perez", date, "Fecha Oficial", LocalDateTime.now());
    }

    @Test
    void firstSignupForAPersonAndDateSucceeds() {
        assertThat(store.signUp(signup("12345672", date))).isEqualTo(EventSignupStore.SignUpResult.OK);
        assertThat(store.all()).hasSize(1);
    }

    @Test
    void samePersonCannotSignUpTwiceOnTheSameDate() {
        store.signUp(signup("12345672", date));

        EventSignupStore.SignUpResult result = store.signUp(signup("12345672", date));

        assertThat(result).isEqualTo(EventSignupStore.SignUpResult.ALREADY_SIGNED_UP);
        assertThat(store.all()).hasSize(1);
    }

    @Test
    void samePersonCanSignUpAgainOnADifferentDate() {
        store.signUp(signup("12345672", date));

        EventSignupStore.SignUpResult result = store.signUp(signup("12345672", otherDate));

        assertThat(result).isEqualTo(EventSignupStore.SignUpResult.OK);
        assertThat(store.all()).hasSize(2);
    }

    @Test
    void differentPeopleCanSignUpForTheSameDate() {
        store.signUp(signup("12345672", date));

        EventSignupStore.SignUpResult result = store.signUp(signup("41234563", date));

        assertThat(result).isEqualTo(EventSignupStore.SignUpResult.OK);
        assertThat(store.all()).hasSize(2);
    }

    @Test
    void allIsSortedByDateThenByCreationTime() {
        EventSignup first = new EventSignup("12345672", "Juan Perez", otherDate, "Fecha Oficial",
                LocalDateTime.of(2026, 1, 1, 10, 0));
        EventSignup second = new EventSignup("41234563", "Ana Diaz", date, "Fecha Oficial",
                LocalDateTime.of(2026, 1, 1, 9, 0));
        EventSignup third = new EventSignup("33534165", "Pedro Gomez", date, "Fecha Oficial",
                LocalDateTime.of(2026, 1, 1, 11, 0));
        store.signUp(first);
        store.signUp(second);
        store.signUp(third);

        assertThat(store.all()).containsExactly(second, third, first);
    }

    @Test
    void isSignedUpIsFalseWithNoMatchingSignup() {
        store.signUp(signup("12345672", date));

        assertThat(store.isSignedUp("12345672", otherDate)).isFalse();
        assertThat(store.isSignedUp("41234563", date)).isFalse();
    }

    @Test
    void isSignedUpIsTrueForAMatchingPersonAndDateWithoutRecordingAnything() {
        store.signUp(signup("12345672", date));

        assertThat(store.isSignedUp("12345672", date)).isTrue();
        assertThat(store.all()).hasSize(1);
    }

    @Test
    void withoutADataDirNothingIsWrittenToDisk(@TempDir Path tempDir) {
        store.signUp(signup("12345672", date));

        assertThat(tempDir.resolve("signups.json")).doesNotExist();
    }

    @Test
    void signupsSurviveBeingReloadedFromTheDataDir(@TempDir Path tempDir) {
        EventSignupStore persisted = new EventSignupStore();
        persisted.dataDir = tempDir.toString();
        persisted.loadFromDisk();
        persisted.signUp(signup("12345672", date));
        persisted.signUp(signup("41234563", otherDate));

        EventSignupStore reloaded = new EventSignupStore();
        reloaded.dataDir = tempDir.toString();
        reloaded.loadFromDisk();

        assertThat(reloaded.all()).containsExactlyElementsOf(persisted.all());
    }
}
