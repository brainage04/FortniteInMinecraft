package com.github.brainage04.fortniteinminecraft.item.weapon;

public class ProjectileGunStats extends GunStats {
    public final int velocity;
    public final int drop;

    public ProjectileGunStats(int damage, int cooldown, int maxCapacity, int velocity, int drop) {
        super(damage, cooldown, maxCapacity);
        this.velocity = velocity;
        this.drop = drop;
    }
}
