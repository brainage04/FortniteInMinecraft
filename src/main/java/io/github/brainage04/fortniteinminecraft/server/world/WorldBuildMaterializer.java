package io.github.brainage04.fortniteinminecraft.server.world;

import io.github.brainage04.fortniteinminecraft.core.model.BlockOffset;
import io.github.brainage04.fortniteinminecraft.core.model.BuildGridPos;
import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.PieceFootprint;
import io.github.brainage04.fortniteinminecraft.core.placement.SnapGrid;
import io.github.brainage04.fortniteinminecraft.core.rules.BuildRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class WorldBuildMaterializer {
    private static final int BLOCK_UPDATE_FLAGS = Block.UPDATE_ALL;
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private final SnapGrid snapGrid;
    private final EnumMap<MaterialType, BlockState> blockStatesByMaterial;
    private final Map<BuildSlot, List<BlockPos>> placedBlocksBySlot = new HashMap<>();
    private final Map<BuildSlot, MaterialType> materialsBySlot = new HashMap<>();
    private final Map<WorldBlockKey, LinkedHashSet<BuildSlot>> ownersByBlock = new HashMap<>();

    public WorldBuildMaterializer(BuildRules rules, Map<MaterialType, BlockState> blockStatesByMaterial) {
        this.snapGrid = new SnapGrid(Objects.requireNonNull(rules, "rules"));
        Objects.requireNonNull(blockStatesByMaterial, "blockStatesByMaterial");
        this.blockStatesByMaterial = new EnumMap<>(MaterialType.class);
        for (MaterialType material : MaterialType.values()) {
            BlockState state = blockStatesByMaterial.get(material);
            if (state == null) {
                throw new IllegalArgumentException("missing block state for " + material);
            }
            this.blockStatesByMaterial.put(material, state);
        }
    }

    public static WorldBuildMaterializer defaults(BuildRules rules) {
        EnumMap<MaterialType, BlockState> states = new EnumMap<>(MaterialType.class);
        states.put(MaterialType.WOOD, Blocks.OAK_PLANKS.defaultBlockState());
        states.put(MaterialType.STONE, Blocks.COBBLESTONE.defaultBlockState());
        states.put(MaterialType.METAL, Blocks.COPPER_BLOCK.weathering().unaffected().defaultBlockState());
        return new WorldBuildMaterializer(rules, states);
    }

    public WorldBuildWriteResult place(ServerLevel level, BuildPieceState piece, PieceFootprint footprint) {
        Objects.requireNonNull(level, "level");
        return place(piece, footprint, (pos, state) -> level.setBlock(pos, state, BLOCK_UPDATE_FLAGS));
    }

    public WorldBuildWriteResult clear(ServerLevel level, BuildPieceState piece) {
        Objects.requireNonNull(level, "level");
        return clear(piece, (pos, state) -> level.setBlock(pos, state, BLOCK_UPDATE_FLAGS));
    }

    public List<BlockPos> blockPositions(PieceFootprint footprint) {
        Objects.requireNonNull(footprint, "footprint");
        BuildGridPos gridPos = footprint.slot().gridPos();
        BlockOffset origin = snapGrid.blockOrigin(gridPos);
        ArrayList<BlockPos> positions = new ArrayList<>(footprint.localBlocks().size());
        for (BlockOffset local : footprint.localBlocks()) {
            positions.add(new BlockPos(origin.x() + local.x(), origin.y() + local.y(), origin.z() + local.z()));
        }
        return List.copyOf(positions);
    }

    public BlockState blockStateFor(MaterialType material) {
        return blockStatesByMaterial.get(Objects.requireNonNull(material, "material"));
    }

    public int trackedBlockCount(BuildSlot slot) {
        List<BlockPos> positions = placedBlocksBySlot.get(Objects.requireNonNull(slot, "slot"));
        return positions == null ? 0 : positions.size();
    }

    public List<BlockPos> trackedBlockPositions(BuildSlot slot) {
        List<BlockPos> positions = placedBlocksBySlot.get(Objects.requireNonNull(slot, "slot"));
        return positions == null ? List.of() : List.copyOf(positions);
    }

    public int ownedBlockCount(String dimension, BlockPos pos) {
        Set<BuildSlot> owners = ownersByBlock.get(new WorldBlockKey(dimension, pos));
        return owners == null ? 0 : owners.size();
    }

    public boolean isTrackedBlock(String dimension, int x, int y, int z) {
        return ownersByBlock.containsKey(new WorldBlockKey(dimension, new BlockPos(x, y, z)));
    }

    public BuildSlot topOwnerAt(String dimension, BlockPos pos) {
        LinkedHashSet<BuildSlot> owners = ownersByBlock.get(new WorldBlockKey(
                Objects.requireNonNull(dimension, "dimension"),
                Objects.requireNonNull(pos, "pos")
        ));
        return owners == null || owners.isEmpty() ? null : lastOwner(owners);
    }

    WorldBuildWriteResult place(BuildPieceState piece, PieceFootprint footprint, BlockWriter writer) {
        Objects.requireNonNull(piece, "piece");
        Objects.requireNonNull(footprint, "footprint");
        Objects.requireNonNull(writer, "writer");
        requireMatchingSlot(piece.slot(), footprint.slot());
        if (placedBlocksBySlot.containsKey(piece.slot())) {
            return WorldBuildWriteResult.failure(0, "build slot already has tracked world blocks");
        }

        BuildSlot slot = piece.slot();
        BlockState blockState = blockStateFor(piece.material());
        String dimension = slot.gridPos().dimension();
        List<BlockPos> positions = blockPositions(footprint);
        ArrayList<BlockRestore> rewrites = new ArrayList<>(positions.size());
        ArrayList<OwnershipSnapshot> ownershipSnapshots = new ArrayList<>(positions.size());
        materialsBySlot.put(slot, piece.material());

        for (BlockPos pos : positions) {
            WorldBlockKey key = new WorldBlockKey(dimension, pos);
            OwnershipSnapshot snapshot = snapshot(key);
            ownershipSnapshots.add(snapshot);
            BlockState previousState = visibleState(snapshot.owners());
            if (!previousState.equals(blockState)) {
                if (!writer.setBlock(pos, blockState)) {
                    rollback(rewrites, writer);
                    restoreOwnership(ownershipSnapshots);
                    materialsBySlot.remove(slot);
                    return WorldBuildWriteResult.failure(rewrites.size(), "world write failed at " + describe(pos));
                }
                rewrites.add(new BlockRestore(pos, previousState));
            }
            ownersByBlock.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(slot);
        }

        placedBlocksBySlot.put(slot, List.copyOf(positions));
        return WorldBuildWriteResult.success(rewrites.size(), "world blocks placed");
    }

    WorldBuildWriteResult clear(BuildPieceState piece, BlockWriter writer) {
        Objects.requireNonNull(piece, "piece");
        Objects.requireNonNull(writer, "writer");
        BuildSlot slot = piece.slot();
        List<BlockPos> positions = placedBlocksBySlot.get(slot);
        if (positions == null) {
            return WorldBuildWriteResult.failure(0, "build slot has no tracked world blocks");
        }

        String dimension = slot.gridPos().dimension();
        ArrayList<BlockRestore> rewrites = new ArrayList<>(positions.size());
        ArrayList<OwnershipSnapshot> ownershipSnapshots = new ArrayList<>(positions.size());
        for (BlockPos pos : positions) {
            WorldBlockKey key = new WorldBlockKey(dimension, pos);
            OwnershipSnapshot snapshot = snapshot(key);
            LinkedHashSet<BuildSlot> owners = ownersByBlock.get(key);
            if (owners == null || !owners.contains(slot)) {
                rollback(rewrites, writer);
                restoreOwnership(ownershipSnapshots);
                return WorldBuildWriteResult.failure(rewrites.size(), "tracked ownership missing at " + describe(pos));
            }

            ownershipSnapshots.add(snapshot);
            boolean visibleOwner = slot.equals(lastOwner(owners));
            owners.remove(slot);
            if (owners.isEmpty()) {
                ownersByBlock.remove(key);
            }

            if (visibleOwner) {
                BlockState nextState = owners.isEmpty() ? AIR : blockStateForOwner(lastOwner(owners));
                BlockState previousState = blockStateFor(piece.material());
                if (!writer.setBlock(pos, nextState)) {
                    rollback(rewrites, writer);
                    restoreOwnership(ownershipSnapshots);
                    return WorldBuildWriteResult.failure(rewrites.size(), "world clear failed at " + describe(pos));
                }
                rewrites.add(new BlockRestore(pos, previousState));
            }
        }

        placedBlocksBySlot.remove(slot);
        materialsBySlot.remove(slot);
        return WorldBuildWriteResult.success(rewrites.size(), "world blocks cleared");
    }

    private OwnershipSnapshot snapshot(WorldBlockKey key) {
        LinkedHashSet<BuildSlot> owners = ownersByBlock.get(key);
        return new OwnershipSnapshot(key, owners == null ? new LinkedHashSet<>() : new LinkedHashSet<>(owners));
    }

    private void restoreOwnership(List<OwnershipSnapshot> snapshots) {
        for (int i = snapshots.size() - 1; i >= 0; i--) {
            OwnershipSnapshot snapshot = snapshots.get(i);
            if (snapshot.owners().isEmpty()) {
                ownersByBlock.remove(snapshot.key());
            } else {
                ownersByBlock.put(snapshot.key(), new LinkedHashSet<>(snapshot.owners()));
            }
        }
    }

    private BlockState visibleState(LinkedHashSet<BuildSlot> owners) {
        return owners.isEmpty() ? AIR : blockStateForOwner(lastOwner(owners));
    }

    private BlockState blockStateForOwner(BuildSlot slot) {
        MaterialType material = materialsBySlot.get(Objects.requireNonNull(slot, "slot"));
        if (material == null) {
            throw new IllegalStateException("missing material for tracked build slot");
        }
        return blockStateFor(material);
    }

    private static BuildSlot lastOwner(LinkedHashSet<BuildSlot> owners) {
        BuildSlot last = null;
        for (BuildSlot owner : owners) {
            last = owner;
        }
        return Objects.requireNonNull(last, "owners cannot be empty");
    }

    private static void rollback(List<BlockRestore> rewrites, BlockWriter writer) {
        for (int i = rewrites.size() - 1; i >= 0; i--) {
            BlockRestore restore = rewrites.get(i);
            writer.setBlock(restore.pos(), restore.previousState());
        }
    }

    private static void requireMatchingSlot(BuildSlot pieceSlot, BuildSlot footprintSlot) {
        if (!pieceSlot.equals(footprintSlot)) {
            throw new IllegalArgumentException("piece and footprint slots must match");
        }
    }

    private static String describe(BlockPos pos) {
        return "[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]";
    }

    @FunctionalInterface
    interface BlockWriter {
        boolean setBlock(BlockPos pos, BlockState state);
    }

    private record WorldBlockKey(String dimension, BlockPos pos) {
        private WorldBlockKey {
            Objects.requireNonNull(dimension, "dimension");
            Objects.requireNonNull(pos, "pos");
        }
    }

    private record OwnershipSnapshot(WorldBlockKey key, LinkedHashSet<BuildSlot> owners) {
    }

    private record BlockRestore(BlockPos pos, BlockState previousState) {
    }
}
