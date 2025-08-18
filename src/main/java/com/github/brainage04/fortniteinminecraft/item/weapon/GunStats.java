package com.github.brainage04.fortniteinminecraft.item.weapon;

// todo: make this an interface instead
public class GunStats {
    public final int damage;
    public final int cooldown;

    public int currentCapacity;
    public final int maxCapacity;

    public GunStats(int damage, int cooldown, int maxCapacity) {
        this.damage = damage;
        this.cooldown = cooldown;

        this.currentCapacity = maxCapacity;
        this.maxCapacity = maxCapacity;
    }
}
