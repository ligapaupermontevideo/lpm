package com.liga.calendar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
        // index() fetches twice: once for the displayed grid range, and once
        // (inside buildSignupUrls/registrableEventsByDate) over a 12-month
        // horizon to find the nearest registrable week — only the first
        // call's range is this test's concern.
        verify(calendarService, org.mockito.Mockito.atLeastOnce()).getEvents(fromCaptor.capture(), toCaptor.capture());

        LocalDate[] expected = CalendarGridBuilder.gridRange(YearMonth.of(2026, 10));
        assertThat(fromCaptor.getAllValues()).contains(expected[0]);
        assertThat(toCaptor.getAllValues()).contains(expected[1]);
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
    void indexBuildsASignupLinkForAnUpcomingSingleDayEvent() {
        CalendarEvent event = new CalendarEvent("1", "Fecha Oficial", null, null,
                LocalDateTime.of(2026, 10, 15, 19, 30),
                LocalDateTime.of(2026, 10, 15, 23, 30), false);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of(event));
        CalendarController controller = controller(fixedClockAt(LocalDate.of(2026, 10, 1)));
        Model model = new ConcurrentModel();

        controller.index(2026, 10, model);

        @SuppressWarnings("unchecked")
        Map<LocalDate, String> signupUrls = (Map<LocalDate, String>) model.getAttribute("signupUrls");
        assertThat(signupUrls).containsEntry(LocalDate.of(2026, 10, 15), "/calendario/anotarse?fecha=2026-10-15");
    }

    @Test
    void indexOmitsSignupLinksForMultiDaySeasonReferenceEvents() {
        CalendarEvent season = new CalendarEvent("1", "Temporada Primavera 26", null, null,
                LocalDateTime.of(2026, 10, 1, 0, 0),
                LocalDateTime.of(2026, 11, 1, 0, 0), true);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of(season));
        CalendarController controller = controller(fixedClockAt(LocalDate.of(2026, 10, 1)));
        Model model = new ConcurrentModel();

        controller.index(2026, 10, model);

        @SuppressWarnings("unchecked")
        Map<LocalDate, String> signupUrls = (Map<LocalDate, String>) model.getAttribute("signupUrls");
        assertThat(signupUrls).isEmpty();
    }

    @Test
    void indexOmitsSignupLinksForDatesThatHaveAlreadyPassed() {
        // 2026-10-10 is a Saturday, so its Mon-Sun week is 10-05..10-11 —
        // 10-15 falls in the following week and is left out by the
        // nearest-week restriction, not just by it being "in the past".
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
        Map<LocalDate, String> signupUrls = (Map<LocalDate, String>) model.getAttribute("signupUrls");
        assertThat(signupUrls).doesNotContainKey(LocalDate.of(2026, 10, 5));
        assertThat(signupUrls).containsKey(LocalDate.of(2026, 10, 10));
        assertThat(signupUrls).doesNotContainKey(LocalDate.of(2026, 10, 15));
    }

    @Test
    void signupUrlsOnlyIncludeTheNearestRegistrableWeek() {
        // Monday 2026-11-02 through Sunday 2026-11-08 is "this week"; the
        // 11th and 20th fall in later weeks and should be left out.
        LocalDate thisWeekDate = LocalDate.of(2026, 11, 4);
        LocalDate nextWeekDate = LocalDate.of(2026, 11, 11);
        LocalDate laterDate = LocalDate.of(2026, 11, 20);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of(
                officialDate("1", thisWeekDate), officialDate("2", nextWeekDate), officialDate("3", laterDate)));
        CalendarController controller = controller(fixedClockAt(LocalDate.of(2026, 11, 2)));
        Model model = new ConcurrentModel();

        controller.index(2026, 11, model);

        @SuppressWarnings("unchecked")
        Map<LocalDate, String> signupUrls = (Map<LocalDate, String>) model.getAttribute("signupUrls");
        assertThat(signupUrls.keySet()).containsExactly(thisWeekDate);
    }

    @Test
    void signupUrlsRollOverToNextWeekOnceThisWeekHasNoneLeft() {
        LocalDate pastThisWeek = LocalDate.of(2026, 11, 4);
        LocalDate nextWeekDate = LocalDate.of(2026, 11, 11);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of(
                officialDate("1", pastThisWeek), officialDate("2", nextWeekDate)));
        // A Sunday, after this week's only date has already passed.
        CalendarController controller = controller(fixedClockAt(LocalDate.of(2026, 11, 8)));
        Model model = new ConcurrentModel();

        controller.index(2026, 11, model);

        @SuppressWarnings("unchecked")
        Map<LocalDate, String> signupUrls = (Map<LocalDate, String>) model.getAttribute("signupUrls");
        assertThat(signupUrls.keySet()).containsExactly(nextWeekDate);
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
        CalendarController controller = new CalendarController(calendarService, new SectionAccess(properties),
                "59800000000", Clock.systemDefaultZone(), new EventSignupStore());
        Model model = new ConcurrentModel();

        assertThatThrownBy(() -> controller.index(null, null, model))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void signupFormExposesEveryUpcomingSingleDayOfficialDate() {
        CalendarEvent event = new CalendarEvent("1", "Fecha Oficial", null, null,
                LocalDateTime.of(2026, 10, 15, 19, 30),
                LocalDateTime.of(2026, 10, 15, 23, 30), false);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of(event));
        CalendarController controller = controller(fixedClockAt(LocalDate.of(2026, 10, 1)));
        Model model = new ConcurrentModel();

        String view = controller.signupForm(null, model);

        assertThat(view).isEqualTo("anotarse");
        @SuppressWarnings("unchecked")
        List<CalendarController.DateOption> dates =
                (List<CalendarController.DateOption>) model.getAttribute("availableDates");
        assertThat(dates).extracting(CalendarController.DateOption::value).containsExactly(LocalDate.of(2026, 10, 15));
    }

    @Test
    void signupFormOnlyIncludesTheNearestRegistrableWeek() {
        LocalDate thisWeekDate = LocalDate.of(2026, 11, 4);
        LocalDate nextWeekDate = LocalDate.of(2026, 11, 11);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of(
                officialDate("1", thisWeekDate), officialDate("2", nextWeekDate)));
        CalendarController controller = controller(fixedClockAt(LocalDate.of(2026, 11, 2)));
        Model model = new ConcurrentModel();

        controller.signupForm(null, model);

        assertThat(signupDateValues(model)).containsExactly(thisWeekDate);
    }

    @Test
    void sameDayDateIsRegistrableBeforeTheCutoff() {
        LocalDate sameDay = LocalDate.of(2026, 11, 14);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of(officialDate("1", sameDay)));
        CalendarController controller = controller(fixedClockAt(sameDay, LocalTime.of(16, 59)));
        Model model = new ConcurrentModel();

        controller.signupForm(null, model);

        assertThat(signupDateValues(model)).containsExactly(sameDay);
    }

    @Test
    void sameDayDateIsNotRegistrableAfterTheCutoff() {
        LocalDate sameDay = LocalDate.of(2026, 11, 14);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of(officialDate("1", sameDay)));
        CalendarController controller = controller(fixedClockAt(sameDay, LocalTime.of(17, 0)));
        Model model = new ConcurrentModel();

        controller.signupForm(null, model);

        assertThat(signupDateValues(model)).isEmpty();
    }

    @Test
    void signupFormExposesTheRequestedDateForPreselection() {
        when(calendarService.getEvents(any(), any())).thenReturn(List.of());
        CalendarController controller = controller();
        Model model = new ConcurrentModel();
        LocalDate fecha = LocalDate.of(2026, 10, 15);

        controller.signupForm(fecha, model);

        assertThat(model.getAttribute("selectedDate")).isEqualTo(fecha);
    }

    @Test
    void signupFormIsNotFoundWhenTheSectionIsDisabled() {
        SectionProperties properties = new SectionProperties();
        properties.setEnabled(Map.of("calendario", false));
        CalendarController controller = new CalendarController(calendarService, new SectionAccess(properties),
                "59800000000", Clock.systemDefaultZone(), new EventSignupStore());

        assertThatThrownBy(() -> controller.signupForm(null, new ConcurrentModel()))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void signUpWithValidDataCreatesSignupAndRedirectsWithConfirmation() {
        CalendarEvent event = new CalendarEvent("1", "Fecha Oficial", null, null,
                LocalDateTime.of(2026, 10, 15, 19, 30),
                LocalDateTime.of(2026, 10, 15, 23, 30), false);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of(event));
        EventSignupStore store = new EventSignupStore();
        CalendarController controller = controllerWithStore(store, fixedClockAt(LocalDate.of(2026, 10, 1)));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        LocalDate fecha = LocalDate.of(2026, 10, 15);

        String view = controller.signUp("1.234.567-2", "Juan Perez", fecha, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/calendario/anotarse?fecha=" + fecha);
        assertThat(store.all()).hasSize(1);
        assertThat(store.all().get(0).cedula()).isEqualTo("12345672");
        assertThat(redirectAttributes.getFlashAttributes().get("confirmedDate")).isNotNull();
        assertThat((String) redirectAttributes.getFlashAttributes().get("whatsappUrl"))
                .startsWith("https://wa.me/59800000000?text=")
                .contains("Fecha+Oficial");
        assertThat(redirectAttributes.getFlashAttributes()).doesNotContainKey("errorMessage");
    }

    @Test
    void signUpRejectsBlankName() {
        EventSignupStore store = new EventSignupStore();
        CalendarController controller = controllerWithStore(store, Clock.systemDefaultZone());
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        controller.signUp("1.234.567-2", "  ", LocalDate.of(2026, 10, 15), redirectAttributes);

        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage")).isNotNull();
        assertThat(store.all()).isEmpty();
    }

    @Test
    void signUpRejectsInvalidCedula() {
        EventSignupStore store = new EventSignupStore();
        CalendarController controller = controllerWithStore(store, Clock.systemDefaultZone());
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        controller.signUp("1.234.567-9", "Juan Perez", LocalDate.of(2026, 10, 15), redirectAttributes);

        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage")).isNotNull();
        assertThat(store.all()).isEmpty();
    }

    @Test
    void signUpRejectsDateThatIsNoLongerRegistrable() {
        when(calendarService.getEvents(any(), any())).thenReturn(List.of());
        EventSignupStore store = new EventSignupStore();
        CalendarController controller = controllerWithStore(store, Clock.systemDefaultZone());
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        controller.signUp("1.234.567-2", "Juan Perez", LocalDate.of(2026, 10, 15), redirectAttributes);

        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage")).isNotNull();
        assertThat(store.all()).isEmpty();
    }

    @Test
    void signUpAllowsSameDayBeforeTheCutoff() {
        LocalDate sameDay = LocalDate.of(2026, 11, 14);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of(officialDate("1", sameDay)));
        EventSignupStore store = new EventSignupStore();
        CalendarController controller = controllerWithStore(store, fixedClockAt(sameDay, LocalTime.of(16, 59)));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.signUp("1.234.567-2", "Juan Perez", sameDay, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/calendario/anotarse?fecha=" + sameDay);
        assertThat(store.all()).hasSize(1);
    }

    @Test
    void signUpRejectsSameDayAtOrAfterTheCutoff() {
        LocalDate sameDay = LocalDate.of(2026, 11, 14);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of(officialDate("1", sameDay)));
        EventSignupStore store = new EventSignupStore();
        CalendarController controller = controllerWithStore(store, fixedClockAt(sameDay, LocalTime.of(17, 0)));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        controller.signUp("1.234.567-2", "Juan Perez", sameDay, redirectAttributes);

        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage")).isNotNull();
        assertThat(store.all()).isEmpty();
    }

    @Test
    void signUpRejectsDateOutsideTheNearestWeek() {
        LocalDate thisWeekDate = LocalDate.of(2026, 11, 4);
        LocalDate nextWeekDate = LocalDate.of(2026, 11, 11);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of(
                officialDate("1", thisWeekDate), officialDate("2", nextWeekDate)));
        EventSignupStore store = new EventSignupStore();
        CalendarController controller = controllerWithStore(store, fixedClockAt(LocalDate.of(2026, 11, 2)));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        controller.signUp("1.234.567-2", "Juan Perez", nextWeekDate, redirectAttributes);

        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage")).isNotNull();
        assertThat(store.all()).isEmpty();
    }

    @Test
    void signUpRejectsSecondSignupBySamePersonOnSameDate() {
        CalendarEvent event = new CalendarEvent("1", "Fecha Oficial", null, null,
                LocalDateTime.of(2026, 10, 15, 19, 30),
                LocalDateTime.of(2026, 10, 15, 23, 30), false);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of(event));
        EventSignupStore store = new EventSignupStore();
        CalendarController controller = controllerWithStore(store, fixedClockAt(LocalDate.of(2026, 10, 1)));
        LocalDate fecha = LocalDate.of(2026, 10, 15);
        controller.signUp("1.234.567-2", "Juan Perez", fecha, new RedirectAttributesModelMap());
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        controller.signUp("1.234.567-2", "Juan Perez", fecha, redirectAttributes);

        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage")).isEqualTo("Ya estas anotado para esa fecha.");
        assertThat(store.all()).hasSize(1);
    }

    @Test
    void signUpIsRejectedWhenTheSectionIsDisabled() {
        SectionProperties properties = new SectionProperties();
        properties.setEnabled(Map.of("calendario", false));
        EventSignupStore store = new EventSignupStore();
        CalendarController controller = new CalendarController(calendarService, new SectionAccess(properties),
                "59800000000", Clock.systemDefaultZone(), store);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        assertThatThrownBy(() -> controller.signUp("1.234.567-2", "Juan Perez", LocalDate.of(2026, 10, 15), redirectAttributes))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(store.all()).isEmpty();
    }

    private CalendarController controller() {
        return controller(Clock.systemDefaultZone());
    }

    private CalendarController controller(Clock clock) {
        return controllerWithStore(new EventSignupStore(), clock);
    }

    private CalendarController controllerWithStore(EventSignupStore store, Clock clock) {
        return new CalendarController(calendarService, new SectionAccess(new SectionProperties()), "59800000000", clock, store);
    }

    private Clock fixedClockAt(LocalDate date) {
        ZoneId zone = ZoneId.of("America/Montevideo");
        return Clock.fixed(date.atStartOfDay(zone).toInstant(), zone);
    }

    private Clock fixedClockAt(LocalDate date, LocalTime time) {
        ZoneId zone = ZoneId.of("America/Montevideo");
        return Clock.fixed(LocalDateTime.of(date, time).atZone(zone).toInstant(), zone);
    }

    private CalendarEvent officialDate(String id, LocalDate date) {
        return new CalendarEvent(id, "Fecha Oficial", null, null,
                date.atTime(19, 0), date.atTime(23, 0), false);
    }

    private List<LocalDate> signupDateValues(Model model) {
        @SuppressWarnings("unchecked")
        List<CalendarController.DateOption> dates =
                (List<CalendarController.DateOption>) model.getAttribute("availableDates");
        return dates.stream().map(CalendarController.DateOption::value).toList();
    }
}
