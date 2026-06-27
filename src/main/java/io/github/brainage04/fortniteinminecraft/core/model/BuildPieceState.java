package io.github.brainage04.fortniteinminecraft.core.model;

import java.util.Objects;
import java.util.UUID;

public record BuildPieceState(
        UUID id,
        UUID owner,
        BuildSlot slot,
        MaterialType material,
        int currentHealth,
        int maxHealth,
        long placedAtTick,
        String editVariant
) {
    public static final String BASE_VARIANT = "base";

    public BuildPieceState {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(editVariant, "editVariant");
        if (maxHealth <= 0) {
            throw new IllegalArgumentException("maxHealth must be positive");
        }
        if (currentHealth < 0 || currentHealth > maxHealth) {
            throw new IllegalArgumentException("currentHealth must be between 0 and maxHealth");
        }
    }

    public static BuildPieceState placed(BuildSlot slot, MaterialType material, UUID owner, long placedAtTick) {
        int maxHealth = material.finalHealth();
        return new BuildPieceState(UUID.randomUUID(), owner, slot, material, maxHealth, maxHealth, placedAtTick, BASE_VARIANT);
    }
}
