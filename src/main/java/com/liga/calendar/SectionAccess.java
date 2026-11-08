package com.liga.calendar;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Gates each {@link Section} behind its own on/off switch in
 * {@link SectionProperties}, so a section can be hidden from the home page
 * and have its URL disabled (404) by flipping a property, without a
 * deploy of code. A section defaults to enabled when its property is
 * absent, so existing sections keep working if nothing is configured.
 */
@Component
public class SectionAccess {

    private final SectionProperties properties;

    public SectionAccess(SectionProperties properties) {
        this.properties = properties;
    }

    public List<Section> enabledSections() {
        return Section.ALL.stream().filter(this::isEnabled).toList();
    }

    public boolean isEnabled(Section section) {
        return properties.getEnabled().getOrDefault(slug(section), true);
    }

    /**
     * Looks up the section mapped to {@code url}, 404-ing if it doesn't
     * exist or has been disabled — the same response a client gets for a
     * URL that was never registered, so a disabled section leaves no trace
     * of why it's gone.
     */
    public Section requireEnabled(String url) {
        Section section = Section.ALL.stream()
                .filter(s -> s.url().equals(url))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!isEnabled(section)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return section;
    }

    /**
     * Same on/off switch as {@link #requireEnabled(String)}, for a feature
     * that has no {@link Section} card of its own on the home page (e.g.
     * the cart) — checks {@code liga.sections.enabled.<slug>} directly
     * without requiring a matching Section to exist.
     */
    public boolean isFeatureEnabled(String slug) {
        return properties.getEnabled().getOrDefault(slug, true);
    }

    /**
     * 404s if the feature named by {@code slug} has been disabled. See
     * {@link #isFeatureEnabled(String)}.
     */
    public void requireFeatureEnabled(String slug) {
        if (!isFeatureEnabled(slug)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private String slug(Section section) {
        return section.url().substring(1);
    }
}
