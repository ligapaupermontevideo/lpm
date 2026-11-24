package com.liga.calendar;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Lets a scheduled GitHub Action push a fresh copy of the league's Moxfield
 * decks into {@link MoxfieldDeckService}, for hosts (Render, as deployed)
 * where Cloudflare blocks the app's own outbound fetch by IP reputation
 * regardless of client -- see that class's Javadoc for the full picture.
 * The Action runs from GitHub's own runners, which aren't blocked, and
 * forwards Moxfield's own search-API response here unchanged; parsing
 * (including the public/private filter) stays in one place,
 * {@link MoxfieldDeckService#parseDecks}, instead of being duplicated in
 * shell script.
 * <p>
 * Same not-found-if-unset-or-wrong token pattern as
 * {@link DeckReservationController}'s admin page, so a guess against this
 * URL can't tell the endpoint even exists.
 */
@RestController
public class MoxfieldSyncController {

    private final MoxfieldDeckService deckService;
    private final String syncToken;

    public MoxfieldSyncController(MoxfieldDeckService deckService,
                                   @Value("${liga.moxfield.sync.token:}") String syncToken) {
        this.deckService = deckService;
        this.syncToken = syncToken;
    }

    @PostMapping("/internal/moxfield-decks-sync")
    public ResponseEntity<String> sync(@RequestParam(required = false) String token, @RequestBody String moxfieldSearchResponse) {
        if (syncToken.isBlank() || !syncToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        List<Deck> decks;
        try {
            decks = deckService.parseDecks(moxfieldSearchResponse);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not parse the Moxfield response: " + e.getMessage());
        }

        deckService.applySynced(decks);
        return ResponseEntity.ok("Synced " + decks.size() + " public decks.");
    }
}
