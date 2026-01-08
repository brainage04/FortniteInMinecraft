package com.github.brainage04.fortnite_in_minecraft.item.weapon.core;

public class ProjectileGunStats extends GunStats {
    public final int velocity;
    public final int drop;

    public ProjectileGunStats(int damage, int cooldown, int timeToReload, int maxCapacity, int velocity, int drop) {
        super(damage, cooldown, timeToReload, maxCapacity);
        this.velocity = velocity;
        this.drop = drop;
    }
}
