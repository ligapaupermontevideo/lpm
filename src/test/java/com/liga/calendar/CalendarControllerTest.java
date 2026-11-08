package com.liga.calendar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        CalendarController controller = controller();
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
        CalendarController controller = controller();
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
        CalendarController controller = controller();
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
        CalendarController controller = controller();
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
        CalendarController controller = controller();
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
                LocalDateTime.of(2026, 10, 15, 19, 30),
                LocalDateTime.of(2026, 10, 15, 23, 30), false);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of(event));
        CalendarController controller = controller();
        Model model = new ConcurrentModel();

        controller.index(2026, 10, model);

        CalendarMonthView month = (CalendarMonthView) model.getAttribute("month");
        boolean found = month.getWeeks().stream()
                .flatMap(List::stream)
                .anyMatch(day -> day.getEvents().contains(event));
        assertThat(found).isTrue();
    }

    @Test
    void indexBuildsARegistrationWhatsAppUrlForAnUpcomingSingleDayEvent() {
        CalendarEvent event = new CalendarEvent("1", "Fecha Oficial", null, null,
                LocalDateTime.of(2026, 10, 15, 19, 30),
                LocalDateTime.of(2026, 10, 15, 23, 30), false);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of(event));
        CalendarController controller = controller(fixedClockAt(LocalDate.of(2026, 10, 1)));
        Model model = new ConcurrentModel();

        controller.index(2026, 10, model);

        @SuppressWarnings("unchecked")
        Map<LocalDate, String> registrationUrls = (Map<LocalDate, String>) model.getAttribute("registrationUrls");
        assertThat(registrationUrls).containsKey(LocalDate.of(2026, 10, 15));
        assertThat(registrationUrls.get(LocalDate.of(2026, 10, 15)))
                .startsWith("https://wa.me/59800000000?text=")
                .contains("Me+anoto+para+jugar+esta+fecha+de+la+liga")
                .contains("Fecha+Oficial")
                .contains("15%2F10%2F2026");
    }

    @Test
    void indexOmitsRegistrationForMultiDaySeasonReferenceEvents() {
        CalendarEvent season = new CalendarEvent("1", "Temporada Primavera 26", null, null,
                LocalDateTime.of(2026, 10, 1, 0, 0),
                LocalDateTime.of(2026, 11, 1, 0, 0), true);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of(season));
        CalendarController controller = controller(fixedClockAt(LocalDate.of(2026, 10, 1)));
        Model model = new ConcurrentModel();

        controller.index(2026, 10, model);

        @SuppressWarnings("unchecked")
        Map<LocalDate, String> registrationUrls = (Map<LocalDate, String>) model.getAttribute("registrationUrls");
        assertThat(registrationUrls).isEmpty();
    }

    @Test
    void indexOmitsRegistrationForDatesThatHaveAlreadyPassed() {
        CalendarEvent past = new CalendarEvent("1", "Fecha Oficial", null, null,
                LocalDateTime.of(2026, 10, 5, 19, 0),
                LocalDateTime.of(2026, 10, 5, 23, 0), false);
        CalendarEvent today = new CalendarEvent("2", "Fecha Oficial", null, null,
                LocalDateTime.of(2026, 10, 10, 19, 0),
                LocalDateTime.of(2026, 10, 10, 23, 0), false);
        CalendarEvent future = new CalendarEvent("3", "Fecha Oficial", null, null,
                LocalDateTime.of(2026, 10, 15, 19, 0),
                LocalDateTime.of(2026, 10, 15, 23, 0), false);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of(past, today, future));
        CalendarController controller = controller(fixedClockAt(LocalDate.of(2026, 10, 10)));
        Model model = new ConcurrentModel();

        controller.index(2026, 10, model);

        @SuppressWarnings("unchecked")
        Map<LocalDate, String> registrationUrls = (Map<LocalDate, String>) model.getAttribute("registrationUrls");
        assertThat(registrationUrls).doesNotContainKey(LocalDate.of(2026, 10, 5));
        assertThat(registrationUrls).containsKey(LocalDate.of(2026, 10, 10));
        assertThat(registrationUrls).containsKey(LocalDate.of(2026, 10, 15));
    }

    @Test
    void indexFlagsTheMonthAsHavingEventsOnlyWhenAnInMonthDayHasOne() {
        CalendarEvent event = new CalendarEvent("1", "Fecha Oficial", null, null,
                LocalDateTime.of(2026, 10, 15, 19, 0),
                LocalDateTime.of(2026, 10, 15, 23, 0), false);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of(event));
        CalendarController controller = controller();
        Model model = new ConcurrentModel();

        controller.index(2026, 10, model);

        assertThat(model.getAttribute("hasEventsThisMonth")).isEqualTo(true);
    }

    @Test
    void indexFlagsTheMonthAsHavingNoEventsWhenNoneAreFetched() {
        when(calendarService.getEvents(any(), any())).thenReturn(List.of());
        CalendarController controller = controller();
        Model model = new ConcurrentModel();

        controller.index(2026, 10, model);

        assertThat(model.getAttribute("hasEventsThisMonth")).isEqualTo(false);
    }

    @Test
    void indexIsNotFoundWhenTheSectionIsDisabled() {
        SectionProperties properties = new SectionProperties();
        properties.setEnabled(Map.of("calendario", false));
        CalendarController controller = new CalendarController(
                calendarService, new SectionAccess(properties), "59800000000", Clock.systemDefaultZone());
        Model model = new ConcurrentModel();

        assertThatThrownBy(() -> controller.index(null, null, model))
                .isInstanceOf(ResponseStatusException.class);
    }

    private CalendarController controller() {
        return controller(Clock.systemDefaultZone());
    }

    private CalendarController controller(Clock clock) {
        return new CalendarController(calendarService, new SectionAccess(new SectionProperties()), "59800000000", clock);
    }

    private Clock fixedClockAt(LocalDate date) {
        ZoneId zone = ZoneId.of("America/Montevideo");
        return Clock.fixed(date.atStartOfDay(zone).toInstant(), zone);
    }
}
