package com.liga.calendar;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ColaboradoresControllerTest {

    private final ColaboradoresController controller = new ColaboradoresController(new SectionAccess(new SectionProperties()));

    @Test
    void colaboradoresListsCollaboratorsAndSectionInfo() {
        Model model = new ConcurrentModel();

        String view = controller.colaboradores(model);

        assertThat(view).isEqualTo("colaboradores");
        assertThat(model.getAttribute("collaborators")).isEqualTo(CollaboratorCatalog.ALL);
        assertThat(((Section) model.getAttribute("section")).url()).isEqualTo("/colaboradores");
    }

    @Test
    void colaboradoresIsNotFoundWhenTheSectionIsDisabled() {
        SectionProperties properties = new SectionProperties();
        properties.setEnabled(Map.of("colaboradores", false));
        ColaboradoresController disabled = new ColaboradoresController(new SectionAccess(properties));

        assertThatThrownBy(() -> disabled.colaboradores(new ConcurrentModel()))
                .isInstanceOf(ResponseStatusException.class);
    }
}
