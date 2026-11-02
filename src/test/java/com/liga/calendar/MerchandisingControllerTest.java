package com.liga.calendar;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MerchandisingControllerTest {

    private final MerchandisingController controller = new MerchandisingController(new SectionAccess(new SectionProperties()));

    @Test
    void merchandisingListsProductsAndSectionInfo() {
        Model model = new ConcurrentModel();

        String view = controller.merchandising(model);

        assertThat(view).isEqualTo("products");
        assertThat(model.getAttribute("products")).isEqualTo(ProductCatalog.forSection("merchandising"));
        assertThat(((Section) model.getAttribute("section")).url()).isEqualTo("/merchandising");
    }

    @Test
    void merchandisingIsNotFoundWhenTheSectionIsDisabled() {
        SectionProperties properties = new SectionProperties();
        properties.setEnabled(Map.of("merchandising", false));
        MerchandisingController disabled = new MerchandisingController(new SectionAccess(properties));

        assertThatThrownBy(() -> disabled.merchandising(new ConcurrentModel()))
                .isInstanceOf(ResponseStatusException.class);
    }
}
