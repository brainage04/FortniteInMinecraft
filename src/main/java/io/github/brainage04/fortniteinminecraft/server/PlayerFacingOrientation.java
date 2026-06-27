package io.github.brainage04.fortniteinminecraft.server;

import io.github.brainage04.fortniteinminecraft.core.model.Orientation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

public final class PlayerFacingOrientation {
    private PlayerFacingOrientation() {
    }

    public static Orientation horizontal(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return switch (player.getDirection()) {
            case NORTH -> Orientation.NORTH;
            case EAST -> Orientation.EAST;
            case SOUTH -> Orientation.SOUTH;
            case WEST -> Orientation.WEST;
            default -> Orientation.NORTH;
        };
    }
}
