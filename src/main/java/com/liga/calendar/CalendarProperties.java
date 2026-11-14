package com.liga.calendar;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Backs {@code liga.calendars.extra.<name>=<calendarId>} in application
 * properties: additional Google Calendars whose events show up on the site
 * as informational only (dot + tooltip on the grid), never open for
 * sign-up — see {@link GoogleCalendarService#getEvents} and
 * {@link CalendarEvent#isFromPrimaryCalendar()}. To show another calendar,
 * add a new {@code liga.calendars.extra.<name>=${SOME_ENV_VAR}} line (and
 * its matching env var) — no other code change needed.
 */
@Component
@ConfigurationProperties(prefix = "liga.calendars")
public class CalendarProperties {

    private Map<String, String> extra = Map.of();

    public Map<String, String> getExtra() {
        return extra;
    }

    public void setExtra(Map<String, String> extra) {
        this.extra = extra;
    }
}
