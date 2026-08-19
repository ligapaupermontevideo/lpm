package com.liga.calendar;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

/**
 * Serves a "coming soon" page for any section registered in
 * {@link Section#ALL} that doesn't have a dedicated controller yet — e.g. a
 * new section can be added to the home page immediately, with its real
 * content and controller filled in later.
 */
@Controller
public class PlaceholderController {

    @GetMapping("/{slug}")
    public String placeholder(@PathVariable String slug, Model model) {
        Section section = Section.ALL.stream()
                .filter(s -> s.url().equals("/" + slug))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        model.addAttribute("section", section);
        return "placeholder";
    }
}
