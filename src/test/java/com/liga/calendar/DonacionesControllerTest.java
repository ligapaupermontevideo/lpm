package com.liga.calendar;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DonacionesControllerTest {

    private final DonacionesController controller =
            new DonacionesController(new SectionAccess(new SectionProperties()), "LigaPauperMVD");

    @Test
    void donacionesExposesAPresetAmountPerOptionPlusACustomAmountOption() {
        Model model = new ConcurrentModel();

        String view = controller.donaciones(model);

        assertThat(view).isEqualTo("donaciones");
        assertThat(((Section) model.getAttribute("section")).url()).isEqualTo("/donaciones");

        @SuppressWarnings("unchecked")
        List<DonationOption> options = (List<DonationOption>) model.getAttribute("donationOptions");
        assertThat(options).extracting(DonationOption::url).containsExactly(
                "https://paypal.me/LigaPauperMVD/5USD",
                "https://paypal.me/LigaPauperMVD/10USD",
                "https://paypal.me/LigaPauperMVD/20USD",
                "https://paypal.me/LigaPauperMVD/50USD",
                "https://paypal.me/LigaPauperMVD");
        assertThat(options.get(options.size() - 1).label()).isEqualTo("Otro monto");
    }

    @Test
    void donacionesIsNotFoundWhenTheSectionIsDisabled() {
        SectionProperties properties = new SectionProperties();
        properties.setEnabled(Map.of("donaciones", false));
        DonacionesController disabled = new DonacionesController(new SectionAccess(properties), "LigaPauperMVD");

        assertThatThrownBy(() -> disabled.donaciones(new ConcurrentModel()))
                .isInstanceOf(ResponseStatusException.class);
    }
}
