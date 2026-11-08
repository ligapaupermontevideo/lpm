package com.liga.calendar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeckReservationControllerTest {

    @Mock
    private GoogleCalendarService calendarService;

    private final DeckReservationStore store = new DeckReservationStore();

    private final LocalDate today = LocalDate.now();
    private final LocalDate eventDate = today.plusDays(10);
    private final Deck deck = DeckCatalog.ALL.get(0);

    private DeckReservationController controller(String adminToken) {
        return controller(adminToken, Clock.systemDefaultZone());
    }

    private DeckReservationController controller(String adminToken, Clock clock) {
        return controller(adminToken, clock, new SectionAccess(new SectionProperties()));
    }

    private DeckReservationController controller(String adminToken, Clock clock, SectionAccess sectionAccess) {
        return new DeckReservationController(calendarService, store, clock, "59800000000", adminToken, sectionAccess);
    }

    private Clock fixedClockAt(LocalDate date, LocalTime time) {
        ZoneId zone = ZoneId.of("America/Montevideo");
        return Clock.fixed(LocalDateTime.of(date, time).atZone(zone).toInstant(), zone);
    }

    private void stubAvailableDate() {
        stubAvailableDate(eventDate);
    }

    private void stubAvailableDate(LocalDate date) {
        stubAvailableDates(List.of(date));
    }

    private void stubAvailableDates(List<LocalDate> dates) {
        List<CalendarEvent> events = dates.stream()
                .map(date -> new CalendarEvent("evt-" + date, "Fecha de liga", null, null,
                        date.atStartOfDay(), date.plusDays(1).atStartOfDay(), true))
                .toList();
        when(calendarService.getEvents(any(), any())).thenReturn(events);
    }

    private List<LocalDate> availableDateValues(Model model) {
        @SuppressWarnings("unchecked")
        List<DeckReservationController.DateOption> dates =
                (List<DeckReservationController.DateOption>) model.getAttribute("availableDates");
        return dates.stream().map(DeckReservationController.DateOption::value).toList();
    }

    @Test
    void decksExposesSectionAndDeckList() {
        Model model = new ConcurrentModel();

        String view = controller("token").decks(model);

        assertThat(view).isEqualTo("decks");
        assertThat(model.getAttribute("section")).isEqualTo(
                Section.ALL.stream().filter(s -> s.url().equals("/prestamo-mazos")).findFirst().orElseThrow());
        assertThat(model.getAttribute("decks")).isEqualTo(DeckCatalog.ALL);
    }

    @Test
    void formExposesSectionDecksAndAvailableDates() {
        stubAvailableDate();
        Model model = new ConcurrentModel();

        String view = controller("token").form(null, model);

        assertThat(view).isEqualTo("reservas");
        assertThat(model.getAttribute("section")).isEqualTo(
                Section.ALL.stream().filter(s -> s.url().equals("/prestamo-mazos")).findFirst().orElseThrow());
        assertThat(model.getAttribute("decks")).isEqualTo(DeckCatalog.ALL);

        @SuppressWarnings("unchecked")
        List<DeckReservationController.DateOption> dates =
                (List<DeckReservationController.DateOption>) model.getAttribute("availableDates");
        assertThat(dates).extracting(DeckReservationController.DateOption::value).containsExactly(eventDate);
    }

    @Test
    void formExposesTheRequestedDeckIdForPreselection() {
        stubAvailableDate();
        Model model = new ConcurrentModel();

        controller("token").form(deck.id(), model);

        assertThat(model.getAttribute("selectedDeckId")).isEqualTo(deck.id());
    }

    @Test
    void reserveWithValidDataCreatesReservationAndRedirects() {
        stubAvailableDate();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller("token").reserve("1.234.567-2", "Juan Perez", deck.id(), eventDate, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/prestamo-mazos/reservar?deckId=" + deck.id());
        assertThat(store.all()).hasSize(1);
        assertThat(store.all().get(0).cedula()).isEqualTo("12345672");
        assertThat(redirectAttributes.getFlashAttributes().get("confirmedDeck")).isEqualTo(deck.name());
        assertThat((String) redirectAttributes.getFlashAttributes().get("whatsappUrl"))
                .startsWith("https://wa.me/59800000000?text=");
        assertThat(redirectAttributes.getFlashAttributes()).doesNotContainKey("errorMessage");
    }

    @Test
    void reserveRejectsBlankName() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        controller("token").reserve("1.234.567-2", "  ", deck.id(), eventDate, redirectAttributes);

        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage")).isNotNull();
        assertThat(store.all()).isEmpty();
    }

    @Test
    void reserveRejectsInvalidCedula() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        controller("token").reserve("1.234.567-9", "Juan Perez", deck.id(), eventDate, redirectAttributes);

        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage")).isNotNull();
        assertThat(store.all()).isEmpty();
    }

    @Test
    void reserveRejectsDateNotInAvailableList() {
        stubAvailableDate();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        controller("token").reserve("1.234.567-2", "Juan Perez", deck.id(), today.plusDays(99), redirectAttributes);

        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage")).isNotNull();
        assertThat(store.all()).isEmpty();
    }

    @Test
    void reserveRejectsUnknownDeck() {
        stubAvailableDate();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        controller("token").reserve("1.234.567-2", "Juan Perez", "no-existe", eventDate, redirectAttributes);

        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage")).isNotNull();
        assertThat(store.all()).isEmpty();
    }

    @Test
    void reserveRejectsSecondReservationBySamePersonOnSameDate() {
        stubAvailableDate();
        store.reserve(new DeckReservation("12345672", "Juan Perez", deck.id(), deck.name(), eventDate, LocalDateTime.now()));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        controller("token").reserve("1.234.567-2", "Juan Perez", deck.id(), eventDate, redirectAttributes);

        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage"))
                .isEqualTo("Ya tenes una reserva para esa fecha.");
        assertThat(store.all()).hasSize(1);
    }

    @Test
    void sameDayDateIsAvailableBeforeTheCutoff() {
        LocalDate sameDay = LocalDate.of(2026, 11, 14);
        stubAvailableDate(sameDay);
        Model model = new ConcurrentModel();

        controller("token", fixedClockAt(sameDay, LocalTime.of(16, 59))).form(null, model);

        @SuppressWarnings("unchecked")
        List<DeckReservationController.DateOption> dates =
                (List<DeckReservationController.DateOption>) model.getAttribute("availableDates");
        assertThat(dates).extracting(DeckReservationController.DateOption::value).containsExactly(sameDay);
    }

    @Test
    void sameDayDateIsNotAvailableAfterTheCutoff() {
        LocalDate sameDay = LocalDate.of(2026, 11, 14);
        stubAvailableDate(sameDay);
        Model model = new ConcurrentModel();

        controller("token", fixedClockAt(sameDay, LocalTime.of(17, 0))).form(null, model);

        @SuppressWarnings("unchecked")
        List<DeckReservationController.DateOption> dates =
                (List<DeckReservationController.DateOption>) model.getAttribute("availableDates");
        assertThat(dates).isEmpty();
    }

    @Test
    void reserveAllowsSameDayBeforeTheCutoff() {
        LocalDate sameDay = LocalDate.of(2026, 11, 14);
        stubAvailableDate(sameDay);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller("token", fixedClockAt(sameDay, LocalTime.of(16, 59)))
                .reserve("1.234.567-2", "Juan Perez", deck.id(), sameDay, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/prestamo-mazos/reservar?deckId=" + deck.id());
        assertThat(store.all()).hasSize(1);
    }

    @Test
    void reserveRejectsSameDayAtOrAfterTheCutoff() {
        LocalDate sameDay = LocalDate.of(2026, 11, 14);
        stubAvailableDate(sameDay);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        controller("token", fixedClockAt(sameDay, LocalTime.of(17, 0)))
                .reserve("1.234.567-2", "Juan Perez", deck.id(), sameDay, redirectAttributes);

        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage")).isNotNull();
        assertThat(store.all()).isEmpty();
    }

    @Test
    void availableDatesOnlyIncludesTheNearestWeek() {
        // Monday 2026-11-02 through Sunday 2026-11-08 is "this week"; the
        // 11th and 20th fall in later weeks and should be left out.
        LocalDate thisWeekDate = LocalDate.of(2026, 11, 4);
        LocalDate nextWeekDate = LocalDate.of(2026, 11, 11);
        LocalDate laterDate = LocalDate.of(2026, 11, 20);
        stubAvailableDates(List.of(thisWeekDate, nextWeekDate, laterDate));
        Model model = new ConcurrentModel();

        controller("token", fixedClockAt(LocalDate.of(2026, 11, 2), LocalTime.of(10, 0))).form(null, model);

        assertThat(availableDateValues(model)).containsExactly(thisWeekDate);
    }

    @Test
    void availableDatesIncludesEveryReservableDateInTheNearestWeek() {
        // All four of the league's usual weekly dates, same Mon-Sun week.
        LocalDate monday = LocalDate.of(2026, 11, 2);
        LocalDate wednesday = LocalDate.of(2026, 11, 4);
        LocalDate friday = LocalDate.of(2026, 11, 6);
        LocalDate saturday = LocalDate.of(2026, 11, 7);
        LocalDate nextWeek = LocalDate.of(2026, 11, 9);
        stubAvailableDates(List.of(monday, wednesday, friday, saturday, nextWeek));
        Model model = new ConcurrentModel();

        controller("token", fixedClockAt(monday, LocalTime.of(10, 0))).form(null, model);

        assertThat(availableDateValues(model)).containsExactly(monday, wednesday, friday, saturday);
    }

    @Test
    void availableDatesRollsOverToNextWeekOnceThisWeekHasNoneLeft() {
        LocalDate pastThisWeek = LocalDate.of(2026, 11, 4);
        LocalDate nextWeekDate = LocalDate.of(2026, 11, 11);
        stubAvailableDates(List.of(pastThisWeek, nextWeekDate));
        Model model = new ConcurrentModel();

        // A Sunday, after this week's only date has already passed.
        controller("token", fixedClockAt(LocalDate.of(2026, 11, 8), LocalTime.of(20, 0))).form(null, model);

        assertThat(availableDateValues(model)).containsExactly(nextWeekDate);
    }

    @Test
    void decksIsNotFoundWhenTheSectionIsDisabled() {
        SectionProperties properties = new SectionProperties();
        properties.setEnabled(Map.of("prestamo-mazos", false));
        DeckReservationController disabled = controller("token", Clock.systemDefaultZone(), new SectionAccess(properties));

        assertThatThrownBy(() -> disabled.decks(new ConcurrentModel()))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void formIsNotFoundWhenTheSectionIsDisabled() {
        SectionProperties properties = new SectionProperties();
        properties.setEnabled(Map.of("prestamo-mazos", false));
        DeckReservationController disabled = controller("token", Clock.systemDefaultZone(), new SectionAccess(properties));

        assertThatThrownBy(() -> disabled.form(null, new ConcurrentModel()))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void reserveIsRejectedWhenTheSectionIsDisabled() {
        SectionProperties properties = new SectionProperties();
        properties.setEnabled(Map.of("prestamo-mazos", false));
        DeckReservationController disabled = controller("token", Clock.systemDefaultZone(), new SectionAccess(properties));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        assertThatThrownBy(() -> disabled.reserve("1.234.567-2", "Juan Perez", deck.id(), eventDate, redirectAttributes))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(store.all()).isEmpty();
    }

    @Test
    void reservasPageRequiresMatchingToken() {
        assertThatThrownBy(() -> controller("secret").reservas("wrong", new ConcurrentModel()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void reservasPageIsDisabledWhenNoTokenIsConfigured() {
        assertThatThrownBy(() -> controller("").reservas(null, new ConcurrentModel()))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void reservasPageListsReservationsWithCorrectToken() {
        store.reserve(new DeckReservation("12345672", "Juan Perez", deck.id(), deck.name(), eventDate, LocalDateTime.now()));
        Model model = new ConcurrentModel();

        String view = controller("secret").reservas("secret", model);

        assertThat(view).isEqualTo("reservas-admin");
        assertThat((List<?>) model.getAttribute("reservations")).hasSize(1);
    }
}
