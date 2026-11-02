package com.liga.calendar;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CalendarDay {

    private final LocalDate date;
    private final boolean inMonth;
    private final boolean today;
    private final boolean beforeMonth;
    private final List<CalendarEvent> events = new ArrayList<>();

    public CalendarDay(LocalDate date, boolean inMonth, boolean today, boolean beforeMonth) {
        this.date = date;
        this.inMonth = inMonth;
        this.today = today;
        this.beforeMonth = beforeMonth;
    }

    public LocalDate getDate() { return date; }
    public int getDayNumber() { return date.getDayOfMonth(); }
    public boolean isInMonth() { return inMonth; }
    public boolean isToday() { return today; }

    /** Leading padding day, borrowed from the previous month. */
    public boolean isPrevMonth() { return !inMonth && beforeMonth; }

    /** Trailing padding day, borrowed from the next month. */
    public boolean isNextMonth() { return !inMonth && !beforeMonth; }

    public List<CalendarEvent> getEvents() { return events; }
    public boolean isHasEvents() { return !events.isEmpty(); }
}
