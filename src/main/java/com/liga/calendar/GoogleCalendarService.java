package com.liga.calendar;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarRequestInitializer;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class GoogleCalendarService {

    private static final ZoneId ZONE = ZoneId.of("America/Montevideo");

    @Value("${google.calendar.id}")
    private String calendarId;

    @Value("${google.api.key}")
    private String apiKey;

    private static final String APPLICATION_NAME = "Liga Pauper Montevideo";

    /**
     * Fetches every event overlapping the given date range (inclusive), so a
     * displayed calendar month always shows its own events regardless of
     * whether that month is in the past or the future.
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

            Events events = service.events().list(calendarId)
                    .setMaxResults(2500)
                    .setTimeMin(timeMin)
                    .setTimeMax(timeMax)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

            for (Event event : events.getItems()) {
                result.add(mapEvent(event));
            }

        } catch (Exception e) {
            // Log and return empty list so the page still renders
            System.err.println("Error fetching calendar events: " + e.getMessage());
        }

        return result;
    }

    private DateTime toGoogleDateTime(LocalDateTime localDateTime) {
        return new DateTime(localDateTime.atZone(ZONE).toInstant().toEpochMilli());
    }

    private CalendarEvent mapEvent(Event event) {
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

    private LocalDateTime toLocalDateTime(DateTime dt) {
        return Instant.ofEpochMilli(dt.getValue())
                .atZone(ZONE)
                .toLocalDateTime();
    }

    private LocalDateTime toLocalDateTimeFromDate(com.google.api.client.util.DateTime dt) {
        // All-day events: parse date string directly
        String dateStr = dt.toStringRfc3339(); // "YYYY-MM-DD"
        return LocalDateTime.parse(dateStr + "T00:00:00");
    }
}
