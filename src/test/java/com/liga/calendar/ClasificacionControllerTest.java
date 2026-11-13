package com.liga.calendar;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClasificacionControllerTest {

    private static final String STANDINGS_URL = "https://www.legapauperitalia.it/seasons/259";

    private final ClasificacionController controller =
            new ClasificacionController(new SectionAccess(new SectionProperties()), STANDINGS_URL);

    @Test
    void clasificacionExposesTheStandingsUrlAndSectionInfo() {
        Model model = new ConcurrentModel();

        String view = controller.clasificacion(model);

        assertThat(view).isEqualTo("clasificacion");
        assertThat(model.getAttribute("standingsUrl")).isEqualTo(STANDINGS_URL);
        assertThat(((Section) model.getAttribute("section")).url()).isEqualTo("/clasificacion");
    }

    @Test
    void clasificacionIsNotFoundWhenTheSectionIsDisabled() {
        SectionProperties properties = new SectionProperties();
        properties.setEnabled(Map.of("clasificacion", false));
        ClasificacionController disabled = new ClasificacionController(new SectionAccess(properties), STANDINGS_URL);

        assertThatThrownBy(() -> disabled.clasificacion(new ConcurrentModel()))
                .isInstanceOf(ResponseStatusException.class);
    }
}
