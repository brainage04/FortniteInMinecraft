package io.github.brainage04.fortniteinminecraft.core.placement;

import io.github.brainage04.fortniteinminecraft.core.model.BlockOffset;
import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.PieceFootprint;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import io.github.brainage04.fortniteinminecraft.core.state.BuildWorldState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class BuildSupportCascade {
  public static final int COLLAPSE_INITIAL_DELAY_TICKS = 4;
  public static final int COLLAPSE_DISTANCE_DELAY_TICKS = 3;
  private static final int[][] FACE_NEIGHBORS = {
      { 1, 0, 0 },
      { -1, 0, 0 },
      { 0, 1, 0 },
      { 0, -1, 0 },
      { 0, 0, 1 },
      { 0, 0, -1 }
  };
  private static final Comparator<BuildSlot> SLOT_ORDER = Comparator
      .comparing((BuildSlot slot) -> slot.gridPos().dimension())
      .thenComparingInt(slot -> slot.gridPos().y())
      .thenComparingInt(slot -> slot.gridPos().x())
      .thenComparingInt(slot -> slot.gridPos().z())
      .thenComparing(BuildSlot::pieceType)
      .thenComparing(BuildSlot::orientation);
  private final FootprintProjector footprints;
  private final SnapGrid snapGrid;
  public BuildSupportCascade(BuildRules rules) {
    Objects.requireNonNull(rules, "rules");
    this.footprints = new FootprintProjector(rules);
    this.snapGrid = new SnapGrid(rules);
  }
  public List<BuildPieceState> unsupportedPieces(BuildWorldState state, String dimension,
      WorldObstruction obstruction) {
    Objects.requireNonNull(state, "state");
    Objects.requireNonNull(dimension, "dimension");
    Objects.requireNonNull(obstruction, "obstruction");
    UnsupportedSnapshot snapshot = unsupportedSnapshot(state, dimension, obstruction);
    ArrayList<BuildPieceState> unsupported = new ArrayList<>(snapshot.unsupportedSlots().size());
    for (BuildSlot slot : snapshot.unsupportedSlots()) {
      unsupported.add(snapshot.pieces().get(slot).piece());
    }
    return List.copyOf(unsupported);
  }
  public List<CollapseStep> collapsePlan(
      BuildWorldState state,
      String dimension,
      WorldObstruction obstruction,
      BuildSlot removedSupport) {
    Objects.requireNonNull(state, "state");
    Objects.requireNonNull(dimension, "dimension");
    Objects.requireNonNull(obstruction, "obstruction");
    Objects.requireNonNull(removedSupport, "removedSupport");
    UnsupportedSnapshot snapshot = unsupportedSnapshot(state, dimension, obstruction);
    if (snapshot.unsupportedSlots().isEmpty()) {
      return List.of();
    }
    Map<BuildSlot, Integer> distances = collapseDistances(snapshot, removedSupport);
    ArrayList<CollapseStep> steps = new ArrayList<>(snapshot.unsupportedSlots().size());
    for (BuildSlot slot : snapshot.unsupportedSlots()) {
      int distance = distances.getOrDefault(slot, gridDistance(slot, removedSupport));
      steps.add(new CollapseStep(
          snapshot.pieces().get(slot).piece(),
          distance,
          collapseDelayTicks(distance)));
    }
    steps.sort(Comparator
        .comparingInt(CollapseStep::distance)
        .thenComparing((CollapseStep step) -> step.piece().slot(), SLOT_ORDER));
    return List.copyOf(steps);
  }
  private UnsupportedSnapshot unsupportedSnapshot(BuildWorldState state, String dimension,
      WorldObstruction obstruction) {
    Map<BuildSlot, PieceBlocks> pieces = piecesInDimension(state, dimension);
    Map<BlockOffset, LinkedHashSet<BuildSlot>> slotsByBlock = slotsByBlock(pieces);
    Map<BuildSlot, LinkedHashSet<BuildSlot>> adjacency = adjacency(pieces, slotsByBlock);
    LinkedHashSet<BuildSlot> supported = supportedSlots(pieces, slotsByBlock, adjacency, dimension, obstruction);
    LinkedHashSet<BuildSlot> unsupportedSlots = new LinkedHashSet<>();
    for (BuildSlot slot : pieces.keySet()) {
      if (!supported.contains(slot)) {
        unsupportedSlots.add(slot);
      }
    }
    return new UnsupportedSnapshot(pieces, slotsByBlock, adjacency, unsupportedSlots);
  }
  private Map<BuildSlot, PieceBlocks> piecesInDimension(BuildWorldState state, String dimension) {
    ArrayList<BuildPieceState> dimensionPieces = new ArrayList<>();
    for (BuildPieceState piece : state.pieces()) {
      if (dimension.equals(piece.slot().gridPos().dimension())) {
        dimensionPieces.add(piece);
      }
    }
    dimensionPieces.sort(Comparator.comparing(BuildPieceState::slot, SLOT_ORDER));
    LinkedHashMap<BuildSlot, PieceBlocks> result = new LinkedHashMap<>();
    for (BuildPieceState piece : dimensionPieces) {
      BuildSlot slot = piece.slot();
      PieceFootprint footprint = footprints.project(piece);
      BlockOffset origin = snapGrid.blockOrigin(slot.gridPos());
      result.put(slot, new PieceBlocks(piece, footprint.absoluteBlocks(origin)));
    }
    return result;
  }
  private Map<BuildSlot, Integer> collapseDistances(UnsupportedSnapshot snapshot, BuildSlot removedSupport) {
    HashMap<BuildSlot, Integer> result = new HashMap<>();
    ArrayDeque<BuildSlot> queue = new ArrayDeque<>();
    HashSet<BlockOffset> removedBlocks = new HashSet<>(absoluteBlocks(removedSupport));
    for (BuildSlot slot : snapshot.unsupportedSlots()) {
      if (touchesAny(snapshot.pieces().get(slot).blocks(), removedBlocks)) {
        result.put(slot, 0);
        queue.add(slot);
      }
    }
    while (!queue.isEmpty()) {
      BuildSlot current = queue.removeFirst();
      int nextDistance = result.get(current) + 1;
      for (BuildSlot next : snapshot.adjacency().getOrDefault(current, new LinkedHashSet<>())) {
        if (!snapshot.unsupportedSlots().contains(next) || result.containsKey(next)) {
          continue;
        }
        result.put(next, nextDistance);
        queue.add(next);
      }
    }
    return result;
  }
  private List<BlockOffset> absoluteBlocks(BuildSlot slot) {
    PieceFootprint footprint = footprints.project(slot);
    return footprint.absoluteBlocks(snapGrid.blockOrigin(slot.gridPos()));
  }
  private static int collapseDelayTicks(int distance) {
    return COLLAPSE_INITIAL_DELAY_TICKS + Math.max(0, distance) * COLLAPSE_DISTANCE_DELAY_TICKS;
  }
  private static int gridDistance(BuildSlot slot, BuildSlot removedSupport) {
    return Math.abs(slot.gridPos().x() - removedSupport.gridPos().x())
        + Math.abs(slot.gridPos().y() - removedSupport.gridPos().y())
        + Math.abs(slot.gridPos().z() - removedSupport.gridPos().z());
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
      Map<BlockOffset, LinkedHashSet<BuildSlot>> slotsByBlock) {
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
      WorldObstruction obstruction) {
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
      WorldObstruction obstruction) {
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
  private static boolean touchesAny(List<BlockOffset> blocks, Set<BlockOffset> supports) {
    for (BlockOffset block : blocks) {
      if (supports.contains(block)) {
        return true;
      }
      for (BlockOffset neighbor : neighbors(block)) {
        if (supports.contains(neighbor)) {
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
      Map<BuildSlot, LinkedHashSet<BuildSlot>> adjacency) {
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
  public record CollapseStep(BuildPieceState piece, int distance, int delayTicks) {
    public CollapseStep {
      Objects.requireNonNull(piece, "piece");
      if (distance < 0) {
        throw new IllegalArgumentException("distance cannot be negative");
      }
      if (delayTicks < 0) {
        throw new IllegalArgumentException("delayTicks cannot be negative");
      }
    }
  }
  private record UnsupportedSnapshot(
      Map<BuildSlot, PieceBlocks> pieces,
      Map<BlockOffset, LinkedHashSet<BuildSlot>> slotsByBlock,
      Map<BuildSlot, LinkedHashSet<BuildSlot>> adjacency,
      LinkedHashSet<BuildSlot> unsupportedSlots) {
    private UnsupportedSnapshot {
      pieces = Map.copyOf(pieces);
      slotsByBlock = Map.copyOf(slotsByBlock);
      adjacency = Map.copyOf(adjacency);
      unsupportedSlots = new LinkedHashSet<>(unsupportedSlots);
    }
  }
  private record PieceBlocks(BuildPieceState piece, List<BlockOffset> blocks) {
    private PieceBlocks {
      Objects.requireNonNull(piece, "piece");
      blocks = List.copyOf(blocks);
    }
  }
}
