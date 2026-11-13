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
import java.util.Set;

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
    void signupUrlsIncludeAPinnedDateEvenOutsideTheNearestWeek() {
        // Same setup as signupUrlsOnlyIncludeTheNearestRegistrableWeek, but
        // the "later" date now carries the [Especial] marker, so it should show
        // up alongside the nearest week instead of being filtered out.
        LocalDate thisWeekDate = LocalDate.of(2026, 11, 4);
        LocalDate nextWeekDate = LocalDate.of(2026, 11, 11);
        LocalDate pinnedDate = LocalDate.of(2026, 11, 20);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of(
                officialDate("1", thisWeekDate), officialDate("2", nextWeekDate), pinnedDate("3", pinnedDate)));
        CalendarController controller = controller(fixedClockAt(LocalDate.of(2026, 11, 2)));
        Model model = new ConcurrentModel();

        controller.index(2026, 11, model);

        @SuppressWarnings("unchecked")
        Map<LocalDate, String> signupUrls = (Map<LocalDate, String>) model.getAttribute("signupUrls");
        assertThat(signupUrls.keySet()).containsExactlyInAnyOrder(thisWeekDate, pinnedDate);
    }

    @Test
    void signUpSucceedsForAPinnedDateOutsideTheNearestWeek() {
        LocalDate thisWeekDate = LocalDate.of(2026, 11, 4);
        LocalDate pinnedDate = LocalDate.of(2026, 11, 20);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of(
                officialDate("1", thisWeekDate), pinnedDate("2", pinnedDate)));
        EventSignupStore store = new EventSignupStore();
        CalendarController controller = controllerWithStore(store, fixedClockAt(LocalDate.of(2026, 11, 2)));
        var redirectAttributes = new RedirectAttributesModelMap();

        controller.signUp("12345672", "Juan Perez", pinnedDate, "propio", redirectAttributes);

        assertThat(store.all()).hasSize(1);
        assertThat(store.all().get(0).eventTitle()).isEqualTo("Torneo Especial");
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
    void indexExposesSignupNamesForTodayAndFutureDates() {
        when(calendarService.getEvents(any(), any())).thenReturn(List.of());
        EventSignupStore store = new EventSignupStore();
        LocalDate today = LocalDate.of(2026, 10, 10);
        LocalDate future = LocalDate.of(2026, 10, 15);
        store.signUp(new EventSignup("12345672", "Juan Perez", today, "Fecha Oficial", LocalDateTime.now()));
        store.signUp(new EventSignup("41234563", "Ana Lopez", today, "Fecha Oficial", LocalDateTime.now()));
        store.signUp(new EventSignup("23456781", "Pedro Gomez", future, "Fecha Oficial", LocalDateTime.now()));
        CalendarController controller = controllerWithStore(store, fixedClockAt(today));
        Model model = new ConcurrentModel();

        controller.index(2026, 10, model);

        @SuppressWarnings("unchecked")
        Map<LocalDate, List<CalendarController.AttendeeInfo>> signupsByDate =
                (Map<LocalDate, List<CalendarController.AttendeeInfo>>) model.getAttribute("signupsByDate");
        assertThat(signupsByDate.get(today))
                .extracting(CalendarController.AttendeeInfo::fullName)
                .containsExactly("Juan Perez", "Ana Lopez");
        assertThat(signupsByDate.get(future))
                .extracting(CalendarController.AttendeeInfo::fullName)
                .containsExactly("Pedro Gomez");
    }

    @Test
    void indexOmitsSignupNamesForDatesThatHaveAlreadyPassed() {
        when(calendarService.getEvents(any(), any())).thenReturn(List.of());
        EventSignupStore store = new EventSignupStore();
        LocalDate past = LocalDate.of(2026, 10, 5);
        store.signUp(new EventSignup("12345672", "Juan Perez", past, "Fecha Oficial", LocalDateTime.now()));
        CalendarController controller = controllerWithStore(store, fixedClockAt(LocalDate.of(2026, 10, 10)));
        Model model = new ConcurrentModel();

        controller.index(2026, 10, model);

        @SuppressWarnings("unchecked")
        Map<LocalDate, List<CalendarController.AttendeeInfo>> signupsByDate =
                (Map<LocalDate, List<CalendarController.AttendeeInfo>>) model.getAttribute("signupsByDate");
        assertThat(signupsByDate).doesNotContainKey(past);
    }

    @Test
    void indexShowsTheReservedDeckNameForAnAttendeeWhoReservedOne() {
        when(calendarService.getEvents(any(), any())).thenReturn(List.of());
        EventSignupStore store = new EventSignupStore();
        LocalDate today = LocalDate.of(2026, 10, 10);
        store.signUp(new EventSignup("12345672", "Juan Perez", today, "Fecha Oficial", LocalDateTime.now()));
        DeckReservationStore reservationStore = new DeckReservationStore();
        Deck deck = DeckCatalog.ALL.get(0);
        reservationStore.reserve(new DeckReservation("12345672", "Juan Perez", deck.id(), deck.name(), today, LocalDateTime.now()));
        CalendarController controller = controllerWithStores(store, reservationStore, fixedClockAt(today));
        Model model = new ConcurrentModel();

        controller.index(2026, 10, model);

        @SuppressWarnings("unchecked")
        Map<LocalDate, List<CalendarController.AttendeeInfo>> signupsByDate =
                (Map<LocalDate, List<CalendarController.AttendeeInfo>>) model.getAttribute("signupsByDate");
        assertThat(signupsByDate.get(today))
                .containsExactly(new CalendarController.AttendeeInfo("Juan Perez", deck.name(), deck.colors()));
    }

    @Test
    void indexShowsOwnDeckForAnAttendeeWhoDidNotReserveOne() {
        when(calendarService.getEvents(any(), any())).thenReturn(List.of());
        EventSignupStore store = new EventSignupStore();
        LocalDate today = LocalDate.of(2026, 10, 10);
        store.signUp(new EventSignup("12345672", "Juan Perez", today, "Fecha Oficial", LocalDateTime.now()));
        CalendarController controller = controllerWithStore(store, fixedClockAt(today));
        Model model = new ConcurrentModel();

        controller.index(2026, 10, model);

        @SuppressWarnings("unchecked")
        Map<LocalDate, List<CalendarController.AttendeeInfo>> signupsByDate =
                (Map<LocalDate, List<CalendarController.AttendeeInfo>>) model.getAttribute("signupsByDate");
        assertThat(signupsByDate.get(today))
                .containsExactly(new CalendarController.AttendeeInfo("Juan Perez", "Propio Deck", List.of()));
    }

    @Test
    void indexIsNotFoundWhenTheSectionIsDisabled() {
        SectionProperties properties = new SectionProperties();
        properties.setEnabled(Map.of("calendario", false));
        CalendarController controller = new CalendarController(calendarService, new SectionAccess(properties),
                "59800000000", Clock.systemDefaultZone(), new EventSignupStore(), new DeckReservationStore());
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
        CalendarController controller = controller(fixedClockAt(sameDay, LocalTime.of(17, 59)));
        Model model = new ConcurrentModel();

        controller.signupForm(null, model);

        assertThat(signupDateValues(model)).containsExactly(sameDay);
    }

    @Test
    void sameDayDateIsNotRegistrableAfterTheCutoff() {
        LocalDate sameDay = LocalDate.of(2026, 11, 14);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of(officialDate("1", sameDay)));
        CalendarController controller = controller(fixedClockAt(sameDay, LocalTime.of(18, 0)));
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
                "59800000000", Clock.systemDefaultZone(), new EventSignupStore(), new DeckReservationStore());

        assertThatThrownBy(() -> controller.signupForm(null, new ConcurrentModel()))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void signUpWithOwnDeckChoiceConfirmsImmediatelyWithoutADeck() {
        CalendarEvent event = new CalendarEvent("1", "Fecha Oficial", null, null,
                LocalDateTime.of(2026, 10, 15, 19, 30),
                LocalDateTime.of(2026, 10, 15, 23, 30), false);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of(event));
        EventSignupStore store = new EventSignupStore();
        CalendarController controller = controllerWithStore(store, fixedClockAt(LocalDate.of(2026, 10, 1)));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        LocalDate fecha = LocalDate.of(2026, 10, 15);

        String view = controller.signUp("1.234.567-2", "Juan Perez", fecha, "propio", redirectAttributes);

        assertThat(view).isEqualTo("redirect:/calendario/anotarse?fecha=" + fecha);
        assertThat(store.all()).hasSize(1);
        assertThat(store.all().get(0).cedula()).isEqualTo("12345672");
        assertThat(redirectAttributes.getFlashAttributes().get("confirmedDate")).isNotNull();
        assertThat(redirectAttributes.getFlashAttributes()).doesNotContainKey("confirmedDeck");
        assertThat((String) redirectAttributes.getFlashAttributes().get("whatsappUrl"))
                .startsWith("https://wa.me/59800000000?text=")
                .contains("Lleva+su+propio+deck");
        assertThat(redirectAttributes.getFlashAttributes()).doesNotContainKey("errorMessage");
    }

    @Test
    void signUpWithReservarChoiceRedirectsToTheDeckListWithoutSigningUpYet() {
        CalendarEvent event = new CalendarEvent("1", "Fecha Oficial", null, null,
                LocalDateTime.of(2026, 10, 15, 19, 30),
                LocalDateTime.of(2026, 10, 15, 23, 30), false);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of(event));
        EventSignupStore store = new EventSignupStore();
        CalendarController controller = controllerWithStore(store, fixedClockAt(LocalDate.of(2026, 10, 1)));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        LocalDate fecha = LocalDate.of(2026, 10, 15);

        String view = controller.signUp("1.234.567-2", "Juan Perez", fecha, "reservar", redirectAttributes);

        assertThat(view).isEqualTo("redirect:/calendario/anotarse/deck/reservar?fecha=" + fecha);
        // Choosing "reservar" doesn't count as attending yet — only
        // finishing the deck pick (reserveDeck) does.
        assertThat(store.all()).isEmpty();
        assertThat(redirectAttributes.getFlashAttributes().get("signupNombre")).isEqualTo("Juan Perez");
        assertThat(redirectAttributes.getFlashAttributes().get("signupCedula")).isEqualTo("12345672");
        assertThat(redirectAttributes.getFlashAttributes()).doesNotContainKey("errorMessage");
    }

    @Test
    void signUpRejectsReservarChoiceWhenAlreadySignedUp() {
        CalendarEvent event = new CalendarEvent("1", "Fecha Oficial", null, null,
                LocalDateTime.of(2026, 10, 15, 19, 30),
                LocalDateTime.of(2026, 10, 15, 23, 30), false);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of(event));
        EventSignupStore store = new EventSignupStore();
        LocalDate fecha = LocalDate.of(2026, 10, 15);
        store.signUp(new EventSignup("12345672", "Juan Perez", fecha, "Fecha Oficial", LocalDateTime.now()));
        CalendarController controller = controllerWithStore(store, fixedClockAt(LocalDate.of(2026, 10, 1)));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.signUp("1.234.567-2", "Juan Perez", fecha, "reservar", redirectAttributes);

        assertThat(view).isEqualTo("redirect:/calendario/anotarse?fecha=" + fecha);
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage")).isEqualTo("Ya estas anotado para esa fecha.");
    }

    @Test
    void signUpRejectsBlankName() {
        EventSignupStore store = new EventSignupStore();
        CalendarController controller = controllerWithStore(store, Clock.systemDefaultZone());
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        controller.signUp("1.234.567-2", "  ", LocalDate.of(2026, 10, 15), "propio", redirectAttributes);

        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage")).isNotNull();
        assertThat(store.all()).isEmpty();
    }

    @Test
    void signUpRejectsInvalidCedula() {
        EventSignupStore store = new EventSignupStore();
        CalendarController controller = controllerWithStore(store, Clock.systemDefaultZone());
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        controller.signUp("1.234.567-9", "Juan Perez", LocalDate.of(2026, 10, 15), "propio", redirectAttributes);

        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage")).isNotNull();
        assertThat(store.all()).isEmpty();
    }

    @Test
    void signUpRejectsDateThatIsNoLongerRegistrable() {
        when(calendarService.getEvents(any(), any())).thenReturn(List.of());
        EventSignupStore store = new EventSignupStore();
        CalendarController controller = controllerWithStore(store, Clock.systemDefaultZone());
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        controller.signUp("1.234.567-2", "Juan Perez", LocalDate.of(2026, 10, 15), "propio", redirectAttributes);

        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage")).isNotNull();
        assertThat(store.all()).isEmpty();
    }

    @Test
    void signUpAllowsSameDayBeforeTheCutoff() {
        LocalDate sameDay = LocalDate.of(2026, 11, 14);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of(officialDate("1", sameDay)));
        EventSignupStore store = new EventSignupStore();
        CalendarController controller = controllerWithStore(store, fixedClockAt(sameDay, LocalTime.of(17, 59)));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.signUp("1.234.567-2", "Juan Perez", sameDay, "propio", redirectAttributes);

        assertThat(view).isEqualTo("redirect:/calendario/anotarse?fecha=" + sameDay);
        assertThat(store.all()).hasSize(1);
        assertThat(redirectAttributes.getFlashAttributes()).doesNotContainKey("errorMessage");
    }

    @Test
    void signUpRejectsSameDayAtOrAfterTheCutoff() {
        LocalDate sameDay = LocalDate.of(2026, 11, 14);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of(officialDate("1", sameDay)));
        EventSignupStore store = new EventSignupStore();
        CalendarController controller = controllerWithStore(store, fixedClockAt(sameDay, LocalTime.of(18, 0)));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        controller.signUp("1.234.567-2", "Juan Perez", sameDay, "propio", redirectAttributes);

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

        controller.signUp("1.234.567-2", "Juan Perez", nextWeekDate, "propio", redirectAttributes);

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
        controller.signUp("1.234.567-2", "Juan Perez", fecha, "propio", new RedirectAttributesModelMap());
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        controller.signUp("1.234.567-2", "Juan Perez", fecha, "propio", redirectAttributes);

        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage")).isEqualTo("Ya estas anotado para esa fecha.");
        assertThat(store.all()).hasSize(1);
    }

    @Test
    void signUpIsRejectedWhenTheSectionIsDisabled() {
        SectionProperties properties = new SectionProperties();
        properties.setEnabled(Map.of("calendario", false));
        EventSignupStore store = new EventSignupStore();
        CalendarController controller = new CalendarController(calendarService, new SectionAccess(properties),
                "59800000000", Clock.systemDefaultZone(), store, new DeckReservationStore());
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        assertThatThrownBy(() -> controller.signUp("1.234.567-2", "Juan Perez", LocalDate.of(2026, 10, 15), "propio", redirectAttributes))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(store.all()).isEmpty();
    }

    @Test
    void deckListExposesDecksAndCarriesForwardTheSignupData() {
        LocalDate fecha = LocalDate.of(2026, 10, 15);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of(officialDate("1", fecha)));
        CalendarController controller = controller(fixedClockAt(LocalDate.of(2026, 10, 1)));
        Model model = new ConcurrentModel();
        model.addAttribute("signupNombre", "Juan Perez");
        model.addAttribute("signupCedula", "12345672");

        String view = controller.deckList(fecha, model);

        assertThat(view).isEqualTo("anotarse-deck-reservar");
        assertThat(model.getAttribute("nombre")).isEqualTo("Juan Perez");
        assertThat(model.getAttribute("cedula")).isEqualTo("12345672");
        assertThat(model.getAttribute("fecha")).isEqualTo(fecha);
        assertThat(model.getAttribute("decks")).isEqualTo(DeckCatalog.ALL);
        assertThat(model.getAttribute("reservedDeckIds")).isEqualTo(Set.of());
    }

    @Test
    void deckListRedirectsToSignupFormWhenSignupDataIsMissing() {
        LocalDate fecha = LocalDate.of(2026, 10, 15);
        CalendarController controller = controller();
        Model model = new ConcurrentModel();

        String view = controller.deckList(fecha, model);

        assertThat(view).isEqualTo("redirect:/calendario/anotarse?fecha=" + fecha);
    }

    @Test
    void deckListRedirectsWhenTheDateIsNoLongerRegistrable() {
        LocalDate fecha = LocalDate.of(2026, 10, 15);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of());
        CalendarController controller = controller();
        Model model = new ConcurrentModel();
        model.addAttribute("signupNombre", "Juan Perez");
        model.addAttribute("signupCedula", "12345672");

        String view = controller.deckList(fecha, model);

        assertThat(view).isEqualTo("redirect:/calendario/anotarse?fecha=" + fecha);
    }

    @Test
    void deckListMarksAlreadyReservedDecksForTheDate() {
        LocalDate fecha = LocalDate.of(2026, 10, 15);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of(officialDate("1", fecha)));
        DeckReservationStore reservationStore = new DeckReservationStore();
        Deck deck = DeckCatalog.ALL.get(0);
        reservationStore.reserve(new DeckReservation("41234563", "Otra Persona", deck.id(), deck.name(), fecha, LocalDateTime.now()));
        CalendarController controller = controllerWithStores(new EventSignupStore(), reservationStore,
                fixedClockAt(LocalDate.of(2026, 10, 1)));
        Model model = new ConcurrentModel();
        model.addAttribute("signupNombre", "Juan Perez");
        model.addAttribute("signupCedula", "12345672");

        controller.deckList(fecha, model);

        @SuppressWarnings("unchecked")
        Set<String> reservedDeckIds = (Set<String>) model.getAttribute("reservedDeckIds");
        assertThat(reservedDeckIds).containsExactly(deck.id());
    }

    @Test
    void reserveDeckCreatesReservationAndRedirectsWithCombinedConfirmation() {
        LocalDate fecha = LocalDate.of(2026, 10, 15);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of(officialDate("1", fecha)));
        EventSignupStore signupStore = new EventSignupStore();
        DeckReservationStore reservationStore = new DeckReservationStore();
        CalendarController controller = controllerWithStores(signupStore, reservationStore,
                fixedClockAt(LocalDate.of(2026, 10, 1)));
        Deck deck = DeckCatalog.ALL.get(0);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.reserveDeck("Juan Perez", "1.234.567-2", fecha, deck.id(), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/calendario/anotarse?fecha=" + fecha);
        assertThat(reservationStore.all()).hasSize(1);
        assertThat(reservationStore.all().get(0).cedula()).isEqualTo("12345672");
        // Completing the reservation is what actually signs them up.
        assertThat(signupStore.all()).hasSize(1);
        assertThat(signupStore.all().get(0).cedula()).isEqualTo("12345672");
        assertThat(redirectAttributes.getFlashAttributes().get("confirmedDate")).isNotNull();
        assertThat(redirectAttributes.getFlashAttributes().get("confirmedDeck")).isEqualTo(deck.name());
        assertThat((String) redirectAttributes.getFlashAttributes().get("whatsappUrl"))
                .startsWith("https://wa.me/59800000000?text=")
                .contains("Fecha+Oficial");
    }

    @Test
    void reserveDeckDoesNotSignUpWhenTheDeckIsAlreadyTaken() {
        LocalDate fecha = LocalDate.of(2026, 10, 15);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of(officialDate("1", fecha)));
        EventSignupStore signupStore = new EventSignupStore();
        DeckReservationStore reservationStore = new DeckReservationStore();
        Deck deck = DeckCatalog.ALL.get(0);
        reservationStore.reserve(new DeckReservation("41234563", "Otra Persona", deck.id(), deck.name(), fecha, LocalDateTime.now()));
        CalendarController controller = controllerWithStores(signupStore, reservationStore,
                fixedClockAt(LocalDate.of(2026, 10, 1)));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        controller.reserveDeck("Juan Perez", "1.234.567-2", fecha, deck.id(), redirectAttributes);

        // A failed attempt (deck taken) leaves them free to try again —
        // they must not already count as signed up.
        assertThat(signupStore.all()).isEmpty();
    }

    @Test
    void reserveDeckRejectsWhenAlreadySignedUpWithoutAReservation() {
        LocalDate fecha = LocalDate.of(2026, 10, 15);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of(officialDate("1", fecha)));
        EventSignupStore signupStore = new EventSignupStore();
        signupStore.signUp(new EventSignup("12345672", "Juan Perez", fecha, "Fecha Oficial", LocalDateTime.now()));
        DeckReservationStore reservationStore = new DeckReservationStore();
        CalendarController controller = controllerWithStores(signupStore, reservationStore,
                fixedClockAt(LocalDate.of(2026, 10, 1)));
        Deck deck = DeckCatalog.ALL.get(0);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.reserveDeck("Juan Perez", "1.234.567-2", fecha, deck.id(), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/calendario/anotarse?fecha=" + fecha);
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage")).isEqualTo("Ya estas anotado para esa fecha.");
        assertThat(reservationStore.all()).isEmpty();
    }

    @Test
    void reserveDeckRejectsBlankName() {
        CalendarController controller = controller();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        Deck deck = DeckCatalog.ALL.get(0);

        String view = controller.reserveDeck("  ", "12345672", LocalDate.of(2026, 10, 15), deck.id(), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/calendario/anotarse?fecha=2026-10-15");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage")).isNotNull();
    }

    @Test
    void reserveDeckRejectsInvalidCedula() {
        CalendarController controller = controller();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        Deck deck = DeckCatalog.ALL.get(0);

        controller.reserveDeck("Juan Perez", "1.234.567-9", LocalDate.of(2026, 10, 15), deck.id(), redirectAttributes);

        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage")).isNotNull();
    }

    @Test
    void reserveDeckRejectsDateThatIsNoLongerRegistrable() {
        when(calendarService.getEvents(any(), any())).thenReturn(List.of());
        CalendarController controller = controller();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        Deck deck = DeckCatalog.ALL.get(0);

        controller.reserveDeck("Juan Perez", "1.234.567-2", LocalDate.of(2026, 10, 15), deck.id(), redirectAttributes);

        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage")).isNotNull();
    }

    @Test
    void reserveDeckRejectsUnknownDeckAndReturnsToTheDeckList() {
        LocalDate fecha = LocalDate.of(2026, 10, 15);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of(officialDate("1", fecha)));
        CalendarController controller = controller(fixedClockAt(LocalDate.of(2026, 10, 1)));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.reserveDeck("Juan Perez", "1.234.567-2", fecha, "no-existe", redirectAttributes);

        assertThat(view).isEqualTo("redirect:/calendario/anotarse/deck/reservar?fecha=" + fecha);
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage")).isNotNull();
        assertThat(redirectAttributes.getFlashAttributes().get("signupNombre")).isEqualTo("Juan Perez");
        assertThat(redirectAttributes.getFlashAttributes().get("signupCedula")).isEqualTo("12345672");
    }

    @Test
    void reserveDeckRejectsSecondReservationBySamePersonOnSameDate() {
        LocalDate fecha = LocalDate.of(2026, 10, 15);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of(officialDate("1", fecha)));
        DeckReservationStore reservationStore = new DeckReservationStore();
        Deck deck = DeckCatalog.ALL.get(0);
        Deck otherDeck = DeckCatalog.ALL.get(1);
        reservationStore.reserve(new DeckReservation("12345672", "Juan Perez", deck.id(), deck.name(), fecha, LocalDateTime.now()));
        CalendarController controller = controllerWithStores(new EventSignupStore(), reservationStore,
                fixedClockAt(LocalDate.of(2026, 10, 1)));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.reserveDeck("Juan Perez", "1.234.567-2", fecha, otherDeck.id(), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/calendario/anotarse/deck/reservar?fecha=" + fecha);
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage")).isEqualTo("Ya tenes una reserva para esa fecha.");
        assertThat(reservationStore.all()).hasSize(1);
    }

    @Test
    void reserveDeckRejectsDeckAlreadyTakenByAnotherPerson() {
        LocalDate fecha = LocalDate.of(2026, 10, 15);
        when(calendarService.getEvents(any(), any())).thenReturn(List.of(officialDate("1", fecha)));
        DeckReservationStore reservationStore = new DeckReservationStore();
        Deck deck = DeckCatalog.ALL.get(0);
        reservationStore.reserve(new DeckReservation("41234563", "Otra Persona", deck.id(), deck.name(), fecha, LocalDateTime.now()));
        CalendarController controller = controllerWithStores(new EventSignupStore(), reservationStore,
                fixedClockAt(LocalDate.of(2026, 10, 1)));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.reserveDeck("Juan Perez", "1.234.567-2", fecha, deck.id(), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/calendario/anotarse/deck/reservar?fecha=" + fecha);
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage"))
                .isEqualTo("Ese deck ya fue reservado para esa fecha. Elegi otro.");
        assertThat(reservationStore.all()).hasSize(1);
    }

    private CalendarController controller() {
        return controller(Clock.systemDefaultZone());
    }

    private CalendarController controller(Clock clock) {
        return controllerWithStore(new EventSignupStore(), clock);
    }

    private CalendarController controllerWithStore(EventSignupStore store, Clock clock) {
        return controllerWithStores(store, new DeckReservationStore(), clock);
    }

    private CalendarController controllerWithStores(EventSignupStore signupStore, DeckReservationStore reservationStore, Clock clock) {
        return new CalendarController(calendarService, new SectionAccess(new SectionProperties()), "59800000000", clock,
                signupStore, reservationStore);
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

    private CalendarEvent pinnedDate(String id, LocalDate date) {
        return new CalendarEvent(id, "Torneo Especial [Especial]", null, null,
                date.atTime(19, 0), date.atTime(23, 0), false);
    }

    private List<LocalDate> signupDateValues(Model model) {
        @SuppressWarnings("unchecked")
        List<CalendarController.DateOption> dates =
                (List<CalendarController.DateOption>) model.getAttribute("availableDates");
        return dates.stream().map(CalendarController.DateOption::value).toList();
    }
}
