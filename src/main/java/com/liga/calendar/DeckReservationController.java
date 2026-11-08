package com.liga.calendar;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * Admin-only view of every deck reservation made through the site (see
 * {@link DeckReservationStore}).
 * <p>
 * The public-facing reservation flow lives in {@link CalendarController}
 * now, as a step right after signing up for an official date — there's no
 * standalone "/prestamo-mazos" page anymore.
 */
@Controller
public class DeckReservationController {

    private final DeckReservationStore reservationStore;
    private final String adminToken;

    public DeckReservationController(DeckReservationStore reservationStore,
                                      @Value("${liga.admin.token:}") String adminToken) {
        this.reservationStore = reservationStore;
        this.adminToken = adminToken;
    }

    @GetMapping("/prestamo-mazos/reservas")
    public String reservas(@RequestParam(required = false) String token, Model model) {
        if (adminToken.isBlank() || !adminToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("reservations", reservationStore.all());
        return "reservas-admin";
    }
}
