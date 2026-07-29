package io.github.brainage04.fortniteinminecraft.server.world;

import io.github.brainage04.fortniteinminecraft.core.placement.WorldObstruction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;

public final class WorldObstructions {
    private WorldObstructions() {
    }

    public static WorldObstruction trackedBuildAware(ServerLevel level, WorldBuildMaterializer materializer) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(materializer, "materializer");
        return (dimension, blockX, blockY, blockZ) -> {
            BlockPos pos = new BlockPos(blockX, blockY, blockZ);
            return isBlockingCollision(level, pos)
                    && !materializer.isTrackedBlock(dimension, blockX, blockY, blockZ);
        };
    }

    public static boolean isBlockingCollision(ServerLevel level, BlockPos pos) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");
        return isBlockingCollision(level, pos, level.getBlockState(pos));
    }

    public static boolean isBlockingCollision(ServerLevel level, BlockPos pos, BlockState state) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(state, "state");
        return !state.canBeReplaced() && !state.getCollisionShape(level, pos).isEmpty();
    }
}
