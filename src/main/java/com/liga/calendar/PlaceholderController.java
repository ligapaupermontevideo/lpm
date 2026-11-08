package com.liga.calendar;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Serves a "coming soon" page for any section registered in
 * {@link Section#ALL} that doesn't have a dedicated controller yet — e.g. a
 * new section can be added to the home page immediately, with its real
 * content and controller filled in later.
 */
@Controller
public class PlaceholderController {

    private final SectionAccess sectionAccess;

    public PlaceholderController(SectionAccess sectionAccess) {
        this.sectionAccess = sectionAccess;
    }

    // The slug must not contain a dot so this catch-all doesn't shadow
    // root-level static files like /favicon.ico — no real section slug
    // has one.
    @GetMapping("/{slug:[^.]+}")
    public String placeholder(@PathVariable String slug, Model model) {
        Section section = sectionAccess.requireEnabled("/" + slug);

        model.addAttribute("section", section);
        return "placeholder";
    }
}
