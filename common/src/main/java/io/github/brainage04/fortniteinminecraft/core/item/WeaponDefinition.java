package io.github.brainage04.fortniteinminecraft.core.item;

import java.util.Objects;

public record WeaponDefinition(
        String path,
        String displayName,
        WeaponCategory category,
        FortniteRarity rarity,
        WeaponStats stats,
        String sourceItemId,
        String sourceStatRow
) {
    public WeaponDefinition {
        path = requireText(path, "path");
        displayName = requireText(displayName, "displayName");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(rarity, "rarity");
        Objects.requireNonNull(stats, "stats");
        sourceItemId = requireText(sourceItemId, "sourceItemId");
        sourceStatRow = requireText(sourceStatRow, "sourceStatRow");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return value;
    }
}
