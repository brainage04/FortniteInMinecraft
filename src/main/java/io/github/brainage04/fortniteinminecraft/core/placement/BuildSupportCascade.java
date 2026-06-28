package io.github.brainage04.fortniteinminecraft.core.placement;

import io.github.brainage04.fortniteinminecraft.core.model.BlockOffset;
import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.PieceFootprint;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class BuildSupportCascade {
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

    public BuildSupportCascade(BuildRules rules) {
        Objects.requireNonNull(rules, "rules");
        this.footprints = new FootprintProjector(rules);
        this.snapGrid = new SnapGrid(rules);
    }

    public List<BuildPieceState> unsupportedPieces(BuildWorldState state, String dimension, WorldObstruction obstruction) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(obstruction, "obstruction");

        Map<BuildSlot, PieceBlocks> pieces = piecesInDimension(state, dimension);
        Map<BlockOffset, LinkedHashSet<BuildSlot>> slotsByBlock = slotsByBlock(pieces);
        Map<BuildSlot, LinkedHashSet<BuildSlot>> adjacency = adjacency(pieces, slotsByBlock);
        LinkedHashSet<BuildSlot> supported = supportedSlots(pieces, slotsByBlock, adjacency, dimension, obstruction);

        ArrayList<BuildPieceState> unsupported = new ArrayList<>();
        for (PieceBlocks piece : pieces.values()) {
            if (!supported.contains(piece.piece().slot())) {
                unsupported.add(piece.piece());
            }
        }
        return List.copyOf(unsupported);
    }

    private Map<BuildSlot, PieceBlocks> piecesInDimension(BuildWorldState state, String dimension) {
        LinkedHashMap<BuildSlot, PieceBlocks> result = new LinkedHashMap<>();
        for (BuildPieceState piece : state.pieces()) {
            BuildSlot slot = piece.slot();
            if (!dimension.equals(slot.gridPos().dimension())) {
                continue;
            }
            PieceFootprint footprint = footprints.project(slot);
            BlockOffset origin = snapGrid.blockOrigin(slot.gridPos());
            result.put(slot, new PieceBlocks(piece, footprint.absoluteBlocks(origin)));
        }
        return result;
    }

    private static Map<BlockOffset, LinkedHashSet<BuildSlot>> slotsByBlock(Map<BuildSlot, PieceBlocks> pieces) {
        HashMap<BlockOffset, LinkedHashSet<BuildSlot>> result = new HashMap<>();
        for (PieceBlocks piece : pieces.values()) {
            for (BlockOffset block : piece.blocks()) {
                result.computeIfAbsent(block, ignored -> new LinkedHashSet<>()).add(piece.piece().slot());
            }
        }
        return result;
    }

    private static Map<BuildSlot, LinkedHashSet<BuildSlot>> adjacency(
            Map<BuildSlot, PieceBlocks> pieces,
            Map<BlockOffset, LinkedHashSet<BuildSlot>> slotsByBlock
    ) {
        HashMap<BuildSlot, LinkedHashSet<BuildSlot>> result = new HashMap<>();
        for (BuildSlot slot : pieces.keySet()) {
            result.put(slot, new LinkedHashSet<>());
        }
        for (Map.Entry<BlockOffset, LinkedHashSet<BuildSlot>> entry : slotsByBlock.entrySet()) {
            connectAll(entry.getValue(), result);
            for (BlockOffset neighbor : neighbors(entry.getKey())) {
                LinkedHashSet<BuildSlot> neighborSlots = slotsByBlock.get(neighbor);
                if (neighborSlots != null) {
                    connectEach(entry.getValue(), neighborSlots, result);
                }
            }
        }
        return result;
    }

    private static LinkedHashSet<BuildSlot> supportedSlots(
            Map<BuildSlot, PieceBlocks> pieces,
            Map<BlockOffset, LinkedHashSet<BuildSlot>> slotsByBlock,
            Map<BuildSlot, LinkedHashSet<BuildSlot>> adjacency,
            String dimension,
            WorldObstruction obstruction
    ) {
        ArrayDeque<BuildSlot> queue = new ArrayDeque<>();
        LinkedHashSet<BuildSlot> supported = new LinkedHashSet<>();
        for (PieceBlocks piece : pieces.values()) {
            BuildSlot slot = piece.piece().slot();
            if (touchesStaticSupport(piece.blocks(), slotsByBlock, dimension, obstruction) && supported.add(slot)) {
                queue.add(slot);
            }
        }
        while (!queue.isEmpty()) {
            BuildSlot current = queue.removeFirst();
            for (BuildSlot next : adjacency.getOrDefault(current, new LinkedHashSet<>())) {
                if (supported.add(next)) {
                    queue.add(next);
                }
            }
        }
        return supported;
    }

    private static boolean touchesStaticSupport(
            List<BlockOffset> blocks,
            Map<BlockOffset, LinkedHashSet<BuildSlot>> slotsByBlock,
            String dimension,
            WorldObstruction obstruction
    ) {
        for (BlockOffset block : blocks) {
            for (BlockOffset neighbor : neighbors(block)) {
                if (!slotsByBlock.containsKey(neighbor)
                        && obstruction.isSolid(dimension, neighbor.x(), neighbor.y(), neighbor.z())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<BlockOffset> neighbors(BlockOffset block) {
        ArrayList<BlockOffset> result = new ArrayList<>(FACE_NEIGHBORS.length);
        for (int[] offset : FACE_NEIGHBORS) {
            result.add(new BlockOffset(block.x() + offset[0], block.y() + offset[1], block.z() + offset[2]));
        }
        return result;
    }

    private static void connectAll(Set<BuildSlot> slots, Map<BuildSlot, LinkedHashSet<BuildSlot>> adjacency) {
        for (BuildSlot left : slots) {
            for (BuildSlot right : slots) {
                connect(left, right, adjacency);
            }
        }
    }

    private static void connectEach(
            Set<BuildSlot> leftSlots,
            Set<BuildSlot> rightSlots,
            Map<BuildSlot, LinkedHashSet<BuildSlot>> adjacency
    ) {
        for (BuildSlot left : leftSlots) {
            for (BuildSlot right : rightSlots) {
                connect(left, right, adjacency);
            }
        }
    }

    private static void connect(BuildSlot left, BuildSlot right, Map<BuildSlot, LinkedHashSet<BuildSlot>> adjacency) {
        if (left.equals(right)) {
            return;
        }
        adjacency.computeIfAbsent(left, ignored -> new LinkedHashSet<>()).add(right);
        adjacency.computeIfAbsent(right, ignored -> new LinkedHashSet<>()).add(left);
    }

    private record PieceBlocks(BuildPieceState piece, List<BlockOffset> blocks) {
        private PieceBlocks {
            Objects.requireNonNull(piece, "piece");
            blocks = List.copyOf(blocks);
        }
    }
}
