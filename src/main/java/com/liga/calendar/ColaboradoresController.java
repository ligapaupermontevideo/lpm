package com.liga.calendar;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ColaboradoresController {

    private final SectionAccess sectionAccess;

    public ColaboradoresController(SectionAccess sectionAccess) {
        this.sectionAccess = sectionAccess;
    }

    @GetMapping("/colaboradores")
    public String colaboradores(Model model) {
        Section section = sectionAccess.requireEnabled("/colaboradores");

        model.addAttribute("section", section);
        model.addAttribute("collaborators", CollaboratorCatalog.ALL);
        return "colaboradores";
    }
}
