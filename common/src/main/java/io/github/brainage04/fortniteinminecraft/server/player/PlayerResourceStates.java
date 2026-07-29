package io.github.brainage04.fortniteinminecraft.server.player;

import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

public final class PlayerResourceStates {
    private static final Map<ServerPlayer, PlayerResourceState> STATES = Collections.synchronizedMap(new WeakHashMap<>());

    private PlayerResourceStates() {
    }

    public static PlayerResourceState stateFor(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return STATES.computeIfAbsent(player, ignored -> new PlayerResourceState());
    }

    public static void clear(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        STATES.remove(player);
    }
}
