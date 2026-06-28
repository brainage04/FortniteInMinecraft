package io.github.brainage04.fortniteinminecraft.core.placement;

import io.github.brainage04.fortniteinminecraft.core.BuildConstants;

import io.github.brainage04.fortniteinminecraft.core.model.BlockOffset;
import io.github.brainage04.fortniteinminecraft.core.model.BuildGridPos;
import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.PieceFootprint;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import io.github.brainage04.fortniteinminecraft.core.session.PlayerBuildContext;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class PlacementService {
    private final BuildWorldState state;
    private final SnapGrid snapGrid;
    private final FootprintProjector footprints;
    private final SupportValidator supportValidator;
    private final WorldObstruction obstruction;

    public PlacementService(BuildWorldState state, BuildRules rules, WorldObstruction obstruction) {
        this.state = Objects.requireNonNull(state, "state");
        Objects.requireNonNull(rules, "rules");
        this.snapGrid = new SnapGrid(rules);
        this.footprints = new FootprintProjector(rules);
        this.supportValidator = new SupportValidator(rules);
        this.obstruction = Objects.requireNonNull(obstruction, "obstruction");
    }

    public PlacementPreview preview(PlacementCandidate candidate, PlayerBuildContext player) {
        Objects.requireNonNull(candidate, "candidate");
        Objects.requireNonNull(player, "player");

        Validation validation = validate(candidate, player);
        if (!validation.valid()) {
            return PlacementPreview.rejected(validation.footprint(), validation.failure(), validation.message());
        }
        return PlacementPreview.valid(validation.footprint());
    }

    public PlacementResult place(PlacementCandidate candidate, PlayerBuildContext player, long tick) {
        Objects.requireNonNull(candidate, "candidate");
        Objects.requireNonNull(player, "player");

        Validation validation = validate(candidate, player);
        if (!validation.valid()) {
            return PlacementResult.rejected(validation.failure(), validation.message());
        }

        BuildSlot slot = candidate.slot();
        BuildPieceState piece = BuildPieceState.placed(slot, candidate.material(), player.playerId(), tick);
        if (!state.addIfNotConflicting(piece, validation.absoluteBlocks())) {
            return PlacementResult.rejected(PlacementFailure.OCCUPIED, "build slot became occupied before commit");
        }

        if (!player.creative() && !player.resources().spend(candidate.material(), candidate.material().placementCost())) {
            state.remove(slot);
            return PlacementResult.rejected(PlacementFailure.INSUFFICIENT_RESOURCES, "not enough selected material");
        }

        return PlacementResult.placed(piece, validation.footprint());
    }

    private Validation validate(PlacementCandidate candidate, PlayerBuildContext player) {
        BuildSlot slot = candidate.slot();
        PieceFootprint footprint = footprints.project(slot);
        BuildGridPos gridPos = slot.gridPos();
        BlockOffset origin = snapGrid.blockOrigin(gridPos);

        List<BlockOffset> absoluteBlocks = footprint.absoluteBlocks(origin);
        if (state.conflicts(slot) || overlapsExistingFootprint(slot, absoluteBlocks)) {
            return Validation.rejected(footprint, absoluteBlocks, PlacementFailure.OCCUPIED, "build footprint overlaps an occupied build piece");
        }
        int obstructedBlocks = 0;
        for (BlockOffset block : absoluteBlocks) {
            if (obstruction.isSolid(gridPos.dimension(), block.x(), block.y(), block.z())) {
                obstructedBlocks++;
            }
        }
        int requiredUnobstructedBlocks = requiredUnobstructedBlocks(absoluteBlocks.size());
        int unobstructedBlocks = absoluteBlocks.size() - obstructedBlocks;
        if (unobstructedBlocks < requiredUnobstructedBlocks) {
            return Validation.rejected(
                    footprint,
                    absoluteBlocks,
                    PlacementFailure.OBSTRUCTED,
                    "less than " + BuildConstants.MIN_UNOBSTRUCTED_PLACEMENT_PERCENT + "% of the footprint is unobstructed"
            );
        }

        if (!supportValidator.hasRequiredSupport(state, footprint, absoluteBlocks, obstruction)) {
            return Validation.rejected(footprint, absoluteBlocks, PlacementFailure.UNSUPPORTED, "placement has fewer than " + BuildConstants.MIN_SUPPORTED_PLACEMENT_BLOCKS + " supported blocks");
        }

        int cost = candidate.material().placementCost();
        if (!player.creative() && !player.resources().canSpend(candidate.material(), cost)) {
            return Validation.rejected(footprint, absoluteBlocks, PlacementFailure.INSUFFICIENT_RESOURCES, "not enough selected material");
        }

        return Validation.valid(footprint, absoluteBlocks);
    }

    private boolean overlapsExistingFootprint(BuildSlot candidateSlot, List<BlockOffset> candidateBlocks) {
        String dimension = candidateSlot.gridPos().dimension();
        Set<BlockOffset> candidateSet = new HashSet<>(candidateBlocks);
        for (BuildPieceState existing : state.pieces()) {
            BuildSlot existingSlot = existing.slot();
            if (!dimension.equals(existingSlot.gridPos().dimension())) {
                continue;
            }
            if (intendedModelPermitsFootprintOverlap(existingSlot, candidateSlot)) {
                continue;
            }
            PieceFootprint existingFootprint = footprints.project(existingSlot);
            BlockOffset existingOrigin = snapGrid.blockOrigin(existingSlot.gridPos());
            for (BlockOffset existingBlock : existingFootprint.absoluteBlocks(existingOrigin)) {
                if (candidateSet.contains(existingBlock)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean intendedModelPermitsFootprintOverlap(BuildSlot existing, BuildSlot candidate) {
        return existing.pieceType() == candidate.pieceType();
    }

    private static int requiredUnobstructedBlocks(int totalBlocks) {
        return Math.max(1, (totalBlocks * BuildConstants.MIN_UNOBSTRUCTED_PLACEMENT_PERCENT + 99) / 100);
    }

    private record Validation(boolean valid, PieceFootprint footprint, List<BlockOffset> absoluteBlocks, PlacementFailure failure, String message) {
        private Validation {
            Objects.requireNonNull(footprint, "footprint");
            absoluteBlocks = List.copyOf(absoluteBlocks);
            Objects.requireNonNull(message, "message");
            if (!valid) {
                Objects.requireNonNull(failure, "failure");
            }
        }

        private static Validation valid(PieceFootprint footprint, List<BlockOffset> absoluteBlocks) {
            return new Validation(true, footprint, absoluteBlocks, null, "placement valid");
        }

        private static Validation rejected(PieceFootprint footprint, List<BlockOffset> absoluteBlocks, PlacementFailure failure, String message) {
            return new Validation(false, footprint, absoluteBlocks, failure, message);
        }
    }
}
