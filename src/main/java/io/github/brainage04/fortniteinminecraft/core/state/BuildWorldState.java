package io.github.brainage04.fortniteinminecraft.core.state;

import io.github.brainage04.fortniteinminecraft.core.placement.BuildSupportCascade;
import io.github.brainage04.fortniteinminecraft.core.model.BlockOffset;
import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
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
    private final Map<BuildSlot, ScheduledCollapse> scheduledCollapsesBySlot = new HashMap<>();

    public BuildPieceState get(BuildSlot slot) {
        return piecesBySlot.get(Objects.requireNonNull(slot, "slot"));
    }

    public boolean contains(BuildSlot slot) {
        return piecesBySlot.containsKey(Objects.requireNonNull(slot, "slot"));
    }

    public boolean addIfAbsent(BuildPieceState piece) {
        Objects.requireNonNull(piece, "piece");
        if (piecesBySlot.putIfAbsent(piece.slot(), piece) != null) {
            return false;
        }
        scheduledCollapsesBySlot.remove(piece.slot());
        return true;
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
        scheduledCollapsesBySlot.remove(piece.slot());
        piecesBySlot.put(piece.slot(), piece);
        trackOccupiedBlocks(piece.slot(), occupiedBlocks);
        return true;
    }

    public BuildPieceState remove(BuildSlot slot) {
        Objects.requireNonNull(slot, "slot");
        scheduledCollapsesBySlot.remove(slot);
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
        return progressConstructionInDimension(null, tick);
    }

    public List<BuildPieceState> progressConstruction(String dimension, long tick) {
        Objects.requireNonNull(dimension, "dimension");
        return progressConstructionInDimension(dimension, tick);
    }

    private List<BuildPieceState> progressConstructionInDimension(String dimension, long tick) {
        ArrayList<BuildPieceState> changed = new ArrayList<>();
        for (Map.Entry<BuildSlot, BuildPieceState> entry : piecesBySlot.entrySet()) {
            BuildPieceState before = entry.getValue();
            if (dimension != null && !dimension.equals(before.slot().gridPos().dimension())) {
                continue;
            }
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

    public int scheduleCollapse(Collection<BuildSupportCascade.CollapseStep> steps, long startTick) {
        Objects.requireNonNull(steps, "steps");
        int scheduled = 0;
        for (BuildSupportCascade.CollapseStep step : steps) {
            BuildPieceState piece = step.piece();
            BuildSlot slot = piece.slot();
            BuildPieceState current = piecesBySlot.get(slot);
            if (current == null || !current.id().equals(piece.id())) {
                continue;
            }
            long dueTick = startTick + Math.max(0, step.delayTicks());
            ScheduledCollapse existing = scheduledCollapsesBySlot.get(slot);
            if (existing != null && existing.dueTick() <= dueTick) {
                continue;
            }
            scheduledCollapsesBySlot.put(slot, new ScheduledCollapse(slot, piece.id(), dueTick, step.distance()));
            scheduled++;
        }
        return scheduled;
    }

    public List<BuildPieceState> drainDueCollapses(String dimension, long tick) {
        return drainDueCollapses(dimension, tick, null);
    }

    public List<BuildPieceState> drainDueCollapses(
            String dimension,
            long tick,
            Collection<BuildSlot> stillUnsupportedSlots
    ) {
        Objects.requireNonNull(dimension, "dimension");
        Set<BuildSlot> unsupportedFilter = stillUnsupportedSlots == null ? null : Set.copyOf(stillUnsupportedSlots);
        ArrayList<ScheduledCollapse> due = new ArrayList<>();
        for (ScheduledCollapse collapse : List.copyOf(scheduledCollapsesBySlot.values())) {
            if (!dimension.equals(collapse.slot().gridPos().dimension())) {
                continue;
            }
            BuildPieceState current = piecesBySlot.get(collapse.slot());
            if (current == null || !current.id().equals(collapse.pieceId())) {
                scheduledCollapsesBySlot.remove(collapse.slot());
                continue;
            }
            if (collapse.dueTick() <= tick) {
                scheduledCollapsesBySlot.remove(collapse.slot());
                if (unsupportedFilter == null || unsupportedFilter.contains(collapse.slot())) {
                    due.add(collapse);
                }
            }
        }
        due.sort(scheduledCollapseOrder());

        ArrayList<BuildPieceState> pieces = new ArrayList<>(due.size());
        for (ScheduledCollapse collapse : due) {
            BuildPieceState current = piecesBySlot.get(collapse.slot());
            if (current != null && current.id().equals(collapse.pieceId())) {
                pieces.add(current);
            }
        }
        return List.copyOf(pieces);
    }

    public int scheduledCollapseCount() {
        return scheduledCollapsesBySlot.size();
    }

    private static Comparator<ScheduledCollapse> scheduledCollapseOrder() {
        return Comparator
                .comparingLong(ScheduledCollapse::dueTick)
                .thenComparingInt(ScheduledCollapse::distance)
                .thenComparing(collapse -> collapse.slot().gridPos().dimension())
                .thenComparingInt(collapse -> collapse.slot().gridPos().y())
                .thenComparingInt(collapse -> collapse.slot().gridPos().x())
                .thenComparingInt(collapse -> collapse.slot().gridPos().z())
                .thenComparing(collapse -> collapse.slot().pieceType())
                .thenComparing(collapse -> collapse.slot().orientation());
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
        return existing.pieceType().permitsFootprintOverlapWith(candidate.pieceType());
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

    private record ScheduledCollapse(BuildSlot slot, UUID pieceId, long dueTick, int distance) {
        private ScheduledCollapse {
            Objects.requireNonNull(slot, "slot");
            Objects.requireNonNull(pieceId, "pieceId");
            if (distance < 0) {
                throw new IllegalArgumentException("distance cannot be negative");
            }
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
