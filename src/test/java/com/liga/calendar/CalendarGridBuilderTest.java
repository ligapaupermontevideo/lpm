package com.liga.calendar;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarGridBuilderTest {

    @Test
    void gridRangeAlwaysStartsOnMondayAndEndsOnSunday() {
        for (int m = 1; m <= 12; m++) {
            YearMonth ym = YearMonth.of(2026, m);
            LocalDate[] range = CalendarGridBuilder.gridRange(ym);

            assertThat(range[0].getDayOfWeek()).as("start of %s", ym).isEqualTo(DayOfWeek.MONDAY);
            assertThat(range[1].getDayOfWeek()).as("end of %s", ym).isEqualTo(DayOfWeek.SUNDAY);
            assertThat(range[0]).isBeforeOrEqualTo(ym.atDay(1));
            assertThat(range[1]).isAfterOrEqualTo(ym.atEndOfMonth());
            assertThat(ChronoUnit.DAYS.between(range[0], range[1]) + 1).isEqualTo(
                    (long) Math.ceil((ym.atDay(1).getDayOfWeek().getValue() - 1 + ym.lengthOfMonth()) / 7.0) * 7);
        }
    }

    @Test
    void buildMonthLabelIsCapitalizedSpanishMonthAndYear() {
        CalendarMonthView month = CalendarGridBuilder.buildMonth(YearMonth.of(2026, 10), List.of());

        assertThat(month.getLabel()).isEqualTo("Octubre 2026");
    }

    @Test
    void buildMonthProducesFullWeeksOfSevenDays() {
        YearMonth ym = YearMonth.of(2026, 10);
        CalendarMonthView month = CalendarGridBuilder.buildMonth(ym, List.of());

        assertThat(month.getWeeks()).isNotEmpty();
        for (List<CalendarDay> week : month.getWeeks()) {
            assertThat(week).hasSize(7);
        }
        assertThat(flatten(month)).hasSize(month.getWeeks().size() * 7);
    }

    @Test
    void buildMonthFlagsDaysAsInMonthPrevOrNextConsistently() {
        YearMonth ym = YearMonth.of(2026, 10);
        LocalDate first = ym.atDay(1);
        LocalDate last = ym.atEndOfMonth();

        CalendarMonthView month = CalendarGridBuilder.buildMonth(ym, List.of());

        for (CalendarDay day : flatten(month)) {
            boolean expectedInMonth = !day.getDate().isBefore(first) && !day.getDate().isAfter(last);
            assertThat(day.isInMonth()).as("date %s", day.getDate()).isEqualTo(expectedInMonth);

            if (expectedInMonth) {
                assertThat(day.isPrevMonth()).isFalse();
                assertThat(day.isNextMonth()).isFalse();
            } else {
                assertThat(day.isPrevMonth()).isEqualTo(day.getDate().isBefore(first));
                assertThat(day.isNextMonth()).isEqualTo(day.getDate().isAfter(last));
            }
        }
    }

    @Test
    void buildMonthMarksOnlyTodayWhenPresentInTheGrid() {
        YearMonth currentMonth = YearMonth.now();
        LocalDate today = LocalDate.now();

        CalendarMonthView month = CalendarGridBuilder.buildMonth(currentMonth, List.of());

        List<CalendarDay> todays = flatten(month).stream().filter(CalendarDay::isToday).toList();
        assertThat(todays).hasSize(1);
        assertThat(todays.get(0).getDate()).isEqualTo(today);

        // A month that can never contain "today" should mark nothing as today.
        CalendarMonthView farFuture = CalendarGridBuilder.buildMonth(currentMonth.plusYears(5), List.of());
        assertThat(flatten(farFuture)).noneMatch(CalendarDay::isToday);
    }

    @Test
    void buildMonthPlacesSingleDayEventOnlyOnItsDate() {
        YearMonth ym = YearMonth.of(2026, 10);
        CalendarEvent event = new CalendarEvent("1", "Fecha", null, null,
                LocalDateTime.of(2026, 10, 15, 19, 30), LocalDateTime.of(2026, 10, 15, 23, 30), false);

        CalendarMonthView month = CalendarGridBuilder.buildMonth(ym, List.of(event));

        for (CalendarDay day : flatten(month)) {
            if (day.getDate().equals(LocalDate.of(2026, 10, 15))) {
                assertThat(day.getEvents()).containsExactly(event);
            } else {
                assertThat(day.getEvents()).isEmpty();
            }
        }
    }

    @Test
    void buildMonthPlacesMultiDayEventOnEveryDayItSpans() {
        YearMonth ym = YearMonth.of(2026, 10);
        // All-day event Oct 20 - Oct 22 inclusive (Google exclusive end = Oct 23).
        CalendarEvent event = new CalendarEvent("1", "Torneo largo", null, null,
                LocalDateTime.of(2026, 10, 20, 0, 0), LocalDateTime.of(2026, 10, 23, 0, 0), true);

        CalendarMonthView month = CalendarGridBuilder.buildMonth(ym, List.of(event));

        List<LocalDate> spanned = List.of(
                LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 21), LocalDate.of(2026, 10, 22));

        for (CalendarDay day : flatten(month)) {
            if (spanned.contains(day.getDate())) {
                assertThat(day.getEvents()).as("date %s", day.getDate()).containsExactly(event);
            } else {
                assertThat(day.getEvents()).as("date %s", day.getDate()).isEmpty();
            }
        }
    }

    @Test
    void buildMonthIgnoresEventsOutsideTheDisplayedGrid() {
        YearMonth ym = YearMonth.of(2026, 10);
        CalendarEvent farAway = new CalendarEvent("1", "Otro mes", null, null,
                LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 1, 2, 0, 0), true);

        CalendarMonthView month = CalendarGridBuilder.buildMonth(ym, List.of(farAway));

        assertThat(flatten(month)).allMatch(day -> day.getEvents().isEmpty());
    }

    @Test
    void buildMonthHandlesMultipleEventsOnTheSameDay() {
        YearMonth ym = YearMonth.of(2026, 10);
        CalendarEvent first = new CalendarEvent("1", "Torneo A", null, null,
                LocalDateTime.of(2026, 10, 15, 10, 0), LocalDateTime.of(2026, 10, 15, 12, 0), false);
        CalendarEvent second = new CalendarEvent("2", "Torneo B", null, null,
                LocalDateTime.of(2026, 10, 15, 19, 0), LocalDateTime.of(2026, 10, 15, 22, 0), false);

        CalendarMonthView month = CalendarGridBuilder.buildMonth(ym, new ArrayList<>(List.of(first, second)));

        CalendarDay day15 = flatten(month).stream()
                .filter(d -> d.getDate().equals(LocalDate.of(2026, 10, 15)))
                .findFirst().orElseThrow();

        assertThat(day15.getEvents()).containsExactly(first, second);
    }

    private static List<CalendarDay> flatten(CalendarMonthView month) {
        List<CalendarDay> all = new ArrayList<>();
        for (List<CalendarDay> week : month.getWeeks()) {
            all.addAll(week);
        }
        return all;
    }
}
