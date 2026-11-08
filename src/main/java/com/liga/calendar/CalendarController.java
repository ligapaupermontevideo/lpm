package com.liga.calendar;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class CalendarController {

    private static final DateTimeFormatter WHATSAPP_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final GoogleCalendarService calendarService;
    private final SectionAccess sectionAccess;
    private final String whatsappNumber;
    private final Clock clock;

    public CalendarController(GoogleCalendarService calendarService, SectionAccess sectionAccess,
                               @Value("${liga.whatsapp.number}") String whatsappNumber, Clock clock) {
        this.calendarService = calendarService;
        this.sectionAccess = sectionAccess;
        this.whatsappNumber = whatsappNumber;
        this.clock = clock;
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
        model.addAttribute("registrationUrls", buildRegistrationUrls(monthView));
        model.addAttribute("hasEventsThisMonth", hasEventsThisMonth(monthView));
        return "index";
    }

    // The agenda (list) view only lists in-month days, unlike the grid which
    // also fills in adjacent-month padding days — so its "no events" check
    // needs its own pass rather than reusing the raw fetched events list.
    private boolean hasEventsThisMonth(CalendarMonthView month) {
        return month.getWeeks().stream()
                .flatMap(List::stream)
                .anyMatch(day -> day.isInMonth() && day.isHasEvents());
    }

    // One registration link per calendar day, keyed by date so the template
    // can show it directly on the day cell. Only single-day events count as
    // "fechas oficiales" people can register for — multi-day events (like
    // the season-long "Temporada ..." entry) are purely informational, and
    // past dates no longer offer the option at all.
    private Map<LocalDate, String> buildRegistrationUrls(CalendarMonthView month) {
        LocalDate today = LocalDate.now(clock);
        Map<LocalDate, String> urls = new LinkedHashMap<>();
        for (List<CalendarDay> week : month.getWeeks()) {
            for (CalendarDay day : week) {
                if (day.getDate().isBefore(today)) continue;
                day.getEvents().stream()
                        .filter(event -> !event.isMultiDay())
                        .findFirst()
                        .ifPresent(event -> urls.put(day.getDate(), buildRegistrationUrl(event)));
            }
        }
        return urls;
    }

    private String buildRegistrationUrl(CalendarEvent event) {
        String dateLabel = event.isMultiDay()
                ? event.getStartDate().format(WHATSAPP_DATE) + " al " + event.getDisplayEndDate().format(WHATSAPP_DATE)
                : event.getStartDate().format(WHATSAPP_DATE);

        String message = "Hola! Me anoto para jugar esta fecha de la liga: %s (%s)."
                .formatted(event.getTitle(), dateLabel);

        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);
        return "https://wa.me/" + whatsappNumber + "?text=" + encoded;
    }
}
