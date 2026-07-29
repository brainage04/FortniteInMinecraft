package io.github.brainage04.fortniteinminecraft.server.player;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class PlayerAimStates {
    private static final Set<UUID> AIMING_PLAYERS = new HashSet<>();

    private PlayerAimStates() {
    }

    public static void setAiming(ServerPlayer player, boolean aiming) {
        Objects.requireNonNull(player, "player");
        if (aiming) {
            AIMING_PLAYERS.add(player.getUUID());
        } else {
            AIMING_PLAYERS.remove(player.getUUID());
        }
    }

    public static boolean isAiming(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return AIMING_PLAYERS.contains(player.getUUID());
    }

    public static void clear(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        AIMING_PLAYERS.remove(player.getUUID());
    }

    public static void clearAll() {
        AIMING_PLAYERS.clear();
    }
}
