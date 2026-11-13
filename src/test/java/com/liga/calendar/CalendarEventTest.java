package com.liga.calendar;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarEventTest {

    @Test
    void getStartDateReturnsNullWhenStartIsNull() {
        CalendarEvent event = new CalendarEvent("1", "Sin fecha", null, null, null, null, false);

        assertThat(event.getStartDate()).isNull();
    }

    @Test
    void getStartDateExtractsDateFromStart() {
        CalendarEvent event = new CalendarEvent("1", "Evento", null, null,
                LocalDateTime.of(2026, 10, 15, 19, 30), LocalDateTime.of(2026, 10, 15, 23, 30), false);

        assertThat(event.getStartDate()).isEqualTo(LocalDate.of(2026, 10, 15));
    }

    @Test
    void getDisplayEndDateFallsBackToStartDateWhenEndIsNull() {
        CalendarEvent event = new CalendarEvent("1", "Evento", null, null,
                LocalDateTime.of(2026, 10, 15, 19, 30), null, false);

        assertThat(event.getDisplayEndDate()).isEqualTo(LocalDate.of(2026, 10, 15));
    }

    @Test
    void getDisplayEndDateNormalizesSingleDayAllDayEvent() {
        // Google represents a one-day all-day event with an exclusive end
        // date (the day after), which should normalize back to the start date.
        CalendarEvent event = new CalendarEvent("1", "Evento de un dia", null, null,
                LocalDateTime.of(2026, 10, 20, 0, 0), LocalDateTime.of(2026, 10, 21, 0, 0), true);

        assertThat(event.getDisplayEndDate()).isEqualTo(LocalDate.of(2026, 10, 20));
        assertThat(event.isMultiDay()).isFalse();
    }

    @Test
    void getDisplayEndDateNormalizesMultiDayAllDayEvent() {
        CalendarEvent event = new CalendarEvent("1", "Paupergeddon", null, null,
                LocalDateTime.of(2026, 12, 3, 0, 0), LocalDateTime.of(2026, 12, 7, 0, 0), true);

        // Exclusive end (Dec 7) normalizes to the last actual day (Dec 6).
        assertThat(event.getDisplayEndDate()).isEqualTo(LocalDate.of(2026, 12, 6));
        assertThat(event.isMultiDay()).isTrue();
    }

    @Test
    void getDisplayEndDateKeepsInclusiveEndForSingleDayTimedEvent() {
        CalendarEvent event = new CalendarEvent("1", "Fecha", null, null,
                LocalDateTime.of(2026, 10, 8, 19, 30), LocalDateTime.of(2026, 10, 8, 23, 30), false);

        assertThat(event.getDisplayEndDate()).isEqualTo(LocalDate.of(2026, 10, 8));
        assertThat(event.isMultiDay()).isFalse();
    }

    @Test
    void getDisplayEndDateKeepsInclusiveEndForMultiDayTimedEvent() {
        CalendarEvent event = new CalendarEvent("1", "Torneo largo", null, null,
                LocalDateTime.of(2026, 10, 14, 20, 0), LocalDateTime.of(2026, 10, 16, 22, 0), false);

        assertThat(event.getDisplayEndDate()).isEqualTo(LocalDate.of(2026, 10, 16));
        assertThat(event.isMultiDay()).isTrue();
    }

    @Test
    void getDisplayEndDateTreatsMidnightEndAsExclusiveForTimedEvents() {
        // A timed event ending exactly at midnight is effectively "over" the
        // day before, same as an all-day event's exclusive end.
        CalendarEvent event = new CalendarEvent("1", "Nocturno", null, null,
                LocalDateTime.of(2026, 10, 14, 20, 0), LocalDateTime.of(2026, 10, 16, 0, 0), false);

        assertThat(event.getDisplayEndDate()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(event.isMultiDay()).isTrue();
    }

    @Test
    void isMultiDayFalseWhenStartDateIsNull() {
        CalendarEvent event = new CalendarEvent("1", "Sin fecha", null, null, null, null, false);

        assertThat(event.isMultiDay()).isFalse();
    }

    @Test
    void getColorHexReturnsMappedColorForKnownColorId() {
        CalendarEvent event = new CalendarEvent("1", "Evento", null, null, null, null, false);
        event.setColorId("7"); // Pavo real

        assertThat(event.getColorHex()).isEqualTo("#039be5");
    }

    @Test
    void getColorHexFallsBackToDefaultForUnknownColorId() {
        CalendarEvent event = new CalendarEvent("1", "Evento", null, null, null, null, false);
        event.setColorId("does-not-exist");

        assertThat(event.getColorHex()).isEqualTo("#e94560");
    }

    @Test
    void getColorHexFallsBackToDefaultWhenColorIdIsNull() {
        CalendarEvent event = new CalendarEvent("1", "Evento", null, null, null, null, false);

        assertThat(event.getColorHex()).isEqualTo("#e94560");
    }

    @Test
    void isPinnedIsFalseWithoutTheMarker() {
        CalendarEvent event = new CalendarEvent("1", "Fecha Oficial", null, null, null, null, false);

        assertThat(event.isPinned()).isFalse();
    }

    @Test
    void isPinnedIsTrueWithTheMarkerAnywhereInTheTitle() {
        CalendarEvent event = new CalendarEvent("1", "Torneo Especial [Especial]", null, null, null, null, false);

        assertThat(event.isPinned()).isTrue();
    }

    @Test
    void isPinnedIsCaseInsensitive() {
        CalendarEvent event = new CalendarEvent("1", "[especial] Torneo Especial", null, null, null, null, false);

        assertThat(event.isPinned()).isTrue();
    }

    @Test
    void getDisplayTitleStripsTheMarkerAndExtraWhitespace() {
        CalendarEvent event = new CalendarEvent("1", "Torneo Especial [Especial]", null, null, null, null, false);

        assertThat(event.getDisplayTitle()).isEqualTo("Torneo Especial");
    }

    @Test
    void getDisplayTitleStripsAMarkerInTheMiddleOfTheTitle() {
        CalendarEvent event = new CalendarEvent("1", "Torneo [Especial] Especial", null, null, null, null, false);

        assertThat(event.getDisplayTitle()).isEqualTo("Torneo Especial");
    }

    @Test
    void getDisplayTitleReturnsTheTitleUnchangedWithoutTheMarker() {
        CalendarEvent event = new CalendarEvent("1", "Fecha Oficial", null, null, null, null, false);

        assertThat(event.getDisplayTitle()).isEqualTo("Fecha Oficial");
    }

    @Test
    void gettersReturnConstructorValues() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 8, 19, 30);
        LocalDateTime end = LocalDateTime.of(2026, 10, 8, 23, 30);
        CalendarEvent event = new CalendarEvent("id-1", "Titulo", "Descripcion", "Lugar", start, end, false);

        assertThat(event.getId()).isEqualTo("id-1");
        assertThat(event.getTitle()).isEqualTo("Titulo");
        assertThat(event.getDescription()).isEqualTo("Descripcion");
        assertThat(event.getLocation()).isEqualTo("Lugar");
        assertThat(event.getStart()).isEqualTo(start);
        assertThat(event.getEnd()).isEqualTo(end);
        assertThat(event.isAllDay()).isFalse();
    }
}
