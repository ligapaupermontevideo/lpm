package com.liga.calendar;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HomeControllerTest {

    @Test
    void homeListsAllSectionsWhenNoneAreDisabled() {
        HomeController controller = new HomeController(new SectionAccess(new SectionProperties()));
        Model model = new ConcurrentModel();

        String view = controller.home(model);

        assertThat(view).isEqualTo("home");
        assertThat(model.getAttribute("sections")).isEqualTo(Section.ALL);
    }

    @Test
    void homeOmitsDisabledSections() {
        SectionProperties properties = new SectionProperties();
        properties.setEnabled(Map.of("merchandising", false));
        HomeController controller = new HomeController(new SectionAccess(properties));
        Model model = new ConcurrentModel();

        controller.home(model);

        @SuppressWarnings("unchecked")
        List<Section> sections = (List<Section>) model.getAttribute("sections");
        assertThat(sections).extracting(Section::url).doesNotContain("/merchandising");
    }
}
