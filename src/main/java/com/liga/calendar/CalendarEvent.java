package com.liga.calendar;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

public class CalendarEvent {

    // Google Calendar's fixed per-event color palette (colorId 1-11), so the
    // hover tooltip reuses the exact colors the event has in Google Calendar
    // itself. Events without a colorId use the calendar's own default color,
    // which isn't fetchable via a plain API key, so they fall back to the
    // site's accent color instead.
    private static final Map<String, String> EVENT_COLORS = Map.ofEntries(
            Map.entry("1", "#7986cb"),  // Lavanda
            Map.entry("2", "#33b679"),  // Salvia
            Map.entry("3", "#8e24aa"),  // Uva
            Map.entry("4", "#e67c73"),  // Flamenco
            Map.entry("5", "#f6bf26"),  // Banana
            Map.entry("6", "#f4511e"),  // Mandarina
            Map.entry("7", "#039be5"),  // Pavo real
            Map.entry("8", "#616161"),  // Grafito
            Map.entry("9", "#3f51b5"),  // Arandano
            Map.entry("10", "#0b8043"), // Albahaca
            Map.entry("11", "#d50000") // Tomate
    );
    private static final String DEFAULT_COLOR = "#e94560";

    private String id;
    private String title;
    private String description;
    private String location;
    private LocalDateTime start;
    private LocalDateTime end;
    private boolean allDay;
    private String colorId;

    public CalendarEvent() {}

    public CalendarEvent(String id, String title, String description, String location,
                         LocalDateTime start, LocalDateTime end, boolean allDay) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.start = start;
        this.end = end;
        this.allDay = allDay;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public LocalDateTime getStart() { return start; }
    public void setStart(LocalDateTime start) { this.start = start; }

    public LocalDateTime getEnd() { return end; }
    public void setEnd(LocalDateTime end) { this.end = end; }

    public boolean isAllDay() { return allDay; }
    public void setAllDay(boolean allDay) { this.allDay = allDay; }

    public String getColorId() { return colorId; }
    public void setColorId(String colorId) { this.colorId = colorId; }

    /** The event's color as it appears in Google Calendar, or the site's accent color if none was set. */
    public String getColorHex() {
        return colorId != null ? EVENT_COLORS.getOrDefault(colorId, DEFAULT_COLOR) : DEFAULT_COLOR;
    }

    public LocalDate getStartDate() {
        return start != null ? start.toLocalDate() : null;
    }

    /**
     * The last calendar date this event should be shown on. Google represents
     * an all-day event's end as exclusive (the day after the last actual day),
     * and a multi-day timed event that ends exactly at midnight is likewise
     * "over" the day before — both are normalized back to an inclusive date here.
     */
    public LocalDate getDisplayEndDate() {
        if (end == null) return getStartDate();
        LocalDate startDate = getStartDate();
        LocalDate endDate = end.toLocalDate();
        if (startDate == null || !endDate.isAfter(startDate)) return endDate;

        if (allDay || end.toLocalTime().equals(LocalTime.MIDNIGHT)) {
            return endDate.minusDays(1);
        }
        return endDate;
    }

    public boolean isMultiDay() {
        LocalDate startDate = getStartDate();
        LocalDate endDate = getDisplayEndDate();
        return startDate != null && endDate != null && endDate.isAfter(startDate);
    }
}
