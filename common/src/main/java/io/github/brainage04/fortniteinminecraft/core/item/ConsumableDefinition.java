package io.github.brainage04.fortniteinminecraft.core.item;

import java.util.Objects;

public record ConsumableDefinition(
        String path,
        String displayName,
        FortniteRarity rarity,
        double castSeconds,
        int healthRestore,
        int healthCap,
        int shieldRestore,
        int shieldCap,
        boolean movementLocked,
        String sourceItemId,
        int effectiveRestore
) {
    public ConsumableDefinition {
        path = requireText(path, "path");
        displayName = requireText(displayName, "displayName");
        Objects.requireNonNull(rarity, "rarity");
        if (castSeconds < 0.0D) {
            throw new IllegalArgumentException("castSeconds cannot be negative");
        }
        if (healthRestore < 0 || healthCap < 0 || shieldRestore < 0 || shieldCap < 0 || effectiveRestore < 0) {
            throw new IllegalArgumentException("restore and cap values cannot be negative");
        }
        sourceItemId = requireText(sourceItemId, "sourceItemId");
    }

    public ConsumableDefinition(
            String path,
            String displayName,
            FortniteRarity rarity,
            double castSeconds,
            int healthRestore,
            int healthCap,
            int shieldRestore,
            int shieldCap,
            boolean movementLocked,
            String sourceItemId
    ) {
        this(path, displayName, rarity, castSeconds, healthRestore, healthCap, shieldRestore, shieldCap, movementLocked, sourceItemId, 0);
    }

    public boolean restoresHealth() {
        return healthRestore > 0 || effectiveRestore > 0;
    }

    public boolean restoresShield() {
        return shieldRestore > 0 || effectiveRestore > 0;
    }

    public boolean restoresEffectiveHealth() {
        return effectiveRestore > 0;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return value;
    }
}
