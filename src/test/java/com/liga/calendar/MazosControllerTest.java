package com.liga.calendar;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MazosControllerTest {

    // A fixed, non-network-dependent stand-in for MoxfieldDeckService, so
    // this test keeps using DeckCatalog.ALL as deterministic fixture data
    // without actually shelling out to curl.
    private final MoxfieldDeckService deckService = new MoxfieldDeckService() {
        @Override
        public List<Deck> getDecks() {
            return DeckCatalog.ALL;
        }
    };

    private final MazosController controller = new MazosController(new SectionAccess(new SectionProperties()), deckService);

    @Test
    void mazosListsDecksAndSectionInfo() {
        Model model = new ConcurrentModel();

        String view = controller.mazos(model);

        assertThat(view).isEqualTo("mazos");
        assertThat(model.getAttribute("decks")).isEqualTo(DeckCatalog.ALL);
        assertThat(((Section) model.getAttribute("section")).url()).isEqualTo("/mazos");
    }

    @Test
    void mazosIsNotFoundWhenTheSectionIsDisabled() {
        SectionProperties properties = new SectionProperties();
        properties.setEnabled(Map.of("mazos", false));
        MazosController disabled = new MazosController(new SectionAccess(properties), deckService);

        assertThatThrownBy(() -> disabled.mazos(new ConcurrentModel()))
                .isInstanceOf(ResponseStatusException.class);
    }
}
