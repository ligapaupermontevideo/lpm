package com.liga.calendar;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarDayTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 15);

    @Test
    void inMonthDayIsNeitherPrevNorNextMonth() {
        CalendarDay day = new CalendarDay(DATE, true, false, false);

        assertThat(day.isInMonth()).isTrue();
        assertThat(day.isPrevMonth()).isFalse();
        assertThat(day.isNextMonth()).isFalse();
    }

    @Test
    void leadingPaddingDayIsPrevMonth() {
        CalendarDay day = new CalendarDay(DATE, false, false, true);

        assertThat(day.isInMonth()).isFalse();
        assertThat(day.isPrevMonth()).isTrue();
        assertThat(day.isNextMonth()).isFalse();
    }

    @Test
    void trailingPaddingDayIsNextMonth() {
        CalendarDay day = new CalendarDay(DATE, false, false, false);

        assertThat(day.isInMonth()).isFalse();
        assertThat(day.isPrevMonth()).isFalse();
        assertThat(day.isNextMonth()).isTrue();
    }

    @Test
    void beforeMonthFlagIsIgnoredWhenDayIsInMonth() {
        // Even if beforeMonth were somehow true, an in-month day should never
        // report itself as padding from the previous month.
        CalendarDay day = new CalendarDay(DATE, true, false, true);

        assertThat(day.isPrevMonth()).isFalse();
        assertThat(day.isNextMonth()).isFalse();
    }

    @Test
    void getDayNumberReturnsDayOfMonth() {
        CalendarDay day = new CalendarDay(DATE, true, false, false);

        assertThat(day.getDayNumber()).isEqualTo(15);
        assertThat(day.getDate()).isEqualTo(DATE);
    }

    @Test
    void isTodayReflectsConstructorFlag() {
        assertThat(new CalendarDay(DATE, true, true, false).isToday()).isTrue();
        assertThat(new CalendarDay(DATE, true, false, false).isToday()).isFalse();
    }

    @Test
    void hasEventsIsFalseUntilEventsAreAdded() {
        CalendarDay day = new CalendarDay(DATE, true, false, false);
        assertThat(day.isHasEvents()).isFalse();
        assertThat(day.getEvents()).isEmpty();

        day.getEvents().add(new CalendarEvent("1", "Evento", null, null, null, null, false));

        assertThat(day.isHasEvents()).isTrue();
        assertThat(day.getEvents()).hasSize(1);
    }
}
