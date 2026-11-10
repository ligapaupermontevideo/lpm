package com.liga.calendar;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CollaboratorTest {

    private Collaborator collaborator(String name) {
        return new Collaborator("id", name, "Rol", "Bio", null, null);
    }

    @Test
    void initialsCombinesFirstAndLastName() {
        assertThat(collaborator("Juan Perez").initials()).isEqualTo("JP");
    }

    @Test
    void initialsUsesOnlyOneLetterForASingleWordName() {
        assertThat(collaborator("Juan").initials()).isEqualTo("J");
    }

    @Test
    void initialsIgnoresMiddleNames() {
        assertThat(collaborator("Juan Carlos Perez Diaz").initials()).isEqualTo("JD");
    }
}
