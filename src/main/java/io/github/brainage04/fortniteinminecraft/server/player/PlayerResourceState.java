package io.github.brainage04.fortniteinminecraft.server.player;

import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.session.ResourceWallet;
import io.github.brainage04.fortniteinminecraft.server.item.AmmoType;

import java.util.EnumMap;
import java.util.Objects;

public final class PlayerResourceState {
    public static final int MAX_MATERIAL = 999;
    public static final int MAX_AMMO = 999;
    public static final int MAX_GOLD = 5_000;

    private final ResourceWallet materials = new ResourceWallet();
    private final EnumMap<AmmoType, Integer> ammo = new EnumMap<>(AmmoType.class);
    private int gold;

    public PlayerResourceState() {
        for (AmmoType type : AmmoType.values()) {
            ammo.put(type, 0);
        }
    }

    public ResourceWallet materials() {
        return materials;
    }

    public int material(MaterialType material) {
        return materials.get(Objects.requireNonNull(material, "material"));
    }

    public int ammo(AmmoType type) {
        return ammo.get(Objects.requireNonNull(type, "type"));
    }

    public int gold() {
        return gold;
    }

    public int addMaterial(MaterialType material, int amount) {
        Objects.requireNonNull(material, "material");
        int accepted = acceptedAmount(material(material), amount, MAX_MATERIAL);
        materials.add(material, accepted);
        return accepted;
    }

    public int addAmmo(AmmoType type, int amount) {
        Objects.requireNonNull(type, "type");
        int accepted = acceptedAmount(ammo(type), amount, MAX_AMMO);
        ammo.put(type, ammo(type) + accepted);
        return accepted;
    }

    public int addGold(int amount) {
        int accepted = acceptedAmount(gold, amount, MAX_GOLD);
        gold += accepted;
        return accepted;
    }

    private static int acceptedAmount(int current, int amount, int cap) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount cannot be negative");
        }
        if (amount == 0 || current >= cap) {
            return 0;
        }
        return Math.min(amount, cap - current);
    }
}
