package com.liga.calendar;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeckReservationControllerTest {

    private final DeckReservationStore store = new DeckReservationStore();
    private final LocalDate eventDate = LocalDate.now().plusDays(10);
    private final Deck deck = DeckCatalog.ALL.get(0);

    private DeckReservationController controller(String adminToken) {
        return new DeckReservationController(store, adminToken);
    }

    @Test
    void reservasPageRequiresMatchingToken() {
        assertThatThrownBy(() -> controller("secret").reservas("wrong", new ConcurrentModel()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void reservasPageIsDisabledWhenNoTokenIsConfigured() {
        assertThatThrownBy(() -> controller("").reservas(null, new ConcurrentModel()))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void reservasPageListsReservationsWithCorrectToken() {
        store.reserve(new DeckReservation("12345672", "Juan Perez", deck.id(), deck.name(), eventDate, LocalDateTime.now()));
        Model model = new ConcurrentModel();

        String view = controller("secret").reservas("secret", model);

        assertThat(view).isEqualTo("reservas-admin");
        assertThat((List<?>) model.getAttribute("reservations")).hasSize(1);
    }
}
