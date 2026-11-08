package com.liga.calendar;

import java.util.List;

/**
 * The decks available to reserve as part of the "anotarse a una fecha" flow
 * (see {@link CalendarController}), mirrored from the league's public
 * profile at
 * {@code https://moxfield.com/users/ligapaupermontevideo}.
 * <p>
 * This isn't fetched live: Moxfield's API sits behind Cloudflare, which
 * fingerprints the TLS handshake itself and blocks every plain Java HTTP
 * client (JDK HttpClient, HttpURLConnection) regardless of User-Agent —
 * even though the identical request succeeds from curl. Working around that
 * reliably would mean fighting Cloudflare's bot detection, so instead this
 * list is refreshed by hand when the league's decks change, by running:
 * <pre>
 * curl -s \
 *   -A "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36" \
 *   -H "Accept: application/json" \
 *   "https://api2.moxfield.com/v2/decks/search?pageNumber=1&amp;pageSize=100&amp;sortType=updated&amp;sortDirection=Descending&amp;board=mainboard&amp;authorUserNames=ligapaupermontevideo"
 * </pre>
 * and copying each entry's {@code publicId}, {@code name}, {@code format}
 * and {@code publicUrl}, plus its {@code colors} array (the deck's actual
 * mainboard colors — not {@code colorIdentity}, which also counts color
 * identity granted by abilities and sideboard cards, and over-reports) into
 * {@link #ALL} below, re-sorted into WUBRG order.
 */
public final class DeckCatalog {

