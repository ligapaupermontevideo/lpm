package com.liga.calendar;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarMonthViewTest {

    @Test
    void startsWithGivenLabelAndNoWeeks() {
        CalendarMonthView month = new CalendarMonthView("Octubre 2026");

        assertThat(month.getLabel()).isEqualTo("Octubre 2026");
        assertThat(month.getWeeks()).isEmpty();
    }

    @Test
    void weeksListIsMutable() {
        CalendarMonthView month = new CalendarMonthView("Octubre 2026");
        List<CalendarDay> week = new ArrayList<>();

        month.getWeeks().add(week);

        assertThat(month.getWeeks()).hasSize(1).containsExactly(week);
    }
}
