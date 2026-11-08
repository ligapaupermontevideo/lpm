package com.liga.calendar;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartControllerTest {

    private final Cart cart = new Cart();
    private final CartController controller = new CartController(cart, "59800000000", new SectionAccess(new SectionProperties()));

    @Test
    void addPutsProductInCartAndRedirectsBackToOrigin() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.add("remera-liga", "/merchandising", redirectAttributes);

        assertThat(view).isEqualTo("redirect:/merchandising");
        assertThat(cart.quantities()).containsEntry("remera-liga", 1);
        assertThat(redirectAttributes.getFlashAttributes().get("added")).isEqualTo(true);
    }

    @Test
    void addTwiceIncreasesQuantity() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        controller.add("remera-liga", "/merchandising", redirectAttributes);
        controller.add("remera-liga", "/merchandising", redirectAttributes);

        assertThat(cart.quantities()).containsEntry("remera-liga", 2);
    }

    @Test
    void addRejectsOffSiteRedirectTarget() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.add("remera-liga", "https://evil.example.com", redirectAttributes);

        assertThat(view).isEqualTo("redirect:/");
    }

    @Test
    void addRejectsProtocolRelativeRedirectTarget() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.add("remera-liga", "//evil.example.com", redirectAttributes);

        assertThat(view).isEqualTo("redirect:/");
    }

    @Test
    void updateQuantityChangesStoredQuantity() {
        cart.add("remera-liga");

        String view = controller.updateQuantity("remera-liga", 3);

        assertThat(view).isEqualTo("redirect:/carrito");
        assertThat(cart.quantities()).containsEntry("remera-liga", 3);
    }

    @Test
    void updateQuantityToZeroRemovesItem() {
        cart.add("remera-liga");

        controller.updateQuantity("remera-liga", 0);

        assertThat(cart.quantities()).doesNotContainKey("remera-liga");
    }

    @Test
    void removeDeletesItemFromCart() {
        cart.add("remera-liga");

        String view = controller.remove("remera-liga");

        assertThat(view).isEqualTo("redirect:/carrito");
        assertThat(cart.quantities()).isEmpty();
    }

    @Test
    void viewBuildsLinesTotalAndWhatsAppMessage() {
        cart.add("remera-liga");
        cart.add("remera-liga");
        cart.add("sticker-pack");

        Model model = new ConcurrentModel();
        String view = controller.view(model);

        assertThat(view).isEqualTo("cart");

        @SuppressWarnings("unchecked")
        List<CartController.CartLine> lines = (List<CartController.CartLine>) model.getAttribute("lines");
        assertThat(lines).hasSize(2);

        BigDecimal total = (BigDecimal) model.getAttribute("total");
        assertThat(total).isEqualByComparingTo("1950"); // 2*900 + 150

        String whatsappUrl = (String) model.getAttribute("whatsappUrl");
        assertThat(whatsappUrl).startsWith("https://wa.me/59800000000?text=");

        String decoded = URLDecoder.decode(whatsappUrl.substring(whatsappUrl.indexOf("text=") + 5), StandardCharsets.UTF_8);
        assertThat(decoded).contains("2x Remera Liga Pauper Montevideo");
        assertThat(decoded).contains("1x Pack de Stickers");
        assertThat(decoded).contains("Total: $1950");
    }

    @Test
    void viewSkipsUnknownProductIds() {
        cart.add("no-existe");

        Model model = new ConcurrentModel();
        controller.view(model);

        @SuppressWarnings("unchecked")
        List<CartController.CartLine> lines = (List<CartController.CartLine>) model.getAttribute("lines");
        assertThat(lines).isEmpty();
    }

    @Test
    void viewIsNotFoundWhenTheCartIsDisabled() {
        CartController disabled = disabledController();

        assertThatThrownBy(() -> disabled.view(new ConcurrentModel()))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void addIsNotFoundWhenTheCartIsDisabled() {
        CartController disabled = disabledController();

        assertThatThrownBy(() -> disabled.add("remera-liga", "/merchandising", new RedirectAttributesModelMap()))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void updateQuantityIsNotFoundWhenTheCartIsDisabled() {
        CartController disabled = disabledController();

        assertThatThrownBy(() -> disabled.updateQuantity("remera-liga", 2))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void removeIsNotFoundWhenTheCartIsDisabled() {
        CartController disabled = disabledController();

        assertThatThrownBy(() -> disabled.remove("remera-liga"))
                .isInstanceOf(ResponseStatusException.class);
    }

    private CartController disabledController() {
        SectionProperties properties = new SectionProperties();
        properties.setEnabled(Map.of("carrito", false));
        return new CartController(cart, "59800000000", new SectionAccess(properties));
    }
}
