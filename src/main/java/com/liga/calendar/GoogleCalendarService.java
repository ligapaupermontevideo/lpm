package com.liga.calendar;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarRequestInitializer;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class GoogleCalendarService {

    private static final ZoneId ZONE = ZoneId.of("America/Montevideo");

    @Value("${google.calendar.id}")
    private String calendarId;

    @Value("${google.api.key}")
    private String apiKey;

    // Package-private so unit tests can set it directly without a Spring
    // context, same reasoning as the other package-private members below.
    @Autowired
    CalendarProperties calendarProperties;

    private static final String APPLICATION_NAME = "Liga Pauper Montevideo";

    /**
     * Fetches every event overlapping the given date range (inclusive) from
     * the main league calendar plus any extra calendars configured in
     * {@link CalendarProperties}, so a displayed calendar month always shows
     * its own events regardless of whether that month is in the past or the
     * future. One misconfigured or unreachable extra calendar doesn't take
     * down the rest of the page — see {@link #fetchCalendar}.
     */
    public List<CalendarEvent> getEvents(LocalDate from, LocalDate to) {
        List<CalendarEvent> result = new ArrayList<>();

        try {
            Calendar service = new Calendar.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    null)
                    .setApplicationName(APPLICATION_NAME)
                    .setGoogleClientRequestInitializer(new CalendarRequestInitializer(apiKey))
                    .build();

            DateTime timeMin = toGoogleDateTime(from.atStartOfDay());
            DateTime timeMax = toGoogleDateTime(to.plusDays(1).atStartOfDay());

            result.addAll(fetchCalendar(service, calendarId, timeMin, timeMax, true));
            for (String extraCalendarId : activeExtraCalendarIds()) {
                result.addAll(fetchCalendar(service, extraCalendarId, timeMin, timeMax, false));
            }
            result.sort(Comparator.comparing(CalendarEvent::getStart,
                    Comparator.nullsLast(Comparator.naturalOrder())));

        } catch (Exception e) {
            // Log and return empty list so the page still renders
            System.err.println("Error fetching calendar events: " + e.getMessage());
        }

        return result;
    }

    // Blank entries (an extra calendar configured but its env var left
    // unset) are skipped rather than attempted, so adding a new line in
    // CalendarProperties without a real ID yet doesn't spam the logs.
    List<String> activeExtraCalendarIds() {
        return calendarProperties.getExtra().values().stream()
                .filter(id -> id != null && !id.isBlank())
                .toList();
    }

    private List<CalendarEvent> fetchCalendar(Calendar service, String calendarId, DateTime timeMin,
                                               DateTime timeMax, boolean fromPrimaryCalendar) {
        List<CalendarEvent> events = new ArrayList<>();
        try {
            Events response = service.events().list(calendarId)
                    .setMaxResults(2500)
                    .setTimeMin(timeMin)
                    .setTimeMax(timeMax)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

            for (Event event : response.getItems()) {
                CalendarEvent mapped = mapEvent(event);
                mapped.setFromPrimaryCalendar(fromPrimaryCalendar);
                events.add(mapped);
            }
        } catch (Exception e) {
            // A single bad calendar (private, wrong ID, etc.) shouldn't take
            // down the whole page — skip it and keep the rest.
            System.err.println("Error fetching calendar " + calendarId + ": " + e.getMessage());
        }
        return events;
    }

    // Package-private (not private) so unit tests can exercise the mapping/
    // timezone logic directly, without going through the network call.
    DateTime toGoogleDateTime(LocalDateTime localDateTime) {
        return new DateTime(localDateTime.atZone(ZONE).toInstant().toEpochMilli());
    }

    CalendarEvent mapEvent(Event event) {
        boolean allDay = event.getStart().getDate() != null;

        LocalDateTime start = allDay
                ? toLocalDateTimeFromDate(event.getStart().getDate())
                : toLocalDateTime(event.getStart().getDateTime());

        LocalDateTime end = allDay
                ? toLocalDateTimeFromDate(event.getEnd().getDate())
                : toLocalDateTime(event.getEnd().getDateTime());

        CalendarEvent calendarEvent = new CalendarEvent(
                event.getId(),
                event.getSummary(),
                event.getDescription(),
                event.getLocation(),
                start,
                end,
                allDay
        );
        calendarEvent.setColorId(event.getColorId());
        return calendarEvent;
    }

    LocalDateTime toLocalDateTime(DateTime dt) {
        return Instant.ofEpochMilli(dt.getValue())
                .atZone(ZONE)
                .toLocalDateTime();
    }

    LocalDateTime toLocalDateTimeFromDate(com.google.api.client.util.DateTime dt) {
        // All-day events: parse date string directly
        String dateStr = dt.toStringRfc3339(); // "YYYY-MM-DD"
        return LocalDateTime.parse(dateStr + "T00:00:00");
    }
}
