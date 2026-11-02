package com.liga.calendar;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MerchandisingController {

    private final SectionAccess sectionAccess;

    public MerchandisingController(SectionAccess sectionAccess) {
        this.sectionAccess = sectionAccess;
    }

    @GetMapping("/merchandising")
    public String merchandising(Model model) {
        Section section = sectionAccess.requireEnabled("/merchandising");

        model.addAttribute("section", section);
        model.addAttribute("products", ProductCatalog.forSection("merchandising"));
        return "products";
    }
}
