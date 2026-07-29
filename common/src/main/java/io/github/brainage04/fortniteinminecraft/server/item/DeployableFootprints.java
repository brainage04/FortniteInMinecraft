package io.github.brainage04.fortniteinminecraft.server.item;

import io.github.brainage04.fortniteinminecraft.core.BuildConstants;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

final class DeployableFootprints {
    static final int LAUNCH_PAD_SIZE_BLOCKS = 3;
    static final int BUILD_FLOOR_SIZE_BLOCKS = BuildConstants.PIECE_FOOTPRINT_BLOCKS;

    private static final int BLOCK_UPDATE_FLAGS = Block.UPDATE_ALL;

    private DeployableFootprints() {
    }

    static BlockState floorTriggerState() {
        return triggerState(Direction.UP);
    }

    static BlockState triggerState(Direction surfaceNormal) {
        return DeployableTriggerBlocks.triggerState(surfaceNormal);
    }

    static List<BlockPos> centeredFloorSquare(BlockPos center, int sizeBlocks) {
        return centeredSurfaceSquare(center, Direction.UP, sizeBlocks);
    }

    static List<BlockPos> centeredSurfaceSquare(BlockPos center, Direction surfaceNormal, int sizeBlocks) {
        Objects.requireNonNull(center, "center");
        Objects.requireNonNull(surfaceNormal, "surfaceNormal");
        if (sizeBlocks <= 0 || (sizeBlocks & 1) == 0) {
            throw new IllegalArgumentException("footprint size must be a positive odd number");
        }
        int radius = sizeBlocks / 2;
        ArrayList<BlockPos> positions = new ArrayList<>(sizeBlocks * sizeBlocks);
        for (int vertical = -radius; vertical <= radius; vertical++) {
            for (int lateral = -radius; lateral <= radius; lateral++) {
                positions.add(offsetOnSurface(center, surfaceNormal, lateral, vertical));
            }
        }
        return List.copyOf(positions);
    }

    static boolean canPlaceAll(ServerLevel level, List<BlockPos> positions, BlockState state) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(positions, "positions");
        Objects.requireNonNull(state, "state");
        if (positions.isEmpty()) {
            return false;
        }
        LinkedHashSet<BlockPos> unique = new LinkedHashSet<>(positions);
        if (unique.size() != positions.size()) {
            return false;
        }
        for (BlockPos pos : positions) {
            if (!level.isInWorldBounds(pos)
                    || !level.getBlockState(pos).canBeReplaced()
                    || !state.canSurvive(level, pos)) {
                return false;
            }
        }
        return true;
    }

    static boolean placeAll(ServerLevel level, List<BlockPos> positions, BlockState state) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(positions, "positions");
        Objects.requireNonNull(state, "state");
        if (!canPlaceAll(level, positions, state)) {
            return false;
        }
        ArrayList<BlockRestore> placed = new ArrayList<>(positions.size());
        for (BlockPos pos : positions) {
            BlockState previous = level.getBlockState(pos);
            if (!level.setBlock(pos, state, BLOCK_UPDATE_FLAGS)) {
                rollback(level, placed);
                return false;
            }
            placed.add(new BlockRestore(pos, previous));
        }
        return true;
    }

    private static BlockPos offsetOnSurface(BlockPos center, Direction surfaceNormal, int lateral, int vertical) {
        return switch (surfaceNormal.getAxis()) {
            case Y -> center.offset(lateral, 0, vertical);
            case X -> center.offset(0, vertical, lateral);
            case Z -> center.offset(lateral, vertical, 0);
        };
    }

    private static void rollback(ServerLevel level, List<BlockRestore> placed) {
        for (int i = placed.size() - 1; i >= 0; i--) {
            BlockRestore restore = placed.get(i);
            level.setBlock(restore.pos(), restore.state(), BLOCK_UPDATE_FLAGS);
        }
    }

    private record BlockRestore(BlockPos pos, BlockState state) {
        private BlockRestore {
            Objects.requireNonNull(pos, "pos");
            Objects.requireNonNull(state, "state");
        }
    }
}
