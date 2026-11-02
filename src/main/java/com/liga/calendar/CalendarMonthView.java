package com.liga.calendar;

import java.util.ArrayList;
import java.util.List;

public class CalendarMonthView {

    private final String label;
    private final List<List<CalendarDay>> weeks = new ArrayList<>();

    public CalendarMonthView(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
    public List<List<CalendarDay>> getWeeks() { return weeks; }
}
