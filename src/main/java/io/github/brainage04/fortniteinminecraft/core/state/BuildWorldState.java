package io.github.brainage04.fortniteinminecraft.core.state;

import io.github.brainage04.fortniteinminecraft.core.model.BlockOffset;
import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class BuildWorldState {
    private final Map<BuildSlot, BuildPieceState> piecesBySlot = new HashMap<>();
    private final Map<BuildSlot, List<BlockOffset>> occupiedBlocksBySlot = new HashMap<>();
    private final Map<OccupiedBlockKey, Set<BuildSlot>> slotsByOccupiedBlock = new HashMap<>();

    public BuildPieceState get(BuildSlot slot) {
        return piecesBySlot.get(Objects.requireNonNull(slot, "slot"));
    }

    public boolean contains(BuildSlot slot) {
        return piecesBySlot.containsKey(Objects.requireNonNull(slot, "slot"));
    }

    public boolean addIfAbsent(BuildPieceState piece) {
        Objects.requireNonNull(piece, "piece");
        return piecesBySlot.putIfAbsent(piece.slot(), piece) == null;
    }

    public boolean addIfNotConflicting(BuildPieceState piece) {
        return addIfNotConflicting(piece, List.of());
    }

    public boolean addIfNotConflicting(BuildPieceState piece, Collection<BlockOffset> occupiedBlocks) {
        Objects.requireNonNull(piece, "piece");
        Objects.requireNonNull(occupiedBlocks, "occupiedBlocks");
        if (conflicts(piece.slot(), occupiedBlocks)) {
            return false;
        }
        piecesBySlot.put(piece.slot(), piece);
        trackOccupiedBlocks(piece.slot(), occupiedBlocks);
        return true;
    }

    public BuildPieceState remove(BuildSlot slot) {
        Objects.requireNonNull(slot, "slot");
        untrackOccupiedBlocks(slot);
        return piecesBySlot.remove(slot);
    }

    public boolean replace(BuildPieceState piece) {
        Objects.requireNonNull(piece, "piece");
        if (!piecesBySlot.containsKey(piece.slot())) {
            return false;
        }
        piecesBySlot.put(piece.slot(), piece);
        return true;
    }

    public boolean replaceIfCurrent(BuildSlot slot, UUID expectedId, BuildPieceState replacement) {
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(expectedId, "expectedId");
        Objects.requireNonNull(replacement, "replacement");
        if (!slot.equals(replacement.slot())) {
            throw new IllegalArgumentException("replacement slot must match target slot");
        }
        BuildPieceState current = piecesBySlot.get(slot);
        if (current == null || !current.id().equals(expectedId)) {
            return false;
        }
        piecesBySlot.put(slot, replacement);
        return true;
    }

    public List<BuildPieceState> progressConstruction(long tick) {
        ArrayList<BuildPieceState> changed = new ArrayList<>();
        for (Map.Entry<BuildSlot, BuildPieceState> entry : piecesBySlot.entrySet()) {
            BuildPieceState before = entry.getValue();
            BuildPieceState after = before.progressedTo(tick);
            if (!after.equals(before)) {
                entry.setValue(after);
                changed.add(after);
            }
        }
        return List.copyOf(changed);
    }

    public DamageResult damage(BuildSlot slot, int damage, long tick) {
        Objects.requireNonNull(slot, "slot");
        BuildPieceState before = piecesBySlot.get(slot);
        if (before == null) {
            return DamageResult.missing();
        }
        BuildPieceState after = before.damagedBy(damage, tick);
        piecesBySlot.put(slot, after);
        return new DamageResult(before, after);
    }

    public boolean conflicts(BuildSlot slot) {
        Objects.requireNonNull(slot, "slot");
        if (piecesBySlot.containsKey(slot)) {
            return true;
        }
        if (slot.pieceType() != PieceType.STAIR) {
            return false;
        }
        for (BuildSlot existing : piecesBySlot.keySet()) {
            if (existing.pieceType() == PieceType.STAIR && existing.gridPos().equals(slot.gridPos())) {
                return true;
            }
        }
        return false;
    }

    public boolean conflicts(BuildSlot slot, Collection<BlockOffset> occupiedBlocks) {
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(occupiedBlocks, "occupiedBlocks");
        if (conflicts(slot)) {
            return true;
        }
        String dimension = slot.gridPos().dimension();
        for (BlockOffset block : occupiedBlocks) {
            Set<BuildSlot> existingSlots = slotsByOccupiedBlock.get(new OccupiedBlockKey(dimension, block));
            if (existingSlots == null) {
                continue;
            }
            for (BuildSlot existing : existingSlots) {
                if (!intendedModelPermitsFootprintOverlap(existing, slot)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void trackOccupiedBlocks(BuildSlot slot, Collection<BlockOffset> occupiedBlocks) {
        List<BlockOffset> blocks = List.copyOf(occupiedBlocks);
        if (blocks.isEmpty()) {
            return;
        }
        occupiedBlocksBySlot.put(slot, blocks);
        String dimension = slot.gridPos().dimension();
        for (BlockOffset block : blocks) {
            slotsByOccupiedBlock
                    .computeIfAbsent(new OccupiedBlockKey(dimension, block), ignored -> new HashSet<>())
                    .add(slot);
        }
    }

    private void untrackOccupiedBlocks(BuildSlot slot) {
        List<BlockOffset> blocks = occupiedBlocksBySlot.remove(slot);
        if (blocks == null) {
            return;
        }
        String dimension = slot.gridPos().dimension();
        for (BlockOffset block : blocks) {
            OccupiedBlockKey key = new OccupiedBlockKey(dimension, block);
            Set<BuildSlot> slots = slotsByOccupiedBlock.get(key);
            if (slots == null) {
                continue;
            }
            slots.remove(slot);
            if (slots.isEmpty()) {
                slotsByOccupiedBlock.remove(key);
            }
        }
    }

    private static boolean intendedModelPermitsFootprintOverlap(BuildSlot existing, BuildSlot candidate) {
        return existing.pieceType() == candidate.pieceType();
    }

    public record DamageResult(BuildPieceState before, BuildPieceState after) {
        public static DamageResult missing() {
            return new DamageResult(null, null);
        }

        public boolean hit() {
            return after != null;
        }

        public boolean destroyed() {
            return after != null && after.destroyed();
        }
    }

    private record OccupiedBlockKey(String dimension, BlockOffset block) {
        private OccupiedBlockKey {
            Objects.requireNonNull(dimension, "dimension");
            Objects.requireNonNull(block, "block");
        }
    }

    public int size() {
        return piecesBySlot.size();
    }

    public Collection<BuildPieceState> pieces() {
        return List.copyOf(piecesBySlot.values());
    }
}
