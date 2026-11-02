package com.liga.calendar;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final SectionAccess sectionAccess;

    public HomeController(SectionAccess sectionAccess) {
        this.sectionAccess = sectionAccess;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("sections", sectionAccess.enabledSections());
        return "home";
    }
}
