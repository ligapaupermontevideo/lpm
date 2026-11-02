package com.liga.calendar;

import java.util.List;

/**
 * The decks available to reserve on {@code /prestamo-mazos}, mirrored from
 * the league's public profile at
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
 * and {@code publicUrl} into {@link #ALL} below.
 */
public final class DeckCatalog {

    public static final List<Deck> ALL = List.of(
            new Deck("ijihAX-zNX-JkWpYlHIprQ", "Esper Affinity", "pauper", "https://moxfield.com/decks/ijihAX-zNX-JkWpYlHIprQ"),
            new Deck("oDqhAY3RBnuAAbnpCyq93w", "Dimir Control", "pauper", "https://moxfield.com/decks/oDqhAY3RBnuAAbnpCyq93w"),
            new Deck("CjGhAbXxany-hQK7gks8cg", "Dimir Terror", "pauper", "https://moxfield.com/decks/CjGhAbXxany-hQK7gks8cg"),
            new Deck("H0WhAZngi3aeWkEHQfmQQQ", "Dimir Faeries", "pauper", "https://moxfield.com/decks/H0WhAZngi3aeWkEHQfmQQQ"),
            new Deck("CTGhASJdUXeHhcZP-5KN6Q", "MonoBlue Terror", "pauper", "https://moxfield.com/decks/CTGhASJdUXeHhcZP-5KN6Q"),
            new Deck("CTGhARsbXXSiYdKL6cW8xg", "UW Familiars", "pauper", "https://moxfield.com/decks/CTGhARsbXXSiYdKL6cW8xg"),
            new Deck("BTGhAa8nq3C99tdyuYS6qg", "Turbofog", "pauper", "https://moxfield.com/decks/BTGhAa8nq3C99tdyuYS6qg"),
            new Deck("CTGhAYG6GnChiCdq5v34vw", "Rakdos Madness", "pauper", "https://moxfield.com/decks/CTGhAYG6GnChiCdq5v34vw"),
            new Deck("CjGhAeWjTXy7AcRYWTomiw", "Monster Tron", "pauper", "https://moxfield.com/decks/CjGhAeWjTXy7AcRYWTomiw"),
            new Deck("CTGhAUJ2bX6RKtLsobHQIg", "MonoWhite Weenie", "pauper", "https://moxfield.com/decks/CTGhAUJ2bX6RKtLsobHQIg"),
            new Deck("CjGhAbwocHCkothQesgiwg", "MonoWhite Heroic", "pauper", "https://moxfield.com/decks/CjGhAbwocHCkothQesgiwg"),
            new Deck("CjGhAWNSxXCH0x8poFP4Rg", "MonoRed Rally", "pauper", "https://moxfield.com/decks/CjGhAWNSxXCH0x8poFP4Rg"),
            new Deck("CjGhAZF5DnOSIpDNyzS2dA", "MonoRed Madness", "pauper", "https://moxfield.com/decks/CjGhAZF5DnOSIpDNyzS2dA"),
            new Deck("CjGhARtlXnGXMG7QYKCP8w", "MonoRed Burn", "pauper", "https://moxfield.com/decks/CjGhARtlXnGXMG7QYKCP8w"),
            new Deck("CTGhAaVGNHKrlN4zMJfMcw", "MonoBlue Faeries", "pauper", "https://moxfield.com/decks/CTGhAaVGNHKrlN4zMJfMcw"),
            new Deck("CjGhAfg9XHubr6_vlmEDIw", "MonoBlack Devotion", "pauper", "https://moxfield.com/decks/CjGhAfg9XHubr6_vlmEDIw"),
            new Deck("CjGhAT2Nf3ObgKZuinMgog", "Mardu Synth", "pauper", "https://moxfield.com/decks/CjGhAT2Nf3ObgKZuinMgog"),
            new Deck("BTGhATRKNHGlBnLP_lIycg", "Jund Wildfire", "pauper", "https://moxfield.com/decks/BTGhATRKNHGlBnLP_lIycg"),
            new Deck("CTGhAT-Ob3So2UKIyiB5VQ", "Jeskai Ephemerate", "pauper", "https://moxfield.com/decks/CTGhAT-Ob3So2UKIyiB5VQ"),
            new Deck("BTGhAcFrjn-mGR50ai0d3w", "Gruul Tokens", "pauper", "https://moxfield.com/decks/BTGhAcFrjn-mGR50ai0d3w"),
            new Deck("BTGhAWiQdXqq3we9JHPrHA", "Gruul Ponza", "pauper", "https://moxfield.com/decks/BTGhAWiQdXqq3we9JHPrHA"),
            new Deck("CDGhASO6w3KBAcdkDE5CGg", "Gruul Monsters", "pauper", "https://moxfield.com/decks/CDGhASO6w3KBAcdkDE5CGg"),
            new Deck("CTGhARoyrXa6aXa3sJDqMg", "Grixis Affinity", "pauper", "https://moxfield.com/decks/CTGhARoyrXa6aXa3sJDqMg"),
            new Deck("CDGhAfrRFXGr4fW91yLzqA", "Golgari Pestilence", "pauper", "https://moxfield.com/decks/CDGhAfrRFXGr4fW91yLzqA"),
            new Deck("CDGhAb7puXeqCP496AwPpg", "Gardens", "pauper", "https://moxfield.com/decks/CDGhAb7puXeqCP496AwPpg"),
            new Deck("CTGhAfymSHCNWXUHwTU2Qg", "Elves", "pauper", "https://moxfield.com/decks/CTGhAfymSHCNWXUHwTU2Qg"),
            new Deck("CjGhAXTbIX6Tk9W4B3uYVg", "Caw Gates", "pauper", "https://moxfield.com/decks/CjGhAXTbIX6Tk9W4B3uYVg"),
            new Deck("CTGhAYgBan22FDmBlVqM8A", "Boros Moxite", "pauper", "https://moxfield.com/decks/CTGhAYgBan22FDmBlVqM8A"),
            new Deck("CjGhAf6-1H6nmZUkeib9qA", "Bogles", "pauper", "https://moxfield.com/decks/CjGhAf6-1H6nmZUkeib9qA"),
            new Deck("CTGhAY3R2Ha_LcigVicRCw", "BlackSac", "pauper", "https://moxfield.com/decks/CTGhAY3R2Ha_LcigVicRCw"),
            new Deck("CTGhAfr8a3-rYKh1XJMg0w", "Balustrade Spy", "pauper", "https://moxfield.com/decks/CTGhAfr8a3-rYKh1XJMg0w"),
            new Deck("CjGhAZAS4nCBzSeHM5tVdw", "Altar Combo", "pauper", "https://moxfield.com/decks/CjGhAZAS4nCBzSeHM5tVdw")
    );

    private DeckCatalog() {
    }
}
