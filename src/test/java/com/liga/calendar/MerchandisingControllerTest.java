package com.liga.calendar;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.assertThat;

class MerchandisingControllerTest {

    private final MerchandisingController controller = new MerchandisingController();

    @Test
    void merchandisingListsProductsAndSectionInfo() {
        Model model = new ConcurrentModel();

        String view = controller.merchandising(model);

        assertThat(view).isEqualTo("products");
        assertThat(model.getAttribute("products")).isEqualTo(ProductCatalog.forSection("merchandising"));
        assertThat(((Section) model.getAttribute("section")).url()).isEqualTo("/merchandising");
    }
}
