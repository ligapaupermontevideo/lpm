package com.liga.calendar;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches the league's decks live from its public Moxfield profile on every
 * call, so a deck that gets taken down or set private on Moxfield stops
 * showing up on the site too (see {@link MazosController} and
 * {@link CalendarController}'s deck reservation flow) without anyone having
 * to remember to edit this codebase. Deliberately not cached: a deck's
 * visibility has to be checked at request time, not served from a snapshot
 * that could be minutes (or a whole deploy) out of date and show a deck
 * that has since been made private.
 * <p>
 * Moxfield's API sits behind Cloudflare, which fingerprints the TLS
 * handshake itself and blocks every plain Java HTTP client (JDK HttpClient,
 * Apache HttpClient, ...) regardless of headers -- even though the identical
 * request succeeds from curl. So this shells out to curl instead of using
 * Java's own HTTP stack (see the runtime Docker image, which installs curl
 * for exactly this reason). That isn't enough on every host, though:
 * Cloudflare can also block a request by the reputation of the IP it comes
 * from, and several PaaS hosts' outbound ranges (Render among them) get
 * blocked that way regardless of what client makes the request -- confirmed
 * in production by curl itself failing there with exit 22 (an HTTP error
 * status), while the identical command succeeds from a residential network.
 * <p>
 * So there are two ways fresh decks reach this service: this class's own
 * live attempt (works wherever Moxfield doesn't block the host's IP, e.g.
 * local dev), and {@link MoxfieldSyncController}, a small token-protected
 * endpoint a scheduled GitHub Action calls from GitHub's own runners every
 * few minutes with the same search-API response, for hosts where the direct
 * attempt is blocked. Whichever last succeeded is kept as {@link #lastGoodDecks}
 * and persisted to {@code liga.data.dir} so it survives a restart -- falling
 * all the way back to {@link DeckCatalog#ALL}, a hand-maintained snapshot,
 * only if neither has ever succeeded (e.g. right after a fresh deploy,
 * before the GitHub Action's next scheduled run). That snapshot can itself
 * be stale/wrong about visibility, so its continued use is the signal
 * something is wrong with both paths and worth checking the logs for.
 */
@Service
public class MoxfieldDeckService {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";

    @Value("${liga.moxfield.username:ligapaupermontevideo}")
    private String userName;

    // Package-private, left blank in tests and whenever liga.data.dir is
    // unset -- same reasoning as EventSignupStore#dataDir.
    @Value("${liga.data.dir:}")
    String dataDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private JsonFileStore<Deck> fileStore;

    private volatile List<Deck> lastGoodDecks = null;

    @PostConstruct
    void loadFromDisk() {
        if (dataDir == null || dataDir.isBlank()) {
            return;
        }
        fileStore = new JsonFileStore<>(dataDir, "moxfield-decks.json", Deck.class);
        List<Deck> persisted = fileStore.readAll();
        if (!persisted.isEmpty()) {
            lastGoodDecks = persisted;
        }
    }

    /** The league's current public decks, fetched live from Moxfield for this call. */
    public List<Deck> getDecks() {
        List<Deck> fresh = fetchFromMoxfield();
        if (fresh != null) {
            rememberAsLastGood(fresh);
            return fresh;
        }

        System.err.println("Moxfield fetch failed; serving "
                + (lastGoodDecks != null ? "the last known-good result (own fetch or GitHub Action sync)" : "the bundled fallback snapshot")
                + " instead");
        return lastGoodDecks != null ? lastGoodDecks : DeckCatalog.ALL;
    }

    /** Called by {@link MoxfieldSyncController} once it's already validated the caller and parsed the payload. */
    void applySynced(List<Deck> decks) {
        rememberAsLastGood(decks);
    }

    private void rememberAsLastGood(List<Deck> decks) {
        lastGoodDecks = decks;
        if (fileStore != null) {
            fileStore.writeAll(decks);
        }
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
