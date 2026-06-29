package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.server.player.PlayerResourceState;

import java.util.Locale;
import java.util.Objects;

public record PickupPayload(MaterialType material, AmmoType ammoType, int amount, int goldAmount) {
    public PickupPayload {
        if (amount < 0 || goldAmount < 0) {
            throw new IllegalArgumentException("pickup amounts cannot be negative");
        }
        int channels = (material == null ? 0 : 1) + (ammoType == null ? 0 : 1) + (goldAmount > 0 ? 1 : 0);
        if (channels != 1) {
            throw new IllegalArgumentException("pickup payload must grant exactly one resource channel");
        }
        if ((material != null || ammoType != null) && amount <= 0) {
            throw new IllegalArgumentException("material and ammo pickups need a positive amount");
        }
    }

    public static PickupPayload material(MaterialType material, int amount) {
        return new PickupPayload(Objects.requireNonNull(material, "material"), null, amount, 0);
    }

    public static PickupPayload ammo(AmmoType type, int amount) {
        return new PickupPayload(null, Objects.requireNonNull(type, "type"), amount, 0);
    }

    public static PickupPayload gold(int amount) {
        return new PickupPayload(null, null, 0, amount);
    }

    public PickupResult applyTo(PlayerResourceState state) {
        Objects.requireNonNull(state, "state");
        if (material != null) {
            return new PickupResult(label(), amount, state.addMaterial(material, amount));
        }
        if (ammoType != null) {
            return new PickupResult(label(), amount, state.addAmmo(ammoType, amount));
        }
        return new PickupResult(label(), goldAmount, state.addGold(goldAmount));
    }

    public String label() {
        if (material != null) {
            return titleCase(material.name());
        }
        if (ammoType != null) {
            return ammoType.label();
        }
        return "Gold";
    }

    private static String titleCase(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    public record PickupResult(String label, int requested, int accepted) {
        public PickupResult {
            Objects.requireNonNull(label, "label");
            if (requested < 0 || accepted < 0 || accepted > requested) {
                throw new IllegalArgumentException("accepted amount must be within requested amount");
            }
        }

        public boolean granted() {
            return accepted > 0;
        }
    }
}