    public static final List<Deck> ALL = List.of(
            new Deck("ijihAX-zNX-JkWpYlHIprQ", "Esper Affinity", "pauper", "https://moxfield.com/decks/ijihAX-zNX-JkWpYlHIprQ", List.of("W", "U", "B")),
            new Deck("oDqhAY3RBnuAAbnpCyq93w", "Dimir Control", "pauper", "https://moxfield.com/decks/oDqhAY3RBnuAAbnpCyq93w", List.of("U", "B")),
            new Deck("CjGhAbXxany-hQK7gks8cg", "Dimir Terror", "pauper", "https://moxfield.com/decks/CjGhAbXxany-hQK7gks8cg", List.of("U", "B")),
            new Deck("H0WhAZngi3aeWkEHQfmQQQ", "Dimir Faeries", "pauper", "https://moxfield.com/decks/H0WhAZngi3aeWkEHQfmQQQ", List.of("U", "B")),
            new Deck("CTGhASJdUXeHhcZP-5KN6Q", "MonoBlue Terror", "pauper", "https://moxfield.com/decks/CTGhASJdUXeHhcZP-5KN6Q", List.of("U")),
            new Deck("CTGhARsbXXSiYdKL6cW8xg", "UW Familiars", "pauper", "https://moxfield.com/decks/CTGhARsbXXSiYdKL6cW8xg", List.of("W", "U")),
            new Deck("BTGhAa8nq3C99tdyuYS6qg", "Turbofog", "pauper", "https://moxfield.com/decks/BTGhAa8nq3C99tdyuYS6qg", List.of("U", "G")),
            new Deck("CTGhAYG6GnChiCdq5v34vw", "Rakdos Madness", "pauper", "https://moxfield.com/decks/CTGhAYG6GnChiCdq5v34vw", List.of("U", "B", "R")),
            new Deck("CjGhAeWjTXy7AcRYWTomiw", "Monster Tron", "pauper", "https://moxfield.com/decks/CjGhAeWjTXy7AcRYWTomiw", List.of("R", "G")),
            new Deck("CTGhAUJ2bX6RKtLsobHQIg", "MonoWhite Weenie", "pauper", "https://moxfield.com/decks/CTGhAUJ2bX6RKtLsobHQIg", List.of("W")),
            new Deck("CjGhAbwocHCkothQesgiwg", "MonoWhite Heroic", "pauper", "https://moxfield.com/decks/CjGhAbwocHCkothQesgiwg", List.of("W", "U")),
            new Deck("CjGhAWNSxXCH0x8poFP4Rg", "MonoRed Rally", "pauper", "https://moxfield.com/decks/CjGhAWNSxXCH0x8poFP4Rg", List.of("R", "G")),
            new Deck("CjGhAZF5DnOSIpDNyzS2dA", "MonoRed Madness", "pauper", "https://moxfield.com/decks/CjGhAZF5DnOSIpDNyzS2dA", List.of("U", "B", "R")),
            new Deck("CjGhARtlXnGXMG7QYKCP8w", "MonoRed Burn", "pauper", "https://moxfield.com/decks/CjGhARtlXnGXMG7QYKCP8w", List.of("R")),
            new Deck("CTGhAaVGNHKrlN4zMJfMcw", "MonoBlue Faeries", "pauper", "https://moxfield.com/decks/CTGhAaVGNHKrlN4zMJfMcw", List.of("U")),
            new Deck("CjGhAfg9XHubr6_vlmEDIw", "MonoBlack Devotion", "pauper", "https://moxfield.com/decks/CjGhAfg9XHubr6_vlmEDIw", List.of("B")),
            new Deck("CjGhAT2Nf3ObgKZuinMgog", "Mardu Synth", "pauper", "https://moxfield.com/decks/CjGhAT2Nf3ObgKZuinMgog", List.of("W", "B", "R")),
            new Deck("BTGhATRKNHGlBnLP_lIycg", "Jund Wildfire", "pauper", "https://moxfield.com/decks/BTGhATRKNHGlBnLP_lIycg", List.of("B", "R", "G")),
            new Deck("CTGhAT-Ob3So2UKIyiB5VQ", "Jeskai Ephemerate", "pauper", "https://moxfield.com/decks/CTGhAT-Ob3So2UKIyiB5VQ", List.of("W", "U", "R")),
            new Deck("BTGhAcFrjn-mGR50ai0d3w", "Gruul Tokens", "pauper", "https://moxfield.com/decks/BTGhAcFrjn-mGR50ai0d3w", List.of("R", "G")),
            new Deck("BTGhAWiQdXqq3we9JHPrHA", "Gruul Ponza", "pauper", "https://moxfield.com/decks/BTGhAWiQdXqq3we9JHPrHA", List.of("R", "G")),
            new Deck("CDGhASO6w3KBAcdkDE5CGg", "Gruul Monsters", "pauper", "https://moxfield.com/decks/CDGhASO6w3KBAcdkDE5CGg", List.of("R", "G")),
            new Deck("CTGhARoyrXa6aXa3sJDqMg", "Grixis Affinity", "pauper", "https://moxfield.com/decks/CTGhARoyrXa6aXa3sJDqMg", List.of("U", "B", "R")),
            new Deck("CDGhAfrRFXGr4fW91yLzqA", "Golgari Pestilence", "pauper", "https://moxfield.com/decks/CDGhAfrRFXGr4fW91yLzqA", List.of("B", "G")),
            new Deck("CDGhAb7puXeqCP496AwPpg", "Gardens", "pauper", "https://moxfield.com/decks/CDGhAb7puXeqCP496AwPpg", List.of("B", "G")),
            new Deck("CTGhAfymSHCNWXUHwTU2Qg", "Elves", "pauper", "https://moxfield.com/decks/CTGhAfymSHCNWXUHwTU2Qg", List.of("G")),
            new Deck("CjGhAXTbIX6Tk9W4B3uYVg", "Caw Gates", "pauper", "https://moxfield.com/decks/CjGhAXTbIX6Tk9W4B3uYVg", List.of("W", "U")),
            new Deck("CTGhAYgBan22FDmBlVqM8A", "Boros Moxite", "pauper", "https://moxfield.com/decks/CTGhAYgBan22FDmBlVqM8A", List.of("W", "U", "B", "R")),
            new Deck("CjGhAf6-1H6nmZUkeib9qA", "Bogles", "pauper", "https://moxfield.com/decks/CjGhAf6-1H6nmZUkeib9qA", List.of("W", "U", "G")),
            new Deck("CTGhAY3R2Ha_LcigVicRCw", "BlackSac", "pauper", "https://moxfield.com/decks/CTGhAY3R2Ha_LcigVicRCw", List.of("B")),
            new Deck("CTGhAfr8a3-rYKh1XJMg0w", "Balustrade Spy", "pauper", "https://moxfield.com/decks/CTGhAfr8a3-rYKh1XJMg0w", List.of("B", "G")),
            new Deck("CjGhAZAS4nCBzSeHM5tVdw", "Altar Combo", "pauper", "https://moxfield.com/decks/CjGhAZAS4nCBzSeHM5tVdw", List.of("B", "G"))
    );

    private DeckCatalog() {
    }
}
