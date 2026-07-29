package io.github.brainage04.fortniteinminecraft.core.session;

import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class ResourceWallet {
    private final EnumMap<MaterialType, Integer> amounts = new EnumMap<>(MaterialType.class);
    private boolean infinite;

    public ResourceWallet() {
        for (MaterialType material : MaterialType.values()) {
            amounts.put(material, 0);
        }
    }

    public static ResourceWallet with(MaterialType material, int amount) {
        ResourceWallet wallet = new ResourceWallet();
        wallet.add(material, amount);
        return wallet;
    }

    public int get(MaterialType material) {
        return amounts.get(Objects.requireNonNull(material, "material"));
    }

    public Map<MaterialType, Integer> snapshot() {
        return Map.copyOf(amounts);
    }

    public void set(MaterialType material, int amount) {
        Objects.requireNonNull(material, "material");
        if (amount < 0) {
            throw new IllegalArgumentException("amount cannot be negative");
        }
        amounts.put(material, amount);
    }

    public void add(MaterialType material, int amount) {
        Objects.requireNonNull(material, "material");
        if (amount < 0) {
            throw new IllegalArgumentException("amount cannot be negative");
        }
        amounts.merge(material, amount, Integer::sum);
    }

    public void clear(MaterialType material) {
        set(material, 0);
    }

    public void clear() {
        for (MaterialType material : MaterialType.values()) {
            amounts.put(material, 0);
        }
    }

    public boolean infinite() {
        return infinite;
    }

    public void setInfinite(boolean infinite) {
        this.infinite = infinite;
    }

    public boolean canSpend(MaterialType material, int amount) {
        Objects.requireNonNull(material, "material");
        if (amount < 0) {
            throw new IllegalArgumentException("amount cannot be negative");
        }
        return infinite || get(material) >= amount;
    }

    public boolean spend(MaterialType material, int amount) {
        if (!canSpend(material, amount)) {
            return false;
        }
        if (!infinite) {
            amounts.put(material, get(material) - amount);
        }
        return true;
    }
}
