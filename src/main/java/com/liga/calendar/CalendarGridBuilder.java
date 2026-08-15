package com.liga.calendar;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds a single month-grid view (weeks of 7 days, Monday first) from a flat
 * list of events, so the page can render a real calendar layout instead of a list.
 */
public final class CalendarGridBuilder {

    private static final Locale ES = new Locale("es", "UY");

    private CalendarGridBuilder() {}

    /**
     * The full range of dates shown on the grid for a given month, including
     * the leading/trailing days borrowed from the adjacent months to fill
     * out whole weeks. Used both to build the grid and to know what range of
     * events to fetch for it.
     */
    public static LocalDate[] gridRange(YearMonth ym) {
        LocalDate firstOfMonth = ym.atDay(1);
        LocalDate lastOfMonth = ym.atEndOfMonth();

        LocalDate gridStart = firstOfMonth.minusDays(firstOfMonth.getDayOfWeek().getValue() - 1);
        LocalDate gridEnd = lastOfMonth.plusDays(DayOfWeek.SUNDAY.getValue() - lastOfMonth.getDayOfWeek().getValue());

        return new LocalDate[] { gridStart, gridEnd };
    }

    public static CalendarMonthView buildMonth(YearMonth ym, List<CalendarEvent> events) {
        LocalDate today = LocalDate.now();
        Map<LocalDate, List<CalendarEvent>> eventsByDay = groupByDay(events);

        String monthName = ym.getMonth().getDisplayName(TextStyle.FULL, ES);
        CalendarMonthView month = new CalendarMonthView(capitalize(monthName) + " " + ym.getYear());

        LocalDate[] range = gridRange(ym);
        LocalDate gridStart = range[0];
        LocalDate gridEnd = range[1];
        LocalDate firstOfMonth = ym.atDay(1);

        List<CalendarDay> week = new ArrayList<>();
        for (LocalDate d = gridStart; !d.isAfter(gridEnd); d = d.plusDays(1)) {
            boolean inMonth = YearMonth.from(d).equals(ym);
            boolean beforeMonth = d.isBefore(firstOfMonth);
            CalendarDay day = new CalendarDay(d, inMonth, d.equals(today), beforeMonth);
            List<CalendarEvent> dayEvents = eventsByDay.get(d);
            if (dayEvents != null) {
                day.getEvents().addAll(dayEvents);
            }
            week.add(day);
            if (week.size() == 7) {
                month.getWeeks().add(week);
                week = new ArrayList<>();
            }
        }
        return month;
    }

    private static Map<LocalDate, List<CalendarEvent>> groupByDay(List<CalendarEvent> events) {
        Map<LocalDate, List<CalendarEvent>> eventsByDay = new LinkedHashMap<>();
        for (CalendarEvent event : events) {
            if (event.getStart() == null) continue;
            eventsByDay
                    .computeIfAbsent(event.getStart().toLocalDate(), d -> new ArrayList<>())
                    .add(event);
        }
        return eventsByDay;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
