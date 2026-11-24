package com.liga.calendar;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoxfieldSyncControllerTest {

    private static final String VALID_PAYLOAD = """
            {"data": [
                {"publicId": "abc123", "name": "Esper Affinity", "format": "pauper",
                 "publicUrl": "https://moxfield.com/decks/abc123", "visibility": "public",
                 "colors": ["W", "U", "B"]},
                {"publicId": "def456", "name": "Secret Tech", "format": "pauper",
                 "publicUrl": "https://moxfield.com/decks/def456", "visibility": "private",
                 "colors": ["U"]}
            ]}
            """;

    private MoxfieldSyncController controller(String syncToken) {
        return new MoxfieldSyncController(new MoxfieldDeckService(), syncToken);
    }

    @Test
    void syncIsRejectedWithoutAMatchingToken() {
        assertThatThrownBy(() -> controller("secret").sync("wrong", VALID_PAYLOAD))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void syncIsDisabledWhenNoTokenIsConfigured() {
        assertThatThrownBy(() -> controller("").sync(null, VALID_PAYLOAD))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void syncAppliesOnlyThePublicDecksFromThePayload() {
        // fetchFromMoxfield stubbed out to null -- as it always is on the
        // real deployment this endpoint exists for -- so getDecks() falls
        // through to whatever sync() just applied, without this test
        // actually shelling out to curl.
        MoxfieldDeckService deckService = new MoxfieldDeckService() {
            @Override
            List<Deck> fetchFromMoxfield() {
                return null;
            }
        };
        MoxfieldSyncController controller = new MoxfieldSyncController(deckService, "secret");

        ResponseEntity<String> response = controller.sync("secret", VALID_PAYLOAD);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(deckService.getDecks()).extracting(Deck::name).containsExactly("Esper Affinity");
    }

    @Test
    void syncRejectsAnInvalidPayload() {
        assertThatThrownBy(() -> controller("secret").sync("secret", "not json"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
