package io.github.brainage04.fortniteinminecraft.core.placement;

import io.github.brainage04.fortniteinminecraft.core.model.BlockOffset;
import io.github.brainage04.fortniteinminecraft.core.model.BuildGridPos;
import io.github.brainage04.fortniteinminecraft.core.model.PieceFootprint;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;

import java.util.Objects;

public final class SupportValidator {
    public boolean isSupported(BuildWorldState state, PieceFootprint footprint, BlockOffset origin, WorldObstruction obstruction) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(footprint, "footprint");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(obstruction, "obstruction");

        BuildGridPos gridPos = footprint.slot().gridPos();
        if (gridPos.y() == 0) {
            return true;
        }

        BuildGridPos below = new BuildGridPos(gridPos.dimension(), gridPos.x(), gridPos.y() - 1, gridPos.z());
        if (state.hasAnyAt(below)) {
            return true;
        }

        for (BlockOffset block : footprint.localBlocks()) {
            if (block.y() == 0) {
                BlockOffset absolute = origin.add(block);
                if (obstruction.isSolid(gridPos.dimension(), absolute.x(), absolute.y() - 1, absolute.z())) {
                    return true;
                }
            }
        }
        return false;
    }
}
