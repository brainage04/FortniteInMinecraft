package io.github.brainage04.fortniteinminecraft.core.item;

public record WeaponStats(
        double damage,
        double criticalMultiplier,
        int magazineSize,
        double fireRatePerSecond,
        double reloadSeconds,
        int pellets,
        double rangeBlocks
) {
    public WeaponStats {
        if (damage < 0.0D) {
            throw new IllegalArgumentException("damage cannot be negative");
        }
        if (criticalMultiplier <= 0.0D) {
            throw new IllegalArgumentException("criticalMultiplier must be positive");
        }
        if (magazineSize <= 0) {
            throw new IllegalArgumentException("magazineSize must be positive");
        }
        if (fireRatePerSecond <= 0.0D) {
            throw new IllegalArgumentException("fireRatePerSecond must be positive");
        }
        if (reloadSeconds < 0.0D) {
            throw new IllegalArgumentException("reloadSeconds cannot be negative");
        }
        if (pellets <= 0) {
            throw new IllegalArgumentException("pellets must be positive");
        }
        if (rangeBlocks <= 0.0D) {
            throw new IllegalArgumentException("rangeBlocks must be positive");
        }
    }
}
