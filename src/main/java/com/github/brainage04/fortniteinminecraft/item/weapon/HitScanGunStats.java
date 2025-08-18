package com.github.brainage04.fortniteinminecraft.item.weapon;

public class HitScanGunStats extends GunStats {
    public final int range;

    public HitScanGunStats(int damage, int cooldown, int maxCapacity, int range) {
        super(damage, cooldown, maxCapacity);
        this.range = range;
    }
}
