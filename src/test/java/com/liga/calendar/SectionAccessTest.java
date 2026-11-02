package com.liga.calendar;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SectionAccessTest {

    @Test
    void everySectionIsEnabledByDefault() {
        SectionAccess access = new SectionAccess(new SectionProperties());

        assertThat(access.enabledSections()).isEqualTo(Section.ALL);
    }

    @Test
    void enabledSectionsExcludesOnesSetToFalse() {
        SectionProperties properties = new SectionProperties();
        properties.setEnabled(Map.of("tokens", false, "accesorios", false));
        SectionAccess access = new SectionAccess(properties);

        assertThat(access.enabledSections())
                .extracting(Section::url)
                .containsExactly("/calendario", "/prestamo-mazos", "/merchandising");
    }

    @Test
    void requireEnabledReturnsTheSectionWhenEnabled() {
        SectionAccess access = new SectionAccess(new SectionProperties());

        Section section = access.requireEnabled("/calendario");

        assertThat(section.url()).isEqualTo("/calendario");
    }

    @Test
    void requireEnabledThrowsNotFoundForADisabledSection() {
        SectionProperties properties = new SectionProperties();
        properties.setEnabled(Map.of("calendario", false));
        SectionAccess access = new SectionAccess(properties);

        assertThatThrownBy(() -> access.requireEnabled("/calendario"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void requireEnabledThrowsNotFoundForAnUnknownUrl() {
        SectionAccess access = new SectionAccess(new SectionProperties());

        assertThatThrownBy(() -> access.requireEnabled("/no-existe"))
                .isInstanceOf(ResponseStatusException.class);
    }
}
