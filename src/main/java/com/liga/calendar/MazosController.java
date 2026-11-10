package com.liga.calendar;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MazosController {

    private final SectionAccess sectionAccess;

    public MazosController(SectionAccess sectionAccess) {
        this.sectionAccess = sectionAccess;
    }

    @GetMapping("/mazos")
    public String mazos(Model model) {
        Section section = sectionAccess.requireEnabled("/mazos");

        model.addAttribute("section", section);
        model.addAttribute("decks", DeckCatalog.ALL);
        return "mazos";
    }
}
