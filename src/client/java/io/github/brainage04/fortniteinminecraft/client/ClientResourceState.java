package io.github.brainage04.fortniteinminecraft.client;

import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.server.item.AmmoType;
import io.github.brainage04.fortniteinminecraft.network.FortnitePayloads.ResourceStatePayload;

import java.util.Objects;

public final class ClientResourceState {
    private static ResourceStatePayload state = new ResourceStatePayload(0, 0, 0, 0, 0, 0, 0, 0, 0, false, false);

    private ClientResourceState() {
    }

    public static void update(ResourceStatePayload payload) {
        state = Objects.requireNonNull(payload, "payload");
    }

    public static int material(MaterialType material) {
        return switch (Objects.requireNonNull(material, "material")) {
            case WOOD -> state.wood();
            case STONE -> state.stone();
            case METAL -> state.metal();
        };
    }

    public static int ammo(AmmoType type) {
        return switch (Objects.requireNonNull(type, "type")) {
            case LIGHT -> state.lightAmmo();
            case MEDIUM -> state.mediumAmmo();
            case SHELLS -> state.shells();
            case HEAVY -> state.heavyAmmo();
            case ROCKETS -> state.rockets();
        };
    }

    public static int gold() {
        return state.gold();
    }

    public static boolean infiniteMaterials() {
        return state.infiniteMaterials();
    }

    public static boolean infiniteAmmo() {
        return state.infiniteAmmo();
    }
}
