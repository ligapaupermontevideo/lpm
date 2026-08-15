package com.liga.calendar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarControllerTest {

    @Mock
    private GoogleCalendarService calendarService;

    @Test
    void indexWithoutParamsUsesCurrentMonth() {
        when(calendarService.getEvents(any(), any())).thenReturn(List.of());
        CalendarController controller = new CalendarController(calendarService);
        Model model = new ConcurrentModel();

        String view = controller.index(null, null, model);

        YearMonth now = YearMonth.now();
        assertThat(view).isEqualTo("index");
        assertThat(((CalendarMonthView) model.getAttribute("month")).getLabel())
                .isEqualTo(CalendarGridBuilder.buildMonth(now, List.of()).getLabel());
        assertThat(model.getAttribute("prevYear")).isEqualTo(now.minusMonths(1).getYear());
        assertThat(model.getAttribute("prevMonth")).isEqualTo(now.minusMonths(1).getMonthValue());
        assertThat(model.getAttribute("nextYear")).isEqualTo(now.plusMonths(1).getYear());
        assertThat(model.getAttribute("nextMonth")).isEqualTo(now.plusMonths(1).getMonthValue());
    }

    @Test
    void indexWithExplicitYearAndMonthUsesThatMonth() {
        when(calendarService.getEvents(any(), any())).thenReturn(List.of());
        CalendarController controller = new CalendarController(calendarService);
        Model model = new ConcurrentModel();

        controller.index(2026, 10, model);

        assertThat(((CalendarMonthView) model.getAttribute("month")).getLabel()).isEqualTo("Octubre 2026");
        assertThat(model.getAttribute("prevYear")).isEqualTo(2026);
        assertThat(model.getAttribute("prevMonth")).isEqualTo(9);
        assertThat(model.getAttribute("nextYear")).isEqualTo(2026);
        assertThat(model.getAttribute("nextMonth")).isEqualTo(11);
    }

    @Test
    void indexRollsOverToNextYearInDecember() {
        when(calendarService.getEvents(any(), any())).thenReturn(List.of());
        CalendarController controller = new CalendarController(calendarService);
        Model model = new ConcurrentModel();

        controller.index(2026, 12, model);

        assertThat(model.getAttribute("prevYear")).isEqualTo(2026);
        assertThat(model.getAttribute("prevMonth")).isEqualTo(11);
        assertThat(model.getAttribute("nextYear")).isEqualTo(2027);
        assertThat(model.getAttribute("nextMonth")).isEqualTo(1);
    }

    @Test
    void indexRollsOverToPreviousYearInJanuary() {
        when(calendarService.getEvents(any(), any())).thenReturn(List.of());
        CalendarController controller = new CalendarController(calendarService);
        Model model = new ConcurrentModel();

        controller.index(2026, 1, model);

        assertThat(model.getAttribute("prevYear")).isEqualTo(2025);
        assertThat(model.getAttribute("prevMonth")).isEqualTo(12);
        assertThat(model.getAttribute("nextYear")).isEqualTo(2026);
        assertThat(model.getAttribute("nextMonth")).isEqualTo(2);
    }

    @Test
    void indexFetchesEventsForTheDisplayedGridRange() {
        when(calendarService.getEvents(any(), any())).thenReturn(List.of());
        CalendarController controller = new CalendarController(calendarService);
        Model model = new ConcurrentModel();

        controller.index(2026, 10, model);

        ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> toCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(calendarService).getEvents(fromCaptor.capture(), toCaptor.capture());

        LocalDate[] expected = CalendarGridBuilder.gridRange(YearMonth.of(2026, 10));
        assertThat(fromCaptor.getValue()).isEqualTo(expected[0]);
        assertThat(toCaptor.getValue()).isEqualTo(expected[1]);
    }

    @Test
    void indexPlacesFetchedEventsIntoTheMonthView() {
        CalendarEvent event = new CalendarEvent("1", "Fecha", null, null,
                java.time.LocalDateTime.of(2026, 10, 15, 19, 30),
                java.time.LocalDateTime.of(2026, 10, 15, 23, 30), false);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of(event));
        CalendarController controller = new CalendarController(calendarService);
        Model model = new ConcurrentModel();

        controller.index(2026, 10, model);

        CalendarMonthView month = (CalendarMonthView) model.getAttribute("month");
        boolean found = month.getWeeks().stream()
                .flatMap(List::stream)
                .anyMatch(day -> day.getEvents().contains(event));
        assertThat(found).isTrue();
    }
}
