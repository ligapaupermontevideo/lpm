package com.liga.calendar;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Controller
public class CalendarController {

    private static final DateTimeFormatter WHATSAPP_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Same-day sign-ups close at 17:00, same cutoff and reasoning as deck
    // reservations — the league needs time to prepare before that day's
    // event.
    private static final LocalTime SAME_DAY_CUTOFF = LocalTime.of(17, 0);

    private final GoogleCalendarService calendarService;
    private final SectionAccess sectionAccess;
    private final String whatsappNumber;
    private final Clock clock;
    private final EventSignupStore signupStore;
    private final DeckReservationStore reservationStore;

    public CalendarController(GoogleCalendarService calendarService, SectionAccess sectionAccess,
                               @Value("${liga.whatsapp.number}") String whatsappNumber, Clock clock,
                               EventSignupStore signupStore, DeckReservationStore reservationStore) {
        this.calendarService = calendarService;
        this.sectionAccess = sectionAccess;
        this.whatsappNumber = whatsappNumber;
        this.clock = clock;
        this.signupStore = signupStore;
        this.reservationStore = reservationStore;
    }

    @GetMapping("/calendario")
    public String index(@RequestParam(value = "year", required = false) Integer year,
                         @RequestParam(value = "month", required = false) Integer month,
                         Model model) {
        sectionAccess.requireEnabled("/calendario");

        YearMonth current = (year != null && month != null)
                ? YearMonth.of(year, month)
                : YearMonth.now();

        LocalDate[] range = CalendarGridBuilder.gridRange(current);
        List<CalendarEvent> events = calendarService.getEvents(range[0], range[1]);

        YearMonth previous = current.minusMonths(1);
        YearMonth next = current.plusMonths(1);

        CalendarMonthView monthView = CalendarGridBuilder.buildMonth(current, events);

        model.addAttribute("month", monthView);
        model.addAttribute("prevYear", previous.getYear());
        model.addAttribute("prevMonth", previous.getMonthValue());
        model.addAttribute("nextYear", next.getYear());
        model.addAttribute("nextMonth", next.getMonthValue());
        model.addAttribute("signupUrls", buildSignupUrls(monthView));
        model.addAttribute("hasEventsThisMonth", hasEventsThisMonth(monthView));
        model.addAttribute("signupsByDate", signupInfoByDate());
        return "index";
    }

    @GetMapping("/calendario/anotarse")
    public String signupForm(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                              Model model) {
        sectionAccess.requireEnabled("/calendario");

        List<DateOption> availableDates = registrableEventsByDate().keySet().stream()
                .sorted()
                .map(this::toDateOption)
                .toList();

        model.addAttribute("availableDates", availableDates);
        model.addAttribute("selectedDate", fecha);
        return "anotarse";
    }

    @PostMapping("/calendario/anotarse")
    public String signUp(@RequestParam String cedula,
                          @RequestParam String nombre,
                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                          @RequestParam(defaultValue = "propio") String deckChoice,
                          RedirectAttributes redirectAttributes) {

        sectionAccess.requireEnabled("/calendario");

        String fullName = nombre == null ? "" : nombre.trim();
        if (fullName.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ingresa tu nombre completo.");
            return redirectToSignupForm(fecha);
        }

        if (!CedulaValidator.isValid(cedula)) {
            redirectAttributes.addFlashAttribute("errorMessage", "La cedula ingresada no es valida.");
            return redirectToSignupForm(fecha);
        }

        CalendarEvent event = registrableEventsByDate().get(fecha);
        if (event == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Esa fecha ya no esta disponible.");
            return redirectToSignupForm(fecha);
        }

        String normalizedCedula = CedulaValidator.normalize(cedula);
        EventSignup signup = new EventSignup(normalizedCedula, fullName, fecha, event.getTitle(), LocalDateTime.now(clock));

        EventSignupStore.SignUpResult result = signupStore.signUp(signup);
        if (result == EventSignupStore.SignUpResult.ALREADY_SIGNED_UP) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ya estas anotado para esa fecha.");
            return redirectToSignupForm(fecha);
        }

        // The deck question is answered right on the signup form itself (two
        // submit buttons) since nombre/cedula/fecha are already at hand —
        // no need for a separate page just to ask it.
        if ("reservar".equals(deckChoice)) {
            return redirectToDeckList(fullName, normalizedCedula, fecha, redirectAttributes);
        }

