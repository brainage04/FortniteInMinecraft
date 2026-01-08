package com.github.brainage04.fortnite_in_minecraft.item.weapon.core;

// todo: make this an interface instead
public class GunStats {
    public final int damage;
    public final int cooldown;
    public final int ticksToReload;

    public int currentCapacity;
    public final int maxCapacity;

    protected GunStats(int damage, int cooldown, int ticksToReload, int maxCapacity) {
        this.damage = damage;
        this.cooldown = cooldown;
        this.ticksToReload = ticksToReload;

        this.currentCapacity = maxCapacity;
        this.maxCapacity = maxCapacity;
    }
}
