package com.liga.calendar;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches the league's decks live from its public Moxfield profile, so a
 * deck that gets taken down or set private on Moxfield stops showing up on
 * the site too (see {@link MazosController} and
 * {@link CalendarController}'s deck reservation flow) without anyone having
 * to remember to edit this codebase.
 * <p>
 * Moxfield's API sits behind Cloudflare, which fingerprints the TLS
 * handshake itself and blocks every plain Java HTTP client (JDK HttpClient,
 * Apache HttpClient, ...) regardless of headers -- even though the identical
 * request succeeds from curl. So this shells out to curl instead of using
 * Java's own HTTP stack (see the runtime Docker image, which installs curl
 * for exactly this reason). Results are cached for {@link #CACHE_TTL} so a
 * burst of page views doesn't spawn a curl process per request; if a
 * refresh fails (Moxfield down, curl missing, network issue) the last good
 * result keeps being served -- falling all the way back to
 * {@link DeckCatalog#ALL}, a hand-maintained snapshot, only if nothing has
 * ever been fetched successfully yet (e.g. right after a fresh deploy with
 * no network).
 */
@Service
public class MoxfieldDeckService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(15);

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";

    @Value("${liga.moxfield.username:ligapaupermontevideo}")
    private String userName;

    // Package-private so unit tests can swap in a fixed point in time
    // without a Spring context, same reasoning as GoogleCalendarService's
    // injected members.
    Clock clock = Clock.systemDefaultZone();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile List<Deck> cachedDecks = null;
    private volatile Instant cachedAt = Instant.EPOCH;

    /** The league's current public decks, freshest available within {@link #CACHE_TTL}. */
    public List<Deck> getDecks() {
        if (cachedDecks != null && Instant.now(clock).isBefore(cachedAt.plus(CACHE_TTL))) {
            return cachedDecks;
        }

        List<Deck> fresh = fetchFromMoxfield();
        if (fresh != null) {
            cachedDecks = fresh;
            cachedAt = Instant.now(clock);
            return fresh;
        }

        // Refresh failed: keep serving whatever we had (even if stale)
        // rather than showing nothing, falling back to the bundled
        // snapshot only if we've never fetched successfully at all.
        return cachedDecks != null ? cachedDecks : DeckCatalog.ALL;
    }

    // Package-private (not private) so unit tests can stub this one method
    // out, exercising getDecks()'s caching/fallback logic without actually
    // shelling out to curl.
    List<Deck> fetchFromMoxfield() {
        try {
            String url = "https://api2.moxfield.com/v2/decks/search?pageNumber=1&pageSize=100"
                    + "&sortType=updated&sortDirection=Descending&board=mainboard&authorUserNames=" + userName;

            // --fail: a Cloudflare block or any other HTTP error status
            // becomes a non-zero exit instead of "successfully" returning
            // an HTML error page. --max-time: curl enforces its own
            // timeout, so a stalled connection can't hang this request.
            Process process = new ProcessBuilder(
                    "curl", "--fail", "--max-time", "15", "-s",
                    "-A", USER_AGENT, "-H", "Accept: application/json", url)
                    .start();

            String body;
            try (var in = process.getInputStream()) {
                body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Error fetching Moxfield decks: curl exited with " + exitCode);
                return null;
            }

            return parseDecks(body);
        } catch (Exception e) {
            System.err.println("Error fetching Moxfield decks: " + e.getMessage());
            return null;
        }
    }

    // Package-private so unit tests can exercise the parsing logic directly
    // with a captured sample response, without shelling out to curl.
    List<Deck> parseDecks(String json) throws IOException {
        List<Deck> decks = new ArrayList<>();
        JsonNode root = objectMapper.readTree(json);
        for (JsonNode entry : root.path("data")) {
            // The search API only ever returns decks of authorUserNames
            // that are visible to an unauthenticated caller in the first
            // place, so this should always be "public" already -- checked
            // explicitly anyway as the actual rule, not an assumption.
            if (!"public".equals(entry.path("visibility").asText())) {
                continue;
            }

            List<String> colors = new ArrayList<>();
            entry.path("colors").forEach(color -> colors.add(color.asText()));

            decks.add(new Deck(
                    entry.path("publicId").asText(),
                    entry.path("name").asText(),
                    entry.path("format").asText(),
                    entry.path("publicUrl").asText(),
                    colors));
        }
        return decks;
    }
}