        redirectAttributes.addFlashAttribute("confirmedDate", toDateOption(fecha).label());
        redirectAttributes.addFlashAttribute("whatsappUrl", buildWhatsAppUrl(event, fullName, normalizedCedula, null));
        return redirectToSignupForm(fecha);
    }

    // The actual "reserva de deck" page: shown only after choosing to
    // reserve, so it never re-asks nombre/cedula/fecha.
    @GetMapping("/calendario/anotarse/deck/reservar")
    public String deckList(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha, Model model) {
        sectionAccess.requireEnabled("/calendario");

        String nombre = (String) model.getAttribute("signupNombre");
        String cedula = (String) model.getAttribute("signupCedula");
        if (nombre == null || cedula == null) {
            return redirectToSignupForm(fecha);
        }

        if (registrableEventsByDate().get(fecha) == null) {
            return redirectToSignupForm(fecha);
        }

        Set<String> reservedDeckIds = reservationStore.reservedDeckIdsByDate().getOrDefault(fecha, Set.of());

        model.addAttribute("fecha", fecha);
        model.addAttribute("fechaLabel", toDateOption(fecha).label());
        model.addAttribute("nombre", nombre);
        model.addAttribute("cedula", cedula);
        model.addAttribute("decks", DeckCatalog.ALL);
        model.addAttribute("reservedDeckIds", reservedDeckIds);
        return "anotarse-deck-reservar";
    }

    @PostMapping("/calendario/anotarse/deck/reservar")
    public String reserveDeck(@RequestParam String nombre,
                               @RequestParam String cedula,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                               @RequestParam String deckId,
                               RedirectAttributes redirectAttributes) {
        sectionAccess.requireEnabled("/calendario");

        String fullName = nombre == null ? "" : nombre.trim();
        if (fullName.isEmpty() || !CedulaValidator.isValid(cedula)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Volve a anotarte, tus datos no son validos.");
            return redirectToSignupForm(fecha);
        }

        CalendarEvent event = registrableEventsByDate().get(fecha);
        if (event == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Esa fecha ya no esta disponible.");
            return redirectToSignupForm(fecha);
        }

        Deck deck = DeckCatalog.ALL.stream().filter(d -> d.id().equals(deckId)).findFirst().orElse(null);
        if (deck == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ese deck ya no esta disponible.");
            return redirectToDeckList(fullName, cedula, fecha, redirectAttributes);
        }

        String normalizedCedula = CedulaValidator.normalize(cedula);
        DeckReservation reservation = new DeckReservation(
                normalizedCedula, fullName, deck.id(), deck.name(), fecha, LocalDateTime.now(clock));

        DeckReservationStore.ReserveResult result = reservationStore.reserve(reservation);
        switch (result) {
            case PERSON_ALREADY_RESERVED -> {
                redirectAttributes.addFlashAttribute("errorMessage", "Ya tenes una reserva para esa fecha.");
                return redirectToDeckList(fullName, cedula, fecha, redirectAttributes);
            }
            case DECK_ALREADY_TAKEN -> {
                redirectAttributes.addFlashAttribute("errorMessage", "Ese deck ya fue reservado para esa fecha. Elegi otro.");
                return redirectToDeckList(fullName, cedula, fecha, redirectAttributes);
            }
            case OK -> {
                redirectAttributes.addFlashAttribute("confirmedDate", toDateOption(fecha).label());
                redirectAttributes.addFlashAttribute("confirmedDeck", deck.name());
                redirectAttributes.addFlashAttribute("whatsappUrl",
                        buildWhatsAppUrl(event, fullName, normalizedCedula, deck.name()));
            }
        }
        return redirectToSignupForm(fecha);
    }

    private String redirectToSignupForm(LocalDate fecha) {
        return "redirect:/calendario/anotarse?fecha=" + fecha;
    }

    private String redirectToDeckList(String nombre, String cedula, LocalDate fecha, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("signupNombre", nombre);
        redirectAttributes.addFlashAttribute("signupCedula", cedula);
        return "redirect:/calendario/anotarse/deck/reservar?fecha=" + fecha;
    }

    // The agenda (list) view only lists in-month days, unlike the grid which
    // also fills in adjacent-month padding days — so its "no events" check
    // needs its own pass rather than reusing the raw fetched events list.
    private boolean hasEventsThisMonth(CalendarMonthView month) {
        return month.getWeeks().stream()
                .flatMap(List::stream)
                .anyMatch(day -> day.isInMonth() && day.isHasEvents());
    }

    // One sign-up link per calendar day, keyed by date so the template can
    // show it directly on the day cell — only for dates currently open for
    // sign-up (see registrableEventsByDate). A date with an official event
    // that falls outside that window still shows on the grid as
    // informational (dot + tooltip), just without a sign-up badge.
    private Map<LocalDate, String> buildSignupUrls(CalendarMonthView month) {
        Map<LocalDate, CalendarEvent> registrable = registrableEventsByDate();
        Map<LocalDate, String> urls = new LinkedHashMap<>();
        for (List<CalendarDay> week : month.getWeeks()) {
            for (CalendarDay day : week) {
                if (registrable.containsKey(day.getDate())) {
                    urls.put(day.getDate(), "/calendario/anotarse?fecha=" + day.getDate());
                }
            }
        }
        return urls;
    }

    // Who's signed up for each date, and with which deck (colors included,
    // so the calendar can show its mana pips the same way the deck list
    // does), so the calendar can show it publicly — but only while the date
    // hasn't passed yet; once it has, the list of attendees is no longer
    // useful to show and just clutters old days.
    private Map<LocalDate, List<AttendeeInfo>> signupInfoByDate() {
        LocalDate today = LocalDate.now(clock);
        Map<String, DeckReservation> reservationByCedulaAndDate = new LinkedHashMap<>();
        reservationStore.all().forEach(reservation ->
                reservationByCedulaAndDate.put(reservation.cedula() + "|" + reservation.date(), reservation));

        Map<LocalDate, List<AttendeeInfo>> byDate = new LinkedHashMap<>();
        signupStore.all().stream()
                .filter(signup -> !signup.date().isBefore(today))
                .forEach(signup -> {
                    DeckReservation reservation = reservationByCedulaAndDate.get(signup.cedula() + "|" + signup.date());
                    String deck = reservation != null ? reservation.deckName() : "Propio Deck";
                    List<String> deckColors = reservation != null ? colorsOf(reservation.deckId()) : List.of();
                    byDate.computeIfAbsent(signup.date(), d -> new ArrayList<>())
                            .add(new AttendeeInfo(signup.fullName(), deck, deckColors));
                });
        return byDate;
    }

    private List<String> colorsOf(String deckId) {
        return DeckCatalog.ALL.stream()
                .filter(deck -> deck.id().equals(deckId))
                .findFirst()
                .map(Deck::colors)
                .orElse(List.of());
    }

    // Same "one week at a time" window deck reservations use (see
    // DeckReservationController#availableDates): only the nearest
    // Monday-Sunday week that still has a registrable date in it, so this
    // naturally rolls over to the following week once every date in the
    // current one has passed or hit the same-day cutoff. Only single-day
    // events count as "fechas oficiales" people can sign up for —
    // multi-day events (like the season-long "Temporada ..." entry) are
    // purely informational.
    private Map<LocalDate, CalendarEvent> registrableEventsByDate() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        LocalDate horizon = today.plusMonths(12);

        Map<LocalDate, CalendarEvent> byDate = new LinkedHashMap<>();
        calendarService.getEvents(today, horizon).stream()
                .filter(event -> !event.isMultiDay())
                .filter(event -> event.getStartDate() != null && isRegistrable(event.getStartDate(), now))
                .sorted(Comparator.comparing(CalendarEvent::getStartDate))
                .forEach(event -> byDate.putIfAbsent(event.getStartDate(), event));

        if (byDate.isEmpty()) {
            return byDate;
        }

        LocalDate nearest = byDate.keySet().iterator().next();
        LocalDate weekStart = nearest.minusDays(nearest.getDayOfWeek().getValue() - 1);
        LocalDate weekEnd = weekStart.plusDays(6);

        Map<LocalDate, CalendarEvent> result = new LinkedHashMap<>();
        byDate.forEach((date, event) -> {
            if (!date.isBefore(weekStart) && !date.isAfter(weekEnd)) {
                result.put(date, event);
            }
        });
        return result;
    }

    private boolean isRegistrable(LocalDate date, LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        if (date.isBefore(today)) {
            return false;
        }
        if (date.isEqual(today)) {
            return now.toLocalTime().isBefore(SAME_DAY_CUTOFF);
        }
        return true;
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

    // deckName is null when the person is bringing their own deck instead
    // of reserving one of the league's.
    private String buildWhatsAppUrl(CalendarEvent event, String fullName, String cedula, String deckName) {
        String deckLine = deckName != null ? "- Deck: " + deckName : "- Lleva su propio deck";
        String message = """
                Hola! Me anoto para jugar esta fecha de la liga:
                - Nombre: %s
                - Cedula: %s
                - Fecha: %s (%s)
                %s"""
                .formatted(fullName, cedula, event.getStartDate().format(WHATSAPP_DATE), event.getTitle(), deckLine);

        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);
        return "https://wa.me/" + whatsappNumber + "?text=" + encoded;
    }

    public record DateOption(LocalDate value, String label) {
    }

    public record AttendeeInfo(String fullName, String deck, List<String> deckColors) {
    }
}
