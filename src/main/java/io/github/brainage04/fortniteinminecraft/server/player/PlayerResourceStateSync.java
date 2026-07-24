package io.github.brainage04.fortniteinminecraft.server.player;

import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.server.item.AmmoType;
import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.network.FortnitePayloads.ResourceStatePayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

public final class PlayerResourceStateSync {
    private PlayerResourceStateSync() {
    }

    public static void send(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        if (!FortniteInMinecraft.platform().canSendToPlayer(player, ResourceStatePayload.TYPE)) {
            return;
        }
        PlayerResourceState state = PlayerResourceStates.stateFor(player);
        FortniteInMinecraft.platform().sendToPlayer(player, new ResourceStatePayload(
                state.material(MaterialType.WOOD),
                state.material(MaterialType.STONE),
                state.material(MaterialType.METAL),
                state.gold(),
                state.ammo(AmmoType.LIGHT),
                state.ammo(AmmoType.MEDIUM),
                state.ammo(AmmoType.SHELLS),
                state.ammo(AmmoType.HEAVY),
                state.ammo(AmmoType.ROCKETS),
                state.infiniteMaterials(),
                state.infiniteAmmo()
        ));
    }
}
