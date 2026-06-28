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
        long lastHealthUpdateTick,
        String editVariant
) {
    public static final String BASE_VARIANT = "base";

    public BuildPieceState {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(editVariant, "editVariant");
        if (maxHealth <= 0) {
            throw new IllegalArgumentException("maxHealth must be positive");
        }
        if (currentHealth < 0 || currentHealth > maxHealth) {
            throw new IllegalArgumentException("currentHealth must be between 0 and maxHealth");
        }
        if (placedAtTick < 0 || lastHealthUpdateTick < placedAtTick) {
            throw new IllegalArgumentException("health update ticks must not predate placement");
        }
    }

    public static BuildPieceState placed(BuildSlot slot, MaterialType material, UUID owner, long placedAtTick) {
        Objects.requireNonNull(material, "material");
        return new BuildPieceState(
                UUID.randomUUID(),
                owner,
                slot,
                material,
                material.initialHealth(),
                material.finalHealth(),
                placedAtTick,
                placedAtTick,
                BASE_VARIANT
        );
    }

    public BuildPieceState progressedTo(long tick) {
        if (tick <= lastHealthUpdateTick || currentHealth >= maxHealth) {
            return this;
        }
        int gainedHealth = constructionHealthAt(tick) - constructionHealthAt(lastHealthUpdateTick);
        if (gainedHealth <= 0) {
            return new BuildPieceState(id, owner, slot, material, currentHealth, maxHealth, placedAtTick, tick, editVariant);
        }
        return withHealth(Math.min(maxHealth, currentHealth + gainedHealth), tick);
    }

    public BuildPieceState damagedBy(int damage, long tick) {
        if (damage <= 0) {
            return progressedTo(tick);
        }
        BuildPieceState progressed = progressedTo(tick);
        return progressed.withHealth(Math.max(0, progressed.currentHealth - damage), Math.max(tick, progressed.lastHealthUpdateTick));
    }

    public double healthRatio() {
        return (double) currentHealth / maxHealth;
    }

    public boolean destroyed() {
        return currentHealth <= 0;
    }

    private BuildPieceState withHealth(int health, long tick) {
        return new BuildPieceState(id, owner, slot, material, health, maxHealth, placedAtTick, tick, editVariant);
    }

    private int constructionHealthAt(long tick) {
        long elapsedTicks = Math.max(0L, tick - placedAtTick);
        int buildTicks = Math.max(1, (int) Math.round(material.buildTimeSeconds() * 20.0D));
        if (elapsedTicks >= buildTicks) {
            return maxHealth;
        }
        int totalGrowth = maxHealth - material.initialHealth();
        return material.initialHealth() + (int) Math.floor(totalGrowth * (double) elapsedTicks / buildTicks);
    }
}
