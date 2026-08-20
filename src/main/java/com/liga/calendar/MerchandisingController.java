package com.liga.calendar;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MerchandisingController {

    @GetMapping("/merchandising")
    public String merchandising(Model model) {
        Section section = Section.ALL.stream()
                .filter(s -> s.url().equals("/merchandising"))
                .findFirst()
                .orElseThrow();

        model.addAttribute("section", section);
        model.addAttribute("products", ProductCatalog.forSection("merchandising"));
        return "products";
    }
}
