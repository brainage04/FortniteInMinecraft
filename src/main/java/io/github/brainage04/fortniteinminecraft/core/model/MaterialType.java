package io.github.brainage04.fortniteinminecraft.core.model;

import io.github.brainage04.fortniteinminecraft.core.BuildConstants;

public enum MaterialType {
    WOOD(150, 90, 4.0, BuildConstants.DEFAULT_RESOURCE_COST),
    STONE(300, 100, 12.0, BuildConstants.DEFAULT_RESOURCE_COST),
    METAL(500, 110, 25.0, BuildConstants.DEFAULT_RESOURCE_COST);

    private final int finalHealth;
    private final int initialHealth;
    private final double buildTimeSeconds;
    private final int placementCost;

    MaterialType(int finalHealth, int initialHealth, double buildTimeSeconds, int placementCost) {
        this.finalHealth = finalHealth;
        this.initialHealth = initialHealth;
        this.buildTimeSeconds = buildTimeSeconds;
        this.placementCost = placementCost;
    }

    public int finalHealth() {
        return finalHealth;
    }

    public int initialHealth() {
        return initialHealth;
    }

    public double buildTimeSeconds() {
        return buildTimeSeconds;
    }

    public int placementCost() {
        return placementCost;
    }
}
