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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    public CalendarController(GoogleCalendarService calendarService, SectionAccess sectionAccess,
                               @Value("${liga.whatsapp.number}") String whatsappNumber, Clock clock,
                               EventSignupStore signupStore) {
        this.calendarService = calendarService;
        this.sectionAccess = sectionAccess;
        this.whatsappNumber = whatsappNumber;
        this.clock = clock;
        this.signupStore = signupStore;
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

        EventSignup signup = new EventSignup(
                CedulaValidator.normalize(cedula), fullName, fecha, event.getTitle(), LocalDateTime.now(clock));

        EventSignupStore.SignUpResult result = signupStore.signUp(signup);
        if (result == EventSignupStore.SignUpResult.ALREADY_SIGNED_UP) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ya estas anotado para esa fecha.");
        } else {
            redirectAttributes.addFlashAttribute("confirmedDate", toDateOption(fecha).label());
            redirectAttributes.addFlashAttribute("whatsappUrl", buildWhatsAppUrl(event));
        }
        return redirectToSignupForm(fecha);
    }

    private String redirectToSignupForm(LocalDate fecha) {
        return "redirect:/calendario/anotarse?fecha=" + fecha;
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

    private String buildWhatsAppUrl(CalendarEvent event) {
        String message = "Hola! Me anoto para jugar esta fecha de la liga: %s (%s)."
                .formatted(event.getTitle(), event.getStartDate().format(WHATSAPP_DATE));

        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);
        return "https://wa.me/" + whatsappNumber + "?text=" + encoded;
    }

    public record DateOption(LocalDate value, String label) {
    }
}
