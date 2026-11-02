package com.liga.calendar;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Backs {@code liga.sections.enabled.<slug>=false} in application
 * properties, where {@code <slug>} is a {@link Section#url()} without its
 * leading slash (e.g. {@code prestamo-mazos}). A slug with no entry here
 * defaults to enabled — see {@link SectionAccess}.
 */
@Component
@ConfigurationProperties(prefix = "liga.sections")
public class SectionProperties {

    private Map<String, Boolean> enabled = Map.of();

    public Map<String, Boolean> getEnabled() {
        return enabled;
    }

    public void setEnabled(Map<String, Boolean> enabled) {
        this.enabled = enabled;
    }
}
