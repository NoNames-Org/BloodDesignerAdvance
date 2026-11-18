package dev.no.name.bdadvance.party;

import dev.no.name.bdadvance.party.NameSuggestions.Suggestion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NameSuggestionsTest {
    private static final List<String> FRIENDS = List.of("Zoey", "Ace_", "1Kit");
    private static final List<String> PLAYERS = List.of("Steve", "Ace_", "Sam_Lexi", "Zed", "Bobby", "Amber");

    @Test
    @DisplayName("друзья первыми, без дублей и без себя")
    void friendsFirst() {
        List<Suggestion> result = NameSuggestions.suggest("", FRIENDS, PLAYERS, "Steve", 6);
        assertEquals(List.of(
                new Suggestion("1Kit", true), new Suggestion("Ace_", true), new Suggestion("Zoey", true),
                new Suggestion("Amber", false), new Suggestion("Bobby", false), new Suggestion("Sam_Lexi", false)), result);
    }

    @Test
    @DisplayName("по началу ника важнее, чем по вхождению; регистр не важен")
    void prefixBeforeContains() {
        List<Suggestion> result = NameSuggestions.suggest("z", FRIENDS, PLAYERS, "Steve", 6);
        assertEquals(List.of(new Suggestion("Zoey", true), new Suggestion("Zed", false)), result);
        List<Suggestion> contains = NameSuggestions.suggest("lex", FRIENDS, PLAYERS, null, 6);
        assertEquals(List.of(new Suggestion("Sam_Lexi", false)), contains);
    }

    @Test
    @DisplayName("не больше лимита и без точного совпадения")
    void limitAndExact() {
        assertEquals(3, NameSuggestions.suggest("", FRIENDS, PLAYERS, null, 3).size());
        assertEquals(List.of(), NameSuggestions.suggest("zed", List.of(), PLAYERS, null, 6));
    }
}
