package com.liga.calendar;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Deck reservations for the league's official dates: pick one of the
 * league's current decks (see {@link DeckCatalog}) and one of the upcoming
 * dates already on the public calendar (see {@link GoogleCalendarService}),
 * identified by a valid cedula so each person only holds one reservation
 * per date (enforced by {@link DeckReservationStore}).
 */
@Controller
public class DeckReservationController {

    private static final DateTimeFormatter WHATSAPP_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Same-day reservations close at 17:00 so the league has time to get
    // the deck ready before that day's event — a date is still reservable
    // for days ahead of time regardless of the current time of day.
    private static final LocalTime SAME_DAY_CUTOFF = LocalTime.of(17, 0);

    private final GoogleCalendarService calendarService;
    private final DeckReservationStore reservationStore;
    private final Clock clock;
    private final String whatsappNumber;
    private final String adminToken;
    private final SectionAccess sectionAccess;

    public DeckReservationController(GoogleCalendarService calendarService,
                                      DeckReservationStore reservationStore,
                                      Clock clock,
                                      @Value("${liga.whatsapp.number}") String whatsappNumber,
                                      @Value("${liga.admin.token:}") String adminToken,
                                      SectionAccess sectionAccess) {
        this.calendarService = calendarService;
        this.reservationStore = reservationStore;
        this.clock = clock;
        this.whatsappNumber = whatsappNumber;
        this.adminToken = adminToken;
        this.sectionAccess = sectionAccess;
    }

    @GetMapping("/prestamo-mazos")
    public String decks(Model model) {
        Section section = sectionAccess.requireEnabled("/prestamo-mazos");

        model.addAttribute("section", section);
        model.addAttribute("decks", DeckCatalog.ALL);
        return "decks";
    }

    @GetMapping("/prestamo-mazos/reservar")
    public String form(@RequestParam(required = false) String deckId, Model model) {
        Section section = sectionAccess.requireEnabled("/prestamo-mazos");

        model.addAttribute("section", section);
        model.addAttribute("decks", DeckCatalog.ALL);
        model.addAttribute("availableDates", availableDates().stream().map(this::toDateOption).toList());
        model.addAttribute("reservedByDate", reservedDeckIdsByDateForDisplay());
        model.addAttribute("selectedDeckId", deckId);
        return "reservas";
    }

    @PostMapping("/prestamo-mazos/reservar")
    public String reserve(@RequestParam String cedula,
                           @RequestParam String nombre,
                           @RequestParam String deckId,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                           RedirectAttributes redirectAttributes) {

        sectionAccess.requireEnabled("/prestamo-mazos");

        String fullName = nombre == null ? "" : nombre.trim();
        if (fullName.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ingresa tu nombre completo.");
            return redirectToForm(deckId);
        }

        if (!CedulaValidator.isValid(cedula)) {
            redirectAttributes.addFlashAttribute("errorMessage", "La cedula ingresada no es valida.");
            return redirectToForm(deckId);
        }

        if (!availableDates().contains(fecha)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Esa fecha ya no esta disponible.");
            return redirectToForm(deckId);
        }

        Deck deck = DeckCatalog.ALL.stream()
                .filter(d -> d.id().equals(deckId))
                .findFirst()
                .orElse(null);
        if (deck == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ese deck ya no esta disponible.");
            return redirectToForm(deckId);
        }

        DeckReservation reservation = new DeckReservation(
                CedulaValidator.normalize(cedula), fullName, deck.id(), deck.name(), fecha, LocalDateTime.now(clock));

        DeckReservationStore.ReserveResult result = reservationStore.reserve(reservation);
        switch (result) {
            case PERSON_ALREADY_RESERVED ->
                    redirectAttributes.addFlashAttribute("errorMessage", "Ya tenes una reserva para esa fecha.");
            case DECK_ALREADY_TAKEN ->
                    redirectAttributes.addFlashAttribute("errorMessage", "Ese deck ya fue reservado para esa fecha. Elegi otro.");
            case OK -> {
                redirectAttributes.addFlashAttribute("confirmedDeck", deck.name());
                redirectAttributes.addFlashAttribute("confirmedDate", toDateOption(fecha).label());
                redirectAttributes.addFlashAttribute("whatsappUrl", buildWhatsAppUrl(reservation));
            }
        }
        return redirectToForm(deckId);
    }

    private String redirectToForm(String deckId) {
        String encoded = URLEncoder.encode(deckId, StandardCharsets.UTF_8);
        return "redirect:/prestamo-mazos/reservar?deckId=" + encoded;
    }

    @GetMapping("/prestamo-mazos/reservas")
    public String reservas(@RequestParam(required = false) String token, Model model) {
        if (adminToken.isBlank() || !adminToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("reservations", reservationStore.all());
        return "reservas-admin";
    }

    // Only the nearest week (Monday-Sunday, same convention as the
    // calendar's own grid) that still has a reservable date in it — the
    // league opens reservations one week at a time, not months ahead, so
    // this naturally rolls over to the following week once every date in
    // the current one has passed or hit the same-day cutoff.
    private List<LocalDate> availableDates() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        LocalDate horizon = today.plusMonths(12);
        List<LocalDate> reservable = calendarService.getEvents(today, horizon).stream()
                .map(CalendarEvent::getStartDate)
                .filter(date -> date != null && isReservable(date, now))
                .distinct()
                .sorted()
                .toList();

        if (reservable.isEmpty()) {
            return reservable;
        }

        LocalDate nearest = reservable.get(0);
        LocalDate weekStart = nearest.minusDays(nearest.getDayOfWeek().getValue() - 1);
        LocalDate weekEnd = weekStart.plusDays(6);

        return reservable.stream()
                .filter(date -> !date.isBefore(weekStart) && !date.isAfter(weekEnd))
                .toList();
    }

    private boolean isReservable(LocalDate date, LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        if (date.isBefore(today)) {
            return false;
        }
        if (date.isEqual(today)) {
            return now.toLocalTime().isBefore(SAME_DAY_CUTOFF);
        }
        return true;
    }

    // Keyed by ISO date string (matching the <option value> the date select
    // uses) so the page's JS can look reserved deck ids up by the date it
    // currently has selected, without another round trip to the server.
    private Map<String, List<String>> reservedDeckIdsByDateForDisplay() {
        return reservationStore.reservedDeckIdsByDate().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().toString(), e -> List.copyOf(e.getValue())));
    }

    private DateOption toDateOption(LocalDate date) {
        String weekday = date.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("es", "UY"));
        String month = date.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "UY"));
        String label = "%s %d de %s".formatted(capitalize(weekday), date.getDayOfMonth(), month);
        return new DateOption(date, label);
    }

    private String capitalize(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private String buildWhatsAppUrl(DeckReservation reservation) {
        String message = """
                Hola! Quiero confirmar mi reserva de deck:
                - Nombre: %s
                - Cedula: %s
                - Deck: %s
                - Fecha: %s"""
                .formatted(reservation.fullName(), reservation.cedula(), reservation.deckName(),
                        reservation.date().format(WHATSAPP_DATE));

        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);
        return "https://wa.me/" + whatsappNumber + "?text=" + encoded;
    }

    public record DateOption(LocalDate value, String label) {
    }
}
