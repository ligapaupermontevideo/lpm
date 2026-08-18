package com.liga.calendar;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Controller
public class CalendarController {

    private final GoogleCalendarService calendarService;

    public CalendarController(GoogleCalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @GetMapping("/calendario")
    public String index(@RequestParam(value = "year", required = false) Integer year,
                         @RequestParam(value = "month", required = false) Integer month,
                         Model model) {
        YearMonth current = (year != null && month != null)
                ? YearMonth.of(year, month)
                : YearMonth.now();

        LocalDate[] range = CalendarGridBuilder.gridRange(current);
        List<CalendarEvent> events = calendarService.getEvents(range[0], range[1]);

        YearMonth previous = current.minusMonths(1);
        YearMonth next = current.plusMonths(1);

        model.addAttribute("month", CalendarGridBuilder.buildMonth(current, events));
        model.addAttribute("prevYear", previous.getYear());
        model.addAttribute("prevMonth", previous.getMonthValue());
        model.addAttribute("nextYear", next.getYear());
        model.addAttribute("nextMonth", next.getMonthValue());
        return "index";
    }
}
