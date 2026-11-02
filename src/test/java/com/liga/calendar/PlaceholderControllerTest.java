package com.liga.calendar;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlaceholderControllerTest {

    private final PlaceholderController controller = new PlaceholderController(new SectionAccess(new SectionProperties()));

    @Test
    void placeholderResolvesKnownSection() {
        Model model = new ConcurrentModel();

        String view = controller.placeholder("tokens", model);

        Section expected = Section.ALL.stream()
                .filter(s -> s.url().equals("/tokens"))
                .findFirst()
                .orElseThrow();

        assertThat(view).isEqualTo("placeholder");
        assertThat(model.getAttribute("section")).isEqualTo(expected);
    }

    @Test
    void placeholderRejectsUnknownSlug() {
        Model model = new ConcurrentModel();

        assertThatThrownBy(() -> controller.placeholder("no-existe", model))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void placeholderRejectsDisabledSection() {
        SectionProperties properties = new SectionProperties();
        properties.setEnabled(Map.of("tokens", false));
        PlaceholderController disabled = new PlaceholderController(new SectionAccess(properties));

        assertThatThrownBy(() -> disabled.placeholder("tokens", new ConcurrentModel()))
                .isInstanceOf(ResponseStatusException.class);
    }
}
