package com.liga.calendar;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClasificacionIframeControllerTest {

    private static final String STANDINGS_URL = "https://www.legapauperitalia.it/seasons/259";

    private final ClasificacionIframeController controller =
            new ClasificacionIframeController(new SectionAccess(new SectionProperties()), STANDINGS_URL);

    @Test
    void clasificacionIframeExposesTheStandingsUrlAndSectionInfo() {
        Model model = new ConcurrentModel();

        String view = controller.clasificacionIframe(model);

        assertThat(view).isEqualTo("clasificacion-iframe");
        assertThat(model.getAttribute("standingsUrl")).isEqualTo(STANDINGS_URL);
        assertThat(((Section) model.getAttribute("section")).url()).isEqualTo("/clasificacion-iframe");
    }

    @Test
    void clasificacionIframeIsNotFoundWhenTheSectionIsDisabled() {
        SectionProperties properties = new SectionProperties();
        properties.setEnabled(Map.of("clasificacion-iframe", false));
        ClasificacionIframeController disabled = new ClasificacionIframeController(new SectionAccess(properties), STANDINGS_URL);

        assertThatThrownBy(() -> disabled.clasificacionIframe(new ConcurrentModel()))
                .isInstanceOf(ResponseStatusException.class);
    }
}
