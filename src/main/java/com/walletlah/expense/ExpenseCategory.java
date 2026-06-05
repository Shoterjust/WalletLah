package com.walletlah.expense;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public enum ExpenseCategory {
    FOOD("Food", Set.of("food", "eat", "meal", "lunch", "dinner", "breakfast", "kopi", "coffee")),
    TRANSPORT("Transport", Set.of("transport", "mrt", "bus", "grab", "taxi", "gojek")),
    GROCERIES("Groceries", Set.of("groceries", "grocery", "supermarket", "fairprice", "sheng", "siong", "cold", "storage", "giant")),
    SCHOOL("School", Set.of("school", "uni", "university", "notes", "books", "printing")),
    SUBSCRIPTIONS("Subscriptions", Set.of("subscriptions", "subscription", "sub", "netflix", "spotify")),
    SHOPPING("Shopping", Set.of("shopping", "shop", "clothes", "shopee", "lazada")),
    ENTERTAINMENT("Entertainment", Set.of("entertainment", "movie", "movies", "game", "games", "kbox")),
    HEALTH("Health", Set.of("health", "doctor", "clinic", "medicine", "gym")),
    FAMILY("Family", Set.of("family", "parents", "home")),
    OTHERS("Others", Set.of("others", "other", "misc", "miscellaneous"));

    private final String displayName;
    private final Set<String> aliases;

    ExpenseCategory(String displayName, Set<String> aliases) {
        this.displayName = displayName;
        this.aliases = aliases;
    }

    public String displayName() {
        return displayName;
    }

    public static Optional<ExpenseCategory> from(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Optional.empty();
        }
        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(category -> category.name().equalsIgnoreCase(normalized)
                        || category.displayName.equalsIgnoreCase(normalized)
                        || category.aliases.contains(normalized))
                .findFirst();
    }

    public static String categoryListForHelp() {
        return Arrays.stream(values())
                .map(category -> "- " + category.displayName)
                .collect(Collectors.joining("\n"));
    }
}
