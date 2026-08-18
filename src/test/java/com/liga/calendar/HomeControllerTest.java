package com.liga.calendar;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.assertThat;

class HomeControllerTest {

    @Test
    void homeListsAllSections() {
        HomeController controller = new HomeController();
        Model model = new ConcurrentModel();

        String view = controller.home(model);

        assertThat(view).isEqualTo("home");
        assertThat(model.getAttribute("sections")).isEqualTo(Section.ALL);
    }
}
