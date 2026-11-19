package com.liga.calendar;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
public class DonacionesController {

    // Quick-pick amounts shown as buttons, in USD — paypal.me doesn't
    // support UYU, so donations go through in dollars regardless of what
    // currency the rest of the site (merch prices) is shown in.
    private static final List<Integer> PRESET_AMOUNTS = List.of(5, 10, 20, 50);

    private final SectionAccess sectionAccess;
    private final String paypalMeBaseUrl;

    public DonacionesController(SectionAccess sectionAccess,
                                 @Value("${liga.paypal.me.username}") String paypalMeUsername) {
        this.sectionAccess = sectionAccess;
        this.paypalMeBaseUrl = "https://paypal.me/" + paypalMeUsername;
    }

    @GetMapping("/donaciones")
    public String donaciones(Model model) {
        Section section = sectionAccess.requireEnabled("/donaciones");

        List<DonationOption> options = new ArrayList<>(PRESET_AMOUNTS.stream()
                .map(amount -> new DonationOption("U$S " + amount, paypalMeBaseUrl + "/" + amount + "USD"))
                .toList());
        options.add(new DonationOption("Otro monto", paypalMeBaseUrl));

        model.addAttribute("section", section);
        model.addAttribute("donationOptions", options);
        return "donaciones";
    }
}
