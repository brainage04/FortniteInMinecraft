package io.github.brainage04.fortniteinminecraft.core.item;

import java.util.Locale;

public enum FortniteRarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGENDARY;

    public String label() {
        String lower = name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    public String pathPrefix() {
        return name().toLowerCase(Locale.ROOT);
    }
}
