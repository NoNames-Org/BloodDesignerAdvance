package dev.no.name.bdadvance.party;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class NameSuggestions {
    public record Suggestion(String name, boolean friend) {
    }

    private NameSuggestions() {
    }

    public static List<Suggestion> suggest(String typed, List<String> friends, List<String> players, String self, int limit) {
        String query = typed.strip().toLowerCase(Locale.ROOT);
        Map<String, Suggestion> unique = new LinkedHashMap<>();
        addMatches(unique, friends, query, self, true);
        addMatches(unique, players, query, self, false);
        List<Suggestion> result = new ArrayList<>(unique.values());

        result.removeIf(suggestion -> suggestion.name().equalsIgnoreCase(query));
        return List.copyOf(result.subList(0, Math.min(limit, result.size())));
    }

    private static void addMatches(Map<String, Suggestion> unique, List<String> names, String query, String self, boolean friend) {
        List<String> sorted = new ArrayList<>(names);
        sorted.sort(Comparator.comparing((String name) -> !name.toLowerCase(Locale.ROOT).startsWith(query))
                .thenComparing(name -> name.toLowerCase(Locale.ROOT)));
        for (String name : sorted) {
            String key = name.toLowerCase(Locale.ROOT);
            if (key.contains(query) && (self == null || !key.equals(self.toLowerCase(Locale.ROOT)))) {
                unique.putIfAbsent(key, new Suggestion(name, friend));
            }
        }
    }
}
