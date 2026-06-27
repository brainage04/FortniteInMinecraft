package io.github.brainage04.fortniteinminecraft.core.placement;

import io.github.brainage04.fortniteinminecraft.core.model.BlockOffset;
import io.github.brainage04.fortniteinminecraft.core.model.BuildGridPos;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;

import java.util.Objects;

public final class SnapGrid {
    private final BuildRules rules;

    public SnapGrid(BuildRules rules) {
        this.rules = Objects.requireNonNull(rules, "rules");
    }

    public BuildGridPos snap(String dimension, int blockX, int blockY, int blockZ) {
        return new BuildGridPos(
                dimension,
                snapAxis(blockX),
                snapAxis(blockY),
                snapAxis(blockZ)
        );
    }

    public BlockOffset blockOrigin(BuildGridPos gridPos) {
        Objects.requireNonNull(gridPos, "gridPos");
        int stride = rules.gridStrideBlocks();
        int offset = rules.originOffsetBlocks();
        return new BlockOffset(
                gridPos.x() * stride + offset,
                gridPos.y() * stride + offset,
                gridPos.z() * stride + offset
        );
    }

    private int snapAxis(int block) {
        return Math.round((float) (block - rules.gridCenterOffsetBlocks()) / rules.gridStrideBlocks());
    }
}
