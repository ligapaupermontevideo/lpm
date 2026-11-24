package com.liga.calendar;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MoxfieldDeckServiceTest {

    @Test
    void parseDecksOnlyIncludesPublicDecks() throws Exception {
        MoxfieldDeckService service = new MoxfieldDeckService();
        String json = """
                {"data": [
                    {"publicId": "abc123", "name": "Esper Affinity", "format": "pauper",
                     "publicUrl": "https://moxfield.com/decks/abc123", "visibility": "public",
                     "colors": ["W", "U", "B"]},
                    {"publicId": "def456", "name": "Secret Tech", "format": "pauper",
                     "publicUrl": "https://moxfield.com/decks/def456", "visibility": "private",
                     "colors": ["U"]}
                ]}
                """;

        List<Deck> decks = service.parseDecks(json);

        assertThat(decks).extracting(Deck::name).containsExactly("Esper Affinity");
    }

    @Test
    void parseDecksMapsEveryField() throws Exception {
        MoxfieldDeckService service = new MoxfieldDeckService();
        String json = """
                {"data": [
                    {"publicId": "abc123", "name": "Esper Affinity", "format": "pauper",
                     "publicUrl": "https://moxfield.com/decks/abc123", "visibility": "public",
                     "colors": ["W", "U", "B"]}
                ]}
                """;

        List<Deck> decks = service.parseDecks(json);

        assertThat(decks).containsExactly(
                new Deck("abc123", "Esper Affinity", "pauper", "https://moxfield.com/decks/abc123", List.of("W", "U", "B")));
    }

    @Test
    void getDecksFetchesFreshOnEveryCallInsteadOfCaching() {
        TestableService service = new TestableService();
        List<Deck> first = List.of(new Deck("1", "Esper Affinity", "pauper", "https://moxfield.com/decks/1", List.of("W")));
        List<Deck> second = List.of(new Deck("2", "Dimir Control", "pauper", "https://moxfield.com/decks/2", List.of("U", "B")));

        service.nextResult = first;
        assertThat(service.getDecks()).isEqualTo(first);

        service.nextResult = second;
        assertThat(service.getDecks()).isEqualTo(second);
        assertThat(service.fetchCount).isEqualTo(2);
    }

    @Test
    void getDecksFallsBackToTheLastGoodResultWhenARefreshFails() {
        TestableService service = new TestableService();
        List<Deck> good = List.of(new Deck("1", "Esper Affinity", "pauper", "https://moxfield.com/decks/1", List.of("W")));

        service.nextResult = good;
        assertThat(service.getDecks()).isEqualTo(good);

        service.nextResult = null;
        assertThat(service.getDecks()).isEqualTo(good);
    }

    @Test
    void getDecksFallsBackToTheBundledSnapshotWhenNothingHasEverSucceeded() {
        TestableService service = new TestableService();
        service.nextResult = null;

        assertThat(service.getDecks()).isEqualTo(DeckCatalog.ALL);
    }

    private static final class TestableService extends MoxfieldDeckService {
        List<Deck> nextResult;
        int fetchCount = 0;

        @Override
        List<Deck> fetchFromMoxfield() {
            fetchCount++;
            return nextResult;
        }
    }

}
