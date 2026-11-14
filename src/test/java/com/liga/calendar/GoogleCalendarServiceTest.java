package com.liga.calendar;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the pure mapping/timezone logic in GoogleCalendarService without
 * making any real network call — getEvents() (the network-calling method) is
 * intentionally left untested here, since a unit test should stay offline.
 */
class GoogleCalendarServiceTest {

    private final GoogleCalendarService service = new GoogleCalendarService();

    @Test
    void mapEventReadsAllDaySingleDayFields() {
        Event event = new Event()
                .setId("evt-1")
                .setSummary("Torneo")
                .setDescription("Descripcion")
                .setLocation("Mates y Magic")
                .setStart(new EventDateTime().setDate(new DateTime("2026-10-20")))
                .setEnd(new EventDateTime().setDate(new DateTime("2026-10-21")));

        CalendarEvent mapped = service.mapEvent(event);

        assertThat(mapped.getId()).isEqualTo("evt-1");
        assertThat(mapped.getTitle()).isEqualTo("Torneo");
        assertThat(mapped.getDescription()).isEqualTo("Descripcion");
        assertThat(mapped.getLocation()).isEqualTo("Mates y Magic");
        assertThat(mapped.isAllDay()).isTrue();
        assertThat(mapped.getStart()).isEqualTo(LocalDateTime.of(2026, 10, 20, 0, 0));
        assertThat(mapped.getEnd()).isEqualTo(LocalDateTime.of(2026, 10, 21, 0, 0));
    }

    @Test
    void mapEventReadsTimedEventAndConvertsToMontevideoZone() {
        Event event = new Event()
                .setId("evt-2")
                .setSummary("Fecha")
                .setColorId("6")
                .setStart(new EventDateTime().setDateTime(new DateTime("2026-10-08T19:30:00-03:00")))
                .setEnd(new EventDateTime().setDateTime(new DateTime("2026-10-08T23:30:00-03:00")));

        CalendarEvent mapped = service.mapEvent(event);

        assertThat(mapped.isAllDay()).isFalse();
        assertThat(mapped.getStart()).isEqualTo(LocalDateTime.of(2026, 10, 8, 19, 30));
        assertThat(mapped.getEnd()).isEqualTo(LocalDateTime.of(2026, 10, 8, 23, 30));
        assertThat(mapped.getColorId()).isEqualTo("6");
    }

    @Test
    void toLocalDateTimeConvertsUtcTimestampToMontevideoLocalTime() {
        // 22:30 UTC is 19:30 in America/Montevideo (UTC-3, no DST).
        LocalDateTime result = service.toLocalDateTime(new DateTime("2026-10-08T22:30:00Z"));

        assertThat(result).isEqualTo(LocalDateTime.of(2026, 10, 8, 19, 30));
    }

    @Test
    void toLocalDateTimeFromDateParsesDateOnlyStringAtMidnight() {
        LocalDateTime result = service.toLocalDateTimeFromDate(new DateTime("2026-10-08"));

        assertThat(result).isEqualTo(LocalDateTime.of(2026, 10, 8, 0, 0));
    }

    @Test
    void toGoogleDateTimeAndBackRoundTripsToTheSameLocalDateTime() {
        LocalDateTime original = LocalDateTime.of(2026, 10, 8, 19, 30);

        DateTime googleDateTime = service.toGoogleDateTime(original);
        LocalDateTime roundTripped = service.toLocalDateTime(googleDateTime);

        assertThat(roundTripped).isEqualTo(original);
    }

    @Test
    void activeExtraCalendarIdsSkipsBlankAndUnsetEntries() {
        Map<String, String> extra = new LinkedHashMap<>();
        extra.put("eventos-pauper", "eventos-pauper@group.calendar.google.com");
        extra.put("not-configured-yet", "");
        extra.put("also-blank", "   ");
        CalendarProperties properties = new CalendarProperties();
        properties.setExtra(extra);
        service.calendarProperties = properties;

        assertThat(service.activeExtraCalendarIds()).containsExactly("eventos-pauper@group.calendar.google.com");
    }
}
