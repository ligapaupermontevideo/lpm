package com.liga.calendar;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The cart has no payment processing — "checkout" hands the visitor off to
 * WhatsApp with the order pre-filled as a message, and the league takes it
 * from there.
 */
@Controller
public class CartController {

    private final Cart cart;
    private final String whatsappNumber;

    public CartController(Cart cart, @Value("${liga.whatsapp.number}") String whatsappNumber) {
        this.cart = cart;
        this.whatsappNumber = whatsappNumber;
    }

    @PostMapping("/carrito/agregar")
    public String add(@RequestParam String productId,
                       @RequestParam(defaultValue = "/") String from,
                       RedirectAttributes redirectAttributes) {
        cart.add(productId);
        redirectAttributes.addFlashAttribute("added", true);
        return "redirect:" + safeRedirectTarget(from);
    }

    @PostMapping("/carrito/actualizar")
    public String updateQuantity(@RequestParam String productId, @RequestParam(defaultValue = "1") int quantity) {
        cart.setQuantity(productId, quantity);
        return "redirect:/carrito";
    }

    @PostMapping("/carrito/eliminar")
    public String remove(@RequestParam String productId) {
        cart.remove(productId);
        return "redirect:/carrito";
    }

    @GetMapping("/carrito")
    public String view(Model model) {
        List<CartLine> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<String, Integer> entry : cart.quantities().entrySet()) {
            Product product = ProductCatalog.findById(entry.getKey()).orElse(null);
            if (product == null) {
                continue;
            }
            int quantity = entry.getValue();
            BigDecimal subtotal = product.price().multiply(BigDecimal.valueOf(quantity));
            lines.add(new CartLine(product, quantity, subtotal));
            total = total.add(subtotal);
        }

        model.addAttribute("lines", lines);
        model.addAttribute("total", total);
        model.addAttribute("whatsappUrl", buildWhatsAppUrl(lines, total));
        return "cart";
    }

    // Only ever redirect back into this app — a raw "from" value is
    // attacker-controlled input, and forwarding it unchecked would let a
    // crafted request send visitors to an external site from our domain.
    private String safeRedirectTarget(String from) {
        if (from != null && from.startsWith("/") && !from.startsWith("//")) {
            return from;
        }
        return "/";
    }

    private String buildWhatsAppUrl(List<CartLine> lines, BigDecimal total) {
        StringBuilder message = new StringBuilder("Hola! Quiero hacer este pedido:\n");
        for (CartLine line : lines) {
            message.append("- ").append(line.quantity()).append("x ").append(line.product().name())
                    .append(" ($").append(line.subtotal()).append(")\n");
        }
        message.append("Total: $").append(total);

        String encoded = URLEncoder.encode(message.toString(), StandardCharsets.UTF_8);
        return "https://wa.me/" + whatsappNumber + "?text=" + encoded;
    }

    public record CartLine(Product product, int quantity, BigDecimal subtotal) {
    }
}
