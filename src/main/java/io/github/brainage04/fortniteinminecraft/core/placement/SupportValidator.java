package io.github.brainage04.fortniteinminecraft.core.placement;

import io.github.brainage04.fortniteinminecraft.core.BuildConstants;
import io.github.brainage04.fortniteinminecraft.core.model.BlockOffset;
import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.PieceFootprint;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class SupportValidator {
    private static final int[][] FACE_NEIGHBORS = {
            {1, 0, 0},
            {-1, 0, 0},
            {0, 1, 0},
            {0, -1, 0},
            {0, 0, 1},
            {0, 0, -1}
    };

    private final FootprintProjector footprints;
    private final SnapGrid snapGrid;

    public SupportValidator(BuildRules rules) {
        Objects.requireNonNull(rules, "rules");
        this.footprints = new FootprintProjector(rules);
        this.snapGrid = new SnapGrid(rules);
    }

    public boolean hasRequiredSupport(
            BuildWorldState state,
            PieceFootprint candidate,
            List<BlockOffset> candidateBlocks,
            WorldObstruction obstruction
    ) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(candidate, "candidate");
        Objects.requireNonNull(candidateBlocks, "candidateBlocks");
        Objects.requireNonNull(obstruction, "obstruction");

        String dimension = candidate.slot().gridPos().dimension();
        Set<BlockOffset> candidateSet = new HashSet<>(candidateBlocks);
        Set<BlockOffset> pieceBlocks = pieceBlocksInDimension(state, dimension);
        int supportedBlocks = 0;
        for (BlockOffset block : candidateBlocks) {
            if (isSupported(dimension, block, candidateSet, pieceBlocks, obstruction)) {
                supportedBlocks++;
                if (supportedBlocks >= BuildConstants.MIN_SUPPORTED_PLACEMENT_BLOCKS) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSupported(
            String dimension,
            BlockOffset block,
            Set<BlockOffset> candidateSet,
            Set<BlockOffset> pieceBlocks,
            WorldObstruction obstruction
    ) {
        if (pieceBlocks.contains(block) || obstruction.isSolid(dimension, block.x(), block.y(), block.z())) {
            return true;
        }
        for (int[] neighbor : FACE_NEIGHBORS) {
            BlockOffset adjacent = new BlockOffset(
                    block.x() + neighbor[0],
                    block.y() + neighbor[1],
                    block.z() + neighbor[2]
            );
            if (candidateSet.contains(adjacent)) {
                continue;
            }
            if (pieceBlocks.contains(adjacent)
                    || obstruction.isSolid(dimension, adjacent.x(), adjacent.y(), adjacent.z())) {
                return true;
            }
        }
        return false;
    }

    private Set<BlockOffset> pieceBlocksInDimension(BuildWorldState state, String dimension) {
        HashSet<BlockOffset> result = new HashSet<>();
        for (BuildPieceState piece : state.pieces()) {
            if (!dimension.equals(piece.slot().gridPos().dimension())) {
                continue;
            }
            PieceFootprint footprint = footprints.project(piece.slot());
            result.addAll(footprint.absoluteBlocks(snapGrid.blockOrigin(piece.slot().gridPos())));
        }
        return result;
    }
}
