package io.github.brainage04.fortniteinminecraft.core.rules;

import io.github.brainage04.fortniteinminecraft.core.BuildConstants;

public record BuildRules(int gridStrideBlocks, int gridCenterOffsetBlocks, int footprintSizeBlocks, int wallHeightBlocks) {
    public BuildRules {
        if (gridStrideBlocks <= 0) {
            throw new IllegalArgumentException("gridStrideBlocks must be positive");
        }
        if (footprintSizeBlocks <= 0) {
            throw new IllegalArgumentException("footprintSizeBlocks must be positive");
        }
        if ((footprintSizeBlocks & 1) == 0) {
            throw new IllegalArgumentException("footprintSizeBlocks must be odd so pieces have a center block");
        }
        if (footprintSizeBlocks < gridStrideBlocks) {
            throw new IllegalArgumentException("footprintSizeBlocks must be at least gridStrideBlocks");
        }
        if (wallHeightBlocks <= 0) {
            throw new IllegalArgumentException("wallHeightBlocks must be positive");
        }
    }

    public int footprintRadiusBlocks() {
        return footprintSizeBlocks / 2;
    }

    public int originOffsetBlocks() {
        return gridCenterOffsetBlocks - footprintRadiusBlocks();
    }

    public static BuildRules defaults() {
        return new BuildRules(
                BuildConstants.GRID_STRIDE_BLOCKS,
                BuildConstants.GRID_CENTER_OFFSET_BLOCKS,
                BuildConstants.PIECE_FOOTPRINT_BLOCKS,
                BuildConstants.WALL_HEIGHT_BLOCKS
        );
    }
}
