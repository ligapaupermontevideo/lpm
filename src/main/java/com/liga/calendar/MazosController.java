package com.liga.calendar;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MazosController {

    private final SectionAccess sectionAccess;
    private final MoxfieldDeckService deckService;

    public MazosController(SectionAccess sectionAccess, MoxfieldDeckService deckService) {
        this.sectionAccess = sectionAccess;
        this.deckService = deckService;
    }

    @GetMapping("/mazos")
    public String mazos(Model model) {
        Section section = sectionAccess.requireEnabled("/mazos");

        model.addAttribute("section", section);
        model.addAttribute("decks", deckService.getDecks());
        return "mazos";
    }
}
