package com.liga.calendar;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ClasificacionIframeController {

    private final SectionAccess sectionAccess;
    private final String standingsUrl;

    public ClasificacionIframeController(SectionAccess sectionAccess,
                                          @Value("${liga.clasificacion.url}") String standingsUrl) {
        this.sectionAccess = sectionAccess;
        this.standingsUrl = standingsUrl;
    }

    @GetMapping("/clasificacion-iframe")
    public String clasificacionIframe(Model model) {
        Section section = sectionAccess.requireEnabled("/clasificacion-iframe");

        model.addAttribute("section", section);
        model.addAttribute("standingsUrl", standingsUrl);
        return "clasificacion-iframe";
    }
}
