package com.liga.calendar;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
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
    void getDecksCachesResultsWithinTheTtl() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        TestableService service = new TestableService();
        service.clock = clock;
        List<Deck> result = List.of(new Deck("1", "Esper Affinity", "pauper", "https://moxfield.com/decks/1", List.of("W")));
        service.nextResult = result;

        List<Deck> first = service.getDecks();
        clock.advance(Duration.ofMinutes(5));
        List<Deck> second = service.getDecks();

        assertThat(first).isEqualTo(result);
        assertThat(second).isEqualTo(result);
        assertThat(service.fetchCount).isEqualTo(1);
    }

    @Test
    void getDecksRefetchesAfterTheTtlExpires() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        TestableService service = new TestableService();
        service.clock = clock;
        List<Deck> first = List.of(new Deck("1", "Esper Affinity", "pauper", "https://moxfield.com/decks/1", List.of("W")));
        List<Deck> second = List.of(new Deck("2", "Dimir Control", "pauper", "https://moxfield.com/decks/2", List.of("U", "B")));

        service.nextResult = first;
        assertThat(service.getDecks()).isEqualTo(first);

        clock.advance(Duration.ofMinutes(16));
        service.nextResult = second;
        assertThat(service.getDecks()).isEqualTo(second);
        assertThat(service.fetchCount).isEqualTo(2);
    }

    @Test
    void getDecksFallsBackToTheLastGoodResultWhenARefreshFails() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        TestableService service = new TestableService();
        service.clock = clock;
        List<Deck> good = List.of(new Deck("1", "Esper Affinity", "pauper", "https://moxfield.com/decks/1", List.of("W")));

        service.nextResult = good;
        assertThat(service.getDecks()).isEqualTo(good);

        clock.advance(Duration.ofMinutes(16));
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

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.systemDefault();
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
